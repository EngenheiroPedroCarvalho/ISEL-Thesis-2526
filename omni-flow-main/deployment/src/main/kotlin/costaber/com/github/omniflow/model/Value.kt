package costaber.com.github.omniflow.model

data class Value<T : Any>(
    val value: T
) : Term<T> {

    override fun term() = value
}