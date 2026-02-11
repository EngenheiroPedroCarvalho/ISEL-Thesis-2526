package costaber.com.github.omniflow.model

class GreaterThanExpression<T>(
    left: Variable,
    right: Term<T>,
) : BinaryExpression<T>(left, BinaryOperator.GREATER_THAN, right)