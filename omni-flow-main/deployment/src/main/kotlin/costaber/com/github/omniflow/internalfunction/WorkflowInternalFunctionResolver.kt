package costaber.com.github.omniflow.internalfunction

import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunLocationsV1RestClient
import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunV2ServiceInspector
import costaber.com.github.omniflow.model.*
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import mu.KotlinLogging
import java.net.URI


/**
 * Resolves CallContext.internalFunction using function-registry.json and Cloud Run APIs
 */
class WorkflowInternalFunctionResolver(
    private val projectId: String,
    private val preferredRegion: String?,
    private val registry: FunctionRegistryStore,
    private val inspector: CloudRunV2ServiceInspector,
    private val locationsClient: CloudRunLocationsV1RestClient = CloudRunLocationsV1RestClient()
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private data class FoundService(
        val region: String,
        val serviceName: String,
        val url: String
    )

    private val regions: List<String> by lazy {
        val all = locationsClient.listProjectLocations(projectId)
        if(preferredRegion.isNullOrBlank()) all
        else listOf(preferredRegion) + all.filterNot { it == preferredRegion }
    }

    fun resolve(workflow: Workflow): Workflow =
        workflow.copy(steps = workflow.steps.toList().map { resolveStep(it) })

    private fun resolveStep(step: Step): Step =
        step.copy(context = resolveContext(step.context))

    private fun resolveContext(ctx: StepContext): StepContext =
        when (ctx){
            is CallContext -> resolveCall(ctx)

            is BranchContext -> ctx.copy(steps = ctx.steps.map { resolveStep(it) })

            is IterationRangeContext -> ctx.copy(steps = ctx.steps.map { resolveStep(it) })

            is IterationForEachContext -> ctx.copy(steps = ctx.steps.map { resolveStep(it) })

            is IterationContext -> IterationContext(ctx.value, ctx.steps.map { resolveStep(it) })

            is ParallelBranchContext -> ctx.copy(branches = ctx.branches.map { b -> b.copy(steps = b.steps.map { resolveStep(it) }) })

            is ParallelIterationContext -> ctx.copy(iterationContext = resolveIteration(ctx.iterationContext))

            else -> ctx
        }

    private fun resolveIteration(itCtx: IterationContext): IterationContext =
        when (itCtx) {
            is IterationRangeContext -> itCtx.copy(steps = itCtx.steps.map { resolveStep(it) })
            is IterationForEachContext -> itCtx.copy(steps = itCtx.steps.map { resolveStep(it) })
            else -> IterationContext(itCtx.value, itCtx.steps.map { resolveStep(it) })
        }

    private fun resolveCall(call: CallContext): CallContext{
        val internal = call.internalFunction ?: return call

        val functionRef = extractFunctionRef(internal)
        //rule: internal call cannot include host/path in the workflow definition
        if (call.host.isNotBlank() || call.path.isNotBlank()) {
            throw IllegalStateException(
                "Invalid workflow: internalFunction('${internal.name}') cannot be combined with host/path. " +
                        "Remove host/path from this call step."
            )
        }
        val resolvedUrl = resolveOrDiscoverInternal(functionRef)

        val (host, path) = splitUrl(resolvedUrl)

        return call.copy(
            host = host,
            path = path,
            internalFunction = null
        )
    }

    private fun extractFunctionRef(internal: InternalFunction): String = internal.name


    private fun resolveOrDiscoverInternal(functionRef: String): String{
        //1) If registry has entry -> validate against Cloud Run get(serviceName)
        val existing = registry.tryResolveEntry(functionRef)

        if (existing != null) {
            val (key, meta) = existing

            return when (val r = inspector.lookupByServiceName(meta.serviceName)){
                is CloudRunV2ServiceInspector.LookupResult.Found -> {
                    if(r.url != meta.url){
                        logger.warn{"Registry drift for '$key': updating URL"}
                        registry.put(key, meta.copy(url = r.url))
                    }
                    r.url
                }

                CloudRunV2ServiceInspector.LookupResult.NotFound -> {
                    //Service deleted (or moved). Try rediscovery by serviceId/functionRef
                    val found = discoverServiceForRef(functionRef)
                    if (found.isEmpty()) {
                        registry.remove(key)
                        throw IllegalStateException(
                            "Internal function '$functionRef' exists in function-registry (key = '$key') " +
                                    "but Cloud Run service '${meta.serviceName}' does not exist (deleted). " +
                                    "Remove stale registry entry. Redeploy the function or update the workflows."
                        )
                    }
                    val chosen = chooseSingleOrFail(functionRef, found)
                    registry.put(key, FunctionInvocationMetadata(serviceName = chosen.serviceName, url = chosen.url))
                    chosen.url
                }

                is CloudRunV2ServiceInspector.LookupResult.Forbidden -> {
                    throw IllegalStateException(
                        "Cannot validate internal function '$functionRef' via Cloud Run APIs: ${r.message}"
                    )
                }
            }
        }
        //2) Missing in registry -> confirm via Cloud Run APIs and auto-populate
        val found = discoverServiceForRef(functionRef)
        val chosen = chooseSingleOrFail(functionRef, found)

        registry.put(functionRef, FunctionInvocationMetadata(serviceName = chosen.serviceName, url = chosen.url))
        logger.info("Added '$functionRef' to function-registry (region=${chosen.region})")
        return chosen.url
    }

    /**
     * Confirm a function exists in Cloud Run:
     * - If functionRef is "region/service": check exact service.
     * - Else scan regions for a unique match by serviceId == functionRef.
     */
    private fun discoverServiceForRef(functionRef: String): List<FoundService>{
        val ref = functionRef.trim()
        if (ref.contains("/")){
            val region = ref.substringBefore("/").trim()
            val serviceId = ref.substringBefore("/").trim()
            if (region.isBlank() || serviceId.isBlank()) {
                throw IllegalStateException("Invalid internal function reference '$functionRef'. Use 'region/service'.")
            }
            return when (val r = inspector.lookup(projectId, region, serviceId)) {
                is CloudRunV2ServiceInspector.LookupResult.Found ->
                    listOf(FoundService(region, r.serviceName, r.url))

                CloudRunV2ServiceInspector.LookupResult.NotFound ->
                    emptyList()

                is CloudRunV2ServiceInspector.LookupResult.Forbidden ->
                    throw IllegalStateException("Cannot confirm '$functionRef' in region '$region': ${r.message}")
            }
        }

        val found = mutableListOf<FoundService>()
        val forbiddenRegions = mutableListOf<String>()

        for (region in regions) {
            when (val r = inspector.lookup(projectId, region, ref)){
                is CloudRunV2ServiceInspector.LookupResult.Found ->
                    found.add(FoundService(region, r.serviceName, r.url))

                CloudRunV2ServiceInspector.LookupResult.NotFound ->
                    Unit

                is CloudRunV2ServiceInspector.LookupResult.Forbidden ->
                    forbiddenRegions.add(region)
            }

            //stop early if ambiguous
            if (found.size > 1) break
        }

        //If nothing found but some regions forbidden -> we cannot be sure
        if (found.isEmpty() && forbiddenRegions.isNotEmpty()) {
            throw IllegalStateException(
                "Internal function '$ref' is not in function-registry and could not be confirmed in Cloud Run. " +
                        "Some regions were not accessible: ${forbiddenRegions.take(5).joinToString(", ")}. " +
                        "Tip: specify the region explicitly: internalFunction(\"<region>/$ref\")."
            )
        }

        return found
    }

    private fun chooseSingleOrFail(functionRef: String, found: List<FoundService>): FoundService =
        when (found.size){
            0 -> throw IllegalStateException(
                "Internal function '$functionRef' is not in function-registry and does not exist in Cloud Run. " +
                        "Deploy it first (QuickFaaS) or create it in the console."
            )
            1-> found[0]
            else -> throw IllegalStateException(
                "Internal function '$functionRef' exists in multiple Cloud Run regions: " +
                        found.joinToString { it.region } +
                        ". Disambiguate using internalFunction(\"<region>/$functionRef\")"
            )
        }



    private fun splitUrl(url: String): Pair<String, String> {
        val uri = try {
            URI(url)
        } catch (e: Exception){
            throw IllegalStateException("Invalid URL resolved for internal function: '$url'", e)
        }
        val scheme = uri.scheme ?: throw java.lang.IllegalStateException("Resolved URL missing scheme: '$url'")
        val authority = uri.authority ?: throw java.lang.IllegalStateException("Registry URL must include host: '$url'")

        val host = "$scheme://$authority"
        val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: ""

        return host to path
    }
}