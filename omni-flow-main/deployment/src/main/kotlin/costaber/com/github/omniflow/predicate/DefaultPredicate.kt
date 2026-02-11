package costaber.com.github.omniflow.predicate

import costaber.com.github.omniflow.model.Node
import java.util.function.Predicate
import kotlin.reflect.KClass

class DefaultPredicate<T : Node>(private val type: KClass<T>) : Predicate<Node> {
    override fun test(node: Node): Boolean =
        type.isInstance(node)
}