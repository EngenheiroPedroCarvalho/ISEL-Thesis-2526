package costaber.com.github.omniflow.registry

interface CloudFunctionsCatalog {
    /**
     * Returns a map where the key is the function identifier used in a registry, and the value is the invocation metadata (host/path).
     */
    fun listHttpFunctions(projectId: String): Map<String, FunctionInvocationMetadata>
}