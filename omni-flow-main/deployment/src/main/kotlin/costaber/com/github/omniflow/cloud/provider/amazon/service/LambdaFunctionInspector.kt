package costaber.com.github.omniflow.cloud.provider.amazon.service

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.LambdaException
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException

/**
 * Validates existence of a Lambda function and returns its current ARN.
 *
 * AWS counterpart of the Cloud Run inspector used on the GCP path. Declared as an interface so
 * that resolution logic can be exercised without reaching the Lambda API.
 */
interface LambdaFunctionInspector {

    sealed class LookupResult {
        data class Found(val functionName: String, val arn: String) : LookupResult()
        object NotFound : LookupResult()
        data class Forbidden(val message: String) : LookupResult()
    }

    fun lookupByFunctionName(functionName: String): LookupResult
}

/**
 * Lambda-backed implementation.
 *
 * Uses:
 *  GetFunction(functionName)
 * in the configured region. The client is built per call so that constructing this class never
 * touches AWS credentials.
 */
class AwsLambdaFunctionInspector(
    private val region: String
) : LambdaFunctionInspector {

    override fun lookupByFunctionName(functionName: String): LambdaFunctionInspector.LookupResult =
        try {
            LambdaClient.builder()
                .region(Region.of(region))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()
                .use { client ->
                    val configuration = client.getFunction(
                        GetFunctionRequest.builder().functionName(functionName).build()
                    ).configuration()
                    LambdaFunctionInspector.LookupResult.Found(
                        functionName = configuration.functionName(),
                        arn = configuration.functionArn()
                    )
                }
        } catch (e: ResourceNotFoundException) {
            LambdaFunctionInspector.LookupResult.NotFound
        } catch (e: LambdaException) {
            LambdaFunctionInspector.LookupResult.Forbidden(
                "Lambda GetFunction failed for '$functionName' in region '$region': ${e.message}"
            )
        }
}
