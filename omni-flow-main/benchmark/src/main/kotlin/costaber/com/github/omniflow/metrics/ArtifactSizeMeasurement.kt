package costaber.com.github.omniflow.metrics

import costaber.com.github.omniflow.cloud.provider.amazon.provider.AmazonDefaultStrategyDeciderProvider
import costaber.com.github.omniflow.cloud.provider.amazon.renderer.AmazonRenderingContext
import costaber.com.github.omniflow.cloud.provider.amazon.traversor.AmazonTraversor
import costaber.com.github.omniflow.cloud.provider.google.provider.GoogleDefaultStrategyDeciderProvider
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleRenderingContext
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleTermContext
import costaber.com.github.omniflow.generator.WorkflowGenerator
import costaber.com.github.omniflow.traversor.DepthFirstNodeVisitorTraversor
import costaber.com.github.omniflow.visitor.NodeContextVisitor
import java.io.File

/**
 * S2 - Rendered-artifact size (NOT a JMH benchmark).
 *
 * Measures the SIZE in bytes of the workflow definition generated locally for
 * each target (Amazon States Language JSON vs GCP Workflows YAML) as the number
 * of steps N grows. This is the size of the produced artifact, complementing the
 * rendering-TIME benchmarks (P1). Purely local; no cloud.
 *
 * Usage: writes a CSV `n,aws_bytes,gcp_bytes` to args[0] (default artifact-size.csv).
 */
object ArtifactSizeMeasurement {

    private val NS = listOf(1, 2, 5, 10, 20, 50, 100, 200)

    @JvmStatic
    fun main(args: Array<String>) {
        val outPath = args.getOrElse(0) { "artifact-size.csv" }

        val rows = StringBuilder("n,aws_bytes,gcp_bytes\n")
        println("N        AWS (bytes)   GCP (bytes)")
        for (n in NS) {
            val workflow = WorkflowGenerator.withIndependentSteps(n)

            val awsContent = AmazonTraversor().traverse(
                NodeContextVisitor(AmazonDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider()),
                workflow,
                AmazonRenderingContext()
            )
            val gcpContent = DepthFirstNodeVisitorTraversor().traverse(
                NodeContextVisitor(GoogleDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider()),
                workflow,
                GoogleRenderingContext(0, StringBuilder(), GoogleTermContext())
            )

            val awsBytes = sizeInBytes(awsContent)
            val gcpBytes = sizeInBytes(gcpContent)

            rows.append("$n,$awsBytes,$gcpBytes\n")
            println("%-8d %-13d %-13d".format(n, awsBytes, gcpBytes))
        }

        File(outPath).writeText(rows.toString())
        println("\nWrote $outPath")
    }

    /** Byte size of the rendered artifact: the non-empty lines joined with newlines, UTF-8. */
    private fun sizeInBytes(content: List<String>): Int =
        content.filterNot(String::isEmpty).joinToString("\n").toByteArray(Charsets.UTF_8).size
}
