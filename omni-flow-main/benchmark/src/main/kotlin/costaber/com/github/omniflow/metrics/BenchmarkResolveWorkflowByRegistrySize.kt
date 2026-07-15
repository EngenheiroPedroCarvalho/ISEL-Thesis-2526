package costaber.com.github.omniflow.metrics

import costaber.com.github.omniflow.generator.WorkflowGenerator
import costaber.com.github.omniflow.model.CallContext
import costaber.com.github.omniflow.model.Workflow
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import costaber.com.github.omniflow.registry.WorkflowInternalCallEndpointResolver
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
 * P9 - The complementary axis of P8 ([BenchmarkResolveWorkflowByFunctionsAndCalls]):
 * isolate the effect of the REGISTRY SIZE (R) on resolution, holding the workflow
 * fixed. The workflow always makes [FIXED_CALLS] calls over [FIXED_FUNCTIONS]
 * distinct functions (N and F fixed); only the registry is padded to R entries
 * (R >= [FIXED_FUNCTIONS]), so the workflow references just the first F of them
 * while R-F extra entries inflate every registry read.
 *
 * Same two strategies as P8, resolution only (no render, which the read
 * optimization does not affect):
 *  - [resolveNaive]     re-reads the whole R-entry registry file per call -> O(N*R)
 *  - [resolveOptimized] reads the R-entry registry once, then N lookups    -> O(N+R)
 *
 * Both paths produce an IDENTICAL resolved workflow, so the measured gap is purely
 * the registry-read overhead. Pure local file I/O - no AWS/GCP SDK, no network.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkResolveWorkflowByRegistrySize {

    /** Number of functions registered in the registry file (R); always >= FIXED_FUNCTIONS. */
    @Param("10", "20", "50", "100", "200", "1000")
    var r: Int = 0

    private lateinit var registryFile: Path
    private lateinit var store: FunctionRegistryStore
    private lateinit var resolver: WorkflowInternalCallEndpointResolver
    private lateinit var workflow: Workflow

    private val internalCallExtractor: (CallContext) -> String? =
        { call -> call.internalFunction?.name }

    @Setup(Level.Trial)
    fun setupWorkflow() {
        registryFile = Files.createTempFile("omniflow-bench-p9-registry", ".json")
        store = FunctionRegistryStore(registryFile)

        // Registry padded to R entries; the workflow references only the first F.
        val functions = (0 until r).associate { idx ->
            val name = "$BASE$idx"
            name to FunctionInvocationMetadata(
                serviceName = name,
                url = "https://internal.example.com/$name"
            )
        }
        store.writeNew(functions)
        resolver = WorkflowInternalCallEndpointResolver(store)

        // FIXED_CALLS calls distributed round-robin over the first FIXED_FUNCTIONS functions.
        workflow = WorkflowGenerator.withDistinctInternalCalls(FIXED_CALLS, FIXED_FUNCTIONS, BASE)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    /** Naive resolution: one full registry read+parse per call -> O(N*R). */
    @Benchmark
    fun resolveNaive(blackhole: Blackhole) {
        blackhole.consume(NaiveEndpointResolver.resolve(workflow, store, internalCallExtractor))
    }

    /** Optimized resolution: one registry read, then N pure lookups -> O(N+R). */
    @Benchmark
    fun resolveOptimized(blackhole: Blackhole) {
        blackhole.consume(resolver.resolve(workflow, internalCallExtractor))
    }

    companion object {
        private const val FIXED_CALLS = 50
        private const val FIXED_FUNCTIONS = 10
        private const val BASE = "benchFn"
    }
}
