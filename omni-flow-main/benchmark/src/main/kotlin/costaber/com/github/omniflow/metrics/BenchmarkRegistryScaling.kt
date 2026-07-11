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
 * P6 - Registry-size scaling of local endpoint resolution.
 *
 * [FunctionRegistryStore.resolveUrl] has no in-memory cache: every call
 * re-reads and re-parses the WHOLE registry file from disk (see
 * [FunctionRegistryStore.readAll]). [WorkflowInternalCallEndpointResolver]
 * calls it once per internal call node it resolves. So resolution cost is
 * O(N*R): N = number of internal calls in the workflow, R = number of
 * functions already in the registry file.
 *
 * P3 ([BenchmarkInternalCallResolution]) already varies N but pins the
 * registry at a single entry (R=1), so the R-dependent half of that cost is
 * never exercised there. P6 varies R independently of N to isolate and
 * quantify it. Pure local file I/O only - no AWS/GCP SDK, no network.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkRegistryScaling {

    /** Number of internal-call steps in the workflow. */
    @Param("1", "10", "50", "200")
    var n: Int = 0

    /** Number of functions already registered in the registry file. */
    @Param("1", "10", "50", "200", "1000")
    var r: Int = 0

    private lateinit var registryFile: Path
    private lateinit var resolver: WorkflowInternalCallEndpointResolver

    private lateinit var internalWorkflow: Workflow
    private lateinit var externalWorkflow: Workflow

    /** Returns the internal function name for a call, or null when external. */
    private val internalCallExtractor: (CallContext) -> String? =
        { call -> call.internalFunction?.name }

    @Setup(Level.Trial)
    fun setup() {
        // Registry pre-loaded once per (n, r) trial into a local temp file:
        // the real target function plus (r - 1) padding entries, so readAll()
        // has to parse a file with exactly r entries on every resolveUrl call.
        registryFile = Files.createTempFile("omniflow-bench-registry", ".json")
        val store = FunctionRegistryStore(registryFile)

        val functions = mutableMapOf(
            FUNCTION_NAME to FunctionInvocationMetadata(
                serviceName = FUNCTION_NAME,
                url = "https://internal.example.com/$FUNCTION_NAME"
            )
        )
        (1 until r).forEach { idx ->
            val paddingName = "paddingFn$idx"
            functions[paddingName] = FunctionInvocationMetadata(
                serviceName = paddingName,
                url = "https://internal.example.com/$paddingName"
            )
        }
        store.writeNew(functions)
        resolver = WorkflowInternalCallEndpointResolver(store)

        internalWorkflow = WorkflowGenerator.withInternalCalls(n, FUNCTION_NAME)
        externalWorkflow = WorkflowGenerator.withExternalCalls(n)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    /** Cost of resolving n internal calls against an r-entry registry file. */
    @Benchmark
    fun resolveAllInternal(blackhole: Blackhole) {
        val resolved = resolver.resolve(internalWorkflow, internalCallExtractor)
        blackhole.consume(resolved)
    }

    /** Control: external calls never touch the registry, so cost should be
     *  independent of r - confirms any r-driven slowdown above is specifically
     *  the registry read/parse, not workflow generation itself. */
    @Benchmark
    fun resolveAllExternal(blackhole: Blackhole) {
        val resolved = resolver.resolve(externalWorkflow, internalCallExtractor)
        blackhole.consume(resolved)
    }

    companion object {
        private const val FUNCTION_NAME = "benchFunction"
    }
}
