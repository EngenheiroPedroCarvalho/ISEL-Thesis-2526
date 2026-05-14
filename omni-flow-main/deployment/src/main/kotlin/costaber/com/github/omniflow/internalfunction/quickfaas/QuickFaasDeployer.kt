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
    private val invokerServiceAccount: String? = null,
    private val readinessTimeoutSeconds: Long = 180,
    private val readinessPollIntervalSeconds: Long = 5
) : InternalFunctionDeployer {

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val INVOKER_ROLE = "roles/cloudfunctions.invoker"
        private const val RESET   = "[0m"
        private const val BOLD    = "[1m"
        private const val GREEN   = "[32m"
        private const val YELLOW  = "[33m"
        private const val BLUE    = "[34m"
        private const val CYAN    = "[36m"
        private const val RED     = "[31m"
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

        println("$BLUE  →$RESET Loading deployment descriptor from '$descriptorPath'...")
        val descriptor = QuickFaasDescriptorLoader.load(descriptorPath)
        QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
        println("$GREEN  ✓$RESET Descriptor validated (provider=${descriptor.cloudProvider}, runtime=${descriptor.function?.runtime})")

        val freshToken = try {
            tokenProvider.getTokenValue()
        } catch (e: Exception) {
            println("$YELLOW  !$RESET Could not obtain ADC token: ${e.message}. QuickFaaS will use descriptor token.")
            logger.warn { "Could not obtain ADC token: ${e.message}. QuickFaaS will use whatever is in the descriptor." }
            null
        }

        println("$BLUE  →$RESET Invoking QuickFaaS subprocess to deploy '$BOLD$functionName$RESET'...")
        val invoker = QuickFaasProcessInvoker(quickFaasJarPath)
        invoker.invoke(descriptorPath, accessToken = freshToken)
        println("$GREEN  ✓$RESET QuickFaaS subprocess completed for '$functionName'")

        val resolvedProject = descriptor.project ?: projectId
        val resolvedRegion = descriptor.function?.location ?: region
        val url = buildFirstGenFunctionUrl(resolvedRegion, resolvedProject, functionName)

        println("$BLUE  →$RESET Polling Cloud Functions API — waiting for '$functionName' to become ACTIVE...")
        waitForFunctionReady(resolvedProject, resolvedRegion, functionName)
        println("$GREEN  ✓$RESET Function '$functionName' is ACTIVE")

        if (invokerServiceAccount != null) {
            println("$BLUE  →$RESET Granting $INVOKER_ROLE to service account on '$functionName'...")
            grantInvokerPermission(resolvedProject, resolvedRegion, functionName, invokerServiceAccount)
        }

        logger.info { "QuickFaaS deployment completed for '$functionName'. Registered URL: $url" }

        return FunctionInvocationMetadata(serviceName = functionName, url = url)
    }

    private fun waitForFunctionReady(projectId: String, region: String, functionName: String) {
        val apiUrl = cloudFunctionsApiUrl(projectId, region, functionName)
        val deadline = System.currentTimeMillis() + readinessTimeoutSeconds * 1000

        logger.info { "Waiting for function '$functionName' to become ACTIVE (timeout: ${readinessTimeoutSeconds}s)..." }

        while (System.currentTimeMillis() < deadline) {
            try {
                val response = authenticatedGet(apiUrl)

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

    private fun grantInvokerPermission(
        projectId: String,
        region: String,
        functionName: String,
        serviceAccountEmail: String
    ) {
        val resource = "projects/$projectId/locations/$region/functions/$functionName"
        val member = "serviceAccount:$serviceAccountEmail"

        logger.info { "Granting $INVOKER_ROLE to '$serviceAccountEmail' on '$functionName'..." }

        val currentPolicy = try {
            getIamPolicy(resource)
        } catch (e: Exception) {
            logManualIamCommand(functionName, region, serviceAccountEmail, e)
            return
        }

        val bindings = currentPolicy.path("bindings")

        val alreadyGranted = bindings?.any { binding ->
            binding.path("role").asText("") == INVOKER_ROLE &&
                    binding.path("members").any { it.asText("") == member }
        } ?: false

        if (alreadyGranted) {
            println("$GREEN  ✓$RESET Service account already has $INVOKER_ROLE on '$functionName' — skipping")
            logger.info { "Service account already has $INVOKER_ROLE on '$functionName' — skipping." }
            return
        }

        val updatedBindings = mutableListOf<Map<String, Any>>()
        var invokerBindingUpdated = false

        bindings?.forEach { binding ->
            val role = binding.path("role").asText("")
            val members = binding.path("members").map { it.asText("") }.toMutableList()
            if (role == INVOKER_ROLE) {
                members.add(member)
                invokerBindingUpdated = true
            }
            updatedBindings.add(mapOf("role" to role, "members" to members))
        }

        if (!invokerBindingUpdated) {
            updatedBindings.add(mapOf("role" to INVOKER_ROLE, "members" to listOf(member)))
        }

        val policyBody = mapOf("policy" to mapOf("bindings" to updatedBindings))

        try {
            setIamPolicy(resource, policyBody)
        } catch (e: Exception) {
            logManualIamCommand(functionName, region, serviceAccountEmail, e)
            return
        }

        println("$GREEN  ✓$RESET Granted $INVOKER_ROLE to service account on '$functionName'")
        println("$BLUE  →$RESET Waiting 10s for IAM propagation...")
        logger.info { "Granted $INVOKER_ROLE to '$serviceAccountEmail' on '$functionName'. Waiting for IAM propagation..." }
        Thread.sleep(10_000)
        println("$GREEN  ✓$RESET IAM propagation wait complete")
        logger.info { "IAM propagation wait complete." }
    }

    private fun logManualIamCommand(
        functionName: String,
        region: String,
        serviceAccountEmail: String,
        cause: Exception
    ) {
        println("$YELLOW  !$RESET Could not auto-grant invoker permission: ${cause.message}")
        println("$YELLOW  !$RESET Run this command manually before executing the workflow:")
        println("    gcloud functions add-invoker-policy-binding $functionName \\")
        println("      --region=$region \\")
        println("      --member=\"serviceAccount:$serviceAccountEmail\"")
        logger.warn {
            "Could not auto-grant invoker permission: ${cause.message}\n" +
                    "  Run this command manually before executing the workflow:\n" +
                    "  gcloud functions add-invoker-policy-binding $functionName \\\n" +
                    "    --region=$region \\\n" +
                    "    --member=\"serviceAccount:$serviceAccountEmail\""
        }
    }

    private fun getIamPolicy(resource: String): com.fasterxml.jackson.databind.JsonNode {
        val url = "https://cloudfunctions.googleapis.com/v1/$resource:getIamPolicy"
        val response = authenticatedGet(url)
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException(
                "Failed to get IAM policy for '$resource' (${response.statusCode()}): ${response.body()}"
            )
        }
        return mapper.readTree(response.body())
    }

    private fun setIamPolicy(resource: String, policyBody: Map<String, Any>) {
        val url = "https://cloudfunctions.googleapis.com/v1/$resource:setIamPolicy"
        val token = tokenProvider.getTokenValue()
        val body = mapper.writeValueAsString(policyBody)
        val request = HttpRequest.newBuilder()
            .uri(URI(url))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException(
                "Failed to set IAM policy for '$resource' (${response.statusCode()}): ${response.body()}"
            )
        }
    }

    private fun authenticatedGet(url: String): HttpResponse<String> {
        val token = tokenProvider.getTokenValue()
        val request = HttpRequest.newBuilder()
            .uri(URI(url))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .GET()
            .build()
        return http.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun cloudFunctionsApiUrl(projectId: String, region: String, functionName: String): String =
        "https://cloudfunctions.googleapis.com/v1/projects/$projectId/locations/$region/functions/$functionName"

    private fun buildFirstGenFunctionUrl(region: String, projectId: String, functionName: String): String =
        "https://$region-$projectId.cloudfunctions.net/$functionName"
}
