package costaber.com.github.omniflow.cloud.provider.amazon.service

import costaber.com.github.omniflow.registry.CloudFunctionsCatalog
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest

/**
 * Catalog that builds a registry from AWS Lambda functions already deployed in a region.
 *
 * Unlike Cloud Run, a Lambda function name is unique within a region, so no cross-region
 * disambiguation is needed: the registry key is simply the function name.
 */
class LambdaFunctionsCatalog(
    private val credentialsProvider: AwsCredentialsProvider = EnvironmentVariableCredentialsProvider.create()
) : CloudFunctionsCatalog {

    /**
     * @param scope the AWS region to list functions in (e.g. "eu-west-1")
     */
    override fun listHttpFunctions(scope: String): Map<String, FunctionInvocationMetadata> {
        LambdaClient.builder()
            .region(Region.of(scope))
            .credentialsProvider(credentialsProvider)
            .build()
            .use { client ->
                val result = linkedMapOf<String, FunctionInvocationMetadata>()
                var marker: String? = null
                do {
                    val response = client.listFunctions(
                        ListFunctionsRequest.builder()
                            .marker(marker)
                            .build()
                    )
                    response.functions().forEach { fn ->
                        result[fn.functionName()] = FunctionInvocationMetadata(
                            serviceName = fn.functionName(),
                            url = fn.functionArn()
                        )
                    }
                    marker = response.nextMarker()
                } while (!marker.isNullOrBlank())
                return result
            }
    }
}
