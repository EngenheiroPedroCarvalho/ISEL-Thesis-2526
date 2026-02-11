package costaber.com.github.omniflow.model

data class AssignContext(
    val variables: Collection<VariableInitialization<*>>
) : StepContext {

    override fun childNodes(): List<Node> {
        return variables.toList()
    }
}