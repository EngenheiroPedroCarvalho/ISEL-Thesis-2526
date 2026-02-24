package costaber.com.github.omniflow.registry

import java.nio.file.Path
/**
 * Registry-based endpoint resolution helpers.
 *
 * -When a call is "internal", host/path MUST be registry keys:
 *      host == "<functionRef>.host"
 *      path == "<functionRef>.path"
 *     and OmniFlow must resolve them at deployment time (before rendering provider workflow).
 *
 * -When a call is "external", host/path remain literal values and are left untouched
 */
object WorkflowEndpointResolver {

    private const val HOST_SUFFIX = ".host"
    private const val PATH_SUFFIX = ".path"
    /**
     * Converts functionRef into the canonical host/path key pair.
     */
    fun registryKeys(functionRef: String): Pair<String, String> =
        FunctionEndpointKeys.hostKey(functionRef) to FunctionEndpointKeys.pathKey(functionRef)

    /**
     * If [functionRef] is not null OR if host/path look like registry keys, tries to resolve host/path
     * using the registry file.
     *
     * If host/path don't represent a registry key pair, returns them unchanged.
     */
    fun resolveHostAndPath(host: String, path: String, registryFile: Path): Pair<String, String> {
        val h = host.trim()
        val p = path.trim()

        val functionRef = inferFunctionRefFromregistryKeys(h, p) ?: return h to p

        val store = FunctionRegistryStore(registryFile)
        val snapshot = store.loadOrCreate()

        val meta = snapshot.functions[functionRef]
        if (meta == null){
            //Create placeholder entry with empty host/path
            store.ensurePlaceholders(setOf(functionRef))

            throw IllegalStateException(
                "Function '$functionRef' does not exist in the function registry: ${registryFile.toAbsolutePath()}" +
                        "A placeholder entry was created with empty 'host' and 'path'." +
                        "Populate it (or deploy the function) and retry."
            )
        }

        if (meta.host.isBlank() || meta.path.isBlank()){
            throw IllegalStateException(
                "Registry entry for functionRef='$functionRef' exists but is not populated (host='${meta.host}', path='${meta.path}')." +
                    "Populate ${registryFile.toAbsolutePath()}."
            )
        }

        return meta.host to meta.path
    }

    /**
     * Returns the functionRef if:
     * - host ends with ".host"
     * - path ends with ".path"
     * - both share the same prefix (the functionRef)
     */
    fun inferFunctionRefFromregistryKeys(host: String, path: String): String? {
        val hostRef = functionRefFromHostKey(host)
        val pathRef = functionRefFromPathKey(path)

        //External call: no registry keys
        if (hostRef == null && pathRef == null) return null

        if (hostRef == null || pathRef == null) {
            throw IllegalArgumentException(
                "Invalid registry reference. If using keys, both host and path must be" +
                        "\"<functionRef>.host\" and \"<functionRef>.path\". Got host='$host', path='$path'"
            )
        }

        if(hostRef != pathRef){
            throw IllegalArgumentException(
                "Mismatched functionRef between host and path." +
                        "host='$host' -> '$hostRef', path='$path' -> '$pathRef'. They must match"
            )
        }

        return hostRef
    }


    fun functionRefFromHostKey(host: String): String? =
        if (host.endsWith(HOST_SUFFIX) && host.length > HOST_SUFFIX.length)
            host.substring(0, host.length - HOST_SUFFIX.length)
        else null
    fun functionRefFromPathKey(path: String): String? =
        if (path.endsWith(PATH_SUFFIX) && path.length > PATH_SUFFIX.length)
            path.substring(0, path.length - PATH_SUFFIX.length)
        else null
}