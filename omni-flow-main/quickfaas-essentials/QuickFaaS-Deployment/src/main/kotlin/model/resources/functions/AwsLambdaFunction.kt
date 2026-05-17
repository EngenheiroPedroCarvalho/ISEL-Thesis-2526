/*
 * Copyright © 2024, QuickFaaS
 */

package model.resources.functions

import controller.General.logMessage
import model.Utils
import model.Utils.FUNC_TEMPLATES
import model.Utils.PROVIDER_CONFIGS
import model.projects.AwsProjectData
import model.projects.ProjectData
import model.requests.AwsRequests
import model.resources.buckets.AwsS3Bucket
import model.resources.functions.runtimes.RuntimeVersion
import model.resources.functions.runtimes.scripts.AwsBuildScripts
import model.resources.functions.runtimes.scripts.CloudBuildScripts
import model.resources.functions.triggers.HttpTrigger
import model.resources.functions.triggers.StorageTrigger

class AwsLambdaFunction : CloudFunction {
    override var name = ""
    override var hookFunction = HookFunction()
    override var buildScripts: CloudBuildScripts = AwsBuildScripts
    override val bucket = AwsS3Bucket()
    override val locations = listOf(
        "eu-west-1",   // Ireland
        "eu-west-2",   // London
        "eu-central-1" // Frankfurt
    )
    override var location = ""
    override val triggers = listOf(HttpTrigger(), StorageTrigger())
    override var trigger = triggers[0]
    override val runtimes = arrayOf(RuntimeVersion.JAVA11, RuntimeVersion.JAVA17)
    override var runtimeVersion: RuntimeVersion? = null

    var iamRoleArn: String = ""

    override suspend fun deployZip(zipFilePath: String, projData: ProjectData): DeploymentTimeData {
        projData as AwsProjectData
        val deploymentInfo = DeploymentTimeData()
        val functionName = name
        val runtime = "java${runtimeVersion!!.version}"
        val handler = "AwsHttpTemplate"
        val s3Bucket = bucket.bucketData.name

        logMessage("Uploading deployment package to S3 bucket '$s3Bucket'...", 2)
        val s3Key = bucket.uploadToBucket(zipFilePath, function = this)

        logMessage("Deploying Lambda function '$functionName' (runtime=$runtime)...", 2)
        if (!AwsRequests.checkLambdaFunctionExistence(functionName)) {
            AwsRequests.createLambdaFunction(
                functionName = functionName,
                runtime = runtime,
                handler = handler,
                roleArn = iamRoleArn,
                s3Bucket = s3Bucket,
                s3Key = s3Key
            )
        } else {
            logMessage("Lambda function '$functionName' already exists — updating code...", 2)
            AwsRequests.updateLambdaFunctionCode(functionName, s3Bucket, s3Key)
        }

        logMessage("Creating/retrieving Function URL for '$functionName' (AuthType=AWS_IAM)...", 2)
        val functionUrl = AwsRequests.getOrCreateFunctionUrl(functionName)
        logMessage("Function URL: $functionUrl", 0)

        return deploymentInfo
    }

    override fun getEntryPoint(): String = "AwsHttpTemplate"

    override fun getTriggerUrl(projData: ProjectData): Pair<String, String> {
        return when (trigger) {
            is HttpTrigger -> {
                val url = AwsRequests.getOrCreateFunctionUrl(name)
                Pair("", url)
            }
            else -> Pair("", "")
        }
    }
}
