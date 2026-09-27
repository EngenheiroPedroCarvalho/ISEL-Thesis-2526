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

        // 2nd gen functions run on Cloud Run, which is where invocation is enforced.
        private const val INVOKER_ROLE = "roles/run.invoker"
        private const val FUNCTIONS_API = "https://cloudfunctions.googleapis.com/v2"
        private const val CLOUD_RUN_API = "https://run.googleapis.com/v2"
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

        println("$BLUE  →$RESET Polling Cloud Functions API — waiting for '$functionName' to become ACTIVE...")
        // A 'region/name' reference names the function by what follows the slash.
        val deployed = waitForFunctionReady(resolvedProject, resolvedRegion, functionName.substringAfterLast('/'))
        println("$GREEN  ✓$RESET Function '$functionName' is ACTIVE")

        if (invokerServiceAccount != null) {
            println("$BLUE  →$RESET Granting $INVOKER_ROLE to service account on '$functionName'...")
            grantInvokerPermission(deployed.service, functionName, resolvedRegion, invokerServiceAccount)
        }

        logger.info { "QuickFaaS deployment completed for '$functionName'. Registered URL: ${deployed.uri}" }

        return FunctionInvocationMetadata(serviceName = deployed.serviceName, url = deployed.uri)
    }

    /**
     * A deployed 2nd gen function: its generated 'run.app' [uri] and the resource name of the
     * Cloud Run [service] backing it, both output-only fields of the Cloud Functions v2 API.
     */
    private data class DeployedFunction(val uri: String, val service: String) {
        val serviceName: String get() = service.substringAfterLast('/')
    }

    private fun waitForFunctionReady(projectId: String, region: String, functionName: String): DeployedFunction {
        val apiUrl = cloudFunctionsApiUrl(projectId, region, functionName)
        val deadline = System.currentTimeMillis() + readinessTimeoutSeconds * 1000

        logger.info { "Waiting for function '$functionName' to become ACTIVE (timeout: ${readinessTimeoutSeconds}s)..." }

        while (System.currentTimeMillis() < deadline) {
            try {
                val response = authenticatedGet(apiUrl)

                when (response.statusCode()) {
                    200 -> {
                        val json = mapper.readTree(response.body())
                        val state = json.path("state").asText("")
                        when (state) {
                            "ACTIVE" -> {
                                logger.info { "Function '$functionName' is ACTIVE and ready." }
                                val serviceConfig = json.path("serviceConfig")
                                val uri = serviceConfig.path("uri").asText("")
                                if (uri.isBlank()) {
                                    throw IllegalStateException(
                                        "Function '$functionName' is ACTIVE but the Cloud Functions v2 API " +
                                                "reported no 'serviceConfig.uri' to bind the workflow to."
                                    )
                                }
                                return DeployedFunction(
                                    uri = uri,
                                    service = serviceConfig.path("service").asText("")
                                )
                            }
                            "FAILED" -> {
                                throw IllegalStateException(
                                    "Function '$functionName' reached terminal state: $state"
                                )
                            }
                            else -> {
                                logger.info { "Function '$functionName' state: $state — waiting..." }
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
        service: String,
        functionName: String,
        region: String,
        serviceAccountEmail: String
    ) {
        val member = "serviceAccount:$serviceAccountEmail"

        if (service.isBlank()) {
            logManualIamCommand(
                functionName, region, serviceAccountEmail,
                IllegalStateException("the API reported no Cloud Run service backing '$functionName'")
            )
            return
        }

        logger.info { "Granting $INVOKER_ROLE to '$serviceAccountEmail' on '$functionName'..." }

        val currentPolicy = try {
            getIamPolicy(service)
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
            setIamPolicy(service, policyBody)
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
        println("    gcloud run services add-iam-policy-binding $functionName \\")
        println("      --region=$region \\")
        println("      --member=\"serviceAccount:$serviceAccountEmail\" \\")
        println("      --role=\"$INVOKER_ROLE\"")
        logger.warn {
            "Could not auto-grant invoker permission: ${cause.message}\n" +
                    "  Run this command manually before executing the workflow:\n" +
                    "  gcloud run services add-iam-policy-binding $functionName \\\n" +
                    "    --region=$region \\\n" +
                    "    --member=\"serviceAccount:$serviceAccountEmail\" \\\n" +
                    "    --role=\"$INVOKER_ROLE\""
        }
    }

    private fun getIamPolicy(resource: String): com.fasterxml.jackson.databind.JsonNode {
        val url = "$CLOUD_RUN_API/$resource:getIamPolicy"
        val response = authenticatedGet(url)
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException(
                "Failed to get IAM policy for '$resource' (${response.statusCode()}): ${response.body()}"
            )
        }
        return mapper.readTree(response.body())
    }

    private fun setIamPolicy(resource: String, policyBody: Map<String, Any>) {
        val url = "$CLOUD_RUN_API/$resource:setIamPolicy"
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
        "$FUNCTIONS_API/projects/$projectId/locations/$region/functions/$functionName"
}
