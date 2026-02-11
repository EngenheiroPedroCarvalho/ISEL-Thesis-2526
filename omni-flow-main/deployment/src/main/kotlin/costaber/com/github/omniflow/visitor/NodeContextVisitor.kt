package costaber.com.github.omniflow.visitor

import costaber.com.github.omniflow.factory.NodeRendererStrategyDecider
import costaber.com.github.omniflow.model.Node
import costaber.com.github.omniflow.renderer.NodeRenderer
import costaber.com.github.omniflow.renderer.RenderingContext

class NodeContextVisitor(
    private val nodeRendererStrategyDecider: NodeRendererStrategyDecider
) : ContextVisitor<Node, RenderingContext, String> {

    override fun beginVisit(element: Node, context: RenderingContext): String =
        getRenderer(element).beginRender(context) as String

    override fun endVisit(element: Node, context: RenderingContext): String =
        getRenderer(element).endRender(context) as String

    /**
     * Get the correct renderer for the given node
     *
     * @param node the node
     * @return a renderer for the node
     */
    private fun getRenderer(node: Node): NodeRenderer<*> {
        return nodeRendererStrategyDecider.decideRenderer(node)
    }
}