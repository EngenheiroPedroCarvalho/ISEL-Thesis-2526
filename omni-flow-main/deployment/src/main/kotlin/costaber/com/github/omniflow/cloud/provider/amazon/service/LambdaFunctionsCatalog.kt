package costaber.com.github.omniflow.cloud.provider.amazon.service

import costaber.com.github.omniflow.registry.CloudFunctionsCatalog
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.LambdaException
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest

/**
 * Catalog that builds a registry from AWS Lambda functions already deployed across every region
 * enabled for the account.
 *
 * A Lambda function name is only unique within a region, so - like Cloud Run's
 * [costaber.com.github.omniflow.cloud.provider.google.service.CloudRunV2RestCatalog] - a function
 * name seen in more than one region gets its registry key promoted from the bare name to
 * "region/name" for both occurrences.
 */
class LambdaFunctionsCatalog(
    private val credentialsProvider: AwsCredentialsProvider = EnvironmentVariableCredentialsProvider.create(),
    private val regionsLister: AwsRegionsLister = Ec2AwsRegionsLister()
) : CloudFunctionsCatalog {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    private data class FunctionInfo(
        val region: String,
        val functionName: String,
        val arn: String
    )

    /**
     * @param scope the preferred/bootstrap AWS region (e.g. "eu-west-1"), tried first and used to
     * anchor the region-discovery call. Every other enabled region is scanned too.
     */
    override fun listHttpFunctions(scope: String): Map<String, FunctionInvocationMetadata> {
        val preferred = scope.trim().takeIf { it.isNotBlank() }
        val allRegions = regionsLister.listRegions(bootstrapRegion = preferred ?: "us-east-1")
        val regionsToQuery = if (preferred == null) allRegions
        else listOf(preferred) + allRegions.filterNot { it == preferred }

        val result = linkedMapOf<String, FunctionInvocationMetadata>()

        // Used to detect duplicates and "upgrade" keys to region/functionName
        val firstRegionByFunctionName = mutableMapOf<String, String>()

        var anyRegionSucceeded = false
        val failures = mutableListOf<String>()

        for (region in regionsToQuery) {
            try {
                val functions = listFunctionsInRegion(region)
                anyRegionSucceeded = true

                for (fn in functions) {
                    val name = fn.functionName

                    if (!firstRegionByFunctionName.containsKey(name)) {
                        firstRegionByFunctionName[name] = fn.region
                        result[name] = FunctionInvocationMetadata(serviceName = fn.functionName, url = fn.arn)
                        continue
                    }

                    val firstRegion = firstRegionByFunctionName.getValue(name)

                    val existing = result.remove(name)
                    if (existing != null) {
                        result["$firstRegion/$name"] = existing
                        logger.warn {
                            "Duplicate Lambda function name '$name' found. " +
                                    "Converted registry key '$name' -> '$firstRegion/$name'"
                        }
                    }

                    result["${fn.region}/$name"] = FunctionInvocationMetadata(serviceName = fn.functionName, url = fn.arn)
                }
            } catch (e: LambdaException) {
                failures.add("$region: ${e.message}")
                logger.warn { "Skipping AWS region '$region' during registry bootstrap: ${e.message}" }
            }
        }

        if (!anyRegionSucceeded) {
            throw IllegalStateException(
                "Unable to list Lambda functions in ANY region for this account. " +
                        "This usually means missing IAM permissions (need lambda:ListFunctions) " +
                        "or the account has no regions enabled. " +
                        "Sample failures: ${failures.take(3).joinToString(" | ")}"
            )
        }

        return result
    }

    private fun listFunctionsInRegion(region: String): List<FunctionInfo> =
        LambdaClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build()
            .use { client ->
                val out = mutableListOf<FunctionInfo>()
                var marker: String? = null
                do {
                    val response = client.listFunctions(
                        ListFunctionsRequest.builder()
                            .marker(marker)
                            .build()
                    )
                    response.functions().forEach { fn ->
                        out.add(FunctionInfo(region = region, functionName = fn.functionName(), arn = fn.functionArn()))
                    }
                    marker = response.nextMarker()
                } while (!marker.isNullOrBlank())
                out
            }
}
