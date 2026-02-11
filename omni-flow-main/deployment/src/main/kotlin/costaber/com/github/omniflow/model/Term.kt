package costaber.com.github.omniflow.model

sealed interface Term<T> : Expression {
    fun term(): T
}