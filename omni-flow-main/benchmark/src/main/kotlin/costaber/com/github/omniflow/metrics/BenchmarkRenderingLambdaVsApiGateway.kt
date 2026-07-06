package costaber.com.github.omniflow.metrics

import costaber.com.github.omniflow.cloud.provider.amazon.provider.AmazonDefaultStrategyDeciderProvider
import costaber.com.github.omniflow.cloud.provider.amazon.renderer.AmazonRenderingContext
import costaber.com.github.omniflow.cloud.provider.amazon.traversor.AmazonTraversor
import costaber.com.github.omniflow.generator.WorkflowGenerator
import costaber.com.github.omniflow.model.Workflow
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
 * P8 - Rendering cost: internal (lambda:invoke) vs external (apigateway:invoke) calls.
 *
 * P1/P2/P4/P5 only ever render EXTERNAL calls (literal host/path); P3/P6/P7 use internal
 * calls but only measure resolve(), never rendering. This benchmark closes that gap: it
 * renders, at scale, a workflow whose calls are ALREADY-RESOLVED Lambda calls
 * (host = "lambda://<arn>", see WorkflowGenerator.withLambdaCalls) and compares against the
 * same apigateway-style rendering already measured in P1 (WorkflowGenerator.withIndependentSteps).
 * No resolver.resolve() call is involved on either side - pure rendering cost, AWS only (GCP's
 * renderer has no internal/external distinction).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Thread)
open class BenchmarkRenderingLambdaVsApiGateway {

    @Param("1", "2", "5", "10", "20", "50", "100", "200")
    var n: Int = 0

    private lateinit var apiGatewayWorkflow: Workflow
    private lateinit var lambdaWorkflow: Workflow

    private lateinit var traversor: AmazonTraversor
    private lateinit var visitor: NodeContextVisitor

    @Setup(Level.Trial)
    fun setupWorkflows() {
        apiGatewayWorkflow = WorkflowGenerator.withIndependentSteps(n)
        lambdaWorkflow = WorkflowGenerator.withLambdaCalls(n)
    }

    @Setup(Level.Iteration)
    fun setupRenderer() {
        traversor = AmazonTraversor()
        visitor = NodeContextVisitor(
            AmazonDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider()
        )
    }

    @Benchmark
    fun renderApiGateway(blackhole: Blackhole) {
        val result = traversor.traverse(visitor, apiGatewayWorkflow, AmazonRenderingContext())
        blackhole.consume(result)
    }

    @Benchmark
    fun renderLambda(blackhole: Blackhole) {
        val result = traversor.traverse(visitor, lambdaWorkflow, AmazonRenderingContext())
        blackhole.consume(result)
    }
}
