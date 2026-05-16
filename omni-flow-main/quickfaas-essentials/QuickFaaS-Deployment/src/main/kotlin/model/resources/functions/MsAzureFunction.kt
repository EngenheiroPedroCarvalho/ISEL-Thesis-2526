/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions

import controller.General.logMessage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import model.Utils
import model.Utils.FUNC_TEMPLATES
import model.Utils.PROVIDER_CONFIGS
import model.projects.MsAzureProjectData
import model.projects.ProjectData
import model.requests.MsAzureRequests
import model.resources.buckets.MsAzureBucket
import model.resources.functions.runtimes.RuntimeVersion
import model.resources.functions.runtimes.scripts.CloudBuildScripts
import model.resources.functions.runtimes.scripts.MsAzureBuildScripts
import model.resources.functions.triggers.HttpTrigger
import model.resources.functions.triggers.StorageTrigger

class MsAzureFunction : CloudFunction {
    override var name = ""
    override var hookFunction = HookFunction()
    override var buildScripts: CloudBuildScripts = MsAzureBuildScripts
    override val bucket = MsAzureBucket()
    override val locations = listOf(
        "France Central",
        "UK South",
        "West Europe",
    )
    override var location = ""
    override val triggers = listOf(HttpTrigger(), StorageTrigger())
    override var trigger = triggers[0]
    override val runtimes = arrayOf(RuntimeVersion.JAVA11, RuntimeVersion.NODEJS14)
    override var runtimeVersion: RuntimeVersion? = null
    lateinit var functionApp: String
    val container = "quick-container" // TODO: let user decide which container to use

    override suspend fun deployZip(zipFilePath: String, projData: ProjectData): DeploymentTimeData {
        projData as MsAzureProjectData
        functionApp = "quickfaas-${runtimeVersion!!.runtime.shortName}-app"
        val deploymentInfo = DeploymentTimeData()
        val subscriptionId = projData.subscriptionId
        val resourceGroup = projData.name
        val funcAppExists = MsAzureRequests.checkFunctionAppExistence(subscriptionId, resourceGroup, functionApp)
        if (!funcAppExists) {
            logMessage("Deploying function app '$functionApp'...", 2)
            val storageAccountKey =
                MsAzureRequests.getStorageAccountAccessKey(subscriptionId, resourceGroup, bucket.bucketData.name).value
            val functionAppJson = getJsonConfigs(storageAccountKey)
            deploymentInfo.deploymentStartDate = MsAzureRequests.deployFunctionApp(
                subscriptionId, resourceGroup, functionApp, functionAppJson
            ).requestTime
        }
        when (trigger) {
            is StorageTrigger -> {
                val storageAccount = (trigger as StorageTrigger).bucketData!!.name
                val storageAccountKey =
                    MsAzureRequests.getStorageAccountAccessKey(subscriptionId, resourceGroup, storageAccount).value
                val appSettings =
                    MsAzureRequests.getAppSettings(subscriptionId, resourceGroup, functionApp).toMutableMap()
                appSettings["AzureWebJobsStorageTrigger-$storageAccount"] =
                    JsonPrimitive("DefaultEndpointsProtocol=https;AccountName=$storageAccount;AccountKey=$storageAccountKey;EndpointSuffix=core.windows.net")
                MsAzureRequests.setAppSettings(
                    subscriptionId, resourceGroup, functionApp, JsonObject(appSettings).toString()
                )
                logMessage("Creating container '$container'...", 2)
                MsAzureRequests.createContainer(subscriptionId, resourceGroup, storageAccount, container)
            }
        }
        logMessage("Deploying function '$name'...", 2)
        val zipUpload = MsAzureRequests.deployAzureFunction(functionApp, zipFilePath)
        deploymentInfo.zipUploadTime = Utils.calculateHttpDuration(zipUpload)
        return deploymentInfo
    }

    override fun getEntryPoint(): String = "MsAzure${hookFunction.templateFile}"

    override fun getTriggerUrl(projData: ProjectData): Pair<String, String> {
        return when (trigger) {
            is HttpTrigger -> Pair("", "https://$functionApp.azurewebsites.net/api/$name")
            is StorageTrigger -> Pair(
                "https://portal.azure.com/storageaccounts",
                "https://portal.azure.com/#blade/HubsExtension/BrowseResource/resourceType/Microsoft.Storage%2FStorageAccounts"
            )
            else -> Pair("", "")
        }
    }

    fun setFunctionApp() {
        functionApp = "quickfaas-${runtimeVersion!!.runtime.shortName}-app"
    }

    // @formatter:off
    // Function app JSON
    private fun getJsonConfigs(storageAccountKey: String): String =
        Utils.readResFile(filePath = "$FUNC_TEMPLATES/$PROVIDER_CONFIGS/msazure-${runtimeVersion!!.runtime.shortName}-config.json")
            .replace("<function_app>", functionApp)
            .replace("<location>", location)
            .replace("<storage_account>", bucket.bucketData.name)
            .replace("<storage_account_key>", storageAccountKey)
            .replace("<runtime_version>", runtimeVersion!!.version)
    // @formatter:on
}
