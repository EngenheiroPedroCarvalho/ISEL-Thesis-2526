package costaber.com.github.omniflow.cloud.provider.google.renderer

import costaber.com.github.omniflow.cloud.provider.google.VARIABLE_DELIMITER
import costaber.com.github.omniflow.model.Variable
import costaber.com.github.omniflow.renderer.TermContext

class GoogleTermContext : TermContext {

    private val resultVariables: MutableSet<String> = mutableSetOf()

    fun addResultVariable(variableName: String) {
        resultVariables.add(variableName)
    }

    fun isResultVariable(variable: Variable): Boolean =
        resultVariables.contains(variable.name.split(VARIABLE_DELIMITER).firstOrNull())
}