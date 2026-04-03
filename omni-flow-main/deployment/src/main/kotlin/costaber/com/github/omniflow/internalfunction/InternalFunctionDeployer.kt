package costaber.com.github.omniflow.internalfunction

import costaber.com.github.omniflow.registry.FunctionInvocationMetadata

interface InternalFunctionDeployer {
    fun deployOrUpdate(functionName: String, deploymentDescriptorPath: String): FunctionInvocationMetadata
}

object NoopInternalFunctionDeployer: InternalFunctionDeployer{
    override fun deployOrUpdate(functionName: String, deploymentDescriptorPath: String): FunctionInvocationMetadata {
        throw UnsupportedOperationException(
            "internalFunction('$functionName', '$deploymentDescriptorPath') requested deployment, " +
                    "but no InternalFunctionDeployer is configured (QuickFaaS integration missing)."
        )
    }
}