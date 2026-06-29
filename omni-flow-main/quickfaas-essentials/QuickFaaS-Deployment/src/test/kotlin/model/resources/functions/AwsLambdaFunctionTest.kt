/*
 * Copyright © 2024, QuickFaaS
 *
 * Unit tests for [model.resources.functions.AwsLambdaFunction].
 *
 * Scope: LOCAL logic only. We assert the statically-declared locations, the
 * supported runtimes, the entry point / handler literal, and that the runtime
 * string pieces deployZip() builds ("java" + version) are correct — WITHOUT
 * calling deployZip() (which talks to S3 + Lambda over the network).
 */

package model.resources.functions

import model.resources.functions.runtimes.Runtime
import model.resources.functions.runtimes.RuntimeVersion
import model.resources.functions.runtimes.scripts.AwsBuildScripts
import model.resources.functions.triggers.HttpTrigger
import model.resources.functions.triggers.StorageTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AwsLambdaFunctionTest {

    @Test
    fun locations_areTheThreeSupportedAwsRegions() {
        val fn = AwsLambdaFunction()
        assertEquals(listOf("eu-west-1", "eu-west-2", "eu-central-1"), fn.locations)
    }

    @Test
    fun runtimes_areJava11AndJava17() {
        val fn = AwsLambdaFunction()
        assertEquals(
            listOf(RuntimeVersion.JAVA11, RuntimeVersion.JAVA17),
            fn.runtimes.toList()
        )
        // Both supported runtimes are Java.
        fn.runtimes.forEach { assertEquals(Runtime.JAVA, it.runtime) }
    }

    @Test
    fun runtimeStringLiteral_isJavaPlusVersion() {
        // deployZip() builds the AWS runtime identifier as "java" + runtimeVersion.version.
        // We verify the constituent literals here without invoking deployZip().
        assertEquals("11", RuntimeVersion.JAVA11.version)
        assertEquals("17", RuntimeVersion.JAVA17.version)
        assertEquals("java11", "java${RuntimeVersion.JAVA11.version}")
        assertEquals("java17", "java${RuntimeVersion.JAVA17.version}")
    }

    @Test
    fun entryPointAndHandler_areAwsHttpTemplate() {
        val fn = AwsLambdaFunction()
        // getEntryPoint() and the handler literal used by deployZip() are both "AwsHttpTemplate".
        assertEquals("AwsHttpTemplate", fn.getEntryPoint())
    }

    @Test
    fun defaults_nameBlank_locationBlank_runtimeNull_arnBlank() {
        val fn = AwsLambdaFunction()
        assertEquals("", fn.name)
        assertEquals("", fn.location)
        assertEquals("", fn.iamRoleArn)
        assertTrue(fn.runtimeVersion == null, "runtimeVersion should default to null")
    }

    @Test
    fun triggers_httpFirst_thenStorage_andDefaultTriggerIsHttp() {
        val fn = AwsLambdaFunction()
        assertTrue(fn.triggers[0] is HttpTrigger)
        assertTrue(fn.triggers[1] is StorageTrigger)
        // Default selected trigger is the first (HTTP).
        assertSame(fn.triggers[0], fn.trigger)
    }

    @Test
    fun buildScripts_wiredToAwsBuildScriptsSingleton() {
        val fn = AwsLambdaFunction()
        assertSame(AwsBuildScripts, fn.buildScripts)
    }
}
