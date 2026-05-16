/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.runtimes

import model.Utils.FUNC_DEPLOYMENT
import model.Utils.FUNC_TEMPLATES
import model.Utils.TEMPORARY

enum class Runtime(
    val runtimeName: String,
    val shortName: String,
    val language: Language,
    val dependsSyntax: String,
    val dependsExtension: String
) {
    JAVA(
        runtimeName = "Java",
        shortName = "java",
        language = Language.JAVA,
        dependsSyntax = "<dependencies>\n    \n</dependencies>",
        dependsExtension = ".xml"
    ),
    NODEJS(
        runtimeName = "Node.js",
        shortName = "nodejs",
        language = Language.JAVASCRIPT,
        dependsSyntax = "",
        dependsExtension = ".json"
    );

    val templatesDirRoot = "${FUNC_TEMPLATES}/${this.shortName}"
    val deploymentDir = "$FUNC_DEPLOYMENT/${this.shortName}"
    val tmpDirsRoot = "$deploymentDir/$TEMPORARY"

}
