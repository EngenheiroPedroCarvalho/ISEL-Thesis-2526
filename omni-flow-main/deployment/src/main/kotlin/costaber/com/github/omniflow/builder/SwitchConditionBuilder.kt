package costaber.com.github.omniflow.builder

import costaber.com.github.omniflow.model.*

class SwitchConditionBuilder : Builder<Condition> {

    private lateinit var match: BinaryExpression<*>
    private lateinit var jump: String

    fun match(value: BinaryExpression<*>) = apply { match = value }

    fun jump(value: String) = apply { jump = value }

    override fun build() = Condition(
        expression = match,
        jump = jump,
    )

    infix fun <T> Variable.equalTo(rightTerm: Term<T>) = EqualToExpression(
        left = this,
        right = rightTerm,
    )

    infix fun <T> Variable.notEqualTo(rightTerm: Term<T>) = NotEqualToExpression(
        left = this,
        right = rightTerm,
    )

    infix fun <T> Variable.greaterThan(rightTerm: Term<T>) = GreaterThanExpression(
        left = this,
        right = rightTerm,
    )

    infix fun <T> Variable.greaterThanOrEqual(rightTerm: Term<T>) = GreaterThanOrEqualExpression(
        left = this,
        right = rightTerm,
    )

    infix fun <T> Variable.lessThan(rightTerm: Term<T>) = LessThanExpression(
        left = this,
        right = rightTerm,
    )

    infix fun <T> Variable.lessThanOrEqual(rightTerm: Term<T>) = LessThanOrEqualExpression(
        left = this,
        right = rightTerm,
    )
}