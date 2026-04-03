package costaber.com.github.omniflow.model

/**
 * Internal function call reference.
 *
 * - name: logical identifier used inside the workflow DSL
 * - deploymentDescriptorPath: optional descriptor path (when provided, workflow generation may trigger QuickFaaS)
 */
data class InternalFunction(
    val name: String,
    val deploymentDescriptorPath: String? = null
)
