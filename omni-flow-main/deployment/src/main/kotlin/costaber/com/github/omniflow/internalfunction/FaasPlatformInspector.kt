package costaber.com.github.omniflow.internalfunction

import costaber.com.github.omniflow.registry.FunctionInvocationMetadata

/**
 * Abstraction over "FaaS_Platform" APIs used for integrity validation.
 *
 * Return null when the function does not exist (or is not invokable via HTTP).
 */
interface FaasPlatformInspector {
    fun getInvocationMetadata(functionName: String): FunctionInvocationMetadata?
}