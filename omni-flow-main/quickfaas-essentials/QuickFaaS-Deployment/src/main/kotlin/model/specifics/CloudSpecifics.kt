/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.specifics

import model.DeploymentData

interface CloudSpecifics {
    /**
     * Sets cloud-specific data specified in [deploymentData].
     */
    fun setSpecifics(deploymentData: DeploymentData)
}
