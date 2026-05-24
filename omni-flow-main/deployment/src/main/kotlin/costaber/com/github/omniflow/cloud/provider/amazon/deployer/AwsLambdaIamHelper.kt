package costaber.com.github.omniflow.cloud.provider.amazon.deployer

import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.iam.IamClient
import software.amazon.awssdk.services.iam.model.*
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.AddPermissionRequest
import software.amazon.awssdk.services.lambda.model.ResourceConflictException
import java.net.URLDecoder

class AwsLambdaIamHelper {

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val RESET  = "[0m"
        private const val BOLD   = "[1m"
        private const val GREEN  = "[32m"
        private const val YELLOW = "[33m"
        private const val BLUE   = "[34m"
        private const val LAMBDA_TRUST_POLICY = """{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}"""
        private const val LAMBDA_BASIC_EXECUTION_POLICY = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
        private const val DEFAULT_ROLE_NAME = "OmniFlowLambdaExecutionRole"
    }

    /**
     * Ensures a valid Lambda execution role exists.
     * - If iamRoleArn is blank or a placeholder, creates OmniFlowLambdaExecutionRole automatically.
     * - If a real ARN is provided, checks and fixes its trust policy if needed.
     * Returns the resolved role ARN ready to use.
     */
    fun resolveOrCreateLambdaExecutionRole(iamRoleArn: String?): String {
        val isPlaceholder = iamRoleArn.isNullOrBlank() || iamRoleArn.contains("<") || iamRoleArn.contains(">")
        return if (isPlaceholder) {
            println("$YELLOW  !$RESET No valid Lambda execution role configured — auto-creating '$BOLD$DEFAULT_ROLE_NAME$RESET'...")
            createOrGetDefaultRole()
        } else {
            ensureLambdaTrustPolicy(iamRoleArn!!)
            iamRoleArn
        }
    }

    private fun createOrGetDefaultRole(): String {
        val iamClient = IamClient.builder()
            .region(Region.AWS_GLOBAL)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()

        return iamClient.use { iam ->
            try {
                val existing = iam.getRole(GetRoleRequest.builder().roleName(DEFAULT_ROLE_NAME).build()).role()
                println("$GREEN  ✓$RESET Role '$DEFAULT_ROLE_NAME' already exists → ${existing.arn()}")
                val trustPolicy = URLDecoder.decode(existing.assumeRolePolicyDocument(), "UTF-8")
                if (!trustPolicy.contains("lambda.amazonaws.com")) {
                    fixTrustPolicy(iam, DEFAULT_ROLE_NAME)
                }
                existing.arn()
            } catch (e: NoSuchEntityException) {
                println("$BLUE  →$RESET Creating role '$BOLD$DEFAULT_ROLE_NAME$RESET'...")
                val roleArn = iam.createRole(
                    CreateRoleRequest.builder()
                        .roleName(DEFAULT_ROLE_NAME)
                        .assumeRolePolicyDocument(LAMBDA_TRUST_POLICY)
                        .description("Lambda execution role managed by OmniFlow")
                        .build()
                ).role().arn()
                iam.attachRolePolicy(
                    AttachRolePolicyRequest.builder()
                        .roleName(DEFAULT_ROLE_NAME)
                        .policyArn(LAMBDA_BASIC_EXECUTION_POLICY)
                        .build()
                )
                println("$GREEN  ✓$RESET Role created → $roleArn")
                println("$BLUE  →$RESET Waiting 15s for IAM propagation...")
                Thread.sleep(15_000)
                roleArn
            }
        }
    }

    private fun ensureLambdaTrustPolicy(roleArn: String) {
        val roleName = roleArn.substringAfterLast("/")
        println("$BLUE  →$RESET Checking trust policy of Lambda execution role '$BOLD$roleName$RESET'...")
        try {
            IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()
                .use { iam ->
                    val trustPolicy = URLDecoder.decode(
                        iam.getRole(GetRoleRequest.builder().roleName(roleName).build())
                            .role().assumeRolePolicyDocument(),
                        "UTF-8"
                    )
                    if (!trustPolicy.contains("lambda.amazonaws.com")) {
                        fixTrustPolicy(iam, roleName)
                    } else {
                        println("$GREEN  ✓$RESET Trust policy already correct")
                    }
                }
        } catch (e: Exception) {
            println("$YELLOW  !$RESET Could not verify/fix trust policy for '$roleName': ${e.message}")
            println("$YELLOW  !$RESET Make sure the role has 'lambda.amazonaws.com' in its trust policy.")
            logger.warn { "Could not fix trust policy for '$roleName': ${e.message}" }
        }
    }

    private fun fixTrustPolicy(iam: IamClient, roleName: String) {
        println("$YELLOW  !$RESET Trust policy missing 'lambda.amazonaws.com' — fixing automatically...")
        iam.updateAssumeRolePolicy(
            UpdateAssumeRolePolicyRequest.builder()
                .roleName(roleName)
                .policyDocument(LAMBDA_TRUST_POLICY)
                .build()
        )
        println("$GREEN  ✓$RESET Trust policy updated — waiting 10s for IAM propagation...")
        Thread.sleep(10_000)
    }

    fun grantStepFunctionsInvoke(functionName: String, region: String, roleArn: String) {
        val statementId = "AllowStepFunctionsInvoke"
        println("$BLUE  →$RESET Granting Step Functions invoke permission on '$BOLD$functionName$RESET'...")
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
            Thread.sleep(5_000)
        } catch (e: ResourceConflictException) {
            println("$GREEN  ✓$RESET Permission already exists on '$functionName' — skipping")
        } catch (e: Exception) {
            println("$YELLOW  !$RESET Could not auto-grant permission: ${e.message}")
            println("$YELLOW  !$RESET Run manually: aws lambda add-permission --function-name $functionName --statement-id $statementId --action lambda:InvokeFunction --principal states.amazonaws.com --region $region")
            logger.warn { "Could not auto-grant Step Functions invoke permission: ${e.message}" }
        }
    }
}
