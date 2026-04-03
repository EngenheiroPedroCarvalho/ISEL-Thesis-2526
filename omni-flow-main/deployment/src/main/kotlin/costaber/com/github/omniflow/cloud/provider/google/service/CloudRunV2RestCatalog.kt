package costaber.com.github.omniflow.cloud.provider.google.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import costaber.com.github.omniflow.cloud.provider.google.auth.GoogleAccessTokenProvider
import costaber.com.github.omniflow.jackson.OmniflowObjectMapper
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import mu.KotlinLogging
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets


/**
 * Catalog that builds a registry from Cloud Run services (Cloud Run functions are deployed as services)
 */
class CloudRunV2RestCatalog(
    private val http: HttpClient = HttpClient.newHttpClient(),
    private val mapper: ObjectMapper = OmniflowObjectMapper.default,
    private val tokenProvider: GoogleAccessTokenProvider = GoogleAccessTokenProvider(),
    private val locationsClient: CloudRunLocationsV1RestClient = CloudRunLocationsV1RestClient(
        http = http,
        mapper = mapper,
        tokenProvider = tokenProvider
    )
) {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    private data class ServiceInfo(
        val region: String,
        val serviceId: String,
        val serviceName: String,
        val url: String
    )

    /**
     * Lists Cloud Run Services and returns them as registry metadata
     *
     * key:
     *  - if unique by serviceId -> "serviceId"
     *  - if duplicates exist in different regions -> both become "region/serviceId"
     *
     * value:
     *  - FunctionInvocationMetadata(serviceName, url)
     *
     */

    fun listHttpServices(projectId: String, regions: List<String>? = null): Map<String, FunctionInvocationMetadata> {
        val regionsToQuery = normalizeRegions(projectId, regions)

        val result = linkedMapOf<String, FunctionInvocationMetadata>()

        //Used to detect duplicates and "upgrade" keys to region/serviceId
        val firstRegionByServiceId = mutableMapOf<String, String>()

        var anyRegionSucceeded = false
        val failures = mutableListOf<String>()

        for (region in regionsToQuery) {
            try {
                val services = listServicesInRegion(projectId, region)
                anyRegionSucceeded = true

                for (svc in services) {
                    val serviceId = svc.serviceId

                    // First time we see this serviceId
                    if (!firstRegionByServiceId.containsKey(serviceId)) {
                        firstRegionByServiceId[serviceId] = svc.region
                        result[serviceId] = FunctionInvocationMetadata(
                            serviceName = svc.serviceName,
                            url = svc.url
                        )
                        continue
                    }

                    //Duplicate serviceId found in another region
                    val firstRegion = firstRegionByServiceId.getValue(serviceId)

                    //If we still have the plain key, rename it to region/serviceId
                    val existing = result.remove(serviceId)
                    if (existing != null) {
                        result["$firstRegion/$serviceId"] = existing
                        logger.warn {
                            "Duplicate Cloud Run serviceId '$serviceId' found. " +
                                    "Converted registry key '$serviceId' -> '$firstRegion/$serviceId"
                        }
                    }

                    //Store current one as region/serviceId too
                    result["${svc.region}/$serviceId"] = FunctionInvocationMetadata(
                        serviceName = svc.serviceName,
                        url = svc.url
                    )
                }
            } catch (e: RegionNotAccessibleException) {
                failures.add("${e.region} (HTTP ${e.statusCode}): ${e.message}")
                logger.warn { "Skipping Cloud Run region '${e.region}' during registry bootstrap: ${e.message}" }
            }
        }

        if (!anyRegionSucceeded) {
            throw IllegalStateException(
                "Unable to list Cloud Run services in ANY region for project '$projectId'." +
                        "This usually means missing IAM permissions (need run.services.list; roles/run.admin)" +
                        "or an organization policy restricting allowed locations. " +
                        "Sample failures: ${failures.take(3).joinToString(" | ")}"
            )
        }

        return result
    }

    private fun normalizeRegions(projectId: String, regions: List<String>?): List<String> {
        val provided = regions
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }

        if (provided != null) return provided

        //Discover Regions from Cloud Run locations API
        return locationsClient.listProjectLocations(projectId)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun listServicesInRegion(projectId: String, region: String): List<ServiceInfo> {
        val out = mutableListOf<ServiceInfo>()
        var pageToken: String? = null

        do {
            val url = buildListServicesUrl(projectId, region, pageToken)
            val json = getJsonOrThrow(url, region)

            val services = json.path("services")
            if (services.isArray) {
                for (svc in services) {
                    val info = parseServiceNode(svc, region)
                    if (info != null) out.add(info)
                }
            }

            pageToken = json.path("nextPageToken").asText("").takeIf { it.isNotBlank() }
        } while (pageToken != null)

        return out
    }

    private fun parseServiceNode(node: JsonNode, region: String): ServiceInfo? {
        val fullName = node.path("name").asText("").trim()
        if (fullName.isBlank()) return null

        val serviceId = fullName.substringAfterLast("/").trim()
        if (serviceId.isBlank()) return null

        //Cloud Run Service has "uri" as main URL serving traffic.
        val uri = node.path("uri").asText("").trim().ifBlank {
            //Some responses may also include urls[]
            val urls = node.path("urls")
            if (urls.isArray && urls.size() > 0) urls[0].asText("").trim() else ""
        }

        if (uri.isBlank()) return null

        return ServiceInfo(
            region = region,
            serviceId = serviceId,
            serviceName = fullName,
            url = uri
        )
    }

    fun buildListServicesUrl(projectId: String, region: String, pageToken: String?): String {
        val base = "https://run.googleapis.com/v2/projects/$projectId/locations/$region/services?pageSize=1000"
        return if (pageToken == null) base
        else base + "&pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8)
    }

    fun getJsonOrThrow(url: String, region: String): JsonNode {
        val token = tokenProvider.getTokenValue()
        val req = HttpRequest.newBuilder()
            .uri(URI(url))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .GET()
            .build()

        val resp = http.send(req, HttpResponse.BodyHandlers.ofString())

        when (resp.statusCode()) {
            in 200..299 -> return mapper.readTree(resp.body())
            403, 404 -> throw RegionNotAccessibleException(
                region = region,
                statusCode = resp.statusCode(),
                message = resp.body().take(300).ifBlank {"Permission denied or region not available. HTTP ${resp.statusCode()} - ${resp.body().take(200)}"}
            )

            else -> throw IllegalStateException(
                "Cloud Run Services API call failed (${resp.statusCode()}): ${resp.body()}"
            )
        }
        if (resp.statusCode() !in 200..299) {
            throw IllegalStateException(
                "Cloud Functions API call failed (${resp.statusCode()}): ${resp.body()}"
            )
        }
    }
}

private class RegionNotAccessibleException(
    val region: String,
    val statusCode: Int,
    message: String
): RuntimeException(message)
