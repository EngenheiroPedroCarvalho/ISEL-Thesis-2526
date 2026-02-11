package costaber.com.github.omniflow.model

class NotEqualToExpression<T>(
    left: Variable,
    right: Term<T>,
) : BinaryExpression<T>(left, BinaryOperator.NOT_EQUAL_TO, right)