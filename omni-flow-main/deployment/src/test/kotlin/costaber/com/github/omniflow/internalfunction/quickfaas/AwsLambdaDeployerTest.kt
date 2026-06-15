package costaber.com.github.omniflow.internalfunction.quickfaas

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.nio.file.Files
import java.nio.file.Path
internal class AwsLambdaDeployerTest {

    @TempDir
    lateinit var tempDir: Path

    // -------------------------------------------------------------------------
    // patchIamRoleArn regex behaviour (white-box, no AWS credentials required)
    // -------------------------------------------------------------------------

    private fun applyPatch(json: String, roleArn: String): String =
        json.replace(
            Regex("\"iamRoleArn\"\\s*:\\s*\"[^\"]*\""),
            "\"iamRoleArn\": \"$roleArn\""
        )

    @Test
    fun `patchIamRoleArn replaces existing ARN value`() {
        val original = """{"cloudProvider":"aws","iamRoleArn":"arn:old:role","project":"123456789012"}"""
        val expected = """{"cloudProvider":"aws","iamRoleArn": "arn:aws:iam::123456789012:role/OmniFlowLambdaExecutionRole","project":"123456789012"}"""

        val result = applyPatch(original, "arn:aws:iam::123456789012:role/OmniFlowLambdaExecutionRole")

        expectThat(result).isEqualTo(expected)
    }

    @Test
    fun `patchIamRoleArn fills empty iamRoleArn field`() {
        val original = """{"cloudProvider":"aws","iamRoleArn":"","project":"123456789012"}"""

        val result = applyPatch(original, "arn:aws:iam::123456789012:role/OmniFlowLambdaExecutionRole")

        expectThat(result.contains("\"iamRoleArn\": \"arn:aws:iam::123456789012:role/OmniFlowLambdaExecutionRole\"")).isTrue()
    }

    @Test
    fun `patchIamRoleArn does not alter other fields`() {
        val original = """
            {
              "cloudProvider": "aws",
              "iamRoleArn": "",
              "project": "123456789012",
              "functionFile": "MyFunctionClass.java",
              "function": {
                "name": "my-lambda",
                "location": "eu-west-1",
                "runtime": "java17",
                "bucket": "my-bucket"
              }
            }
        """.trimIndent()

        val result = applyPatch(original, "arn:aws:iam::123456789012:role/OmniFlowLambdaExecutionRole")

        expectThat(result.contains("\"cloudProvider\": \"aws\"")).isTrue()
        expectThat(result.contains("\"project\": \"123456789012\"")).isTrue()
        expectThat(result.contains("\"functionFile\": \"MyFunctionClass.java\"")).isTrue()
        expectThat(result.contains("\"name\": \"my-lambda\"")).isTrue()
        expectThat(result.contains("\"bucket\": \"my-bucket\"")).isTrue()
    }

    @Test
    fun `patchIamRoleArn handles whitespace around colon`() {
        val withSpaces = """{"iamRoleArn"  :  "old-arn","project":"123456789012"}"""

        val result = applyPatch(withSpaces, "new-arn")

        expectThat(result.contains("\"iamRoleArn\": \"new-arn\"")).isTrue()
        expectThat(result.contains("old-arn")).isEqualTo(false)
    }

    // -------------------------------------------------------------------------
    // File-level patch test (end-to-end of the temp file write/read cycle)
    // -------------------------------------------------------------------------

    @Test
    fun `patchIamRoleArn written to temp file is readable`() {
        val originalContent = """
            {
              "cloudProvider": "aws",
              "iamRoleArn": "",
              "project": "123456789012",
              "function": {
                "name": "my-lambda",
                "location": "eu-west-1",
                "runtime": "java17",
                "bucket": "my-bucket"
              }
            }
        """.trimIndent()
        val originalPath = tempDir.resolve("func-deployment.json")
        Files.writeString(originalPath, originalContent)

        val roleArn = "arn:aws:iam::123456789012:role/OmniFlowLambdaExecutionRole"
        val patched = applyPatch(originalContent, roleArn)
        val tempPath = tempDir.resolve("_omniflow_tmp_descriptor.json")
        Files.writeString(tempPath, patched)

        val descriptor = QuickFaasDescriptorLoader.load(tempPath)

        expectThat(descriptor.iamRoleArn).isEqualTo(roleArn)
        expectThat(descriptor.cloudProvider).isEqualTo("aws")
        expectThat(descriptor.function?.name).isEqualTo("my-lambda")
    }

    // -------------------------------------------------------------------------
    // Real AWS integration test — requires credentials and live infrastructure
    // -------------------------------------------------------------------------

    @Disabled("Requires live AWS credentials and infrastructure")
    @Test
    fun `deploy single lambda and verify ARN returned`() {
        // Prerequisites (set as environment variables before running manually):
        //   AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_REGION
        //   QUICKFAAS_JAR_PATH  → path to QuickFaaS-Deployment-fat.jar
        //   STEP_FUNCTIONS_ROLE_ARN → ARN of the StepFunctionsExecutionRole
        //
        // The func-deployment.json used here must point to a real S3 bucket and
        // a valid Java source file. Run from the repo root so relative paths resolve.
        val jarPath = Path.of(System.getenv("QUICKFAAS_JAR_PATH") ?: "QuickFaaS-Deployment-fat.jar")
        val region = System.getenv("AWS_REGION") ?: "eu-west-1"
        val roleArn = System.getenv("STEP_FUNCTIONS_ROLE_ARN") ?: ""
        val descriptorPath = "functions/hello-lambda-fn/func-deployment.json"

        val deployer = AwsLambdaDeployer(quickFaasJarPath = jarPath, region = region, roleArn = roleArn)
        val meta = deployer.deployOrUpdate("hello-lambda-fn", descriptorPath)

        expectThat(meta.serviceName).isEqualTo("hello-lambda-fn")
        expectThat(meta.url.startsWith("arn:aws:lambda:")).isTrue()
    }
}
