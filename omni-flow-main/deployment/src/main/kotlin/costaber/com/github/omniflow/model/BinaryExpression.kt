package costaber.com.github.omniflow.model

sealed class BinaryExpression<R>(
    val left: Variable,
    val operator: BinaryOperator,
    val right: Term<R>,
) : Expression, Node {

    override fun childNodes() = emptyList<Node>()
}