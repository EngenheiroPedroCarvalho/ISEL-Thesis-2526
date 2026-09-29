/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions

import controller.General
import controller.General.logMessage
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import model.Utils
import model.Utils.FUNC_TEMPLATES
import model.Utils.PROVIDER_CONFIGS
import model.Utils.calculateHttpDuration
import model.projects.GcpProjectData
import model.projects.ProjectData
import model.requests.GcpRequests
import model.resources.buckets.GcpBucket
import model.resources.functions.runtimes.RuntimeVersion
import model.resources.functions.runtimes.scripts.CloudBuildScripts
import model.resources.functions.runtimes.scripts.GcpBuildScripts
import model.resources.functions.triggers.HttpTrigger
import model.resources.functions.triggers.StorageTrigger
import model.resources.functions.triggers.StorageTrigger.EventType

@Serializable
data class GcpServiceConfigData(val uri: String = "", val service: String = "")

@Serializable
data class GcpFunctionData(val state: String = "", val serviceConfig: GcpServiceConfigData? = null)

class GcpFunction : CloudFunction {
    override var name = ""
    override var hookFunction = HookFunction()
    override var buildScripts: CloudBuildScripts = GcpBuildScripts
    override val bucket = GcpBucket()
    override val locations = listOf(
        "europe-west1", // Belgium
        "europe-west2"  // London
    )
    override var location = ""
    override val triggers = listOf(HttpTrigger(), StorageTrigger())
    override var trigger = triggers[0]
    // java11 and nodejs14 were decommissioned by Google (2025-10-31 and 2025-01-30)
    override val runtimes = arrayOf(RuntimeVersion.JAVA17)
    override var runtimeVersion: RuntimeVersion? = null

    // 2nd gen functions are served on a generated 'run.app' URL, so it cannot be composed
    // offline. It is read from the deployed function and kept here for getTriggerUrl.
    private var deployedUri = ""

    private companion object {
        const val READINESS_TIMEOUT_MS = 600_000L  // Cloud Build makes 2nd gen deployments slow
        const val READINESS_POLL_MS = 5_000L
    }

    override suspend fun deployZip(zipFilePath: String, projData: ProjectData): DeploymentTimeData {
        val deploymentInfo = DeploymentTimeData()
        logMessage("Storing source code in bucket...", 2)
        val zipUpload = bucket.uploadToBucket(zipFilePath, function = this)
        val projectId = (projData as GcpProjectData).projectId
        val faasJson = getJsonConfigs(projectId, zipFilePath.substringAfterLast('/'))
        logMessage("Deploying function '$name'...", 2)
        val response = if (!GcpRequests.checkCloudFunctionExistence(projectId, location, name)) {
            GcpRequests.deployCloudFunction(projectId, location, name, faasJson)
                .also { deploymentInfo.deploymentStartDate = it.requestTime }
        } else {
            GcpRequests.updateCloudFunction(projectId, location, name, faasJson)
        }
        if (!response.status.isSuccess()) {
            logMessage("Deployment of function '$name' was rejected: ${response.bodyAsText()}", 1)
        }
        // The v2 deployment is a long-running operation: the backing Cloud Run service only
        // exists once the build finishes, so both the URL and the IAM policy have to wait.
        val deployed = awaitActiveFunction(projectId)
        deployedUri = deployed.serviceConfig?.uri.orEmpty()
        deployed.serviceConfig?.service?.takeIf { it.isNotEmpty() }?.let { service ->
            GcpRequests.setCloudFunctionInvokePolicy(service)
        }
        deploymentInfo.zipUploadTime = calculateHttpDuration(zipUpload)
        return deploymentInfo
    }

    override fun getEntryPoint(): String = "Gcp${hookFunction.templateFile}"

    override fun getTriggerUrl(projData: ProjectData): Pair<String, String> {
        return when (trigger) {
            is HttpTrigger -> Pair("", deployedUri)
            is StorageTrigger -> Pair(
                "https://console.cloud.google.com/storage/browser", "https://console.cloud.google.com/storage/browser"
            )
            else -> Pair("", "")
        }
    }

    /**
     * Polls the deployed function until it reports an ACTIVE state, and returns it.
     */
    private suspend fun awaitActiveFunction(projectId: String): GcpFunctionData {
        logMessage("Waiting for function '$name' to become ACTIVE...", 2)
        val deadline = System.currentTimeMillis() + READINESS_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val response = GcpRequests.getCloudFunction(projectId, location, name)
            if (response.status == HttpStatusCode.OK) {
                val function: GcpFunctionData = response.body()
                when (function.state) {
                    "ACTIVE" -> return function
                    "FAILED" -> logMessage("Deployment of function '$name' failed.", 1)
                }
            }
            delay(READINESS_POLL_MS)
        }
        logMessage("Timed out waiting for function '$name' to become ACTIVE.", 1)
        return GcpFunctionData()  // Unreachable: logMessage(.., 1) terminates the process
    }

    private fun getJsonConfigs(projectId: String, zipFile: String): String {
        // @formatter:off
        var faasJson =
            Utils.readResFile(filePath = "$FUNC_TEMPLATES/$PROVIDER_CONFIGS/gcp-${trigger.shortName}-config.json")
                .replace("<bucket>", bucket.bucketData.name)
                .replace("<entry_point>", getEntryPoint().substringBeforeLast('.'))  // Remove file extension if exists
                .replace("<location>", location)
                .replace("<name>", name)
                .replace("<project_id>", projectId)
                .replace("<runtime>", runtimeVersion!!.let { rv -> rv.runtime.shortName + rv.version })
                .replace("<zip_file>", zipFile)
        // @formatter:on
        when (trigger) {
            is StorageTrigger -> {
                val storageTrigger = trigger as StorageTrigger
                faasJson = faasJson.replace("<trigger_bucket>", storageTrigger.bucketData!!.name).replace(
                    // 2nd gen routes storage events through Eventarc, which uses CloudEvents types
                    "<event_type>", when (storageTrigger.eventType) {
                        EventType.CREATE -> "google.cloud.storage.object.v1.finalized"
                        EventType.DELETE -> "google.cloud.storage.object.v1.deleted"
                        EventType.UPDATE -> "google.cloud.storage.object.v1.metadataUpdated"
                    }
                )
            }
        }
        return faasJson
    }

}
