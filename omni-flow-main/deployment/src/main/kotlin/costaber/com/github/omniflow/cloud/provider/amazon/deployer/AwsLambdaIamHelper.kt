package costaber.com.github.omniflow.cloud.provider.amazon.deployer

import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.AddPermissionRequest
import software.amazon.awssdk.services.lambda.model.ResourceConflictException

class AwsLambdaIamHelper {

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val RESET  = "[0m"
        private const val BOLD   = "[1m"
        private const val GREEN  = "[32m"
        private const val YELLOW = "[33m"
        private const val BLUE   = "[34m"
    }

    fun grantStepFunctionsInvoke(functionName: String, region: String, roleArn: String) {
        val statementId = "AllowStepFunctionsInvoke"
        println("$BLUE  →$RESET Granting Step Functions invoke permission on '$BOLD$functionName$RESET'...")
        logger.info { "Granting Step Functions invoke permission to '$roleArn' on Lambda '$functionName'..." }

        try {
            LambdaClient.builder()
                .region(Region.of(region))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()
                .use { client ->
                    client.addPermission(
                        AddPermissionRequest.builder()
                            .functionName(functionName)
                            .statementId(statementId)
                            .principal("states.amazonaws.com")
                            .action("lambda:InvokeFunction")
                            .build()
                    )
                }
            println("$GREEN  ✓$RESET Step Functions invoke permission granted on '$functionName'")
            logger.info { "Invoke permission granted. Waiting 5s for IAM propagation..." }
            Thread.sleep(5_000)
        } catch (e: ResourceConflictException) {
            println("$GREEN  ✓$RESET Permission already exists on '$functionName' — skipping")
            logger.info { "Permission '$statementId' already exists on '$functionName' — skipping." }
        } catch (e: Exception) {
            println("$YELLOW  !$RESET Could not auto-grant permission: ${e.message}")
            println("$YELLOW  !$RESET Run manually:")
            println("    aws lambda add-permission --function-name $functionName \\")
            println("      --statement-id $statementId \\")
            println("      --action lambda:InvokeFunction \\")
            println("      --principal states.amazonaws.com \\")
            println("      --region $region")
            logger.warn { "Could not auto-grant Step Functions invoke permission: ${e.message}" }
        }
    }
}
