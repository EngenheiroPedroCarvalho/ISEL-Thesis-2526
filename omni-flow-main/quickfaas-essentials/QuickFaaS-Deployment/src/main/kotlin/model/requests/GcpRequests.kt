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
    suspend fun getBuckets(projectName: String): GcpBucketsData =
        httpClient.get("https://storage.googleapis.com/storage/v1/b?project=$projectName")
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
        httpClient.get("https://cloudfunctions.googleapis.com/v1/projects/$projectId/locations/$location/functions/$functionName")
        { bearerAuth(token) }

    suspend fun checkCloudFunctionExistence(projectId: String, location: String, functionName: String) =
        getCloudFunction(projectId, location, functionName).status == HttpStatusCode.OK

    suspend fun deployCloudFunction(projectId: String, location: String, faasJson: String) =
        httpClient.post("https://cloudfunctions.googleapis.com/v1/projects/$projectId/locations/$location/functions") {
            bearerAuth(token)
            setBody(faasJson)
        }

    suspend fun updateCloudFunction(projectId: String, location: String, functionName: String, faasJson: String) =
        httpClient.patch("https://cloudfunctions.googleapis.com/v1/projects/$projectId/locations/$location/functions/$functionName") {
            bearerAuth(token)
            setBody(faasJson)
        }

    suspend fun setCloudFunctionInvokePolicy(projectId: String, location: String, functionName: String) =
        httpClient.post("https://cloudfunctions.googleapis.com/v1/projects/$projectId/locations/$location/functions/$functionName:setIamPolicy") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement("{\"policy\": {\"bindings\":[{\"role\":\"roles/cloudfunctions.invoker\", \"members\":[\"allUsers\"]}]}}"))
        }
}

// @formatter:on
