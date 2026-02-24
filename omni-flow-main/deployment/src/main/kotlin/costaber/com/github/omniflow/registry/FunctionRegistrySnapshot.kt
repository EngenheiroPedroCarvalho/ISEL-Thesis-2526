package costaber.com.github.omniflow.registry

/**
 * Function invocation metadata needed by OmniFlow deployment to materialize an internal call.
 */
data class FunctionInvocationMetadata(
    val host: String,
    val path: String
)

/**
 * JSON registry format:
 * {
 *  "updatedAt": "2026-02-12T18:40:00Z"
 *  "functions": {
 *  "fraud-check": { "host": "...", "path": "..." }
 *  }
 * }
 */

data class FunctionRegistrySnapshot(
    val updatedAt: String,
    val functions: MutableMap<String, FunctionInvocationMetadata>
)
