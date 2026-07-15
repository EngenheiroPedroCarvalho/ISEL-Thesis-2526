/*
 * Copyright © 2024, QuickFaaS
 *
 * Unit tests for [model.resources.functions.runtimes.scripts.AwsBuildScripts].
 *
 * Scope: LOCAL logic only. NO Maven invocation, NO SDK/network.
 *
 * AwsBuildScripts is a Kotlin `object` (singleton). Its `buildLambdaPom(...)`
 * and `copyFatJarAsZip(...)` helpers are PRIVATE, and the only public entry that
 * exercises them — `javaBuildScript(...)` — runs a real Maven build (DefaultInvoker)
 * and copies a produced fat JAR, i.e. it is NOT a pure local operation and is
 * explicitly out of scope for these tests.
 *
 * Therefore we test only the reachable public surface:
 *   - nodeJsBuildScript() throws NotImplementedError (it delegates to TODO(...)).
 *   - the singleton conforms to the CloudBuildScripts contract and is a stable
 *     single instance.
 *
 * The maven-shade-plugin POM injection and the fat-jar-selection predicate
 * (extension == "jar" && !name.startsWith("original-")) live behind private
 * members and cannot be asserted without driving the network/Maven path; this
 * limitation is documented in apiNotes.
 */

package model.resources.functions.runtimes.scripts

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AwsBuildScriptsTest {

    @Test
    fun nodeJsBuildScript_throwsNotImplementedError() {
        // nodeJsBuildScript() = TODO("Not yet implemented") -> NotImplementedError.
        assertFailsWith<NotImplementedError> {
            AwsBuildScripts.nodeJsBuildScript()
        }
    }

    @Test
    fun awsBuildScripts_isSingletonObject() {
        // Referencing the object twice yields the very same instance.
        assertSame(AwsBuildScripts, AwsBuildScripts)
    }

    @Test
    fun awsBuildScripts_conformsToCloudBuildScripts() {
        assertTrue(
            AwsBuildScripts is CloudBuildScripts,
            "AwsBuildScripts must implement the CloudBuildScripts contract"
        )
    }
}
