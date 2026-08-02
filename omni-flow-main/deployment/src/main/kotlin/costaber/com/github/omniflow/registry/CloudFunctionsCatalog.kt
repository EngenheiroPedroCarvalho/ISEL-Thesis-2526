package costaber.com.github.omniflow.registry

interface CloudFunctionsCatalog {
    /**
     * Lists the functions already deployed on the target provider, scoped to whatever the
     * provider needs to enumerate them (a GCP project id, an AWS region, ...).
     *
     * Returns a map where the key is the function identifier used in a registry, and the value is the invocation metadata (host/path).
     */
    fun listHttpFunctions(scope: String): Map<String, FunctionInvocationMetadata>
}