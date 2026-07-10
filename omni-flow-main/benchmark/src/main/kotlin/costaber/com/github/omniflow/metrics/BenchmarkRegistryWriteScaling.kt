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
 * P13 - Cost of incremental registry writes, [FunctionRegistryStore.put], vs registry size
 * before writing starts (M0) and number of sequential writes (K).
 *
 * P6-P11 measured the READ side (`resolveUrl`/`tryResolveEntry`, re-reads the whole file per
 * call). `put` has never been measured, and is exercised by the real auto-deploy resolvers
 * (`AwsInternalFunctionResolver`, `WorkflowInternalFunctionResolver`) every time a newly
 * discovered/deployed function is registered. `put` reads the whole file (`readRootOrNew`) then
 * rewrites it whole (`writeRoot`), so registering K functions in sequence into a registry that
 * starts at M0 entries costs sum_{i=0}^{K-1} O(M0+i) = Θ(K*M0 + K^2) - quadratic in K itself when
 * M0 is small, not just linear. Pure local file I/O - no AWS/GCP SDK, no network.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkRegistryWriteScaling {

    /** Registry size before the K writes start. */
    @Param("0", "10", "50", "200", "1000")
    var m0: Int = 0

    /** Number of sequential put() calls measured per invocation. */
    @Param("1", "5", "10", "50", "100")
    var k: Int = 0

    private lateinit var registryFile: Path
    private lateinit var store: FunctionRegistryStore

    // Level.Invocation (not Trial): each measured invocation must start from the same M0-sized
    // registry, otherwise the K writes of one invocation would grow the registry for the next,
    // contaminating the measurement.
    @Setup(Level.Invocation)
    fun setupRegistry() {
        registryFile = Files.createTempFile("omniflow-bench-p13-registry", ".json")
        store = FunctionRegistryStore(registryFile)

        val baseline = (0 until m0).associate { idx ->
            val name = "base$idx"
            name to FunctionInvocationMetadata(
                serviceName = name,
                url = "https://internal.example.com/$name"
            )
        }
        store.writeNew(baseline)
    }

    @TearDown(Level.Invocation)
    fun tearDown() {
        Files.deleteIfExists(registryFile)
    }

    @Benchmark
    fun putKSequential(blackhole: Blackhole) {
        for (i in 0 until k) {
            val name = "newFn$i"
            store.put(
                name,
                FunctionInvocationMetadata(serviceName = name, url = "https://internal.example.com/$name")
            )
        }
        blackhole.consume(store)
    }
}
