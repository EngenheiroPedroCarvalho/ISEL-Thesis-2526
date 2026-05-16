/*
 * Copyright © 9/15/2022, Pexers (https://github.com/Pexers)
 */

package model.resources.functions

import controller.General.AUTO_DELETE_FUNC_ZIP
import controller.General.logMessage
import controller.logPropertyMissing
import model.TriggerDeploymentData
import model.Utils
import model.projects.ProjectData
import model.resources.buckets.BucketData
import model.resources.buckets.CloudBucket
import model.resources.functions.runtimes.Runtime
import model.resources.functions.runtimes.RuntimeVersion
import model.resources.functions.runtimes.scripts.CloudBuildScripts
import model.resources.functions.triggers.StorageTrigger
import model.resources.functions.triggers.Trigger
import java.io.File

interface CloudFunction {
    var name: String
    var hookFunction: HookFunction
    var buildScripts: CloudBuildScripts
    val bucket: CloudBucket
    val locations: List<String>
    var location: String
    val triggers: List<Trigger>
    var trigger: Trigger
    val runtimes: Array<RuntimeVersion>
    var runtimeVersion: RuntimeVersion?

    /**
     * Builds the necessary sources and zips the results to be
     * deployed to the cloud provider [cpShortName].
     */
    fun buildAndZip(cpShortName: String): File {  // TODO: try to remove cpShortName parameter
        var tmpDir = File("")
        try {
            tmpDir = Utils.createTempDir(runtimeVersion!!.runtime.tmpDirsRoot).toFile()
            // resources/function-templates/{runtime}/{cloudProvider}/{trigger}
            val templatesDir = "$cpShortName/${trigger.shortName}"
            when (runtimeVersion!!.runtime) {
                Runtime.JAVA -> {
                    logMessage("Building maven project...", 2)
                    buildScripts.javaBuildScript(this, templatesDir, tmpDir.name)
                }
                else -> throw NotImplementedError()
            }
        } catch (e: Exception) {
            if (AUTO_DELETE_FUNC_ZIP && tmpDir.path.isNotEmpty()) tmpDir.deleteRecursively()
            throw e
        }
        return tmpDir
    }

    /**
     * Deploys the ZIP file placed in [zipFilePath]
     * to the FaaS resource held by the project [projData].
     */
    suspend fun deployZip(zipFilePath: String, projData: ProjectData): DeploymentTimeData

    /**
     * Returns the FaaS resource entry point. Should be pointing to a template file
     */
    fun getEntryPoint(): String

    /**
     * Returns the URL that triggers the function's execution (HTTP trigger)
     * held by a given project [projData].
     */
    fun getTriggerUrl(projData: ProjectData): Pair<String, String>

    /**
     * Sets the [triggerData] for the function.
     */
    // TODO: Find a better way to give Storage Trigger access to project buckets (remove projectBuckets param)
    fun setTrigger(triggerData: TriggerDeploymentData, projectBuckets: List<BucketData>) {
        val trigger = triggers.find { trigger -> trigger.shortName == triggerData.type }
        if (trigger == null) {
            logPropertyMissing("function.trigger.type", triggerData.type)
            return
        }
        this.trigger = trigger
        runtimeVersion = null
        hookFunction.definition = ""
        hookFunction.dependencies = ""

        when (trigger) {
            is StorageTrigger -> {
                val triggerBucket = projectBuckets.find { bucket -> bucket.name == triggerData.bucket }
                if (triggerBucket == null) {
                    logPropertyMissing("function.trigger.bucket", triggerData.bucket ?: "")
                    return
                }
                trigger.bucketData = triggerBucket
                val eventType =
                    StorageTrigger.EventType.values().find { type -> type.eventName == triggerData.eventType }
                if (eventType == null) {
                    logPropertyMissing("function.trigger.eventType", triggerData.eventType ?: "")
                    return
                }
                trigger.eventType = eventType
            }
            else -> return //TODO.
        }
    }

    /**
     * Sets the [runtimeVersionName] for the function.
     */
    fun setRuntimeVersion(runtimeVersionName: String) {
        val (runtimeName, version) = runtimeVersionName.split(
            ("(?=\\d)(?<=\\D)").toRegex(), limit = 2
        )  // d: char, D:number
        val runtime = Runtime.values().find { runtime -> runtime.shortName == runtimeName }
        runtimeVersion = RuntimeVersion.values()
            .find { runtimeVersion -> runtimeVersion.runtime == runtime && runtimeVersion.version == version }
        if (runtimeVersion == null) {
            logPropertyMissing("function.runtime", runtimeVersionName)
            return
        }
        hookFunction.dependencies = runtimeVersion!!.runtime.dependsSyntax
    }
}
