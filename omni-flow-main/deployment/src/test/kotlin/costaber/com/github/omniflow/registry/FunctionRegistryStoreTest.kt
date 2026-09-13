package costaber.com.github.omniflow.registry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.nio.file.Files
import java.nio.file.Path

/**
 * Unit tests for [FunctionRegistryStore].
 *
 * Everything is exercised against a real on-disk JSON file under @TempDir: pure local
 * file I/O and JSON (de)serialization. No AWS/GCP SDK calls, no HTTP, no network.
 */
internal class FunctionRegistryStoreTest {

    @TempDir
    lateinit var tempDir: Path

    private fun store(fileName: String = "function-registry.json"): FunctionRegistryStore =
        FunctionRegistryStore(tempDir.resolve(fileName))

    @Test
    fun `writeNew then readAll round-trips entries`() {
        val store = store()
        val functions = mapOf(
            "greeting-fn" to FunctionInvocationMetadata(
                serviceName = "greeting-service",
                url = "https://greeting.example.com/invoke"
            ),
            "sum-fn" to FunctionInvocationMetadata(
                serviceName = "sum-service",
                url = "https://sum.example.com/invoke"
            )
        )

        store.writeNew(functions)
        val readBack = store.readAll()

        expectThat(readBack).hasSize(2)
        expectThat(readBack).containsKey("greeting-fn")
        expectThat(readBack).containsKey("sum-fn")
        expectThat(readBack["greeting-fn"]).isNotNull().and {
            get { serviceName }.isEqualTo("greeting-service")
            get { url }.isEqualTo("https://greeting.example.com/invoke")
        }
        expectThat(readBack["sum-fn"]).isNotNull().and {
            get { serviceName }.isEqualTo("sum-service")
            get { url }.isEqualTo("https://sum.example.com/invoke")
        }
    }

    @Test
    fun `writeNew creates the file`() {
        val store = store()
        expectThat(store.exists()).isFalse()

        store.writeNew(mapOf("fn" to FunctionInvocationMetadata("svc", "https://h/p")))

        expectThat(store.exists()).isTrue()
    }

    @Test
    fun `writeNew creates missing parent directories`() {
        val nested = tempDir.resolve("deep").resolve("nested").resolve("registry.json")
        val store = FunctionRegistryStore(nested)

        store.writeNew(mapOf("fn" to FunctionInvocationMetadata("svc", "https://h/p")))

        expectThat(store.exists()).isTrue()
        expectThat(store.readAll()).containsKey("fn")
    }

    @Test
    fun `readAll on non-existent registry returns empty map without exception`() {
        val store = store("does-not-exist.json")

        expectThat(store.exists()).isFalse()
        expectThat(store.readAll()).isEmpty()
    }

    @Test
    fun `readAll tolerates JSON without functions node`() {
        val file = tempDir.resolve("no-functions.json")
        Files.writeString(file, """{"updatedAt":"2026-01-01T00:00:00Z"}""")
        val store = FunctionRegistryStore(file)

        expectThat(store.readAll()).isEmpty()
    }

    @Test
    fun `readAll trims serviceName and url whitespace`() {
        val file = tempDir.resolve("whitespace.json")
        Files.writeString(
            file,
            """
            {
              "updatedAt": "2026-01-01T00:00:00Z",
              "functions": {
                "fn": { "serviceName": "  svc  ", "url": "  https://h/p  " }
              }
            }
            """.trimIndent()
        )
        val store = FunctionRegistryStore(file)

        val meta = store.readAll()["fn"]
        expectThat(meta).isNotNull().and {
            get { serviceName }.isEqualTo("svc")
            get { url }.isEqualTo("https://h/p")
        }
    }

    @Test
    fun `writeNew with empty map produces an empty functions object`() {
        val store = store("empty.json")

        store.writeNew(emptyMap())

        expectThat(store.exists()).isTrue()
        expectThat(store.readAll()).isEmpty()
    }

    @Test
    fun `resolveUrl returns exact match`() {
        val store = store()
        store.writeNew(mapOf("greeting-fn" to FunctionInvocationMetadata("svc", "https://greeting/invoke")))

        expectThat(store.resolveUrl("greeting-fn")).isEqualTo("https://greeting/invoke")
    }

    @Test
    fun `resolveUrl matches regional suffix key`() {
        val store = store()
        store.writeNew(mapOf("eu-west-1/greeting-fn" to FunctionInvocationMetadata("svc", "https://greeting/invoke")))

        expectThat(store.resolveUrl("greeting-fn")).isEqualTo("https://greeting/invoke")
    }

    @Test
    fun `resolveUrl throws when function is missing`() {
        val store = store()
        store.writeNew(mapOf("present" to FunctionInvocationMetadata("svc", "https://h/p")))

        assertThrows<IllegalStateException> {
            store.resolveUrl("missing")
        }
    }

    @Test
    fun `resolveUrl throws when function reference is ambiguous`() {
        val store = store()
        store.writeNew(
            mapOf(
                "eu-west-1/greeting-fn" to FunctionInvocationMetadata("svc", "https://a/invoke"),
                "us-east-1/greeting-fn" to FunctionInvocationMetadata("svc", "https://b/invoke")
            )
        )

        assertThrows<IllegalStateException> {
            store.resolveUrl("greeting-fn")
        }
    }

    @Test
    fun `tryResolveEntry returns null when missing`() {
        val store = store()
        store.writeNew(mapOf("present" to FunctionInvocationMetadata("svc", "https://h/p")))

        expectThat(store.tryResolveEntry("missing")).isEqualTo(null)
    }

    @Test
    fun `tryResolveEntry returns exact match pair`() {
        val store = store()
        store.writeNew(mapOf("greeting-fn" to FunctionInvocationMetadata("svc", "https://h/p")))

        val entry = store.tryResolveEntry("greeting-fn")
        expectThat(entry).isNotNull().and {
            get { first }.isEqualTo("greeting-fn")
            get { second.url }.isEqualTo("https://h/p")
        }
    }

    @Test
    fun `tryResolveEntry resolves regional suffix to fully qualified key`() {
        val store = store()
        store.writeNew(mapOf("eu-west-1/greeting-fn" to FunctionInvocationMetadata("svc", "https://h/p")))

        val entry = store.tryResolveEntry("greeting-fn")
        expectThat(entry).isNotNull().and {
            get { first }.isEqualTo("eu-west-1/greeting-fn")
            get { second.url }.isEqualTo("https://h/p")
        }
    }

    @Test
    fun `put adds an entry to an existing registry`() {
        val store = store()
        store.writeNew(mapOf("a" to FunctionInvocationMetadata("svcA", "https://a/p")))

        store.put("b", FunctionInvocationMetadata("svcB", "https://b/p"))

        val all = store.readAll()
        expectThat(all).hasSize(2)
        expectThat(all).containsKey("a")
        expectThat(all).containsKey("b")
        expectThat(all["b"]!!.url).isEqualTo("https://b/p")
    }

    @Test
    fun `put creates registry when file does not exist`() {
        val store = store("created-by-put.json")
        expectThat(store.exists()).isFalse()

        store.put("a", FunctionInvocationMetadata("svcA", "https://a/p"))

        expectThat(store.exists()).isTrue()
        expectThat(store.readAll()).containsKey("a")
    }

    @Test
    fun `remove deletes an entry`() {
        val store = store()
        store.writeNew(
            mapOf(
                "a" to FunctionInvocationMetadata("svcA", "https://a/p"),
                "b" to FunctionInvocationMetadata("svcB", "https://b/p")
            )
        )

        store.remove("a")

        val all = store.readAll()
        expectThat(all).hasSize(1)
        expectThat(all).containsKey("b")
    }

    @Test
    fun `remove on non-existent registry is a no-op`() {
        val store = store("absent.json")

        store.remove("anything")

        expectThat(store.exists()).isFalse()
    }

    @Test
    fun `resolveEntry throws when function is missing`() {
        val store = store()
        store.writeNew(mapOf("present" to FunctionInvocationMetadata("svc", "https://h/p")))

        assertThrows<IllegalStateException> {
            store.resolveEntry("missing")
        }
    }

    @Test
    fun `tryResolveEntryIn returns exact match pair against a pre-loaded map`() {
        val all = mapOf("greeting-fn" to FunctionInvocationMetadata("svc", "https://h/p"))

        val entry = store().tryResolveEntryIn("greeting-fn", all)

        expectThat(entry).isNotNull().and {
            get { first }.isEqualTo("greeting-fn")
            get { second.url }.isEqualTo("https://h/p")
        }
    }

    @Test
    fun `tryResolveEntryIn resolves regional suffix against a pre-loaded map`() {
        val all = mapOf("eu-west-1/greeting-fn" to FunctionInvocationMetadata("svc", "https://h/p"))

        val entry = store().tryResolveEntryIn("greeting-fn", all)

        expectThat(entry).isNotNull().and {
            get { first }.isEqualTo("eu-west-1/greeting-fn")
            get { second.url }.isEqualTo("https://h/p")
        }
    }

    @Test
    fun `tryResolveEntryIn returns null when missing from the pre-loaded map`() {
        val all = mapOf("present" to FunctionInvocationMetadata("svc", "https://h/p"))

        expectThat(store().tryResolveEntryIn("missing", all)).isEqualTo(null)
    }

    @Test
    fun `tryResolveEntryIn throws when ambiguous in the pre-loaded map`() {
        val all = mapOf(
            "eu-west-1/greeting-fn" to FunctionInvocationMetadata("svc", "https://a/invoke"),
            "us-east-1/greeting-fn" to FunctionInvocationMetadata("svc", "https://b/invoke")
        )

        assertThrows<IllegalStateException> {
            store().tryResolveEntryIn("greeting-fn", all)
        }
    }

    @Test
    fun `tryResolveEntry delegates to tryResolveEntryIn against a fresh read`() {
        val store = store()
        store.writeNew(mapOf("greeting-fn" to FunctionInvocationMetadata("svc", "https://h/p")))

        expectThat(store.tryResolveEntry("greeting-fn")).isEqualTo(store.tryResolveEntryIn("greeting-fn", store.readAll()))
    }

    @Test
    fun `put on an existing file does not add a stray updateAt key`() {
        val store = store()
        store.writeNew(mapOf("a" to FunctionInvocationMetadata("svcA", "https://a/p")))

        store.put("b", FunctionInvocationMetadata("svcB", "https://b/p"))

        val raw = Files.readString(tempDir.resolve("function-registry.json"))
        expectThat(raw).contains("\"updatedAt\"")
        expectThat(raw.contains("\"updateAt\"")).isFalse()
    }

    @Test
    fun `remove on an existing file does not add a stray updateAt key`() {
        val store = store()
        store.writeNew(
            mapOf(
                "a" to FunctionInvocationMetadata("svcA", "https://a/p"),
                "b" to FunctionInvocationMetadata("svcB", "https://b/p")
            )
        )

        store.remove("a")

        val raw = Files.readString(tempDir.resolve("function-registry.json"))
        expectThat(raw).contains("\"updatedAt\"")
        expectThat(raw.contains("\"updateAt\"")).isFalse()
    }
}
