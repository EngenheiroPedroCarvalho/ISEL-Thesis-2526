package costaber.com.github.omniflow.model

class LessThanExpression<T>(
    left: Variable,
    right: Term<T>,
) : BinaryExpression<T>(left, BinaryOperator.LESS_THAN, right)