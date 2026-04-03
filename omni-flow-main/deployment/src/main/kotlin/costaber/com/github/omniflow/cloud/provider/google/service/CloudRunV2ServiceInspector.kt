package costaber.com.github.omniflow.cloud.provider.google.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.Message
import costaber.com.github.omniflow.cloud.provider.google.auth.GoogleAccessTokenProvider
import costaber.com.github.omniflow.jackson.OmniflowObjectMapper
import software.amazon.awssdk.regions.Region
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Validates existance of a Cloud Run service and returns its current endpoint.
 *
 * Uses:
 *  GET https://run.googleapis.com/v2/{name}
 * where name = projects/{project}/locations/{region}/services/{serviceId}
 */
class CloudRunV2ServiceInspector(
    private val http: HttpClient = HttpClient.newHttpClient(),
    private val mapper: ObjectMapper = OmniflowObjectMapper.default,
    private val tokenProvider: GoogleAccessTokenProvider = GoogleAccessTokenProvider()
) {
    sealed class LookupResult{
        data class Found(val serviceName: String, val url: String): LookupResult()
        object NotFound: LookupResult()
        data class Forbidden(val message: String): LookupResult()
    }


    fun lookupByServiceName(serviceName: String): LookupResult {
        val normalized = serviceName.trim().removePrefix("/")
        val url = "https://run.googleapis.com/v2/$normalized"

        val token = tokenProvider.getTokenValue()
        val req = HttpRequest.newBuilder()
            .uri(URI(url))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .GET()
            .build()

        val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
        return when (resp.statusCode()){
            in 200..209 -> {
                val json = mapper.readTree(resp.body())

                val name = json.path("name").asText("").trim()
                val uri = json.path("uri").asText("").trim()
                if (uri.isNotBlank()) return LookupResult.Found(name, uri)

                //Some responses may include urls[]
                val urls = json.path("urls")
                if (urls.isArray && urls.size() > 0) {
                    val u = urls[0].asText("").trim()
                    if(u.isNotBlank()) return LookupResult.Found(name, u)
                }

                LookupResult.Forbidden("Service exists but response has no 'uri'/'urls' field")
            }
            404 -> LookupResult.NotFound
            403 -> LookupResult.Forbidden(
                "Permission denied calling Cloud Run sevices.get for '$normalized'. " +
                        "Ensure the deployer identity has run.services.get."
            )
            else -> throw IllegalStateException(
                "Cloud Run services.get failed (${resp.statusCode()}): ${resp.body()}"
            )
        }
    }

    fun lookup(projectId: String, region: String, serviceId: String): LookupResult {
        val name = "projects/$projectId/locations/$region/services/$serviceId"
        return lookupByServiceName(name)
    }
}