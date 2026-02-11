package costaber.com.github.omniflow.model

data class Condition(
    val expression: BinaryExpression<*>,
    val jump: String,
) : Node {

    override fun childNodes() = listOf<Node>(expression)
}