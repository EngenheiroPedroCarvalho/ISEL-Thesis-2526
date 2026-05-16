/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions

import controller.General
import controller.General.logMessage
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
    override val runtimes = arrayOf(RuntimeVersion.JAVA11, RuntimeVersion.JAVA17, RuntimeVersion.NODEJS14)
    override var runtimeVersion: RuntimeVersion? = null

    override suspend fun deployZip(zipFilePath: String, projData: ProjectData): DeploymentTimeData {
        val deploymentInfo = DeploymentTimeData()
        logMessage("Storing source code in bucket...", 2)
        val zipUpload = bucket.uploadToBucket(zipFilePath, function = this)
        val projectId = (projData as GcpProjectData).projectId
        val faasJson = getJsonConfigs(projectId, zipFilePath.substringAfterLast('/'))
        logMessage("Deploying function '$name'...", 2)
        if (!GcpRequests.checkCloudFunctionExistence(projectId, location, name)) {
            deploymentInfo.deploymentStartDate =
                GcpRequests.deployCloudFunction(projectId, location, faasJson).requestTime
        } else {
            GcpRequests.updateCloudFunction(projectId, location, name, faasJson)
        }
        GcpRequests.setCloudFunctionInvokePolicy(projectId, location, name)
        deploymentInfo.zipUploadTime = calculateHttpDuration(zipUpload)
        return deploymentInfo
    }

    override fun getEntryPoint(): String = "Gcp${hookFunction.templateFile}"

    override fun getTriggerUrl(projData: ProjectData): Pair<String, String> {
        val projectId = (projData as GcpProjectData).projectId
        return when (trigger) {
            is HttpTrigger -> Pair("", "https://$location-$projectId.cloudfunctions.net/$name")
            is StorageTrigger -> Pair(
                "https://console.cloud.google.com/storage/browser", "https://console.cloud.google.com/storage/browser"
            )
            else -> Pair("", "")
        }
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
                    "<event_type>", when (storageTrigger.eventType) {
                        EventType.CREATE -> "google.storage.object.finalize"
                        EventType.DELETE -> "google.storage.object.delete"
                        EventType.UPDATE -> "google.storage.object.metadataUpdate"
                    }
                )
            }
        }
        return faasJson
    }

}
