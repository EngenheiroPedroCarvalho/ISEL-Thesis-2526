/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.projects

import kotlinx.serialization.Serializable
import model.requests.GcpRequests
import model.resources.buckets.BucketData
import model.resources.functions.CloudFunction
import model.resources.functions.GcpFunction

@Serializable
data class GcpProjectsData(var projects: List<GcpProjectData>, val nextPageToken: String = "")

@Serializable
data class GcpProjectData(
    override var name: String = "", var projectId: String = "", val lifecycleState: String = ""
) : ProjectData

class GcpProject : CloudProject {
    override var projectData: ProjectData = GcpProjectData()
    override var buckets: List<BucketData> = listOf()
    override val function: CloudFunction = GcpFunction()

    override suspend fun requestBuckets(): List<BucketData> {
        function.bucket.bucketData.name = ""
        buckets = GcpRequests.getBuckets(projectData.name).items
        return buckets
    }

}
