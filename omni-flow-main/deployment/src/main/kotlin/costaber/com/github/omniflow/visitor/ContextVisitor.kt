package costaber.com.github.omniflow.visitor

interface ContextVisitor<T, K, R> {

    fun beginVisit(element: T, context: K): R

    fun endVisit(element: T, context: K): R
}