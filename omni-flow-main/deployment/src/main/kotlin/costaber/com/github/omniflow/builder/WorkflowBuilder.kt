package costaber.com.github.omniflow.builder

import costaber.com.github.omniflow.model.Workflow

class WorkflowBuilder : Builder<Workflow> {

    // mandatory
    private lateinit var name: String
    private lateinit var description: String
    private val steps = mutableListOf<StepBuilder>()
    private lateinit var result: String

    // optional
    private var params: String? = null

    fun name(value: String) = apply { this.name = value }

    fun description(value: String) = apply { this.description = value }

    fun params(value: String) = apply { this.params = value }

    fun steps(vararg value: StepBuilder) = apply { this.steps.addAll(value) }

    fun result(value: String) = apply { this.result = value }

    override fun build() = Workflow(
        name = name,
        description = description,
        input = params,
        steps = steps.map { it.build() },
        result = result
    )
}