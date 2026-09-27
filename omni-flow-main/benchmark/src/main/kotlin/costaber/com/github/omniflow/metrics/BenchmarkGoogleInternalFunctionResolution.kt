package costaber.com.github.omniflow.metrics

import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import costaber.com.github.omniflow.cloud.provider.google.auth.GoogleAccessTokenProvider
import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunLocationsV1RestClient
import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunV2ServiceInspector
import costaber.com.github.omniflow.generator.WorkflowGenerator
import costaber.com.github.omniflow.internalfunction.WorkflowInternalFunctionResolver
import costaber.com.github.omniflow.model.Workflow
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.net.Authenticator
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSession

/**
 * T7 - GCP twin of T6: cost of the REAL auto-deploy resolver,
 * [WorkflowInternalFunctionResolver.resolve], vs the number of internal calls (N) and registry
 * size (R=F), same grid as T4/T6.
 *
 * Every registry hit is validated against Cloud Run (`inspector.lookupByServiceName`), once per
 * call. The benchmark keeps the real [CloudRunV2ServiceInspector] - token lookup, request
 * building, JSON parsing - and swaps only its [HttpClient] for [FakeCloudRunHttpClient], which
 * answers every `services.get` locally with the registered `run.app` URL (no drift, no
 * rediscovery). As in T6, this measures the local part of the cascade: no network.
 *
 * GOTCHA: both [CloudRunV2ServiceInspector] and [CloudRunLocationsV1RestClient] default-construct
 * a [GoogleAccessTokenProvider], whose OWN default argument calls
 * `GoogleCredentials.getApplicationDefault()` EAGERLY at construction time (not lazily when a
 * token is actually requested). So merely instantiating either class with no-arg defaults would
 * try to resolve real Application Default Credentials and fail/hang without
 * `gcloud auth application-default login` configured on the machine. Both are therefore
 * constructed here with an explicit [GoogleAccessTokenProvider] wrapping a fixed dummy
 * [AccessToken] with no expiry (via [GoogleCredentials.create], which does no I/O), avoiding ADC
 * entirely.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkGoogleInternalFunctionResolution {

    /** Number of DISTINCT internal functions the workflow calls (F); registry R = F. */
    @Param("1", "2", "5", "10", "20", "50")
    var f: Int = 0

    /** Number of internal call steps in the workflow (N). */
    @Param("1", "5", "10", "50", "200")
    var n: Int = 0

    private lateinit var registryFile: Path
    private lateinit var resolver: WorkflowInternalFunctionResolver
    private lateinit var workflow: Workflow

    @Setup(Level.Trial)
    fun setupWorkflow() {
        registryFile = Files.createTempFile("omniflow-bench-t7-registry", ".json")
        val store = FunctionRegistryStore(registryFile)

        // Registry holds exactly the F functions the workflow references (R = F) -> always a
        // registry hit, which FakeCloudRunHttpClient confirms with the same URL.
        val functions = (0 until f).associate { idx ->
            val name = "$BASE$idx"
            name to FunctionInvocationMetadata(
                serviceName = "projects/$PROJECT/locations/$REGION/services/$name",
                url = runAppUrl(name)
            )
        }
        store.writeNew(functions)

        val noAdcTokenProvider = GoogleAccessTokenProvider(
            credentials = GoogleCredentials.create(AccessToken("bench-noop-token", null))
        )
        resolver = WorkflowInternalFunctionResolver(
            projectId = PROJECT,
            preferredRegion = null,
            registry = store,
            inspector = CloudRunV2ServiceInspector(
                http = FakeCloudRunHttpClient(), tokenProvider = noAdcTokenProvider
            ),
            locationsClient = CloudRunLocationsV1RestClient(tokenProvider = noAdcTokenProvider)
        )

        // N calls distributed round-robin over the f registered functions.
        workflow = WorkflowGenerator.withDistinctInternalCalls(n, f, BASE)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    @Benchmark
    fun resolveGoogleInternal(blackhole: Blackhole) {
        blackhole.consume(resolver.resolve(workflow))
    }

    /**
     * Local stand-in for the Cloud Run Admin API: answers every `services.get` with 200 and the
     * service's `run.app` URL, as the live API does for a deployed function.
     */
    private class FakeCloudRunHttpClient : HttpClient() {
        override fun <T> send(request: HttpRequest, handler: HttpResponse.BodyHandler<T>): HttpResponse<T> {
            val name = request.uri().path.removePrefix("/v2/")
            val body = """{"name":"$name","uri":"${runAppUrl(name.substringAfterLast('/'))}"}"""
            @Suppress("UNCHECKED_CAST")
            return FakeResponse(request, body as T)
        }

        override fun <T> sendAsync(request: HttpRequest, handler: HttpResponse.BodyHandler<T>) =
            CompletableFuture.completedFuture(send(request, handler))

        override fun <T> sendAsync(
            request: HttpRequest,
            handler: HttpResponse.BodyHandler<T>,
            pushPromiseHandler: HttpResponse.PushPromiseHandler<T>?
        ) = sendAsync(request, handler)

        override fun cookieHandler(): Optional<CookieHandler> = Optional.empty()
        override fun connectTimeout(): Optional<Duration> = Optional.empty()
        override fun followRedirects(): Redirect = Redirect.NEVER
        override fun proxy(): Optional<ProxySelector> = Optional.empty()
        override fun sslContext(): SSLContext = SSLContext.getDefault()
        override fun sslParameters(): SSLParameters = SSLParameters()
        override fun authenticator(): Optional<Authenticator> = Optional.empty()
        override fun version(): Version = Version.HTTP_2
        override fun executor(): Optional<Executor> = Optional.empty()
    }

    private class FakeResponse<T>(private val request: HttpRequest, private val body: T) : HttpResponse<T> {
        override fun statusCode() = 200
        override fun request() = request
        override fun previousResponse(): Optional<HttpResponse<T>> = Optional.empty()
        override fun headers(): HttpHeaders = HttpHeaders.of(emptyMap()) { _, _ -> true }
        override fun body() = body
        override fun sslSession(): Optional<SSLSession> = Optional.empty()
        override fun uri(): URI = request.uri()
        override fun version() = HttpClient.Version.HTTP_2
    }

    companion object {
        private const val BASE = "benchFn"
        private const val PROJECT = "bench-project"
        private const val REGION = "europe-west1"

        private fun runAppUrl(name: String) = "https://$name-bench-ew.a.run.app"
    }
}
