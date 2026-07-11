package costaber.com.github.omniflow.metrics

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
 * P7 - Before/after of the registry-read optimization, measured in the SAME run
 * on the SAME machine (so the two strategies are directly comparable, unlike
 * comparing P3 and P6 across machines).
 *
 * Resolving N internal calls against an R-entry registry:
 *  - [resolveNaive]     re-reads + re-parses the whole registry file on EVERY call
 *                       (legacy `resolveUrl` path)                      -> O(N*R)
 *  - [resolveOptimized] reads the registry ONCE, then does N pure lookups
 *                       (`readAll` + `resolveUrlIn`, the new resolver path) -> O(N+R)
 *
 * Pure local file I/O only - no AWS/GCP SDK, no network.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkResolutionOptimization {

    /** Number of internal-call resolutions (workflow size). */
    @Param("1", "10", "50", "200")
    var n: Int = 0

    /** Number of functions already registered in the registry file. */
    @Param("1", "50", "1000")
    var r: Int = 0

    private lateinit var registryFile: Path
    private lateinit var store: FunctionRegistryStore

    @Setup(Level.Trial)
    fun setup() {
        registryFile = Files.createTempFile("omniflow-bench-p7-registry", ".json")
        store = FunctionRegistryStore(registryFile)

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
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    /** Legacy path: one full registry read+parse per internal call -> O(N*R). */
    @Benchmark
    fun resolveNaive(blackhole: Blackhole) {
        repeat(n) {
            blackhole.consume(store.resolveUrl(FUNCTION_NAME))
        }
    }

    /** Optimized path: one registry read, then N pure lookups -> O(N+R). */
    @Benchmark
    fun resolveOptimized(blackhole: Blackhole) {
        val all = store.readAll()
        repeat(n) {
            blackhole.consume(store.resolveUrlIn(FUNCTION_NAME, all))
        }
    }

    companion object {
        private const val FUNCTION_NAME = "benchFunction"
    }
}
