package costaber.com.github.omniflow.model

class EqualToExpression<T>(
    left: Variable,
    right: Term<T>,
) : BinaryExpression<T>(left, BinaryOperator.EQUAL_TO, right)