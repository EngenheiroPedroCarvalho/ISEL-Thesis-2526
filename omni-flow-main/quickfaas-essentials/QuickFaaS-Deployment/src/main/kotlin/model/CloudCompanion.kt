/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model

import model.requests.CloudRequests

interface CloudCompanion {
    val name: String
    val shortName: String
    val cloudRequests: CloudRequests

    /**
     * Instantiates a new [CloudProvider].
     */
    fun newCloudProvider(): CloudProvider
}
