package costaber.com.github.omniflow.cloud.provider.amazon.strategy

import costaber.com.github.omniflow.cloud.provider.amazon.renderer.AmazonPassRenderer
import costaber.com.github.omniflow.factory.NodeRendererStrategyFactory
import costaber.com.github.omniflow.model.AssignContext
import costaber.com.github.omniflow.model.Node
import costaber.com.github.omniflow.predicate.DefaultPredicate
import costaber.com.github.omniflow.renderer.NodeRenderer
import java.util.function.Predicate

class AmazonPassStrategyFactory : NodeRendererStrategyFactory<String> {

    override fun getMatcher(): Predicate<Node> =
        DefaultPredicate(AssignContext::class)

    override fun getRenderer(node: Node): NodeRenderer<String> =
        AmazonPassRenderer(node as AssignContext)
}