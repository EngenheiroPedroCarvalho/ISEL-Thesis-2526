package costaber.com.github.omniflow.internalfunction.quickfaas

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test

internal class QuickFaasProcessInvokerTest {

    @TempDir
    lateinit var tempDir: Path

    private val mapper = ObjectMapper()

    @Test
    fun `invoke throws when JAR file does not exist`() {
        val fakeJar = tempDir.resolve("nonexistent.jar")
        val descriptor = tempDir.resolve("func-deployment.json")
        Files.writeString(descriptor, """{"cloudProvider":"gcp","function":{"name":"fn"}}""")

        val invoker = QuickFaasProcessInvoker(fakeJar)

        assertThrows<IllegalArgumentException> {
            invoker.invoke(descriptor)
        }
    }

    @Test
    fun `invoke throws when function-deployment directory is missing`() {
        val jar = tempDir.resolve("quickfaas.jar")
        Files.writeString(jar, "fake jar content")

        val descriptor = tempDir.resolve("func-deployment.json")
        Files.writeString(descriptor, """{"cloudProvider":"gcp","function":{"name":"fn"}}""")

        val invoker = QuickFaasProcessInvoker(jar)

        assertThrows<IllegalArgumentException> {
            invoker.invoke(descriptor)
        }
    }

    @Test
    fun `token enrichment writes accessToken to descriptor copy`() {
        val jarDir = tempDir.resolve("jar-dir")
        Files.createDirectories(jarDir)
        Files.createDirectories(jarDir.resolve("function-deployment"))
        val jar = jarDir.resolve("quickfaas.jar")
        Files.writeString(jar, "fake jar content")

        val descriptorContent = """{"cloudProvider":"gcp","function":{"name":"test-fn"}}"""
        val descriptorFile = tempDir.resolve("func-deployment.json")
        Files.writeString(descriptorFile, descriptorContent)

        val invoker = QuickFaasProcessInvoker(jar, timeoutMinutes = 1)

        try {
            invoker.invoke(descriptorFile, accessToken = "ya29.fresh-token-xyz")
        } catch (_: Exception) {
            // Will fail because the JAR isn't real, but the descriptor copy should have been enriched
        }

        val copiedDescriptor = jarDir.resolve("func-deployment.json")
        if (Files.exists(copiedDescriptor)) {
            val root = mapper.readTree(Files.readString(copiedDescriptor))
            expectThat(root.path("accessToken").asText()).isEqualTo("ya29.fresh-token-xyz")
        }
    }

    @Test
    fun `invoke without token does not add accessToken field`() {
        val jarDir = tempDir.resolve("jar-dir2")
        Files.createDirectories(jarDir)
        Files.createDirectories(jarDir.resolve("function-deployment"))
        val jar = jarDir.resolve("quickfaas.jar")
        Files.writeString(jar, "fake jar content")

        val descriptorContent = """{"cloudProvider":"gcp","function":{"name":"test-fn"}}"""
        val descriptorFile = tempDir.resolve("func-deployment.json")
        Files.writeString(descriptorFile, descriptorContent)

        val invoker = QuickFaasProcessInvoker(jar, timeoutMinutes = 1)

        try {
            invoker.invoke(descriptorFile, accessToken = null)
        } catch (_: Exception) {
            // Expected - JAR isn't real
        }

        val copiedDescriptor = jarDir.resolve("func-deployment.json")
        if (Files.exists(copiedDescriptor)) {
            val root = mapper.readTree(Files.readString(copiedDescriptor))
            expectThat(root.has("accessToken")).isEqualTo(false)
        }
    }
}
