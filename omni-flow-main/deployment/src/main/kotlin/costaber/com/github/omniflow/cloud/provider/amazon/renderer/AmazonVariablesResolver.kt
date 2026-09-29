package costaber.com.github.omniflow.cloud.provider.amazon.renderer

import costaber.com.github.omniflow.cloud.provider.amazon.AMAZON_INPUT_PATH
import costaber.com.github.omniflow.cloud.provider.amazon.AMAZON_RESULT
import costaber.com.github.omniflow.cloud.provider.amazon.AMAZON_START_RESULT_PATH
import costaber.com.github.omniflow.cloud.provider.amazon.jackson.AmazonObjectMapper
import costaber.com.github.omniflow.model.Node
import costaber.com.github.omniflow.model.Variable
import costaber.com.github.omniflow.model.VariableInitialization
import costaber.com.github.omniflow.renderer.IndentedRenderingContext
import costaber.com.github.omniflow.resource.util.render

class AmazonVariablesResolver(
    private val variableInitialization: VariableInitialization<*>
) : AmazonRenderer() {

    private val objectMapper = AmazonObjectMapper.default

    override val element: Node = variableInitialization

    override fun internalBeginRender(renderingContext: IndentedRenderingContext): String {
        val amazonContext = renderingContext as AmazonRenderingContext
        if (amazonContext.isSingleVariableAssign()) return renderSingleVariable(renderingContext)
        return render(renderingContext) {
            val term = when (variableInitialization.term) {
                is Variable -> "\"$.${variableInitialization.term.term()}\""
                else -> objectMapper.writeValueAsString(variableInitialization.term.term())
            }
            add("\"${variableInitialization.variable.name}\": $term")

            if (amazonContext.isNotLastVariable(variableInitialization)) {
                append(",")
            }
        }
    }

    // A variable is copied from its path (Result is a literal and would store the path string);
    // a value is stored as the Result. Either way, only this variable's path is written.
    private fun renderSingleVariable(renderingContext: IndentedRenderingContext): String =
        render(renderingContext) {
            // These are fields of the state itself, one level up from where the traversor puts a child.
            decIndentationLevel()
            when (val term = variableInitialization.term) {
                is Variable -> addLine("$AMAZON_INPUT_PATH\"$.${term.term()}\",")
                else -> addLine("$AMAZON_RESULT${objectMapper.writeValueAsString(term.term())},")
            }
            add("$AMAZON_START_RESULT_PATH\"$.${variableInitialization.variable.name}\",")
            incIndentationLevel()
        }

    override fun internalEndRender(renderingContext: IndentedRenderingContext): String = "" // nothing
}