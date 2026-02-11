package costaber.com.github.omniflow.model

data class ConditionalContext(
    val conditions: Collection<Condition>,
    val default: String?,
) : StepContext {

    override fun childNodes(): List<Node> {
        return conditions.toList()
    }
}