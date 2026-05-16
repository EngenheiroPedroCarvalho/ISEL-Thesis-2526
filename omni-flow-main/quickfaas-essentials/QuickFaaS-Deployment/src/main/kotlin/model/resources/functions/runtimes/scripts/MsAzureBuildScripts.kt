/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions.runtimes.scripts

import model.Utils
import model.resources.functions.CloudFunction
import model.resources.functions.MsAzureFunction
import model.resources.functions.runtimes.utils.JavaUtils
import model.resources.functions.triggers.StorageTrigger

object MsAzureBuildScripts : CloudBuildScripts {

    override fun javaBuildScript(func: CloudFunction, templatesDir: String, tmpDir: String) {
        func as MsAzureFunction
        func.setFunctionApp()
        val signatureFile = func.runtimeVersion!!.runtime.language.getConfigurations().signatureFile
        val entryPoint = func.getEntryPoint()
        JavaUtils.let {
            var pomContent = it.readPom(templatesDir)
            pomContent = it.setPomDependencies(pomContent, func.hookFunction.dependencies)
            pomContent = it.setPomProperties(pomContent, arrayOf(Pair("functionAppName", func.functionApp)))
            var templateContent = it.readTemplateFile(entryPoint, templatesDir)
            templateContent = templateContent.replace("<name>", func.name).replace("<configs_file>", Utils.CONFIGS_FILE)
            when (func.trigger) {
                is StorageTrigger -> {
                    val storageTrigger = func.trigger as StorageTrigger
                    templateContent = templateContent.replace("<storage_account>", storageTrigger.bucketData!!.name)
                        .replace("<container>", func.container)
                }
            }
            it.copyTemplateFileToTmp("host.json", templatesDir, tmpDir)
            it.createPom(pomContent, tmpDir)
            it.createJavaSourceFile(entryPoint, templateContent, tmpDir)
            it.createJavaSourceFile(signatureFile, func.hookFunction.definition, tmpDir)
            it.createFileInTmp(Utils.CONFIGS_FILE, func.hookFunction.configurations, "$tmpDir/src/main/resources")
            it.mavenBuild(tmpDir)
            it.zipMavenBuild(tmpDir, "azure-functions/${func.functionApp}")
        }
    }

    override fun nodeJsBuildScript() {
        TODO("Not yet implemented")
    }
}