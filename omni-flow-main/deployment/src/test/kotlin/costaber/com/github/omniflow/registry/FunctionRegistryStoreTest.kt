package costaber.com.github.omniflow.registry

import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Files
import java.nio.file.Path

class FunctionRegistryStoreTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loadOrCreate creates registry file when missing`() {
        val file = tempDir.resolve("function-registry.json")
        assertFalse(Files.exists(file))

        val store = FunctionRegistryStore(file)
        val registry = store.loadOrCreate()

        assertTrue(Files.exists(file))
        assertTrue(registry.updatedAt.isNotBlank())
        assertTrue(registry.functions.isEmpty())
    }

    @Test
    fun `ensurePlaceholders creates missing entries with host and path`(){
        val file = tempDir.resolve("function-registry.json")
        val store = FunctionRegistryStore(file)

        store.ensurePlaceholders(setOf("fraud-check"))

        val meta = store.resolve("fraud-check")

        assertEquals("", meta.host)
        assertEquals("", meta.path)
    }

    @Test
    fun `ensurePlaceholders does not override existing entries`(){
        val file = tempDir.resolve("functions-registry.json")
        val store = FunctionRegistryStore(file)

        store.upsert("fraud-check", host = "fraud.internal.example", path = "/v1/fraud-check")
        store.ensurePlaceholders(setOf("fraud-check"))

        val meta = store.resolve("fraud-check")
        assertEquals("fraud.internal.example", meta.host)
        assertEquals("/v1/fraud-check", meta.path)
    }

    @Test
    fun `upsert persists and reloads` () {
        val file = tempDir.resolve("functions-registry.json")
        val store = FunctionRegistryStore(file)

        val initialUpdatedAt = store.loadOrCreate().updatedAt

        store.upsert("risk-score", host = "risk.internal.example", path = "/v1/risk-score")

        val registry = store.loadOrCreate()
        assertTrue(registry.functions.containsKey("risk-score"))
        assertEquals("risk.internal.example", registry.functions["risk-score"]!!.host)
        assertEquals("/v1/risk-score", registry.functions["risk-score"]!!.path)
        assertNotEquals(initialUpdatedAt, registry.updatedAt)

        val raw = Files.readString(file)
        assertTrue(raw.contains("\"risk-score\""))
        assertTrue(raw.contains("\"host\": \"risk.internal.example\""))
        assertTrue(raw.contains("\"path\": \"/v1/risk-score\""))
    }

    @Test
    fun `resolve fails for missing functionRef`() {
        val file = tempDir.resolve("functions-registry.json")
        val store = FunctionRegistryStore(file)

        val ex = assertThrows(IllegalStateException::class.java){
            store.resolve("missing-ref")
        }

        assertTrue(ex.message!!.contains("Missing registry entry"))
    }

}