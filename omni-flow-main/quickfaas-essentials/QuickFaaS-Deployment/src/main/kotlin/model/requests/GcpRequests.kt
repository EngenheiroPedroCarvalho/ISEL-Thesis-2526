/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.requests

import controller.httpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import model.projects.GcpProjectsData
import model.resources.buckets.GcpBucketsData
import java.io.File

// @formatter:off
object GcpRequests : CloudRequests {

    // Cloud Run functions (2nd gen). Functions are backed by Cloud Run services, so
    // invoker permissions are granted through the Cloud Run Admin API, not through
    // the Cloud Functions one.
    private const val FUNCTIONS_API = "https://cloudfunctions.googleapis.com/v2"
    private const val CLOUD_RUN_API = "https://run.googleapis.com/v2"

    private lateinit var token: String
    override fun setBearerToken(token: String) {
        this.token = token
    }

    // TODO: Pagination
    suspend fun getProjects(): GcpProjectsData {
        val body: GcpProjectsData = httpClient.get("https://cloudresourcemanager.googleapis.com/v1/projects")
        { bearerAuth(token) }.body()
        body.projects = body.projects.filter { proj -> proj.lifecycleState == "ACTIVE" }
        return body
    }

    // TODO: Pagination
    suspend fun getBuckets(projectId: String): GcpBucketsData =
        httpClient.get("https://storage.googleapis.com/storage/v1/b?project=$projectId")
        { bearerAuth(token) }.body()

    suspend fun getSessionUri(bucketName: String, functionName: String, zipFile: String): String =
        httpClient.post("https://storage.googleapis.com/upload/storage/v1/b/$bucketName/o?uploadType=resumable&name=quickfaas/$functionName/$zipFile")
        { bearerAuth(token) }.headers["Location"]!!

    suspend fun uploadZipToBucket(sessionUri: String, zipFilePath: String) = httpClient.put(sessionUri) {
        bearerAuth(token)
        contentType(ContentType.Application.Zip)
        setBody(File(zipFilePath).readBytes())
    }

    suspend fun getCloudFunction(projectId: String, location: String, functionName: String) =
        httpClient.get("$FUNCTIONS_API/projects/$projectId/locations/$location/functions/$functionName")
        { bearerAuth(token) }

    suspend fun checkCloudFunctionExistence(projectId: String, location: String, functionName: String) =
        getCloudFunction(projectId, location, functionName).status == HttpStatusCode.OK

    // The v2 API takes the function name as a query parameter instead of in the body.
    suspend fun deployCloudFunction(projectId: String, location: String, functionName: String, faasJson: String) =
        httpClient.post("$FUNCTIONS_API/projects/$projectId/locations/$location/functions?functionId=$functionName") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement(faasJson))
        }

    suspend fun updateCloudFunction(projectId: String, location: String, functionName: String, faasJson: String) =
        httpClient.patch("$FUNCTIONS_API/projects/$projectId/locations/$location/functions/$functionName") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement(faasJson))
        }

    /**
     * Opens a 2nd gen function to unauthenticated invocations. The policy is set on the
     * Cloud Run service that backs the function ([serviceResourceName], as returned in
     * 'serviceConfig.service'), since that is where invocation is enforced.
     */
    suspend fun setCloudFunctionInvokePolicy(serviceResourceName: String) =
        httpClient.post("$CLOUD_RUN_API/$serviceResourceName:setIamPolicy") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement("{\"policy\": {\"bindings\":[{\"role\":\"roles/run.invoker\", \"members\":[\"allUsers\"]}]}}"))
        }
}

// @formatter:on
