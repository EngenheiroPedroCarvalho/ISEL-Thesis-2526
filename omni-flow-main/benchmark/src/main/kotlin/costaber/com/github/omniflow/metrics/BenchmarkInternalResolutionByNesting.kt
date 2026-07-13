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
 * P18 - Internal function resolution cost vs. NESTING DEPTH, the structural axis P5 measured only
 * for rendering (external-only workflows, never touching the registry).
 *
 * [WorkflowInternalCallEndpointResolver.resolveContext] recurses explicitly into
 * IterationRangeContext/ParallelBranchContext/etc., but every prior resolution benchmark (P3,
 * P6-P17) only ever built FLAT call sequences - that recursive path was never exercised. Mirrors
 * [WorkflowGenerator.withNestedSteps]'s structure exactly (FIXED_LEAF_STEPS leaves, [depth]
 * levels alternating iteration/parallel wrappers), but the leaves are INTERNAL calls (round-robin
 * over FIXED_FUNCTIONS distinct functions, R=F, as in P8/P15) instead of independent external
 * calls. If resolution cost tracks total call count regardless of tree shape, all depths should
 * measure the same - confirming the recursive traversal adds no cost beyond the leaves it visits.
 * Only the ALREADY-OPTIMIZED resolver is measured (single registry read) - the naive-vs-optimized
 * comparison is already exhaustively established in P6/P7/P8/P9. Pure local file I/O - no AWS/GCP
 * SDK, no network.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkInternalResolutionByNesting {

    /** Number of container levels wrapping the fixed set of leaf internal-call steps. */
    @Param("0", "1", "2", "3", "4", "5")
    var depth: Int = 0

    private lateinit var registryFile: Path
    private lateinit var resolver: WorkflowInternalCallEndpointResolver
    private lateinit var workflow: Workflow

    private val internalCallExtractor: (CallContext) -> String? =
        { call -> call.internalFunction?.name }

    @Setup(Level.Trial)
    fun setupWorkflow() {
        registryFile = Files.createTempFile("omniflow-bench-p18-registry", ".json")
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

        workflow = WorkflowGenerator.withNestedInternalCalls(FIXED_LEAF_STEPS, depth, FIXED_FUNCTIONS, BASE)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    /** Optimized resolution (single registry read, then per-call lookups) of a nested workflow. */
    @Benchmark
    fun resolveOptimized(blackhole: Blackhole) {
        blackhole.consume(resolver.resolve(workflow, internalCallExtractor))
    }

    companion object {
        /** Fixed number of innermost leaf internal-call steps across all depths. */
        private const val FIXED_LEAF_STEPS = 20
        private const val FIXED_FUNCTIONS = 10
        private const val BASE = "benchFn"
    }
}
