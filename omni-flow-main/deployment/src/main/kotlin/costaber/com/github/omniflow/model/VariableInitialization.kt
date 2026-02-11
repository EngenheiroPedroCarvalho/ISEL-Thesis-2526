package costaber.com.github.omniflow.model

data class VariableInitialization<T : Any>(
    val variable: Variable,
    val term: Term<T>,
) : Node {

    override fun childNodes() = emptyList<Node>()
}