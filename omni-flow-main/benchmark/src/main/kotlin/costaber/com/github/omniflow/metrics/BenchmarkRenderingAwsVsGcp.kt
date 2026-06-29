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
 * P4 - Amazon (ASL JSON) vs Google (Workflows YAML) local rendering cost.
 *
 * Renders the SAME workflow to each target across a range of step counts so
 * the relative cost of the two renderers can be compared directly. Local only:
 * no deployment, no network, no cloud SDK calls.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkRenderingAwsVsGcp {

    @Param("1", "5", "10", "50", "100", "200")
    var n: Int = 0

    private lateinit var workflow: Workflow

    private lateinit var amazonTraversor: AmazonTraversor
    private lateinit var amazonVisitor: NodeContextVisitor

    private lateinit var googleTraversor: DepthFirstNodeVisitorTraversor
    private lateinit var googleVisitor: NodeContextVisitor

    @Setup(Level.Trial)
    fun setupWorkflow() {
        workflow = WorkflowGenerator.withIndependentSteps(n)
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
    fun renderAslJson(blackhole: Blackhole) {
        val result = amazonTraversor.traverse(
            amazonVisitor,
            workflow,
            AmazonRenderingContext()
        )
        blackhole.consume(result)
    }

    @Benchmark
    fun renderGcpYaml(blackhole: Blackhole) {
        val result = googleTraversor.traverse(
            googleVisitor,
            workflow,
            GoogleRenderingContext(0, StringBuilder(), GoogleTermContext())
        )
        blackhole.consume(result)
    }
}
