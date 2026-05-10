package costaber.com.github.omniflow.internalfunction.quickfaas

import com.fasterxml.jackson.databind.ObjectMapper
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import org.junit.jupiter.api.Test

internal class QuickFaasDescriptorTest {

    private val mapper = ObjectMapper()

    @Test
    fun `deserialize full descriptor`() {
        val json = """
        {
          "cloudProvider": "gcp",
          "accessToken": "ya29.test-token",
          "project": "my-project",
          "functionFile": "MyFunction.java",
          "function": {
            "name": "greeting-fn",
            "location": "europe-west1",
            "runtime": "java17",
            "bucket": "my-bucket",
            "trigger": { "type": "HTTP" }
          }
        }
        """.trimIndent()

        val descriptor = mapper.readValue(json, QuickFaasDescriptor::class.java)

        expectThat(descriptor.cloudProvider).isEqualTo("gcp")
        expectThat(descriptor.accessToken).isEqualTo("ya29.test-token")
        expectThat(descriptor.project).isEqualTo("my-project")
        expectThat(descriptor.functionFile).isEqualTo("MyFunction.java")
        expectThat(descriptor.function).isNotNull()
        expectThat(descriptor.function!!.name).isEqualTo("greeting-fn")
        expectThat(descriptor.function!!.location).isEqualTo("europe-west1")
        expectThat(descriptor.function!!.runtime).isEqualTo("java17")
        expectThat(descriptor.function!!.bucket).isEqualTo("my-bucket")
        expectThat(descriptor.function!!.trigger).isNotNull()
        expectThat(descriptor.function!!.trigger!!.type).isEqualTo("HTTP")
    }

    @Test
    fun `deserialize with missing optional fields`() {
        val json = """
        {
          "cloudProvider": "gcp",
          "function": {
            "name": "my-fn"
          }
        }
        """.trimIndent()

        val descriptor = mapper.readValue(json, QuickFaasDescriptor::class.java)

        expectThat(descriptor.cloudProvider).isEqualTo("gcp")
        expectThat(descriptor.accessToken).isNull()
        expectThat(descriptor.project).isNull()
        expectThat(descriptor.functionFile).isNull()
        expectThat(descriptor.function!!.location).isNull()
        expectThat(descriptor.function!!.runtime).isNull()
        expectThat(descriptor.function!!.bucket).isNull()
        expectThat(descriptor.function!!.trigger).isNull()
    }

    @Test
    fun `unknown fields are ignored`() {
        val json = """
        {
          "cloudProvider": "gcp",
          "unknownField": "should be ignored",
          "function": {
            "name": "my-fn",
            "extraField": 42
          }
        }
        """.trimIndent()

        val descriptor = mapper.readValue(json, QuickFaasDescriptor::class.java)

        expectThat(descriptor.cloudProvider).isEqualTo("gcp")
        expectThat(descriptor.function!!.name).isEqualTo("my-fn")
    }

    @Test
    fun `accessToken field is deserialized`() {
        val json = """
        {
          "cloudProvider": "gcp",
          "accessToken": "ya29.fresh-token-12345",
          "function": { "name": "test-fn" }
        }
        """.trimIndent()

        val descriptor = mapper.readValue(json, QuickFaasDescriptor::class.java)
        expectThat(descriptor.accessToken).isEqualTo("ya29.fresh-token-12345")
    }

    @Test
    fun `trigger field is deserialized`() {
        val json = """
        {
          "cloudProvider": "gcp",
          "function": {
            "name": "test-fn",
            "trigger": { "type": "HTTP" }
          }
        }
        """.trimIndent()

        val descriptor = mapper.readValue(json, QuickFaasDescriptor::class.java)
        expectThat(descriptor.function!!.trigger).isNotNull()
        expectThat(descriptor.function!!.trigger!!.type).isEqualTo("HTTP")
    }
}
