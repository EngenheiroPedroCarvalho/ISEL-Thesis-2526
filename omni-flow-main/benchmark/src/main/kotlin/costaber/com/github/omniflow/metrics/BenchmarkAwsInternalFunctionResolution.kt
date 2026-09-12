package costaber.com.github.omniflow.metrics

import costaber.com.github.omniflow.cloud.provider.amazon.service.LambdaFunctionInspector
import costaber.com.github.omniflow.generator.WorkflowGenerator
import costaber.com.github.omniflow.internalfunction.quickfaas.AwsInternalFunctionResolver
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
 * P10 - Cost of the REAL AWS auto-deploy resolver, [AwsInternalFunctionResolver.resolve], vs the
 * number of internal calls (N) and registry size (R=F), same grid as P8.
 *
 * This exercises the actual "unification" glue that decides whether an internal function should
 * be reused or deployed. It now carries the single-read optimization from
 * [OptimizedEndpointResolver] (the former P7-P9 reference implementation): `resolve()` reads the
 * registry once into an in-memory snapshot and every call's `resolveOrDeploy` looks up against
 * that snapshot (`registry.tryResolveEntryIn`) instead of re-reading the file, so per-call cost is
 * Θ(1) against the snapshot rather than Θ(R) - the same fix as P7-P9, now applied on the real
 * production path.
 *
 * The registry is pre-populated with exactly the F functions the workflow references (R=F, as in
 * P8), each with an ARN-shaped URL so `regionFromArn` resolves a region and the registry-hit path
 * validates against [FakeInspector] instead of the real [costaber.com.github.omniflow.cloud.provider.amazon.service.AwsLambdaFunctionInspector]/EC2
 * region listing default - pure local file I/O, no AWS SDK calls, no network.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkAwsInternalFunctionResolution {

    /** Number of DISTINCT internal functions the workflow calls (F); registry R = F. */
    @Param("1", "2", "5", "10", "20", "50")
    var f: Int = 0

    /** Number of internal call steps in the workflow (N). */
    @Param("1", "5", "10", "50", "200")
    var n: Int = 0

    private lateinit var registryFile: Path
    private lateinit var resolver: AwsInternalFunctionResolver
    private lateinit var workflow: Workflow

    @Setup(Level.Trial)
    fun setupWorkflow() {
        registryFile = Files.createTempFile("omniflow-bench-p10-registry", ".json")
        val store = FunctionRegistryStore(registryFile)

        // Registry holds exactly the F functions the workflow references (R = F) -> always a
        // registry hit. Entries use an ARN-shaped URL so `regionFromArn` resolves a region, and
        // FakeInspector confirms it locally, so resolveOrDeploy never reaches the AWS Lambda SDK.
        val functions = (0 until f).associate { idx ->
            val name = "$BASE$idx"
            name to FunctionInvocationMetadata(
                serviceName = name,
                url = "arn:aws:lambda:us-east-1:123456789012:function:$name"
            )
        }
        store.writeNew(functions)
        resolver = AwsInternalFunctionResolver(
            preferredRegion = "us-east-1",
            registry = store,
            inspector = FakeInspector(functions)
        )

        // N calls distributed round-robin over the f registered functions.
        workflow = WorkflowGenerator.withDistinctInternalCalls(n, f, BASE)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    @Benchmark
    fun resolveAwsInternal(blackhole: Blackhole) {
        blackhole.consume(resolver.resolve(workflow))
    }

    /** Local stand-in for the Lambda API: confirms every registered function with no drift. */
    private class FakeInspector(
        private val functions: Map<String, FunctionInvocationMetadata>
    ) : LambdaFunctionInspector {
        override fun lookup(region: String, functionName: String): LambdaFunctionInspector.LookupResult {
            val meta = functions.values.find { it.serviceName == functionName }
            return if (meta != null) LambdaFunctionInspector.LookupResult.Found(functionName, meta.url)
            else LambdaFunctionInspector.LookupResult.NotFound
        }
    }

    companion object {
        private const val BASE = "benchFn"
    }
}
