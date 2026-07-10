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
 * P14 - Cost of [FunctionRegistryStore.resolveUrlIn]'s SUFFIX-match fallback vs its EXACT-match
 * fast path, inside the ALREADY-OPTIMIZED [WorkflowInternalCallEndpointResolver] (the P7-P9
 * "com cache" resolver: reads the registry once, then does N in-memory lookups).
 *
 * `resolveUrlIn` tries `all[functionName]` first (O(1)); only on a miss does it fall through to
 * `all.filterKeys { it.endsWith("/$functionName") }` (O(M), a full scan of the WHOLE in-memory
 * map on every single call). P6-P11 always used bare exact-match names, so that fallback was
 * never exercised. But the registry supports (and region-disambiguation documented in
 * FunctionRegistryStore relies on) region-qualified keys ("region/functionName") while call sites
 * still reference the bare name - which makes the exact-match ALWAYS miss and EVERY lookup pay
 * the O(M) scan. That silently degrades the P7-P9 fix from Theta(N+M) back to Theta(N*M), without
 * changing a single line of production code - just the shape of the registry keys.
 *
 * Mirrors P9's structure exactly (F/N fixed, M swept) rather than P8's (F swept): the suffix scan
 * is an in-memory Map.filterKeys pass, not a disk re-read, so its cost only becomes visible at
 * P9-scale M (up to 1000) - at P8-scale M (<=50) the effect is real but too small to be visible
 * against JVM/JIT noise.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkResolutionKeyMatchStrategy {

    /** Total registry size (M); always >= FIXED_FUNCTIONS. The workflow only ever references the
     * first FIXED_FUNCTIONS of them - the rest is padding that inflates every suffix scan. */
    @Param("10", "20", "50", "100", "200", "1000")
    var m: Int = 0

    private lateinit var exactRegistryFile: Path
    private lateinit var suffixRegistryFile: Path
    private lateinit var exactResolver: WorkflowInternalCallEndpointResolver
    private lateinit var suffixResolver: WorkflowInternalCallEndpointResolver
    private lateinit var workflow: Workflow

    private val internalCallExtractor: (CallContext) -> String? =
        { call -> call.internalFunction?.name }

    @Setup(Level.Trial)
    fun setupWorkflow() {
        // Registry padded to M entries; the workflow references only the first FIXED_FUNCTIONS,
        // by bare name, in both registries - only the KEY naming below differs.
        val names = (0 until m).map { "$BASE$it" }

        // Exact-match registry: keys are the bare names -> every lookup hits `all[functionName]`
        // in O(1), regardless of M.
        exactRegistryFile = Files.createTempFile("omniflow-bench-p14-exact-registry", ".json")
        val exactStore = FunctionRegistryStore(exactRegistryFile)
        exactStore.writeNew(
            names.associateWith { name -> FunctionInvocationMetadata(serviceName = name, url = "https://internal.example.com/$name") }
        )
        exactResolver = WorkflowInternalCallEndpointResolver(exactStore)

        // Suffix-match registry: ALL M keys are region-qualified -> `all[functionName]` always
        // misses, every lookup pays the O(M) filterKeys scan over the whole map.
        suffixRegistryFile = Files.createTempFile("omniflow-bench-p14-suffix-registry", ".json")
        val suffixStore = FunctionRegistryStore(suffixRegistryFile)
        suffixStore.writeNew(
            names.associate { name -> "$REGION/$name" to FunctionInvocationMetadata(serviceName = name, url = "https://internal.example.com/$name") }
        )
        suffixResolver = WorkflowInternalCallEndpointResolver(suffixStore)

        // FIXED_CALLS calls distributed round-robin over the first FIXED_FUNCTIONS functions.
        workflow = WorkflowGenerator.withDistinctInternalCalls(FIXED_CALLS, FIXED_FUNCTIONS, BASE)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(exactRegistryFile)
        Files.deleteIfExists(suffixRegistryFile)
    }

    /** Exact-match fast path: every resolveUrlIn lookup is O(1) -> Theta(N+M) overall. */
    @Benchmark
    fun resolveExactMatch(blackhole: Blackhole) {
        blackhole.consume(exactResolver.resolve(workflow, internalCallExtractor))
    }

    /** Suffix-match fallback: every resolveUrlIn lookup is O(M) -> Theta(N*M) overall. */
    @Benchmark
    fun resolveSuffixMatch(blackhole: Blackhole) {
        blackhole.consume(suffixResolver.resolve(workflow, internalCallExtractor))
    }

    companion object {
        private const val FIXED_CALLS = 50
        private const val FIXED_FUNCTIONS = 10
        private const val BASE = "benchFn"
        private const val REGION = "region1"
    }
}
