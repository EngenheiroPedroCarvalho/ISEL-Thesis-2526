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
 * P19 - Internal function resolution cost vs. PARALLEL BRANCH WIDTH, the structural axis P12
 * measured only for rendering (and only for the Parallel/Choice renderers, external-only
 * workflows).
 *
 * There is no Choice-width analogue for resolution: a ConditionalContext only carries
 * Condition/target-name pairs, never nested CALL steps, so internal function resolution has
 * nothing to recurse into there - Choice stays a rendering-only axis for this contribution.
 * Parallel, however, DOES nest real Step lists per branch, and
 * [WorkflowInternalCallEndpointResolver]'s `resolveContext` recurses into ParallelBranchContext
 * explicitly - a path never exercised by P3/P6-P17's flat workflows. Mirrors
 * [WorkflowGenerator.withParallelBranchWidth] (a single Parallel block, [branchWidth] swept,
 * FIXED_LEAVES_PER_BRANCH leaves per branch), but the leaves are INTERNAL calls (round-robin over
 * FIXED_FUNCTIONS distinct functions, R=F, as in P8/P15) instead of independent external calls.
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
open class BenchmarkInternalResolutionByBranchWidth {

    /** Number of branches in the single Parallel step. */
    @Param("1", "2", "5", "10", "20", "50", "100")
    var branchWidth: Int = 0

    private lateinit var registryFile: Path
    private lateinit var resolver: WorkflowInternalCallEndpointResolver
    private lateinit var workflow: Workflow

    private val internalCallExtractor: (CallContext) -> String? =
        { call -> call.internalFunction?.name }

    @Setup(Level.Trial)
    fun setupWorkflow() {
        registryFile = Files.createTempFile("omniflow-bench-p19-registry", ".json")
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

        workflow = WorkflowGenerator.withParallelBranchWidthInternalCalls(
            branchWidth, FIXED_LEAVES_PER_BRANCH, FIXED_FUNCTIONS, BASE
        )
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    /** Optimized resolution (single registry read, then per-call lookups) of a wide-Parallel workflow. */
    @Benchmark
    fun resolveOptimized(blackhole: Blackhole) {
        blackhole.consume(resolver.resolve(workflow, internalCallExtractor))
    }

    companion object {
        private const val FIXED_LEAVES_PER_BRANCH = 5
        private const val FIXED_FUNCTIONS = 10
        private const val BASE = "benchFn"
    }
}
