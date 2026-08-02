package costaber.com.github.omniflow.registry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.nio.file.Path

/**
 * Unit tests for [FunctionRegistryBootstrapper] against a fake [CloudFunctionsCatalog]:
 * pure local logic (file existence check + write), no cloud SDK calls.
 */
internal class FunctionRegistryBootstrapperTest {

    @TempDir
    lateinit var tempDir: Path

    private class FakeCatalog(private val functions: Map<String, FunctionInvocationMetadata>) : CloudFunctionsCatalog {
        var callCount = 0
            private set
        var lastScope: String? = null
            private set

        override fun listHttpFunctions(scope: String): Map<String, FunctionInvocationMetadata> {
            callCount++
            lastScope = scope
            return functions
        }
    }

    @Test
    fun `populates the registry from the catalog when the file is missing`() {
        val store = FunctionRegistryStore(tempDir.resolve("function-registry.json"))
        val catalog = FakeCatalog(
            mapOf(
                "hello-fn" to FunctionInvocationMetadata(
                    serviceName = "hello-fn",
                    url = "arn:aws:lambda:eu-west-1:123456789012:function:hello-fn"
                )
            )
        )

        FunctionRegistryBootstrapper(store, catalog).bootstrapIfMissing("eu-west-1")

        expectThat(store.exists()).isTrue()
        expectThat(store.readAll()).hasSize(1)
        expectThat(store.readAll()["hello-fn"]).isEqualTo(
            FunctionInvocationMetadata(
                serviceName = "hello-fn",
                url = "arn:aws:lambda:eu-west-1:123456789012:function:hello-fn"
            )
        )
        expectThat(catalog.callCount).isEqualTo(1)
        expectThat(catalog.lastScope).isEqualTo("eu-west-1")
    }

    @Test
    fun `does not call the catalog when a registry already exists`() {
        val store = FunctionRegistryStore(tempDir.resolve("function-registry.json"))
        store.writeNew(emptyMap())
        val catalog = FakeCatalog(
            mapOf("should-not-appear" to FunctionInvocationMetadata("x", "y"))
        )

        FunctionRegistryBootstrapper(store, catalog).bootstrapIfMissing("eu-west-1")

        expectThat(catalog.callCount).isEqualTo(0)
        expectThat(store.readAll()).hasSize(0)
    }
}
