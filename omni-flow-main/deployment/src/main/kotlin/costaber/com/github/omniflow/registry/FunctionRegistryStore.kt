package costaber.com.github.omniflow.registry

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import costaber.com.github.omniflow.registry.FunctionRegistrySnapshot
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories

/**
 * Registry store: creates, loads and updates the JSON function registry file.
 *
 * Notes:
 * - This is intentionally dependency-free (no Jackson/kotlinx-serialization),
 *   so it can be used inside the deployment module with minimal friction.
 * - The parser is intentionally minimal and only extracts the fields we need:
 *   updatedAt, and for each function: host + path.
 */
class FunctionRegistryStore(
    private val registryFile: Path,
    private val mapper: ObjectMapper = ObjectMapper()
) {

    //fun loadOrCreate(): FunctionRegistry = FunctionRegistryIO.loadOrCreate(registryFile)

    fun loadOrCreate(): FunctionRegistrySnapshot{
        if (!Files.exists(registryFile)){
            writeRoot(createEmptyRoot())
        }
        return readSnapshot()
    }

    fun resolve(functionRef: String): FunctionInvocationMetadata {
        val registry = loadOrCreate()
        return registry.functions[functionRef]
            ?: error(
                "Missing registry entry for functionRef='$functionRef'. " +
                        "Expected file '$registryFile' to contain functions['$functionRef'] with host/path."
            )
    }

    /*/**
     * Upsert a registry entry and persist it to disk.
     */
    fun upsert(functionRef: String, host: String, path: String) {
        val registry = loadOrCreate()
        registry.functions[functionRef] = FunctionInvocationMetadata(host = host, path = path)
        registry.updatedAt = nowIso()
        FunctionRegistryIO.save(registryFile, registry)
    }*/

    /**
     * Create or update an entry for a given functionRef.
     */
    fun upsert(functionRef: String, host: String, path: String): FunctionRegistrySnapshot{
        val root = readOrCreateRoot()
        val functions = root.withObject("functions")
        val entry = functions.withObject("functionRef")

        entry.put("host", host)
        entry.put("path", path)

        root.put("updatedAt", nowIsoInstant())
        writeRoot(root)

        return readSnapshot()
    }

    /**
     * Ensure entries exist for a set of functionRefs.
     *
     * If missing, creates placeholder entries (host/path empty) so the file is populated
     * and easy to fill later.
     */
    fun ensurePlaceholders(functionRefs: Set<String>): FunctionRegistrySnapshot {
        val root = readOrCreateRoot()
        val functions = root.withObject("functions")

        var changed = false

        for (ref in functionRefs) {
            if (!functions.has(ref)) {
                val entry = functions.putObject(ref)
                entry.put("host", "")
                entry.put("path", "")
                changed = true
            }
        }

        if (changed) {
            root.put("updatedAt", nowIsoInstant())
            writeRoot(root)
        }

        return readSnapshot()
    }


    private fun readSnapshot(): FunctionRegistrySnapshot {
        val root = readOrCreateRoot()

        val updatedAt = root.path("updatedAt").asText(nowIsoInstant())
        val functionsNode = root.withObject("functions")

        val functions = mutableMapOf<String, FunctionInvocationMetadata>()
        val fields = functionsNode.fields()

        while (fields.hasNext()) {
            val (key, node) = fields.next()
            val host = node.path("host").asText("")
            val path = node.path("path").asText("")
            functions[key] = FunctionInvocationMetadata(host = host, path = path)
        }
        return FunctionRegistrySnapshot(updatedAt = updatedAt, functions = functions)
    }

    private fun readOrCreateRoot(): ObjectNode {
        if (!Files.exists(registryFile)){
            val root = createEmptyRoot()
            writeRoot(root)
            return root
        }
        return readRoot()
    }

    private fun readRoot(): ObjectNode {
        val tree = mapper.readTree(registryFile.toFile())
        if (tree !is ObjectNode){
            throw IllegalStateException("Registry file is not a JSON object: $registryFile")
        }
        return tree
    }

    private fun createEmptyRoot(): ObjectNode {
        val root = mapper.createObjectNode()
        root.put("updatedAt", nowIsoInstant())
        root.putObject("functions")
        return root
    }

    private fun writeRoot(root: ObjectNode) {
        registryFile.parent?.createDirectories()
        mapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(registryFile.toFile(), root)
    }

    companion object{
        fun nowIsoInstant(): String = Instant.now().toString()
    }

    private fun nowIso(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
}


