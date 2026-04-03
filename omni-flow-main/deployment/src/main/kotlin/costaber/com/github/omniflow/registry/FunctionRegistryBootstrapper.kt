package costaber.com.github.omniflow.registry

import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunV2RestCatalog

class FunctionRegistryBootstrapper(
    private val store: FunctionRegistryStore,
    private val catalog: CloudRunV2RestCatalog
) {
    /**
     * If registry file is missing:
     * - call Google Cloud APIs to list functions in projectId
     * - create the local file registry and populate it
     */
    fun bootstrapIfMissing(projectId: String) {
        if(store.exists()) return

        val functions = catalog.listHttpServices(projectId)
        store.writeNew(functions)
    }
}