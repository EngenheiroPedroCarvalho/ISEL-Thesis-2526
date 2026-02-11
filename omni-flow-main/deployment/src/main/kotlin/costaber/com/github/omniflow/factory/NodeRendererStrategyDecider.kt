package costaber.com.github.omniflow.factory

import costaber.com.github.omniflow.model.Node
import costaber.com.github.omniflow.renderer.NodeRenderer

interface NodeRendererStrategyDecider {
    fun decideRenderer(node: Node): NodeRenderer<*>
}