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
