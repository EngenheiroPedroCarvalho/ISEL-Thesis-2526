package costaber.com.github.omniflow.registry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

/**
 * Parity + branch coverage for [FunctionRegistryStore.resolveUrl] and the pure
 * [FunctionRegistryStore.resolveUrlIn] extracted for the P7 optimization.
 *
 * Purely local: a real store backed by a @TempDir JSON file. No AWS/GCP.
 * Guarantees the read-once path (`resolveUrlIn(name, readAll())`) is behaviourally
 * identical to the legacy per-call path (`resolveUrl(name)`), across the exact,
 * regional-suffix, ambiguous and missing cases.
 */
internal class FunctionRegistryStoreResolveTest {

    @TempDir
    lateinit var tempDir: Path

    private fun storeWith(vararg entries: Pair<String, String>): FunctionRegistryStore {
        val store = FunctionRegistryStore(tempDir.resolve("function-registry.json"))
        store.writeNew(entries.associate { (k, v) -> k to FunctionInvocationMetadata(serviceName = k, url = v) })
        return store
    }

    @Test
    fun `resolveUrlIn matches resolveUrl for an exact key`() {
        val store = storeWith("greeting-fn" to "https://host.example.com/greeting")
        val all = store.readAll()

        expectThat(store.resolveUrlIn("greeting-fn", all)).isEqualTo("https://host.example.com/greeting")
        expectThat(store.resolveUrlIn("greeting-fn", all)).isEqualTo(store.resolveUrl("greeting-fn"))
    }

    @Test
    fun `resolveUrlIn resolves a regional suffix key`() {
        val store = storeWith("europe-west1/greeting-fn" to "https://eu.example.com/greeting")
        val all = store.readAll()

        expectThat(store.resolveUrlIn("greeting-fn", all)).isEqualTo("https://eu.example.com/greeting")
        expectThat(store.resolveUrlIn("greeting-fn", all)).isEqualTo(store.resolveUrl("greeting-fn"))
    }

    @Test
    fun `resolveUrlIn throws on ambiguous regional keys`() {
        val store = storeWith(
            "eu/greeting-fn" to "https://eu.example.com/greeting",
            "us/greeting-fn" to "https://us.example.com/greeting"
        )
        val all = store.readAll()

        assertThrows<IllegalStateException> { store.resolveUrlIn("greeting-fn", all) }
        assertThrows<IllegalStateException> { store.resolveUrl("greeting-fn") }
    }

    @Test
    fun `resolveUrlIn throws when the function is missing`() {
        val store = storeWith("other-fn" to "https://host.example.com/other")

        assertThrows<IllegalStateException> { store.resolveUrlIn("greeting-fn", store.readAll()) }
        assertThrows<IllegalStateException> { store.resolveUrl("greeting-fn") }
    }
}
