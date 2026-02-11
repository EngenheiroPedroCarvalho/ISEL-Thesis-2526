package costaber.com.github.omniflow.builder

import costaber.com.github.omniflow.model.Condition
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

internal class SwitchConditionBuilderTest {

    @ParameterizedTest
    @MethodSource("costaber.com.github.omniflow.util.data.SwitchConditionBuilderDataProvider#switchConditionBuilderWithExpectedDataProvider")
    fun `condition has expected result`(
        conditionBuilder: SwitchConditionBuilder,
        expectedResult: Condition,
    ) {
        val actualResult = conditionBuilder.build()
        expectThat(actualResult)
            .isA<Condition>()
            .and {
                get { expression }.and {
                    get { left }.isEqualTo(expectedResult.expression.left)
                    get { operator }.isEqualTo(expectedResult.expression.operator)
                    get { right }.isEqualTo(expectedResult.expression.right)
                }
                get { jump }.isEqualTo(expectedResult.jump)
            }
    }
}