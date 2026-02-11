package costaber.com.github.omniflow.builder

import costaber.com.github.omniflow.model.StepContext
import costaber.com.github.omniflow.model.StepType

interface ContextBuilder : Builder<StepContext> {
    fun stepType(): StepType
}