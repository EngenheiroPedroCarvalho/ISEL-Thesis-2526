/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.triggers

import model.resources.buckets.BucketData

class StorageTrigger : Trigger {

    enum class EventType(val eventName: String) { CREATE("Create"), DELETE("Delete"), UPDATE("Update") }

    override val name = "Storage"
    override val shortName = "storage"
    override val postDeploymentMsg = "access storage buckets"

    var bucketData: BucketData? = null
    var eventType = EventType.CREATE
}
