package costaber.com.github.omniflow.model

class LessThanOrEqualExpression<T>(
    left: Variable,
    right: Term<T>,
) : BinaryExpression<T>(left, BinaryOperator.LESS_THAN_OR_EQUAL, right)