package costaber.com.github.omniflow.util.data

import costaber.com.github.omniflow.dsl.condition
import costaber.com.github.omniflow.util.*
import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

internal object SwitchConditionBuilderDataProvider {

    @JvmStatic
    fun switchConditionBuilderWithExpectedDataProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            condition {
                match(VARIABLE_1 equalTo VALUE_1)
                jump(STEP_NAME_1)
            },
            CONDITION_1,
        ),
        Arguments.of(
            condition {
                match(VARIABLE_1 greaterThan VALUE_1)
                jump(STEP_NAME_1)
            },
            CONDITION_2,
        ),
        Arguments.of(
            condition {
                match(VARIABLE_1 greaterThanOrEqual VALUE_1)
                jump(STEP_NAME_1)
            },
            CONDITION_3,
        ),
        Arguments.of(
            condition {
                match(VARIABLE_1 lessThan VALUE_1)
                jump(STEP_NAME_1)
            },
            CONDITION_4,
        ),
        Arguments.of(
            condition {
                match(VARIABLE_1 lessThanOrEqual VALUE_1)
                jump(STEP_NAME_1)
            },
            CONDITION_5,
        ),
        Arguments.of(
            condition {
                match(VARIABLE_1 notEqualTo VALUE_1)
                jump(STEP_NAME_1)
            },
            CONDITION_6,
        ),
    )
}