package costaber.com.github.omniflow.internalfunction.quickfaas

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test

internal class QuickFaasDescriptorLoaderTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `load valid descriptor`() {
        val content = """
        {
          "cloudProvider": "gcp",
          "accessToken": "ya29.token",
          "project": "test-project",
          "functionFile": "Main.java",
          "function": {
            "name": "greeting-fn",
            "location": "europe-west1",
            "runtime": "java17",
            "bucket": "my-bucket"
          }
        }
        """.trimIndent()
        val file = tempDir.resolve("func-deployment.json")
        Files.writeString(file, content)

        val descriptor = QuickFaasDescriptorLoader.load(file)

        expectThat(descriptor.cloudProvider).isEqualTo("gcp")
        expectThat(descriptor.accessToken).isEqualTo("ya29.token")
        expectThat(descriptor.project).isEqualTo("test-project")
        expectThat(descriptor.functionFile).isEqualTo("Main.java")
        expectThat(descriptor.function).isNotNull()
        expectThat(descriptor.function!!.name).isEqualTo("greeting-fn")
        expectThat(descriptor.function!!.location).isEqualTo("europe-west1")
        expectThat(descriptor.function!!.runtime).isEqualTo("java17")
        expectThat(descriptor.function!!.bucket).isEqualTo("my-bucket")
    }

    @Test
    fun `load throws when file does not exist`() {
        val nonExistent = tempDir.resolve("does-not-exist.json")

        assertThrows<IllegalArgumentException> {
            QuickFaasDescriptorLoader.load(nonExistent)
        }
    }

    @Test
    fun `validate throws when cloudProvider is missing`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = null,
            function = FunctionDescriptor(name = "test-fn")
        )

        assertThrows<IllegalStateException> {
            QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
        }
    }

    @Test
    fun `validate throws when cloud provider mismatches`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "aws",
            function = FunctionDescriptor(name = "test-fn")
        )

        assertThrows<IllegalStateException> {
            QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
        }
    }

    @Test
    fun `validate throws when function name is missing`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "gcp",
            function = FunctionDescriptor(name = null, location = "europe-west1")
        )

        assertThrows<IllegalArgumentException> {
            QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
        }
    }

    @Test
    fun `validate throws when runtime is invalid`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "gcp",
            function = FunctionDescriptor(name = "test-fn", runtime = "python39")
        )

        assertThrows<IllegalArgumentException> {
            QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
        }
    }

    @Test
    fun `validate passes for java17`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "gcp",
            function = FunctionDescriptor(name = "test-fn", runtime = "java17")
        )
        QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
    }

    @Test
    fun `validate passes for java21`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "gcp",
            function = FunctionDescriptor(name = "test-fn", runtime = "java21")
        )
        QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
    }

    @Test
    fun `validate passes for nodejs20`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "gcp",
            function = FunctionDescriptor(name = "test-fn", runtime = "nodejs20")
        )
        QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
    }

    @Test
    fun `validate passes when runtime is null`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "gcp",
            function = FunctionDescriptor(name = "test-fn", runtime = null)
        )
        QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
    }

    @Test
    fun `validate passes for aws with java17`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "aws",
            function = FunctionDescriptor(name = "my-lambda", runtime = "java17")
        )
        QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "aws")
    }

    @Test
    fun `validate passes for aws with java21`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "aws",
            function = FunctionDescriptor(name = "my-lambda", runtime = "java21")
        )
        QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "aws")
    }

    @Test
    fun `validate passes for aws with nodejs20x`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "aws",
            function = FunctionDescriptor(name = "my-lambda", runtime = "nodejs20.x")
        )
        QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "aws")
    }

    @Test
    fun `validate throws when runtime is invalid for aws`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "aws",
            function = FunctionDescriptor(name = "my-lambda", runtime = "nodejs20")
        )

        assertThrows<IllegalArgumentException> {
            QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "aws")
        }
    }

    @Test
    fun `validate throws when cloud provider is aws but expected gcp`() {
        val descriptor = QuickFaasDescriptor(
            cloudProvider = "aws",
            function = FunctionDescriptor(name = "my-lambda")
        )

        assertThrows<IllegalStateException> {
            QuickFaasDescriptorLoader.validate(descriptor, expectedCloudProvider = "gcp")
        }
    }

    @Test
    fun `load valid aws descriptor with iamRoleArn`() {
        val content = """
        {
          "cloudProvider": "aws",
          "iamRoleArn": "arn:aws:iam::123456789012:role/OmniFlowLambdaExecutionRole",
          "project": "123456789012",
          "functionFile": "MyFunctionClass.java",
          "function": {
            "name": "my-lambda",
            "location": "eu-west-1",
            "runtime": "java17",
            "bucket": "my-s3-bucket"
          }
        }
        """.trimIndent()
        val file = tempDir.resolve("func-deployment.json")
        Files.writeString(file, content)

        val descriptor = QuickFaasDescriptorLoader.load(file)

        expectThat(descriptor.cloudProvider).isEqualTo("aws")
        expectThat(descriptor.iamRoleArn).isEqualTo("arn:aws:iam::123456789012:role/OmniFlowLambdaExecutionRole")
        expectThat(descriptor.project).isEqualTo("123456789012")
        expectThat(descriptor.function!!.name).isEqualTo("my-lambda")
        expectThat(descriptor.function!!.bucket).isEqualTo("my-s3-bucket")
        expectThat(descriptor.function!!.runtime).isEqualTo("java17")
        expectThat(descriptor.functionFile).isEqualTo("MyFunctionClass.java")
    }

    @Test
    fun `load aws descriptor with empty iamRoleArn`() {
        val content = """
        {
          "cloudProvider": "aws",
          "iamRoleArn": "",
          "project": "123456789012",
          "function": {
            "name": "my-lambda",
            "location": "eu-west-1",
            "runtime": "java17",
            "bucket": "my-s3-bucket"
          }
        }
        """.trimIndent()
        val file = tempDir.resolve("func-deployment-empty-arn.json")
        Files.writeString(file, content)

        val descriptor = QuickFaasDescriptorLoader.load(file)

        expectThat(descriptor.iamRoleArn).isEqualTo("")
    }

    @Test
    fun `load minimal descriptor with only required fields`() {
        val content = """
        {
          "cloudProvider": "gcp",
          "function": {
            "name": "my-function"
          }
        }
        """.trimIndent()
        val file = tempDir.resolve("minimal.json")
        Files.writeString(file, content)

        val descriptor = QuickFaasDescriptorLoader.load(file)

        expectThat(descriptor.cloudProvider).isEqualTo("gcp")
        expectThat(descriptor.accessToken).isNull()
        expectThat(descriptor.project).isNull()
        expectThat(descriptor.function!!.name).isEqualTo("my-function")
        expectThat(descriptor.function!!.runtime).isNull()
    }
}
