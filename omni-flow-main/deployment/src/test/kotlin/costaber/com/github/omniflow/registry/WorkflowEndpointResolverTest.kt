package costaber.com.github.omniflow.registry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals

class WorkflowEndpointResolverTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `registry key helpers follow functionRef-dot-host and functionRef-dot-path convention`() {
        assertEquals("fraud-check.host", FunctionEndpointKeys.hostKey("fraud-check"))
        assertEquals("fraud-check.path", FunctionEndpointKeys.pathKey("fraud-check"))

        val (h, p) = WorkflowInternalEndpointResolver.registryKeys("risk-score")
        assertEquals("risk-score.host", h)
        assertEquals("risk-score.path", p)
    }

    @Test
    fun `resolveHostAndPath resolves internal call when host and path are registry keys`() {
        val registryFile = tempDir.resolve("function-registry.json")
        val store = FunctionRegistryStore(registryFile)
        store.upsert("fraud-check", host = "fraud.internal.example", path = "/v1/fraud-check")

        val (resolvedHost, resolvedPath) = WorkflowInternalEndpointResolver.resolveHostAndPath(
            host = "fraud-check.host",
            path = "fraud-check.path",
            registryFile = registryFile
        )

        assertEquals("fraud.internal.example", resolvedHost)
        assertEquals("/v1/fraud-check", resolvedPath)
    }

    @Test
    fun `resolveHostAndPath leaves external call unchanged when host and path are literal`() {
        val registryFile = tempDir.resolve("function-registry.json")

        val (resolvedHost, resolvedPath) = WorkflowInternalEndpointResolver.resolveHostAndPath(
            host = "api.partner.example",
            path = "/v1/risk-score",
            registryFile = registryFile
        )

        assertEquals("api.partner.example", resolvedHost)
        assertEquals("/v1/risk-score", resolvedPath)
    }

    @Test
    fun `resolveHostAndPath leaves call unchanged when registry key pair is inconsistent`() {
        val registryFile = tempDir.resolve("function-registry.json")

        val (resolvedHost, resolvedPath) = WorkflowInternalEndpointResolver.resolveHostAndPath(
            host = "fraud-check.host",
            path = "risk-score.path",
            registryFile = registryFile
        )

        // host/path disagree -> treat as non-registry and keeps values as-is
        assertEquals("fraud-check.host", resolvedHost)
        assertEquals("risk-score.path", resolvedPath)
    }

    @Test
    fun `resolveHostAndPath can resolve using explicit functionRef if host and path are not keys`() {
        val registryFile = tempDir.resolve("function-registry.json")
        val store = FunctionRegistryStore(registryFile)
        store.upsert("risk-score", host = "risk.internal.example", path = "/v1/risk-score")

        val (resolvedHost, resolvedPath) = WorkflowInternalEndpointResolver.resolveHostAndPath(
            host = "ignored-host.example",
            path = "/ignored",
            registryFile = registryFile,
            functionRef = "risk-score"
        )

        assertEquals("risk.internal.example", resolvedHost)
        assertEquals("/v1/risk-score", resolvedPath)
    }
}