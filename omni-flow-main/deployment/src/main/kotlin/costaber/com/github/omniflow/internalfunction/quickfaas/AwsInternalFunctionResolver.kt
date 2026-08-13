package costaber.com.github.omniflow.internalfunction.quickfaas

import costaber.com.github.omniflow.cloud.provider.amazon.service.AwsLambdaFunctionInspector
import costaber.com.github.omniflow.cloud.provider.amazon.service.AwsRegionsLister
import costaber.com.github.omniflow.cloud.provider.amazon.service.Ec2AwsRegionsLister
import costaber.com.github.omniflow.cloud.provider.amazon.service.LambdaFunctionInspector
import costaber.com.github.omniflow.internalfunction.InternalFunctionDeployer
import costaber.com.github.omniflow.internalfunction.NoopInternalFunctionDeployer
import costaber.com.github.omniflow.model.*
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import mu.KotlinLogging

/**
 * Resolves CallContext.internalFunction using function-registry.json and the AWS Lambda API.
 *
 * Unlike a single fixed AWS region, functions are searched for across every region enabled for
 * the account (discovered via [regionsLister]), with [preferredRegion] - if set - tried first.
 * This mirrors how the GCP counterpart,
 * [costaber.com.github.omniflow.internalfunction.WorkflowInternalFunctionResolver], scans all
 * Cloud Run locations rather than being pinned to one.
 *
 * When a function is not found in the registry or AWS Lambda, and its
 * [InternalFunction.deploymentDescriptorPath] is set, the [internalFunctionDeployer] is invoked to
 * deploy the function via QuickFaaS before continuing.
 */
class AwsInternalFunctionResolver(
    private val preferredRegion: String?,
    private val registry: FunctionRegistryStore,
    private val internalFunctionDeployer: InternalFunctionDeployer = NoopInternalFunctionDeployer,
    private val inspector: LambdaFunctionInspector = AwsLambdaFunctionInspector(),
    private val regionsLister: AwsRegionsLister = Ec2AwsRegionsLister()
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val RESET   = "[0m"
        private const val BOLD    = "[1m"
        private const val GREEN   = "[32m"
        private const val YELLOW  = "[33m"
        private const val BLUE    = "[34m"
        private const val CYAN    = "[36m"
    }

    private data class FoundFunction(
        val region: String,
        val functionName: String,
        val arn: String
    )

    private val regions: List<String> by lazy {
        val all = regionsLister.listRegions(bootstrapRegion = preferredRegion ?: "us-east-1")
        if (preferredRegion.isNullOrBlank()) all
        else listOf(preferredRegion) + all.filterNot { it == preferredRegion }
    }

    fun resolve(workflow: Workflow): Workflow =
        workflow.copy(steps = workflow.steps.toList().map { resolveStep(it) })

    private fun resolveStep(step: Step): Step =
        step.copy(context = resolveContext(step.context))

    private fun resolveContext(ctx: StepContext): StepContext = when (ctx) {
        is CallContext -> resolveCall(ctx)
        is BranchContext -> ctx.copy(steps = ctx.steps.map { resolveStep(it) })
        is IterationRangeContext -> ctx.copy(steps = ctx.steps.map { resolveStep(it) })
        is IterationForEachContext -> ctx.copy(steps = ctx.steps.map { resolveStep(it) })
        is IterationContext -> IterationContext(ctx.value, ctx.steps.map { resolveStep(it) })
        is ParallelBranchContext -> ctx.copy(branches = ctx.branches.map { b ->
            b.copy(steps = b.steps.map { resolveStep(it) })
        })
        is ParallelIterationContext -> ctx.copy(iterationContext = resolveIteration(ctx.iterationContext))
        else -> ctx
    }

    private fun resolveIteration(itCtx: IterationContext): IterationContext = when (itCtx) {
        is IterationRangeContext -> itCtx.copy(steps = itCtx.steps.map { resolveStep(it) })
        is IterationForEachContext -> itCtx.copy(steps = itCtx.steps.map { resolveStep(it) })
        else -> IterationContext(itCtx.value, itCtx.steps.map { resolveStep(it) })
    }

    private fun resolveCall(call: CallContext): CallContext {
        val internal = call.internalFunction ?: return call
        if (call.host.isNotBlank() || call.path.isNotBlank()) {
            throw IllegalStateException(
                "Invalid workflow: internalFunction('${internal.name}') cannot be combined with host/path."
            )
        }
        val arn = resolveOrDeploy(internal.name, internal)
        return call.copy(host = "lambda://$arn", path = "", internalFunction = null)
    }

    private fun resolveOrDeploy(functionRef: String, internal: InternalFunction): String {
        println("$BLUE  →$RESET Resolving Lambda '$BOLD$functionRef$RESET'...")

        // 1. Check registry first, then validate the binding against the live Lambda
        val existing = registry.tryResolveEntry(functionRef)
        if (existing != null) {
            val (key, meta) = existing
            val knownRegion = regionFromArn(meta.url)
            val liveResult = knownRegion?.let { inspector.lookup(it, meta.serviceName) }

            when (liveResult) {
                is LambdaFunctionInspector.LookupResult.Found -> {
                    if (liveResult.arn != meta.url) {
                        println("$YELLOW  !$RESET Registry drift for '$BOLD$key$RESET' — updating ARN → ${liveResult.arn}")
                        logger.warn { "Registry drift for '$key': updating ARN" }
                        registry.put(key, meta.copy(url = liveResult.arn))
                    } else {
                        println("$GREEN  ✓$RESET Lambda '$functionRef' found in registry → ${liveResult.arn}")
                        logger.info { "Registry hit for Lambda '$functionRef' → ${liveResult.arn}" }
                    }
                    return liveResult.arn
                }

                is LambdaFunctionInspector.LookupResult.Forbidden -> {
                    throw IllegalStateException(
                        "Cannot validate internal function '$functionRef' via the Lambda API: ${liveResult.message}"
                    )
                }

                LambdaFunctionInspector.LookupResult.NotFound, null -> {
                    // Stale/unknown region for the stored ARN: try to rediscover across all regions
                    // before giving up on the registry entry.
                    val found = discoverFunctionForRef(functionRef)
                    if (found.isEmpty()) {
                        registry.remove(key)
                        throw IllegalStateException(
                            "Internal function '$functionRef' exists in function-registry (key = '$key') " +
                                    "but Lambda '${meta.serviceName}' does not exist in AWS (deleted). " +
                                    "Stale registry entry removed. Redeploy the function or update the workflows."
                        )
                    }
                    val chosen = chooseSingleOrFail(functionRef, found)
                    registry.put(key, FunctionInvocationMetadata(serviceName = chosen.functionName, url = chosen.arn))
                    return chosen.arn
                }
            }
        }

        // 2. Not in registry — search AWS Lambda across every enabled region
        println("$YELLOW  !$RESET '$functionRef' not in registry — searching AWS Lambda across regions...")
        val found = try {
            discoverFunctionForRef(functionRef)
        } catch (e: IllegalStateException) {
            if (internal.deploymentDescriptorPath != null) {
                println("$YELLOW  !$RESET AWS Lambda discovery failed for '$functionRef' — deployment descriptor available")
                logger.info { "Lambda discovery failed for '$functionRef', but deployment descriptor is available. Proceeding to QuickFaaS deployment." }
                emptyList()
            } else {
                throw e
            }
        }

        // 3. Deploy via QuickFaaS if descriptor available
        if (found.isEmpty() && internal.deploymentDescriptorPath != null) {
            println("$CYAN$BOLD[QUICKFAAS-AWS]$RESET Lambda '$BOLD$functionRef$RESET' not deployed — using QuickFaaS to deploy...")
            logger.info { "Lambda '$functionRef' not deployed. Triggering QuickFaaS deployment..." }
            val meta = internalFunctionDeployer.deployOrUpdate(functionRef, internal.deploymentDescriptorPath)
            registry.put(functionRef, meta)
            println("$GREEN  ✓$RESET Lambda '$BOLD$functionRef$RESET' deployed → ${meta.url}")
            return meta.url
        }

        val chosen = chooseSingleOrFail(functionRef, found)
        println("$GREEN  ✓$RESET Lambda '$BOLD$functionRef$RESET' found in AWS (${chosen.region}) → ${chosen.arn}")
        registry.put(functionRef, FunctionInvocationMetadata(serviceName = chosen.functionName, url = chosen.arn))
        logger.info { "Added '$functionRef' to function-registry (region=${chosen.region})" }
        return chosen.arn
    }

    /**
     * Confirm a function exists in AWS Lambda:
     * - If functionRef is "region/functionName": check that exact region.
     * - Else scan every enabled region for a unique match by function name == functionRef.
     */
    private fun discoverFunctionForRef(functionRef: String): List<FoundFunction> {
        val ref = functionRef.trim()
        if (ref.contains("/")) {
            val region = ref.substringBefore("/").trim()
            val name = ref.substringAfter("/").trim()
            if (region.isBlank() || name.isBlank()) {
                throw IllegalStateException("Invalid internal function reference '$functionRef'. Use 'region/functionName'.")
            }
            return when (val r = inspector.lookup(region, name)) {
                is LambdaFunctionInspector.LookupResult.Found ->
                    listOf(FoundFunction(region, r.functionName, r.arn))

                LambdaFunctionInspector.LookupResult.NotFound ->
                    emptyList()

                is LambdaFunctionInspector.LookupResult.Forbidden ->
                    throw IllegalStateException("Cannot confirm '$functionRef' in region '$region': ${r.message}")
            }
        }

        val found = mutableListOf<FoundFunction>()
        val forbiddenRegions = mutableListOf<String>()

        for (region in regions) {
            when (val r = inspector.lookup(region, ref)) {
                is LambdaFunctionInspector.LookupResult.Found ->
                    found.add(FoundFunction(region, r.functionName, r.arn))

                LambdaFunctionInspector.LookupResult.NotFound ->
                    Unit

                is LambdaFunctionInspector.LookupResult.Forbidden ->
                    forbiddenRegions.add(region)
            }

            // stop early if ambiguous
            if (found.size > 1) break
        }

        if (found.isEmpty() && forbiddenRegions.isNotEmpty()) {
            throw IllegalStateException(
                "Internal function '$ref' is not in function-registry and could not be confirmed in AWS Lambda. " +
                        "Some regions were not accessible: ${forbiddenRegions.take(5).joinToString(", ")}. " +
                        "Tip: specify the region explicitly: internalFunction(\"<region>/$ref\")."
            )
        }

        return found
    }

    private fun chooseSingleOrFail(functionRef: String, found: List<FoundFunction>): FoundFunction =
        when (found.size) {
            0 -> throw IllegalStateException(
                "Lambda '$functionRef' not found in registry or AWS and no deployment descriptor was provided. " +
                        "Deploy it first or provide deploymentDescriptorPath in internalFunction()."
            )
            1 -> found[0]
            else -> throw IllegalStateException(
                "Internal function '$functionRef' exists in multiple AWS regions: " +
                        found.joinToString { it.region } +
                        ". Disambiguate using internalFunction(\"<region>/$functionRef\")"
            )
        }

    /** Extracts the region from a Lambda ARN (`arn:aws:lambda:<region>:<account>:function:<name>`). */
    private fun regionFromArn(arn: String): String? {
        val parts = arn.split(":")
        if (parts.size < 4 || parts[0] != "arn") return null
        return parts[3].takeIf { it.isNotBlank() }
    }
}
