package model.resources.functions

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Exercises the REAL QuickFaaS AWS build pipeline: [AwsLambdaFunction.buildAndZip] ->
 * `AwsBuildScripts.javaBuildScript` -> `JavaUtils.mavenBuild` (an actual `mvn package` run via
 * Maven Invoker, using the Maven distribution bundled at `function-deployment/java/`) ->
 * `copyFatJarAsZip`. This is the production build step `controller.Main.main()` runs before
 * `deployZip(...)`.
 *
 * [AwsLambdaFunction.deployZip] (S3 upload + Lambda create/update) is NEVER called here — this
 * test genuinely goes through QuickFaaS's own code, but stops exactly at the local/cloud boundary.
 * No AWS/GCP API call is made; the only network activity is ordinary Maven dependency resolution
 * (aws-lambda-java-core, maven-shade-plugin), identical to any other Java build in this repo.
 *
 * Reuses the same source file Example 8 deploys (`functions/hello-lambda-fn/MyFunctionClass.java`).
 * Requires the test JVM's working directory to be `omni-flow-main/` (see the `workingDir` set on
 * `tasks.test` in build.gradle.kts) so the bundled-Maven/scratch-dir relative paths resolve.
 */
class AwsLambdaFunctionBuildIntegrationTest {

    private var producedTmpDir: File? = null

    @AfterTest
    fun cleanup() {
        producedTmpDir?.takeIf { it.exists() }?.deleteRecursively()
    }

    @Test
    fun `buildAndZip runs the real QuickFaaS Maven build and produces a Lambda zip (no cloud call)`() {
        val sourceFile = File("functions/hello-lambda-fn/MyFunctionClass.java")
        check(sourceFile.exists()) {
            "Fixture not found at ${sourceFile.absolutePath} - run with workingDir = omni-flow-main/"
        }

        val function = AwsLambdaFunction().apply {
            hookFunction.definition = sourceFile.readText()
            setRuntimeVersion("java17")
        }

        val tmpDir = function.buildAndZip("aws")
        producedTmpDir = tmpDir

        val zip = File(tmpDir, "function-source.zip")
        assertTrue(zip.exists(), "Expected the real QuickFaaS build to produce a zip at ${zip.path}")
        assertTrue(zip.length() > 0, "Built zip should not be empty")
        println("Real QuickFaaS-built AWS Lambda zip: ${zip.length()} bytes (${zip.length() / 1024.0} KB)")
    }
}
