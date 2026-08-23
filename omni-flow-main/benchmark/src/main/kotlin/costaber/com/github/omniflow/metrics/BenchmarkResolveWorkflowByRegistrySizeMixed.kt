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
 * P16 - The registry-size twin of P15, mirroring how P9 complements P8: isolate the effect of the
 * REGISTRY SIZE (R) on resolution, holding the MIXED I/E workflow fixed. The workflow always makes
 * [FIXED_INTERNAL] internal calls (round-robin over [FIXED_FUNCTIONS] distinct functions) and
 * [FIXED_EXTERNAL] external calls (I, E and F fixed, N = I+E = [BenchmarkResolveWorkflowByRegistrySize]'s
 * FIXED_CALLS=50); only the registry is padded to R entries (R >= FIXED_FUNCTIONS).
 *
 * Only the ALREADY-OPTIMIZED resolver is measured (single registry read) - confirms that the Θ(R)
 * dependency already established in P9 (all-internal workflow) holds unchanged when the same
 * workflow also contains external calls, i.e. R is independent of I/E just as it was independent
 * of F/N. Pure local file I/O - no AWS/GCP SDK, no network.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkResolveWorkflowByRegistrySizeMixed {

    /** Number of functions registered in the registry file (R); always >= FIXED_FUNCTIONS. */
    @Param("10", "20", "50", "100", "200", "1000")
    var r: Int = 0

    private lateinit var registryFile: Path
    private lateinit var store: FunctionRegistryStore
    private lateinit var workflow: Workflow

    private val internalCallExtractor: (CallContext) -> String? =
        { call -> call.internalFunction?.name }

    @Setup(Level.Trial)
    fun setupWorkflow() {
        registryFile = Files.createTempFile("omniflow-bench-p16-registry", ".json")
        store = FunctionRegistryStore(registryFile)

        // Registry padded to R entries; the workflow's internal calls reference only the first F.
        val functions = (0 until r).associate { idx ->
            val name = "$BASE$idx"
            name to FunctionInvocationMetadata(
                serviceName = name,
                url = "https://internal.example.com/$name"
            )
        }
        store.writeNew(functions)

        // FIXED_INTERNAL internal calls (round-robin over FIXED_FUNCTIONS) interleaved with
        // FIXED_EXTERNAL external calls.
        workflow = WorkflowGenerator.withMixedCalls(FIXED_INTERNAL, FIXED_EXTERNAL, FIXED_FUNCTIONS, BASE)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    /** Optimized resolution (single registry read, then per-call lookups) of a mixed I/E workflow. */
    @Benchmark
    fun resolveOptimized(blackhole: Blackhole) {
        blackhole.consume(OptimizedEndpointResolver.resolve(workflow, store, internalCallExtractor))
    }

    companion object {
        private const val FIXED_INTERNAL = 40
        private const val FIXED_EXTERNAL = 10
        private const val FIXED_FUNCTIONS = 10
        private const val BASE = "benchFn"
    }
}
