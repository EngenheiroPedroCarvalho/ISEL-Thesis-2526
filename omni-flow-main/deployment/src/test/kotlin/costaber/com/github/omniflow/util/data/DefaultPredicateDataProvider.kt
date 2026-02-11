package costaber.com.github.omniflow.util.data

import costaber.com.github.omniflow.model.*
import costaber.com.github.omniflow.util.*
import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

internal object DefaultPredicateDataProvider {

    @JvmStatic
    fun testDefaultPredicateDataProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            AssignContext::class,
            ASSIGN_CONTEXT_1,
            true,
        ),
        Arguments.of(
            CallContext::class,
            CALL_CONTEXT_1,
            true,
        ),
        Arguments.of(
            Condition::class,
            CONDITION_1,
            true,
        ),
        Arguments.of(
            ConditionalContext::class,
            CONDITION_CONTEXT_1,
            true,
        ),
        Arguments.of(
            EqualToExpression::class,
            EQUAL_TO_EXPRESSION_1,
            true,
        ),
        Arguments.of(
            GreaterThanExpression::class,
            GREATER_THAN_EXPRESSION_1,
            true,
        ),
        Arguments.of(
            GreaterThanOrEqualExpression::class,
            GREATER_THAN_OR_EQUAL_EXPRESSION_1,
            true,
        ),
        Arguments.of(
            LessThanExpression::class,
            LESS_THAN_EXPRESSION_1,
            true,
        ),
        Arguments.of(
            LessThanOrEqualExpression::class,
            LESS_THAN_OR_EQUAL_EXPRESSION_1,
            true,
        ),
        Arguments.of(
            NotEqualToExpression::class,
            NOT_EQUAL_TO_EXPRESSION_1,
            true,
        ),
        Arguments.of(
            Step::class,
            STEP_1,
            true,
        ),
        Arguments.of(
            VariableInitialization::class,
            VARIABLE_INITIALIZATION_1,
            true,
        ),
        Arguments.of(
            Workflow::class,
            WORKFLOW_1,
            true
        ),
        Arguments.of(
            Workflow::class,
            object : Node {
                override fun childNodes(): List<Node> = emptyList()
            },
            false,
        )
    )
}