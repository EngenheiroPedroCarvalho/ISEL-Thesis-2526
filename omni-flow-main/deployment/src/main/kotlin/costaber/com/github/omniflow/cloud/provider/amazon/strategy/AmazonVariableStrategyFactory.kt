package costaber.com.github.omniflow.cloud.provider.amazon.strategy

import costaber.com.github.omniflow.cloud.provider.amazon.renderer.AmazonVariablesResolver
import costaber.com.github.omniflow.factory.NodeRendererStrategyFactory
import costaber.com.github.omniflow.model.Node
import costaber.com.github.omniflow.model.VariableInitialization
import costaber.com.github.omniflow.predicate.DefaultPredicate
import costaber.com.github.omniflow.renderer.NodeRenderer
import java.util.function.Predicate

class AmazonVariableStrategyFactory : NodeRendererStrategyFactory<String> {

    override fun getMatcher(): Predicate<Node> =
        DefaultPredicate(VariableInitialization::class)

    override fun getRenderer(node: Node): NodeRenderer<String> =
        AmazonVariablesResolver(node as VariableInitialization<*>)
}