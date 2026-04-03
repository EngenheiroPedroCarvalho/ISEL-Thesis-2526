package costaber.com.github.omniflow.registry

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.api.client.util.Key
import costaber.com.github.omniflow.jackson.OmniflowObjectMapper
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Ref
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories

class FunctionRegistryStore(
    private val file: Path,
    private val mapper: ObjectMapper = ObjectMapper()
) {
    fun exists(): Boolean = Files.exists(file)

    @Synchronized
    fun writeNew(functions: Map<String, FunctionInvocationMetadata>){
        file.parent?.let { Files.createDirectories(it) }

        val root = mapper.createObjectNode()
        root.put("updatedAt", Instant.now().toString())

        val functionsNode = root.putObject("functions")
        functions.forEach { (name, meta) ->
            val fn = functionsNode.putObject(name)
            fn.put("serviceName", meta.serviceName)
            fn.put("url", meta.url)
        }

        Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root))
    }

    /**
     * Reads all the registry
     */
    @Synchronized
    fun readAll(): Map<String, FunctionInvocationMetadata> {
        if (!exists()) return emptyMap()

        val json = mapper.readTree(Files.readString(file))
        val functionsNode = json.path("functions")
        if (!functionsNode.isObject) return emptyMap()

        val out = linkedMapOf<String, FunctionInvocationMetadata>()

        val names = functionsNode.fieldNames()
        while (names.hasNext()){
            val key = names.next()
            val value = functionsNode.get(key)

            val serviceName = value.path("serviceName").asText("").trim()

            val url = value.path("url").asText().trim()

            out[key] = FunctionInvocationMetadata(
                serviceName = serviceName,
                url = url
            )
        }

        return out
    }

    /**
     *Resolves a function reference to a registry entry without throwing when missing
     *
     * Resolution rules:
     * - exact match: functionRef
     * - suffix match: "region/functionRef"
     */
    @Synchronized
    fun tryResolveEntry(functionRef: String): Pair<String, FunctionInvocationMetadata>? {
        val all = readAll()

        all[functionRef]?.let { return functionRef to it }

        val matches = all.filterKeys { it.endsWith("/$functionRef") }
        return when (matches.size){
            1 -> matches.entries.first().let { it.key to it.value }
            0 -> null
            else -> throw IllegalStateException(
                "Internal function '$functionRef' is ambiguous in function-registry at '$file'." +
                        "Use fully-qualified key like 'region/$functionRef'."
            )
        }
    }

    @Synchronized
    fun resolveEntry(functionRef: String): Pair<String, FunctionInvocationMetadata> =
        tryResolveEntry(functionRef)
            ?: throw java.lang.IllegalStateException(
                "Internal function '$functionRef' not found in function-registry at '$file'"
            )

    @Synchronized
    fun put(key: String, meta: FunctionInvocationMetadata){
        val root = readRootOrNew()
        root.put("updatedAt", Instant.now().toString())

        val functionsNode = root.withObjectProperty("functions")
        val fn = functionsNode.putObject(key)
        fn.put("serviceName", meta.serviceName)
        fn.put("url", meta.url)


        writeRoot(root)
    }

    @Synchronized
    fun remove(key: String){
        if (!exists()) return

        val root = readRootOrNew()
        root.put("updatedAt", Instant.now().toString())

        val functionsNode = ensureObject(root, "functions")
        functionsNode.remove(key)

        writeRoot(root)
    }
    private fun readRootOrNew(): ObjectNode{
        if(!exists()){
            val root = mapper.createObjectNode()
            root.put("updatedAt", Instant.now().toString())
            root.putObject("functions")
            return root
        }

        val node = mapper.readTree(Files.readString(file))
        if (node is ObjectNode) {
            if (!node.has("updateAt")) node.put("updateAt", Instant.now().toString())
            return node
        }

        val root = mapper.createObjectNode()
        root.put("updatedAt", Instant.now().toString())
        root.putObject("functions")
        return root
    }

    private fun writeRoot(root: ObjectNode){
        file.parent?.let { Files.createDirectories(it) }
        Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root))
    }

    private fun ensureObject(root: ObjectNode, field: String): ObjectNode{
        val existing = root.get(field)
        return if (existing is ObjectNode) existing else root.putObject(field)
    }
    fun resolveUrl(functionName: String): String {
        val all = readAll()

        all[functionName]?.url?.let { return it }
        val matches = all.filterKeys { it.endsWith("/$functionName") }.values.map { it.url }
        return when (matches.size){
            1 -> matches[0]
            0 -> throw IllegalStateException(
                "Internal Function '$functionName' not found in function-registry at '$file'." +
                        "Deploy it first."
            )
            else -> throw IllegalStateException(
                "Internal Function '$functionName' is ambiguous in function-registry at '$file'." +
                        "(found multiple regional entries)."
            )
        }
    }
}




