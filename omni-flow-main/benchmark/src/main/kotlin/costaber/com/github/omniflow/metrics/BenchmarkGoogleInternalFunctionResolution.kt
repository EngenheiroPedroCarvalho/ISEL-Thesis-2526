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
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * P11 - GCP twin of P10: cost of the REAL auto-deploy resolver,
 * [WorkflowInternalFunctionResolver.resolve], vs the number of internal calls (N) and registry
 * size (M=F), same grid as P8/P10.
 *
 * Registry entries use `.cloudfunctions.net` URLs (1st-gen Cloud Function) so that
 * `resolveOrDiscoverInternal`'s `isFirstGenCloudFunction` check short-circuits BEFORE any Cloud
 * Run REST call (`inspector.lookupByServiceName`) or region discovery (`regions`, a `by lazy`
 * that would call `locationsClient.listProjectLocations`) - pure local file I/O, no network.
 *
 * GOTCHA: both [CloudRunV2ServiceInspector] and [CloudRunLocationsV1RestClient] default-construct
 * a [GoogleAccessTokenProvider], whose OWN default argument calls
 * `GoogleCredentials.getApplicationDefault()` EAGERLY at construction time (not lazily when a
 * token is actually requested). So merely instantiating either class with no-arg defaults would
 * try to resolve real Application Default Credentials and fail/hang without
 * `gcloud auth application-default login` configured on the machine - even though, on this
 * registry-hit-only benchmark path, no token is ever actually used. Both are therefore
 * constructed here with an explicit [GoogleAccessTokenProvider] wrapping a fixed dummy
 * [AccessToken] (via [GoogleCredentials.create], which does no I/O), avoiding ADC entirely.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkGoogleInternalFunctionResolution {

    /** Number of DISTINCT internal functions the workflow calls (F); registry M = F. */
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
        registryFile = Files.createTempFile("omniflow-bench-p11-registry", ".json")
        val store = FunctionRegistryStore(registryFile)

        // Registry holds exactly the F functions the workflow references (M = F), with 1st-gen
        // Cloud Function URLs -> always a registry hit that short-circuits before Cloud Run/ADC.
        val functions = (0 until f).associate { idx ->
            val name = "$BASE$idx"
            name to FunctionInvocationMetadata(
                serviceName = name,
                url = "https://us-central1-bench.cloudfunctions.net/$name"
            )
        }
        store.writeNew(functions)

        val noAdcTokenProvider = GoogleAccessTokenProvider(
            credentials = GoogleCredentials.create(AccessToken("bench-noop-token", null))
        )
        resolver = WorkflowInternalFunctionResolver(
            projectId = "bench-project",
            preferredRegion = null,
            registry = store,
            inspector = CloudRunV2ServiceInspector(tokenProvider = noAdcTokenProvider),
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

    companion object {
        private const val BASE = "benchFn"
    }
}
