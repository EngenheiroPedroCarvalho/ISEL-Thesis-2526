/*
 * Copyright © 2024, QuickFaaS
 */

package model.requests

import controller.General.logMessage
import kotlinx.coroutines.runBlocking
import model.resources.buckets.AwsBucketData
import model.resources.buckets.BucketData
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.*
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.File

object AwsRequests : CloudRequests {

    private var region: String = System.getenv("AWS_REGION") ?: "us-east-1"

    fun setRegion(r: String) {
        region = r
    }

    // AWS uses env vars (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY) — bearer token is not applicable
    override fun setBearerToken(token: String) = Unit

    private fun lambdaClient(): LambdaClient = LambdaClient.builder()
        .region(Region.of(region))
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .build()

    private fun s3Client(): S3Client = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .build()

    fun checkLambdaFunctionExistence(functionName: String): Boolean = try {
        lambdaClient().use { it.getFunction(GetFunctionRequest.builder().functionName(functionName).build()) }
        true
    } catch (e: ResourceNotFoundException) {
        false
    }

    fun uploadZipToS3(bucket: String, key: String, zipFilePath: String) {
        s3Client().use { client ->
            client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromFile(File(zipFilePath))
            )
        }
    }

    fun createLambdaFunction(
        functionName: String,
        runtime: String,
        handler: String,
        roleArn: String,
        s3Bucket: String,
        s3Key: String
    ) {
        lambdaClient().use { client ->
            client.createFunction(
                CreateFunctionRequest.builder()
                    .functionName(functionName)
                    .runtime(Runtime.fromValue(runtime))
                    .handler(handler)
                    .role(roleArn)
                    .code(FunctionCode.builder().s3Bucket(s3Bucket).s3Key(s3Key).build())
                    .build()
            )
            client.waiter().waitUntilFunctionActive(
                GetFunctionRequest.builder().functionName(functionName).build()
            )
        }
    }

    fun updateLambdaFunctionCode(functionName: String, s3Bucket: String, s3Key: String) {
        lambdaClient().use { client ->
            client.updateFunctionCode(
                UpdateFunctionCodeRequest.builder()
                    .functionName(functionName)
                    .s3Bucket(s3Bucket)
                    .s3Key(s3Key)
                    .build()
            )
            client.waiter().waitUntilFunctionUpdated(
                GetFunctionRequest.builder().functionName(functionName).build()
            )
        }
    }

    fun getOrCreateFunctionUrl(functionName: String): String {
        return try {
            lambdaClient().use { client ->
                client.getFunctionUrlConfig(
                    GetFunctionUrlConfigRequest.builder().functionName(functionName).build()
                ).functionUrl()
            }
        } catch (e: ResourceNotFoundException) {
            lambdaClient().use { client ->
                client.createFunctionUrlConfig(
                    CreateFunctionUrlConfigRequest.builder()
                        .functionName(functionName)
                        .authType(FunctionUrlAuthType.AWS_IAM)
                        .build()
                ).functionUrl()
            }
        }
    }

    fun listBuckets(): List<BucketData> = s3Client().use { client ->
        client.listBuckets().buckets().map { AwsBucketData(it.name()) }
    }

    fun addPermission(functionName: String, statementId: String, principal: String, action: String) {
        try {
            lambdaClient().use { client ->
                client.addPermission(
                    AddPermissionRequest.builder()
                        .functionName(functionName)
                        .statementId(statementId)
                        .principal(principal)
                        .action(action)
                        .build()
                )
            }
        } catch (e: ResourceConflictException) {
            logMessage("Lambda permission '$statementId' already exists — skipping.", 2)
        }
    }
}
