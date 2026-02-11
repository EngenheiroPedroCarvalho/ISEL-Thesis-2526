package costaber.com.github.omniflow.predicate

import costaber.com.github.omniflow.model.Node
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.reflect.KClass

internal class DefaultPredicateTest {

    @ParameterizedTest
    @MethodSource("costaber.com.github.omniflow.util.data.DefaultPredicateDataProvider#testDefaultPredicateDataProvider")
    fun `test has expected results`(
        type: KClass<Node>,
        node: Node,
        expectedResult: Boolean,
    ) {
        val actualResult = DefaultPredicate(type).test(node)
        expectThat(actualResult).isEqualTo(expectedResult)
    }
}