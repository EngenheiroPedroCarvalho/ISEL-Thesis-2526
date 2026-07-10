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
 * P12 - Local rendering cost vs. Choice/Parallel branch width.
 *
 * P1/P2/P4/P5 only ever vary CALL step count, per-call parameter count, or nesting depth - never
 * the width of a Choice (nº of conditions) or a Parallel (nº of branches). `GoogleParallelRenderer
 * .internalEndRender` computes a set intersection of shared variable names between the branch and
 * outer scope, which reading the source suggested might scale worse than the Amazon side as
 * branch count grows - but measured results show both providers scaling near-linearly with AWS
 * slightly, not more, expensive (see RESULTS.md P12): the generated workflows have no variables
 * in scope (leaf steps are plain `independent()` calls), so that intersection stays O(1) per
 * branch regardless of width. Isolating branch-count cost from variables-in-scope cost is exactly
 * what this benchmark measures; the latter is a separate, still-unmeasured axis. Renders LOCALLY
 * to both Amazon and Google targets. No deployment, no network, no cloud SDK calls.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkRenderingByBranchWidth {

    /** Number of conditions (Choice) or branches (Parallel) in the single step rendered. */
    @Param("1", "2", "5", "10", "20", "50", "100")
    var branchWidth: Int = 0

    private lateinit var choiceWorkflow: Workflow
    private lateinit var parallelWorkflow: Workflow

    private lateinit var amazonTraversor: AmazonTraversor
    private lateinit var amazonVisitor: NodeContextVisitor

    private lateinit var googleTraversor: DepthFirstNodeVisitorTraversor
    private lateinit var googleVisitor: NodeContextVisitor

    @Setup(Level.Trial)
    fun setupWorkflows() {
        choiceWorkflow = WorkflowGenerator.withChoiceBranches(branchWidth)
        parallelWorkflow = WorkflowGenerator.withParallelBranchWidth(branchWidth)
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
    fun renderChoiceToAmazon(blackhole: Blackhole) {
        val result = amazonTraversor.traverse(amazonVisitor, choiceWorkflow, AmazonRenderingContext())
        blackhole.consume(result)
    }

    @Benchmark
    fun renderChoiceToGoogle(blackhole: Blackhole) {
        val result = googleTraversor.traverse(
            googleVisitor, choiceWorkflow, GoogleRenderingContext(0, StringBuilder(), GoogleTermContext())
        )
        blackhole.consume(result)
    }

    @Benchmark
    fun renderParallelToAmazon(blackhole: Blackhole) {
        val result = amazonTraversor.traverse(amazonVisitor, parallelWorkflow, AmazonRenderingContext())
        blackhole.consume(result)
    }

    @Benchmark
    fun renderParallelToGoogle(blackhole: Blackhole) {
        val result = googleTraversor.traverse(
            googleVisitor, parallelWorkflow, GoogleRenderingContext(0, StringBuilder(), GoogleTermContext())
        )
        blackhole.consume(result)
    }
}
