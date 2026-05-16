/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.runtimes.scripts

import model.Utils
import model.resources.functions.CloudFunction
import model.resources.functions.runtimes.utils.JavaUtils

object GcpBuildScripts : CloudBuildScripts {

    override fun javaBuildScript(func: CloudFunction, templatesDir: String, tmpDir: String) {
        val signatureFile = func.runtimeVersion!!.runtime.language.getConfigurations().signatureFile
        val entryPoint = func.getEntryPoint()
        JavaUtils.let {
            var pomContent = it.readPom(templatesDir)
            pomContent = it.setPomDependencies(pomContent, func.hookFunction.dependencies)
            var templateContent = it.readTemplateFile(entryPoint, templatesDir)
            templateContent = templateContent.replace("<configs_file>", Utils.CONFIGS_FILE)
            it.createPom(pomContent, tmpDir)
            it.createJavaSourceFile(entryPoint, templateContent, tmpDir)
            it.createJavaSourceFile(signatureFile, func.hookFunction.definition, tmpDir)
            it.createFileInTmp(Utils.CONFIGS_FILE, func.hookFunction.configurations, "$tmpDir/src/main/resources")
            it.mavenBuild(tmpDir)
            it.zipMavenBuild(tmpDir, "gcp-function")
        }
    }

    override fun nodeJsBuildScript() {
        TODO("Not yet implemented")
    }
}