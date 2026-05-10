package costaber.com.github.omniflow.internalfunction.quickfaas

import com.fasterxml.jackson.databind.ObjectMapper
import costaber.com.github.omniflow.cloud.provider.google.auth.GoogleAccessTokenProvider
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class CloudFunctionsIamHelper(
    private val tokenProvider: GoogleAccessTokenProvider = GoogleAccessTokenProvider(),
    private val http: HttpClient = HttpClient.newHttpClient(),
    private val mapper: ObjectMapper = ObjectMapper()
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val INVOKER_ROLE = "roles/cloudfunctions.invoker"
    }

    fun waitForFunctionActive(
        projectId: String,
        location: String,
        functionName: String,
        maxWaitSeconds: Long = 180,
        pollIntervalSeconds: Long = 10
    ): Boolean {
        val apiUrl = "https://cloudfunctions.googleapis.com/v1/projects/$projectId/locations/$location/functions/$functionName"
        val deadline = System.currentTimeMillis() + maxWaitSeconds * 1000

        logger.info { "Waiting for function '$functionName' to become ACTIVE (timeout: ${maxWaitSeconds}s)..." }

        while (System.currentTimeMillis() < deadline) {
            try {
                val response = authenticatedGet(apiUrl)
                when (response.statusCode()) {
                    200 -> {
                        val json = mapper.readTree(response.body())
                        val status = json.path("status").asText("")
                        when (status) {
                            "ACTIVE" -> {
                                logger.info { "Function '$functionName' is ACTIVE." }
                                return true
                            }
                            "DEPLOY_FAILURE", "DELETE_FAILURE" -> {
                                logger.error { "Function '$functionName' reached terminal status: $status" }
                                return false
                            }
                            else -> logger.info { "Function '$functionName' status: $status — waiting..." }
                        }
                    }
                    404 -> logger.info { "Function '$functionName' not found yet — waiting..." }
                    else -> logger.warn { "Unexpected status ${response.statusCode()} checking '$functionName'" }
                }
            } catch (e: Exception) {
                logger.warn { "Error checking function status: ${e.message} — retrying..." }
            }
            Thread.sleep(pollIntervalSeconds * 1000)
        }

        logger.warn { "Timed out waiting for function '$functionName' to become ACTIVE." }
        return false
    }

    fun makePubliclyInvocable(projectId: String, location: String, functionName: String) {
        val resource = "projects/$projectId/locations/$location/functions/$functionName"
        val policyBody = mapOf(
            "policy" to mapOf(
                "bindings" to listOf(
                    mapOf(
                        "role" to INVOKER_ROLE,
                        "members" to listOf("allUsers")
                    )
                )
            )
        )

        logger.info { "Setting IAM policy: allUsers -> $INVOKER_ROLE on '$functionName'" }

        try {
            setIamPolicy(resource, policyBody)
            logger.info { "IAM policy set successfully on '$functionName'." }
        } catch (e: Exception) {
            logger.warn { "Failed to set IAM policy on first attempt: ${e.message}. Retrying in 5s..." }
            Thread.sleep(5000)
            setIamPolicy(resource, policyBody)
            logger.info { "IAM policy set successfully on retry." }
        }
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
                "Failed to set IAM policy on '$resource' (${response.statusCode()}): ${response.body()}"
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
}
