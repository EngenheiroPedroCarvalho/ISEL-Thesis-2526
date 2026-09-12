package costaber.com.github.omniflow.metrics

import costaber.com.github.omniflow.generator.WorkflowGenerator
import costaber.com.github.omniflow.model.CallContext
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
 * P3 - Local internal-call endpoint resolution.
 *
 * Measures ONLY the cost of [OptimizedEndpointResolver.resolve]
 * (registry lookup + URL splitting + tree rebuild) - NO rendering, NO
 * deployment, NO AWS/GCP SDK or network. The AWS-specific resolver
 * (AwsInternalFunctionResolver) is intentionally NOT used here because it
 * would hit the Lambda API; the registry-based resolver is pure local logic.
 *
 * The function registry is pre-loaded once in @Setup into a tiny temp file
 * (local file I/O only). It is never written in the hot path. The all-internal
 * case resolves every call against this in-memory-backed registry; the
 * all-external case touches the registry zero times (baseline).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkInternalCallResolution {

    /** Number of call steps in the workflow. */
    @Param("1", "2", "5", "10", "20", "50", "100", "200")
    var n: Int = 0

    private lateinit var registryFile: Path
    private lateinit var store: FunctionRegistryStore

    private lateinit var internalWorkflow: Workflow
    private lateinit var externalWorkflow: Workflow

    /** Returns the internal function name for a call, or null when external. */
    private val internalCallExtractor: (CallContext) -> String? =
        { call -> call.internalFunction?.name }

    @Setup(Level.Trial)
    fun setup() {
        // Pre-load the registry once into a small local file. The resolver
        // reads this single-entry registry during resolution; we never write
        // to it in the hot path.
        registryFile = Files.createTempFile("omniflow-bench-registry", ".json")
        store = FunctionRegistryStore(registryFile)
        store.writeNew(
            mapOf(
                FUNCTION_NAME to FunctionInvocationMetadata(
                    serviceName = FUNCTION_NAME,
                    url = "https://internal.example.com/$FUNCTION_NAME"
                )
            )
        )

        internalWorkflow = WorkflowGenerator.withInternalCalls(n, FUNCTION_NAME)
        externalWorkflow = WorkflowGenerator.withExternalCalls(n)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    @Benchmark
    fun resolveAllInternal(blackhole: Blackhole) {
        val resolved = OptimizedEndpointResolver.resolve(internalWorkflow, store, internalCallExtractor)
        blackhole.consume(resolved)
    }

    @Benchmark
    fun resolveAllExternal(blackhole: Blackhole) {
        val resolved = OptimizedEndpointResolver.resolve(externalWorkflow, store, internalCallExtractor)
        blackhole.consume(resolved)
    }

    companion object {
        private const val FUNCTION_NAME = "benchFunction"
    }
}
