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
 * P17 - Cost of resolving K brand-new internal functions end-to-end at the registry level: a miss
 * lookup (`tryResolveEntry`, always a miss since each function name is unique) followed by a
 * `put` to register it, vs registry size before the K functions start (R0).
 *
 * P10/P11 measure the real auto-deploy resolvers
 * ([costaber.com.github.omniflow.internalfunction.quickfaas.AwsInternalFunctionResolver],
 * [costaber.com.github.omniflow.internalfunction.WorkflowInternalFunctionResolver]) but only on
 * the registry-HIT path (functions already registered) - step 2 on a miss is a real cloud SDK
 * call, out of unit-test scope. P13 measures the write side alone ([FunctionRegistryStore.put]).
 * Neither measures the sequence both resolvers actually run locally on a miss before any cloud
 * call would happen: `tryResolveEntry` (miss, Θ(R)) then `put` (Θ(R)). This benchmarks that
 * combined local I/O cost for K never-before-seen functions, same (R0, K) grid as P13 so the two
 * are directly comparable. Pure local file I/O - no AWS/GCP SDK, no network.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkRegistryMissAndDeploy {

    /** Registry size before the K new functions start being resolved (R0). */
    @Param("0", "10", "50", "200", "1000")
    var r0: Int = 0

    /** Number of brand-new functions resolved (miss) then registered (put), in sequence. */
    @Param("1", "5", "10", "50", "100")
    var k: Int = 0

    private lateinit var registryFile: Path
    private lateinit var store: FunctionRegistryStore

    // Level.Invocation (not Trial): each measured invocation must start from the same R0-sized
    // registry, otherwise the K miss+put pairs of one invocation would grow the registry for the
    // next, contaminating the measurement (same reasoning as P13).
    @Setup(Level.Invocation)
    fun setupRegistry() {
        registryFile = Files.createTempFile("omniflow-bench-p17-registry", ".json")
        store = FunctionRegistryStore(registryFile)

        val baseline = (0 until r0).associate { idx ->
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
    fun missThenDeployKSequential(blackhole: Blackhole) {
        for (i in 0 until k) {
            val name = "newFn$i"
            blackhole.consume(store.tryResolveEntry(name)) // always null: brand-new function
            store.put(
                name,
                FunctionInvocationMetadata(serviceName = name, url = "https://internal.example.com/$name")
            )
        }
        blackhole.consume(store)
    }
}
