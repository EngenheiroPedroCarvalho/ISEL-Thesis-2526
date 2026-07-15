package costaber.com.github.omniflow.registry

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/**
 * Unit tests for [FunctionEndpointKeys] (registry key generation) and the
 * [FunctionInvocationMetadata] value object.
 *
 * Pure in-memory logic: no file I/O, no AWS/GCP SDK, no HTTP, no network.
 */
internal class FunctionRegistryKeysAndMetadataTest {

    @Test
    fun `hostKey appends host suffix to the function reference`() {
        expectThat(FunctionEndpointKeys.hostKey("greeting-fn")).isEqualTo("greeting-fn.host")
    }

    @Test
    fun `pathKey appends path suffix to the function reference`() {
        expectThat(FunctionEndpointKeys.pathKey("greeting-fn")).isEqualTo("greeting-fn.path")
    }

    @Test
    fun `keys returns the host and path pair`() {
        val keys = FunctionEndpointKeys.keys("greeting-fn")
        expectThat(keys.first).isEqualTo("greeting-fn.host")
        expectThat(keys.second).isEqualTo("greeting-fn.path")
    }

    @Test
    fun `keys is consistent with hostKey and pathKey`() {
        val ref = "eu-west-1/sum-fn"
        val (host, path) = FunctionEndpointKeys.keys(ref)
        expectThat(host).isEqualTo(FunctionEndpointKeys.hostKey(ref))
        expectThat(path).isEqualTo(FunctionEndpointKeys.pathKey(ref))
    }

    @Test
    fun `key generation handles a regionally qualified reference`() {
        expectThat(FunctionEndpointKeys.hostKey("eu-west-1/greeting-fn"))
            .isEqualTo("eu-west-1/greeting-fn.host")
        expectThat(FunctionEndpointKeys.pathKey("eu-west-1/greeting-fn"))
            .isEqualTo("eu-west-1/greeting-fn.path")
    }

    @Test
    fun `metadata exposes serviceName and url`() {
        val meta = FunctionInvocationMetadata(
            serviceName = "greeting-service",
            url = "https://greeting.example.com/invoke"
        )
        expectThat(meta.serviceName).isEqualTo("greeting-service")
        expectThat(meta.url).isEqualTo("https://greeting.example.com/invoke")
    }

    @Test
    fun `metadata data class equality is value-based`() {
        val a = FunctionInvocationMetadata("svc", "https://h/p")
        val b = FunctionInvocationMetadata("svc", "https://h/p")
        expectThat(a).isEqualTo(b)
    }

    @Test
    fun `metadata copy overrides only the requested field`() {
        val original = FunctionInvocationMetadata("svc", "https://h/p")
        val copy = original.copy(url = "https://h/p2")
        expectThat(copy.serviceName).isEqualTo("svc")
        expectThat(copy.url).isEqualTo("https://h/p2")
    }
}
