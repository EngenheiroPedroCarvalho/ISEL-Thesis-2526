/*
 * Copyright © 2024, QuickFaaS
 */

package model.projects

import kotlinx.serialization.Serializable
import model.requests.AwsRequests
import model.resources.buckets.BucketData
import model.resources.functions.AwsLambdaFunction
import model.resources.functions.CloudFunction

@Serializable
data class AwsProjectData(override var name: String = "") : ProjectData

class AwsProject : CloudProject {
    override var projectData: ProjectData = AwsProjectData()
    override var buckets: List<BucketData> = listOf()
    override val function: CloudFunction = AwsLambdaFunction()

    override suspend fun requestBuckets(): List<BucketData> {
        function.bucket.bucketData.name = ""
        buckets = AwsRequests.listBuckets()
        return buckets
    }
}
