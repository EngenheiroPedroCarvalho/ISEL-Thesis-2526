package costaber.com.github.omniflow.internalfunction.quickfaas

import com.fasterxml.jackson.databind.ObjectMapper
import costaber.com.github.omniflow.cloud.provider.google.auth.GoogleAccessTokenProvider
import costaber.com.github.omniflow.internalfunction.InternalFunctionDeployer
import costaber.com.github.omniflow.jackson.OmniflowObjectMapper
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path

class QuickFaasDeployer(
    private val quickFaasJarPath: Path,
    private val projectId: String,
    private val region: String,
    private val readinessTimeoutSeconds: Long = 180,
    private val readinessPollIntervalSeconds: Long = 5
) : InternalFunctionDeployer {

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val http: HttpClient = HttpClient.newHttpClient()
    private val tokenProvider: GoogleAccessTokenProvider = GoogleAccessTokenProvider()
    private val mapper: ObjectMapper = OmniflowObjectMapper.default

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

        waitForFunctionReady(resolvedProject, resolvedRegion, functionName)

        logger.info { "QuickFaaS deployment completed for '$functionName'. Registered URL: $url" }

        return FunctionInvocationMetadata(serviceName = functionName, url = url)
    }

    private fun waitForFunctionReady(projectId: String, region: String, functionName: String) {
        val apiUrl = "https://cloudfunctions.googleapis.com/v1/projects/$projectId/locations/$region/functions/$functionName"
        val deadline = System.currentTimeMillis() + readinessTimeoutSeconds * 1000

        logger.info { "Waiting for function '$functionName' to become ACTIVE (timeout: ${readinessTimeoutSeconds}s)..." }

        while (System.currentTimeMillis() < deadline) {
            try {
                val token = tokenProvider.getTokenValue()
                val request = HttpRequest.newBuilder()
                    .uri(URI(apiUrl))
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .GET()
                    .build()

                val response = http.send(request, HttpResponse.BodyHandlers.ofString())

                when (response.statusCode()) {
                    200 -> {
                        val json = mapper.readTree(response.body())
                        val status = json.path("status").asText("")
                        when (status) {
                            "ACTIVE" -> {
                                logger.info { "Function '$functionName' is ACTIVE and ready." }
                                return
                            }
                            "DEPLOY_FAILURE", "DELETE_FAILURE" -> {
                                throw IllegalStateException(
                                    "Function '$functionName' reached terminal status: $status"
                                )
                            }
                            else -> {
                                logger.info { "Function '$functionName' status: $status — waiting..." }
                            }
                        }
                    }
                    404 -> logger.info { "Function '$functionName' not found yet — waiting..." }
                    else -> logger.warn { "Unexpected status ${response.statusCode()} checking '$functionName' — waiting..." }
                }
            } catch (e: IllegalStateException) {
                throw e
            } catch (e: Exception) {
                logger.warn { "Error checking function readiness: ${e.message} — retrying..." }
            }

            Thread.sleep(readinessPollIntervalSeconds * 1000)
        }

        throw IllegalStateException(
            "Timed out after ${readinessTimeoutSeconds}s waiting for function '$functionName' to become ACTIVE. " +
                    "The function may still be deploying — check the Cloud Functions console."
        )
    }

    private fun buildFirstGenFunctionUrl(region: String, projectId: String, functionName: String): String =
        "https://$region-$projectId.cloudfunctions.net/$functionName"
}
