package costaber.com.github.omniflow.cloud.provider.google.service

import ch.qos.logback.core.subst.Token
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import costaber.com.github.omniflow.cloud.provider.google.auth.GoogleAccessTokenProvider
import costaber.com.github.omniflow.jackson.OmniflowObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * Lists Cloud Run locations (regions) visible to project
 */
class CloudRunLocationsV1RestClient(
    private val http: HttpClient = HttpClient.newHttpClient(),
    private val mapper: ObjectMapper = OmniflowObjectMapper.default,
    private val tokenProvider: GoogleAccessTokenProvider = GoogleAccessTokenProvider(),
){
    fun listProjectLocations(projectId: String): List<String> {
        val out = mutableListOf<String>()
        var pageToken: String? = null

        do {
            val url = buildUrl(projectId, pageToken)
            val json = getJson(url)

            val locations = json.path("locations")
            if(locations.isArray){
                for (loc in locations){
                    val locationId = loc.path("locationId").asText("").trim()
                    if (locationId.isNotEmpty()) out.add(locationId)
                }
            }

            pageToken = json.path("nextPageToken").asText("").takeIf { it.isNotBlank() }
        } while (pageToken != null)

        return out.distinct()
    }

    private fun buildUrl(projectId: String, pageToken: String?): String {
        val base = "https://run.googleapis.com/v1/projects/$projectId/locations?pageSize=1000"
        return if(pageToken == null) base
        else base + "&pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8)
    }

    private fun getJson(url: String): JsonNode {
        val token = tokenProvider.getTokenValue()
        val req = HttpRequest.newBuilder()
            .uri(URI(url))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .GET()
            .build()

        val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
        if(resp.statusCode() !in 200..299) {
            throw IllegalStateException(
                "Cloud Run Locations API call failed (${resp.statusCode()}): ${resp.body()}"
            )
        }
        return mapper.readTree(resp.body())
    }
}