package costaber.com.github.omniflow.internalfunction.quickfaas

import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunV2ServiceInspector
import costaber.com.github.omniflow.internalfunction.InternalFunctionDeployer
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import mu.KotlinLogging
import java.nio.file.Path

class QuickFaasDeployer(
    private val quickFaasJarPath: Path,
    private val projectId: String,
    private val region: String,
    private val inspector: CloudRunV2ServiceInspector = CloudRunV2ServiceInspector()
) : InternalFunctionDeployer {

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun deployOrUpdate(
        functionName: String,
        deploymentDescriptorPath: String
    ): FunctionInvocationMetadata {
        val descriptorPath = Path.of(deploymentDescriptorPath)

        logger.info { "Deploying internal function '$functionName' via QuickFaaS (descriptor=$descriptorPath)" }

        val descriptor = QuickFaasDescriptorLoader.load(descriptorPath)

        QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")

        val invoker = QuickFaasProcessInvoker(quickFaasJarPath)
        invoker.invoke(descriptorPath)

        logger.info { "QuickFaaS deployment completed for '$functionName'. Discovering URL via Cloud Run..." }

        val lookupRegion = descriptor.function?.location ?: region

        return discoverDeployedFunction(functionName, lookupRegion)
    }

    private fun discoverDeployedFunction(
        functionName: String,
        region: String
    ): FunctionInvocationMetadata {
        return when (val result = inspector.lookup(projectId, region, functionName)) {
            is CloudRunV2ServiceInspector.LookupResult.Found ->
                FunctionInvocationMetadata(
                    serviceName = result.serviceName,
                    url = result.url
                )

            CloudRunV2ServiceInspector.LookupResult.NotFound ->
                throw IllegalStateException(
                    "QuickFaaS reported success but function '$functionName' was not found " +
                            "in Cloud Run (project=$projectId, region=$region). " +
                            "Check QuickFaaS logs and verify the function name matches."
                )

            is CloudRunV2ServiceInspector.LookupResult.Forbidden ->
                throw IllegalStateException(
                    "QuickFaaS deployed '$functionName' but Cloud Run inspection is forbidden: ${result.message}"
                )
        }
    }
}
