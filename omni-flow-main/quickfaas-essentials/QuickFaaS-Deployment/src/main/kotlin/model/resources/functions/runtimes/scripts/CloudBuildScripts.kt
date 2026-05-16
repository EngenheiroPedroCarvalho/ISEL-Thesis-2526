/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.runtimes.scripts

import model.resources.functions.CloudFunction

interface CloudBuildScripts {
    /**
     * Build java project in a temporary directory [tmpDir], for a given [func].
     * Cloud-specific sources are found in [templatesDir].
     */
    fun javaBuildScript(func: CloudFunction, templatesDir: String, tmpDir: String)

    // TODO: To be implemented
    fun nodeJsBuildScript()
}