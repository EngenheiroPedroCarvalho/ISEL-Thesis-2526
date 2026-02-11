package costaber.com.github.omniflow.builder

import costaber.com.github.omniflow.model.ConditionalContext
import costaber.com.github.omniflow.model.StepType

class SwitchContextBuilder : ContextBuilder {

    private val conditions: MutableList<SwitchConditionBuilder> = mutableListOf()
    private var default: String? = null

    fun conditions(vararg value: SwitchConditionBuilder) = apply { this.conditions.addAll(value) }

    fun default(value: String) = apply { this.default = value }

    override fun stepType() = StepType.CONDITIONAL

    override fun build() = ConditionalContext(
        conditions = conditions.map { it.build() },
        default = default,
    )
}