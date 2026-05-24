package costaber.com.github.omniflow.internalfunction.quickfaas

import costaber.com.github.omniflow.cloud.provider.amazon.deployer.AwsLambdaIamHelper
import costaber.com.github.omniflow.internalfunction.InternalFunctionDeployer
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest
import java.nio.file.Files
import java.nio.file.Path

class AwsLambdaDeployer(
    private val quickFaasJarPath: Path,
    private val region: String,
    private val roleArn: String,
    private val readinessTimeoutSeconds: Long = 180,
    private val readinessPollIntervalSeconds: Long = 5
) : InternalFunctionDeployer {

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val RESET   = "[0m"
        private const val BOLD    = "[1m"
        private const val GREEN   = "[32m"
        private const val YELLOW  = "[33m"
        private const val BLUE    = "[34m"
    }

    private val iamHelper = AwsLambdaIamHelper()

    override fun deployOrUpdate(
        functionName: String,
        deploymentDescriptorPath: String
    ): FunctionInvocationMetadata {
        val descriptorPath = Path.of(deploymentDescriptorPath)

        logger.info { "Deploying Lambda '$functionName' via QuickFaaS (descriptor=$descriptorPath)" }

        // Load raw descriptor to read iamRoleArn and resolve/create the execution role
        val rawDescriptor = QuickFaasDescriptorLoader.load(descriptorPath)
        val lambdaExecutionRoleArn = iamHelper.resolveOrCreateLambdaExecutionRole(rawDescriptor.iamRoleArn)

        // Patch only iamRoleArn so QuickFaaS receives the resolved ARN;
        // all other fields are expected to be filled in func-deployment.json
        val patchedDescriptorPath = patchIamRoleArn(descriptorPath, lambdaExecutionRoleArn)
        try {
            println("$BLUE  →$RESET Loading deployment descriptor from '$descriptorPath'...")
            val descriptor = QuickFaasDescriptorLoader.load(patchedDescriptorPath)
            QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "aws")
            println("$GREEN  ✓$RESET Descriptor validated (provider=aws, runtime=${descriptor.function?.runtime})")

            val bucketName = descriptor.function?.bucket
            val effectiveRegion = if (!bucketName.isNullOrBlank()) {
                resolveBucketRegion(bucketName).also {
                    if (it != region) println("$YELLOW  !$RESET Bucket '$bucketName' is in '$it' — using that region for Lambda")
                }
            } else {
                region
            }

            println("$BLUE  →$RESET Invoking QuickFaaS subprocess to deploy '$BOLD$functionName$RESET' on AWS Lambda...")
            val invoker = QuickFaasProcessInvoker(quickFaasJarPath)
            invokeWithRoleRetry(invoker, patchedDescriptorPath)
            println("$GREEN  ✓$RESET QuickFaaS subprocess completed for '$functionName'")

            println("$BLUE  →$RESET Waiting for Lambda '$functionName' to become Active...")
            waitForLambdaActive(functionName, effectiveRegion)
            println("$GREEN  ✓$RESET Lambda '$functionName' is Active")

            println("$BLUE  →$RESET Retrieving Lambda ARN for '$functionName'...")
            val functionArn = getLambdaFunctionArn(functionName, effectiveRegion)
            println("$GREEN  ✓$RESET Lambda ARN: $BOLD$functionArn$RESET")

            iamHelper.grantStepFunctionsInvoke(functionName, effectiveRegion, roleArn)

            logger.info { "AwsLambdaDeployer completed for '$functionName'. ARN: $functionArn" }
            return FunctionInvocationMetadata(serviceName = functionName, url = functionArn)
        } finally {
            Files.deleteIfExists(patchedDescriptorPath)
        }
    }

    private fun waitForLambdaActive(functionName: String, effectiveRegion: String) {
        val deadline = System.currentTimeMillis() + readinessTimeoutSeconds * 1_000

        while (System.currentTimeMillis() < deadline) {
            try {
                val state = lambdaClient(effectiveRegion).use { client ->
                    client.getFunction(
                        GetFunctionRequest.builder().functionName(functionName).build()
                    ).configuration().stateAsString()
                }
                when (state) {
                    "Active" -> return
                    "Failed" -> throw IllegalStateException(
                        "Lambda '$functionName' reached Failed state. Check CloudWatch logs."
                    )
                    else -> logger.info { "Lambda '$functionName' state: $state — waiting..." }
                }
            } catch (e: ResourceNotFoundException) {
                logger.info { "Lambda '$functionName' not found yet — waiting..." }
            } catch (e: IllegalStateException) {
                throw e
            } catch (e: Exception) {
                logger.warn { "Error checking Lambda state: ${e.message} — retrying..." }
            }
            Thread.sleep(readinessPollIntervalSeconds * 1_000)
        }

        throw IllegalStateException(
            "Timed out after ${readinessTimeoutSeconds}s waiting for Lambda '$functionName' to become Active."
        )
    }

    private fun getLambdaFunctionArn(functionName: String, effectiveRegion: String): String =
        lambdaClient(effectiveRegion).use { client ->
            client.getFunction(
                GetFunctionRequest.builder().functionName(functionName).build()
            ).configuration().functionArn()
        }

    private fun lambdaClient(effectiveRegion: String = region): LambdaClient = LambdaClient.builder()
        .region(Region.of(effectiveRegion))
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .build()

    private fun invokeWithRoleRetry(invoker: QuickFaasProcessInvoker, descriptorPath: Path, maxRetries: Int = 3) {
        repeat(maxRetries) { attempt ->
            try {
                invoker.invoke(descriptorPath, accessToken = null)
                return
            } catch (e: IllegalStateException) {
                val isRoleError = e.message?.contains("cannot be assumed by Lambda") == true
                if (isRoleError && attempt < maxRetries - 1) {
                    val waitSec = 20
                    println("$YELLOW  !$RESET IAM role not yet propagated — waiting ${waitSec}s before retry ${attempt + 2}/$maxRetries...")
                    Thread.sleep(waitSec * 1_000L)
                } else {
                    throw e
                }
            }
        }
    }

    private fun patchIamRoleArn(originalPath: Path, roleArn: String): Path {
        val content = Files.readString(originalPath).replace(
            Regex("\"iamRoleArn\"\\s*:\\s*\"[^\"]*\""),
            "\"iamRoleArn\": \"$roleArn\""
        )
        val tempPath = originalPath.parent.resolve("_omniflow_tmp_descriptor.json")
        Files.writeString(tempPath, content)
        return tempPath
    }

    private fun resolveBucketRegion(bucket: String): String = try {
        S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()
            .use { client ->
                val location = client.getBucketLocation(
                    GetBucketLocationRequest.builder().bucket(bucket).build()
                ).locationConstraintAsString()
                if (location.isNullOrBlank()) "us-east-1" else location
            }
    } catch (e: Exception) {
        logger.warn { "Could not resolve bucket region for '$bucket': ${e.message} — using '$region'" }
        region
    }
}
