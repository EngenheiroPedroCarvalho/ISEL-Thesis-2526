/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.requests

import controller.httpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import model.projects.MsAzureProjectsData
import model.resources.buckets.MsAzureBucketsData
import model.resources.buckets.StorageAccountKeyData
import model.resources.buckets.StorageAccountKeysData
import model.specifics.SubscriptionsData
import java.io.File

// @formatter:off
object MsAzureRequests : CloudRequests {

    private lateinit var token: String
    override fun setBearerToken(token: String) {
        this.token = token
    }

    private fun funcAppUri(subscriptionId: String, resourceGroup: String, functionApp: String) =
        "https://management.azure.com/subscriptions/$subscriptionId/resourceGroups/$resourceGroup/providers/Microsoft.Web/sites/$functionApp"

    private fun insightsAppUri(subscriptionId: String, resourceGroup: String, insightsApp: String) =
        "https://management.azure.com/subscriptions/$subscriptionId/resourceGroups/$resourceGroup/providers/Microsoft.Insights/components/$insightsApp"

    suspend fun getSubscriptions(): SubscriptionsData {
        val body: SubscriptionsData =
            httpClient.get("https://management.azure.com/subscriptions?api-version=2020-01-01")
            { bearerAuth(token) }.body()
        body.value = body.value.filter { subscription -> subscription.state == "Enabled" }
        return body
    }

    // TODO: Pagination
    suspend fun getResourceGroups(subscriptionId: String): MsAzureProjectsData =
        httpClient.get("https://management.azure.com/subscriptions/$subscriptionId/resourceGroups?api-version=2021-04-01")
        { bearerAuth(token) }.body()

    // TODO: Pagination
    suspend fun getStorageAccounts(subscriptionId: String, resourceGroup: String): MsAzureBucketsData =
        httpClient.get("https://management.azure.com/subscriptions/$subscriptionId/resourceGroups/$resourceGroup/providers/Microsoft.Storage/storageAccounts?api-version=2021-04-01")
        { bearerAuth(token) }.body()

    suspend fun getFunctionApp(subscriptionId: String, resourceGroup: String, functionApp: String) =
        httpClient.get("${funcAppUri(subscriptionId, resourceGroup, functionApp)}?api-version=2022-03-01")
        { bearerAuth(token) }

    suspend fun checkFunctionAppExistence(subscriptionId: String, resourceGroup: String, functionApp: String) =
        getFunctionApp(subscriptionId, resourceGroup, functionApp).status == HttpStatusCode.OK

    suspend fun getStorageAccountAccessKey(subscriptionId: String, resourceGroup: String, storageAccount: String): StorageAccountKeyData {
        val body: StorageAccountKeysData =
            httpClient.post("https://management.azure.com/subscriptions/$subscriptionId/resourceGroups/$resourceGroup/providers/Microsoft.Storage/storageAccounts/$storageAccount/listKeys?api-version=2021-04-01")
            { bearerAuth(token) }.body()
        return body.keys.find { key -> key.keyName == "key1" }!!
    }

    suspend fun getAppSettings(subscriptionId: String, resourceGroup: String, functionApp: String): JsonObject =
        httpClient.post("${funcAppUri(subscriptionId, resourceGroup, functionApp)}/config/appsettings/list?api-version=2021-02-01")
        { bearerAuth(token) }.body<JsonObject>()["properties"]!!.jsonObject

    suspend fun setAppSettings(subscriptionId: String, resourceGroup: String, functionApp: String, appSettingsJson: String) =
        httpClient.put("${funcAppUri(subscriptionId, resourceGroup, functionApp)}/config/appsettings?api-version=2021-02-01") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement("{\"kind\":\"functionapp\",\"properties\":$appSettingsJson}"))
        }

    suspend fun createContainer(subscriptionId: String, resourceGroup: String, storageAccount: String, container: String) =
        httpClient.put("https://management.azure.com/subscriptions/$subscriptionId/resourceGroups/$resourceGroup/providers/Microsoft.Storage/storageAccounts/$storageAccount/blobServices/default/containers/$container?api-version=2021-09-01") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement("{\"properties\": {\"publicAccess\":\"None\"}}"))
        }

    suspend fun deployFunctionApp(subscriptionId: String, resourceGroup: String, functionApp: String, functionAppJson: String) =
        httpClient.put("${funcAppUri(subscriptionId, resourceGroup, functionApp)}?api-version=2021-03-01") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement(functionAppJson))
        }

    suspend fun deployAzureFunction(functionApp: String, zipFilePath: String) =
        httpClient.post("https://$functionApp.scm.azurewebsites.net/api/zipdeploy") {
            bearerAuth(token)
            contentType(ContentType.Application.Zip)
            setBody(File(zipFilePath).readBytes())
        }

    suspend fun createAppInsights(subscriptionId: String, resourceGroup: String, functionApp: String, insightsApp: String, location: String): String {
        val insightsProperties =
            httpClient.put("${insightsAppUri(subscriptionId, resourceGroup, insightsApp)}?api-version=2015-05-01") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(Json.parseToJsonElement("{\"kind\":\"web\",\"location\":\"$location\",\"properties\":{\"Application_Type\":\"functionapp\"}}"))
            }.body<JsonObject>()["properties"]!!.jsonObject
        val instrumentationKey = insightsProperties["InstrumentationKey"]!!.jsonPrimitive
        val connectionStr = insightsProperties["ConnectionString"]!!.jsonPrimitive
        val appSettings = getAppSettings(subscriptionId, resourceGroup, functionApp).toMutableMap()
        appSettings["APPINSIGHTS_INSTRUMENTATIONKEY"] = instrumentationKey
        appSettings["APPLICATIONINSIGHTS_CONNECTION_STRING"] = connectionStr
        setAppSettings(subscriptionId, resourceGroup, functionApp, JsonObject(appSettings).toString())
        return insightsProperties["AppId"]!!.jsonPrimitive.content
    }

    suspend fun createInsightsAppApiKey(subscriptionId: String, resourceGroup: String, insightsApp: String, keyName: String): String? {
        val response = httpClient.post("${insightsAppUri(subscriptionId, resourceGroup, insightsApp)}/ApiKeys?api-version=2015-05-01") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement("{\"name\":\"$keyName\", \"linkedReadProperties\":[\"/subscriptions/$subscriptionId/resourceGroups/$resourceGroup/providers/microsoft.insights/components/$insightsApp/api\"]}"))
        }
        return if(response.status == HttpStatusCode.OK) response.body<JsonObject>()["apiKey"]!!.jsonPrimitive.content else null
    }

    suspend fun fetchInsightsAppMetrics(subscriptionId: String, resourceGroup: String, functionApp: String, metricsId: String, timestamp: String, parameters: String) =
        httpClient.get("${funcAppUri(subscriptionId, resourceGroup, functionApp)}/providers/Microsoft.Insights/metrics?metricnames=$metricsId&timespan=$timestamp&$parameters&api-version=2019-07-01") {
            bearerAuth(token)
        }.body<JsonObject>()["value"]!!.jsonArray[0].jsonObject["timeseries"]!!.jsonArray[0].jsonObject["data"]!!.jsonArray

    suspend fun queryInsightsAppMetrics(appId: String, apiKey: String, timestamp: String, query: String): JsonArray {
        val tables = httpClient.post("https://api.applicationinsights.io/v1/apps/$appId/query?api_key=$apiKey&timespan=$timestamp") {
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement("{\"query\": \"$query\"}"))
        }.body<JsonObject>()["tables"]!!.jsonArray
        if (tables.isEmpty()) return tables
        return tables[0].jsonObject["rows"]!!.jsonArray
    }

    suspend fun getActivityLogs(subscriptionId: String, resourceGroup: String, functionApp: String, timestamp: String) : JsonArray {
        val timestampParts = timestamp.split('/')
        return httpClient.post("https://management.azure.com/providers/Microsoft.ResourceGraph/resourceChanges?api-version=2018-09-01-preview") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement("{\"resourceId\": \"/subscriptions/$subscriptionId/resourceGroups/$resourceGroup/providers/Microsoft.Web/sites/$functionApp\",\"interval\":{\"start\":\"${timestampParts[0]}\",\"end\":\"${timestampParts[1]}\"}}"))
        }.body<JsonObject>()["changes"]!!.jsonArray
    }
}
// @formatter:on
