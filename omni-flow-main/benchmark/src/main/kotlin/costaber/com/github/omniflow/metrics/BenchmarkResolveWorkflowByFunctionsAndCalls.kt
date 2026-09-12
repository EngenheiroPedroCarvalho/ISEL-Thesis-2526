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
 * P8 - Resolution cost of a real workflow along its two intrinsic axes:
 * the number of DISTINCT internal functions the workflow calls ([f], F) and the
 * number of CALLS ([n], N). The N calls are distributed round-robin over the F
 * distinct functions, so F=4/N=6 gives F0,F1,F2,F3,F0,F1 (F0 2x, F1 2x, F2 1x,
 * F3 1x). The registry holds exactly those F functions (R = F): the realistic
 * case where the registry contains precisely the functions the workflow uses.
 * (The effect of an oversized registry, R independent of F, is P9's axis.)
 *
 * Each benchmark resolves every internal call's endpoint against the F-entry
 * registry, WITHOUT rendering - the read optimization only affects resolution,
 * so rendering would just be a shared constant baseline that shifts both curves:
 *  - [resolveNaive]     resolves with [NaiveEndpointResolver] (re-reads the whole
 *                       registry file per call, legacy `resolveUrl`)      -> O(N*R)
 *  - [resolveOptimized] resolves with [OptimizedEndpointResolver]
 *                       (reads once, `readAll` + `resolveUrlIn` per call)  -> O(N+R)
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
open class BenchmarkResolveWorkflowByFunctionsAndCalls {

    /** Number of DISTINCT internal functions the workflow calls (F); registry R = F. */
    @Param("1", "2", "5", "10", "20", "50")
    var f: Int = 0

    /** Number of internal call steps in the workflow (N). */
    @Param("1", "5", "10", "50", "200")
    var n: Int = 0

    private lateinit var registryFile: Path
    private lateinit var store: FunctionRegistryStore
    private lateinit var workflow: Workflow

    private val internalCallExtractor: (CallContext) -> String? =
        { call -> call.internalFunction?.name }

    @Setup(Level.Trial)
    fun setupWorkflow() {
        registryFile = Files.createTempFile("omniflow-bench-p8-registry", ".json")
        store = FunctionRegistryStore(registryFile)

        // Registry holds exactly the F functions the workflow references (R = F).
        val functions = (0 until f).associate { idx ->
            val name = "$BASE$idx"
            name to FunctionInvocationMetadata(
                serviceName = name,
                url = "https://internal.example.com/$name"
            )
        }
        store.writeNew(functions)

        // N calls distributed round-robin over the f registered functions.
        workflow = WorkflowGenerator.withDistinctInternalCalls(n, f, BASE)
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
        blackhole.consume(OptimizedEndpointResolver.resolve(workflow, store, internalCallExtractor))
    }

    companion object {
        private const val BASE = "benchFn"
    }
}
