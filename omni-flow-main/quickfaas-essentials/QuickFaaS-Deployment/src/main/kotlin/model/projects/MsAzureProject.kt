/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.projects

import kotlinx.serialization.Serializable
import model.requests.MsAzureRequests
import model.resources.buckets.BucketData
import model.resources.functions.CloudFunction
import model.resources.functions.MsAzureFunction

@Serializable
data class MsAzureProjectsData(val value: List<MsAzureProjectData>, val nextLink: String = "")

@Serializable
data class MsAzureProjectData(override var name: String = "", var subscriptionId: String = "") : ProjectData

class MsAzureProject : CloudProject {
    override var projectData: ProjectData = MsAzureProjectData()
    override var buckets: List<BucketData> = listOf()
    override val function: CloudFunction = MsAzureFunction()

    override suspend fun requestBuckets(): List<BucketData> {
        function.bucket.bucketData.name = ""
        buckets = MsAzureRequests.getStorageAccounts(
            (projectData as MsAzureProjectData).subscriptionId,
            projectData.name
        ).value
        return buckets
    }

}
