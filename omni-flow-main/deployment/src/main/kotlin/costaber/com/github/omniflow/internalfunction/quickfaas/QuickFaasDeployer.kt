package costaber.com.github.omniflow.internalfunction.quickfaas

import costaber.com.github.omniflow.internalfunction.InternalFunctionDeployer
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import mu.KotlinLogging
import java.nio.file.Path

class QuickFaasDeployer(
    private val quickFaasJarPath: Path,
    private val projectId: String,
    private val region: String
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

        val resolvedProject = descriptor.project ?: projectId
        val resolvedRegion = descriptor.function?.location ?: region
        val url = buildFirstGenFunctionUrl(resolvedRegion, resolvedProject, functionName)

        logger.info {
            "QuickFaaS deployment completed for '$functionName'. " +
                    "Registered URL: $url (Cloud Functions 1st gen creation is asynchronous; " +
                    "the function may take ~1 minute to become reachable)."
        }

        return FunctionInvocationMetadata(serviceName = functionName, url = url)
    }

    private fun buildFirstGenFunctionUrl(region: String, projectId: String, functionName: String): String =
        "https://$region-$projectId.cloudfunctions.net/$functionName"
}
