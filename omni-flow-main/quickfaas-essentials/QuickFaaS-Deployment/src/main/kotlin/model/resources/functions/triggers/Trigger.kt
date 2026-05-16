/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.triggers

interface Trigger {
    val name: String
    val shortName: String
    val postDeploymentMsg: String
}
