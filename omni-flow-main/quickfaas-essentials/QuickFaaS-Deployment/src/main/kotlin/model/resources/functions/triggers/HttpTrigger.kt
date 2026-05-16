/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.triggers

class HttpTrigger : Trigger {
    override val name = "HTTP"
    override val shortName = "http"
    override val postDeploymentMsg = "trigger URL"
}
