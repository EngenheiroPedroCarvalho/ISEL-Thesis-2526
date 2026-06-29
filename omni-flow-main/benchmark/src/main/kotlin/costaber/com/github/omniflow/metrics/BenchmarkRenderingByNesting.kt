package costaber.com.github.omniflow.metrics

import costaber.com.github.omniflow.cloud.provider.amazon.provider.AmazonDefaultStrategyDeciderProvider
import costaber.com.github.omniflow.cloud.provider.amazon.renderer.AmazonRenderingContext
import costaber.com.github.omniflow.cloud.provider.amazon.traversor.AmazonTraversor
import costaber.com.github.omniflow.cloud.provider.google.provider.GoogleDefaultStrategyDeciderProvider
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleRenderingContext
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleTermContext
import costaber.com.github.omniflow.generator.WorkflowGenerator
import costaber.com.github.omniflow.model.Workflow
import costaber.com.github.omniflow.traversor.DepthFirstNodeVisitorTraversor
import costaber.com.github.omniflow.visitor.NodeContextVisitor
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
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * P5 - Local rendering cost by nesting depth.
 *
 * Keeps the number of leaf steps FIXED and varies how deeply they are nested
 * inside iteration/parallel containers. Renders LOCALLY to both Amazon and
 * Google targets. No deployment, no network, no cloud SDK calls.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkRenderingByNesting {

    /** Number of container levels wrapping the fixed set of leaf steps. */
    @Param("0", "1", "2", "3", "4", "5")
    var depth: Int = 0

    private lateinit var workflow: Workflow

    private lateinit var amazonTraversor: AmazonTraversor
    private lateinit var amazonVisitor: NodeContextVisitor

    private lateinit var googleTraversor: DepthFirstNodeVisitorTraversor
    private lateinit var googleVisitor: NodeContextVisitor

    @Setup(Level.Trial)
    fun setupWorkflow() {
        workflow = WorkflowGenerator.withNestedSteps(FIXED_LEAF_STEPS, depth)
    }

    @Setup(Level.Iteration)
    fun setupRenderers() {
        amazonTraversor = AmazonTraversor()
        amazonVisitor = NodeContextVisitor(
            AmazonDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider()
        )
        googleTraversor = DepthFirstNodeVisitorTraversor()
        googleVisitor = NodeContextVisitor(
            GoogleDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider()
        )
    }

    @Benchmark
    fun renderToAmazon(blackhole: Blackhole) {
        val result = amazonTraversor.traverse(
            amazonVisitor,
            workflow,
            AmazonRenderingContext()
        )
        blackhole.consume(result)
    }

    @Benchmark
    fun renderToGoogle(blackhole: Blackhole) {
        val result = googleTraversor.traverse(
            googleVisitor,
            workflow,
            GoogleRenderingContext(0, StringBuilder(), GoogleTermContext())
        )
        blackhole.consume(result)
    }

    companion object {
        /** Fixed number of innermost leaf call steps across all depths. */
        private const val FIXED_LEAF_STEPS = 20
    }
}
