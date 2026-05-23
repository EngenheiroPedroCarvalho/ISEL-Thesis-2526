package costaber.com.github.omniflow.internalfunction.quickfaas

import costaber.com.github.omniflow.internalfunction.InternalFunctionDeployer
import costaber.com.github.omniflow.internalfunction.NoopInternalFunctionDeployer
import costaber.com.github.omniflow.model.*
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException

class AwsInternalFunctionResolver(
    private val region: String,
    private val registry: FunctionRegistryStore,
    private val internalFunctionDeployer: InternalFunctionDeployer = NoopInternalFunctionDeployer
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

    private fun resolveOrDeploy(functionName: String, internal: InternalFunction): String {
        println("$BLUE  →$RESET Resolving Lambda '$BOLD$functionName$RESET'...")

        // 1. Check registry first
        val existing = registry.tryResolveEntry(functionName)
        if (existing != null) {
            println("$GREEN  ✓$RESET Lambda '$functionName' found in registry → ${existing.second.url}")
            logger.info { "Registry hit for Lambda '$functionName' → ${existing.second.url}" }
            return existing.second.url
        }

        // 2. Check if Lambda exists in AWS and has a Function URL
        println("$YELLOW  !$RESET '$functionName' not in registry — checking AWS Lambda API...")
        val existingUrl = checkLambdaFunctionUrl(functionName)
        if (existingUrl != null) {
            registry.put(functionName, FunctionInvocationMetadata(serviceName = functionName, url = existingUrl))
            println("$GREEN  ✓$RESET Lambda '$functionName' found in AWS → $existingUrl")
            logger.info { "Lambda '$functionName' found in AWS (url=$existingUrl)" }
            return existingUrl
        }

        // 3. Deploy via QuickFaaS if descriptor available
        if (internal.deploymentDescriptorPath != null) {
            println("$CYAN$BOLD[QUICKFAAS-AWS]$RESET Lambda '$BOLD$functionName$RESET' not deployed — using QuickFaaS to deploy...")
            logger.info { "Lambda '$functionName' not deployed. Triggering QuickFaaS deployment..." }
            val meta = internalFunctionDeployer.deployOrUpdate(functionName, internal.deploymentDescriptorPath)
            registry.put(functionName, meta)
            println("$GREEN  ✓$RESET Lambda '$BOLD$functionName$RESET' deployed → ${meta.url}")
            return meta.url
        }

        throw IllegalStateException(
            "Lambda '$functionName' not found in registry or AWS and no deployment descriptor was provided. " +
                    "Deploy it first or provide deploymentDescriptorPath in internalFunction()."
        )
    }

    private fun checkLambdaFunctionUrl(functionName: String): String? = try {
        LambdaClient.builder()
            .region(Region.of(region))
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()
            .use { client ->
                client.getFunction(
                    GetFunctionRequest.builder().functionName(functionName).build()
                ).configuration().functionArn()
            }
    } catch (e: ResourceNotFoundException) {
        null
    } catch (e: Exception) {
        logger.warn { "Could not check Lambda '$functionName' in AWS: ${e.message}" }
        null
    }

}
