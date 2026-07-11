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
 * P15 - Resolution cost of a workflow that MIXES internal and external calls, along its two new
 * axes: the number of internal calls ([i], I) and the number of external calls ([e], E). N = I+E.
 *
 * P1-P14 only ever benchmarked HOMOGENEOUS workflows: all-internal (P3, P6-P11, P13, P14) or
 * all-external (P1, P2, P4, P5) - no benchmark mixed both in the same workflow, even though
 * production workflows realistically do (auto-deployed internal functions alongside third-party
 * external calls). [WorkflowGenerator.withMixedCalls] interleaves I internal calls (round-robin
 * over [FIXED_FUNCTIONS] distinct functions, same scheme as P8/P9) with E external calls, evenly
 * spread rather than grouped.
 *
 * The registry holds exactly the [FIXED_FUNCTIONS] functions the internal calls reference (R=F,
 * the "right-sized" registry case, as in P8/P10/P11). Only the ALREADY-OPTIMIZED resolver (single
 * registry read, `WorkflowInternalCallEndpointResolver`) is measured here - the naive-vs-optimized
 * comparison is already exhaustively established in P6/P7/P8/P9; the point of P15 is to isolate
 * whether resolution cost tracks I (internal calls, the only ones that touch the registry) or the
 * full N, now that a single workflow contains both. Pure local file I/O - no AWS/GCP SDK, no
 * network.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkResolveWorkflowByInternalExternalMix {

    /** Number of internal call steps in the workflow (I). */
    @Param("0", "1", "5", "10", "50", "200")
    var i: Int = 0

    /** Number of external call steps in the workflow (E). */
    @Param("0", "1", "5", "10", "50", "200")
    var e: Int = 0

    private lateinit var registryFile: Path
    private lateinit var resolver: WorkflowInternalCallEndpointResolver
    private lateinit var workflow: Workflow

    private val internalCallExtractor: (CallContext) -> String? =
        { call -> call.internalFunction?.name }

    @Setup(Level.Trial)
    fun setupWorkflow() {
        registryFile = Files.createTempFile("omniflow-bench-p15-registry", ".json")
        val store = FunctionRegistryStore(registryFile)

        // Registry holds exactly the FIXED_FUNCTIONS functions the internal calls reference (R=F).
        val functions = (0 until FIXED_FUNCTIONS).associate { idx ->
            val name = "$BASE$idx"
            name to FunctionInvocationMetadata(
                serviceName = name,
                url = "https://internal.example.com/$name"
            )
        }
        store.writeNew(functions)
        resolver = WorkflowInternalCallEndpointResolver(store)

        // I internal calls (round-robin over FIXED_FUNCTIONS) interleaved with E external calls.
        workflow = WorkflowGenerator.withMixedCalls(i, e, FIXED_FUNCTIONS, BASE)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    /** Optimized resolution (single registry read, then per-call lookups) of a mixed I/E workflow. */
    @Benchmark
    fun resolveOptimized(blackhole: Blackhole) {
        blackhole.consume(resolver.resolve(workflow, internalCallExtractor))
    }

    companion object {
        private const val FIXED_FUNCTIONS = 10
        private const val BASE = "benchFn"
    }
}
