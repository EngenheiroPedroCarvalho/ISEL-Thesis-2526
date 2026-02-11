package costaber.com.github.omniflow.model

interface Node {
    fun childNodes(): List<Node>
}