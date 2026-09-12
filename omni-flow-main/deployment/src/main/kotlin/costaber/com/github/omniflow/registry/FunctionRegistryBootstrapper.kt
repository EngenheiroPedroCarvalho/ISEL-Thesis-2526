package costaber.com.github.omniflow.registry

class FunctionRegistryBootstrapper(
    private val store: FunctionRegistryStore,
    private val catalog: CloudFunctionsCatalog
) {
    /**
     * If registry file is missing:
     * - call the target provider's API to list already-deployed functions
     * - create the local file registry and populate it
     */
    fun bootstrapIfMissing(scope: String) {
        if(store.exists()) return

        val functions = catalog.listHttpFunctions(scope)
        store.writeNew(functions)
    }
}