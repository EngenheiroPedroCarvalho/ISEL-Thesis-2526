/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.buckets

import kotlinx.serialization.Serializable

@Serializable
data class MsAzureBucketsData(val value: List<MsAzureBucketData>, val nextLink: String = "")

@Serializable
data class MsAzureBucketData(override var name: String = "") : BucketData

@Serializable
data class StorageAccountKeysData(val keys: List<StorageAccountKeyData>)

@Serializable
data class StorageAccountKeyData(val keyName: String, val value: String)

class MsAzureBucket : CloudBucket {
    override var bucketData: BucketData = MsAzureBucketData()
}
