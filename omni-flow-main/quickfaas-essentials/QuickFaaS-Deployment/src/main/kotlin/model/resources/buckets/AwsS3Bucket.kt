/*
 * Copyright © 2024, QuickFaaS
 */

package model.resources.buckets

import kotlinx.serialization.Serializable
import model.requests.AwsRequests
import model.resources.functions.CloudFunction

@Serializable
data class AwsBucketData(override var name: String = "") : BucketData

class AwsS3Bucket : CloudBucket {
    override var bucketData: BucketData = AwsBucketData()

    fun uploadToBucket(zipFilePath: String, function: CloudFunction): String {
        val key = "quickfaas/${function.name}/${zipFilePath.substringAfterLast('/')}"
        AwsRequests.uploadZipToS3(bucketData.name, key, zipFilePath)
        return key
    }
}
