package costaber.com.github.omniflow.model

class GreaterThanOrEqualExpression<T>(
    left: Variable,
    right: Term<T>,
) : BinaryExpression<T>(left, BinaryOperator.GREATER_THAN_OR_EQUAL, right)