package costaber.com.github.omniflow.model

data class Workflow(
    val name: String,
    val description: String? = null,
    val input: String? = null,
    val steps: Collection<Step> = emptyList(),
    val result: String,
) : Node {

    override fun childNodes(): List<Node> {
        return steps.toList()
    }
}