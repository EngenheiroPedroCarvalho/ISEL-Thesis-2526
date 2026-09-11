package costaber.com.github.omniflow.cloud.provider.amazon.deployer

import costaber.com.github.omniflow.cloud.provider.amazon.renderer.AmazonRenderingContext
import costaber.com.github.omniflow.cloud.provider.amazon.service.AmazonStateMachineService
import costaber.com.github.omniflow.cloud.provider.amazon.service.LambdaFunctionsCatalog
import costaber.com.github.omniflow.cloud.provider.amazon.provider.AmazonDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider
import costaber.com.github.omniflow.deployer.CloudDeployer
import costaber.com.github.omniflow.internalfunction.InternalFunctionDeployer
import costaber.com.github.omniflow.internalfunction.NoopInternalFunctionDeployer
import costaber.com.github.omniflow.internalfunction.quickfaas.AwsInternalFunctionResolver
import costaber.com.github.omniflow.model.*
import costaber.com.github.omniflow.registry.CloudFunctionsCatalog
import costaber.com.github.omniflow.registry.FunctionRegistryBootstrapper
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import costaber.com.github.omniflow.resource.util.joinToStringNewLines
import costaber.com.github.omniflow.traversor.DepthFirstNodeVisitorTraversor
import costaber.com.github.omniflow.visitor.NodeContextVisitor
import mu.KotlinLogging
import java.nio.file.Path

class AmazonCloudDeployer internal constructor(
    private val nodeTraversor: DepthFirstNodeVisitorTraversor,
    private val contextVisitor: NodeContextVisitor,
    private val amazonStateMachineService: AmazonStateMachineService,
    private val registryPath: Path,
    private val functionsCatalog: CloudFunctionsCatalog = LambdaFunctionsCatalog(),
    private val internalFunctionDeployer: InternalFunctionDeployer
) : CloudDeployer<AmazonDeployContext> {

    private companion object {
        private val logger = KotlinLogging.logger { }
        private const val RESET  = "[0m"
        private const val BOLD   = "[1m"
        private const val GREEN  = "[32m"
        private const val YELLOW = "[33m"
        private const val CYAN   = "[36m"
    }

    override fun deploy(workflow: Workflow, deployContext: AmazonDeployContext) {
        println("$CYAN$BOLD[DEPLOY]$RESET Checking if function-registry exists at '$registryPath'...")
        bootstrapFunctionRegisterIfMissing(deployContext.region)

        val internalCount = countInternalFunctions(workflow)

        val resolvedWorkflow = if (internalCount > 0) {
            println("$CYAN$BOLD[DEPLOY]$RESET Detected $BOLD$internalCount$RESET internal Lambda function(s) — resolving...")
            AwsInternalFunctionResolver(
                preferredRegion = deployContext.region,
                registry = FunctionRegistryStore(registryPath),
                internalFunctionDeployer = internalFunctionDeployer
            ).resolve(workflow)
        } else {
            workflow
        }

        val content = nodeTraversor.traverse(contextVisitor, resolvedWorkflow, AmazonRenderingContext())
            .filterNot(String::isEmpty)
            .joinToStringNewLines()

        println("\n--- Generated State Machine JSON ---")
        println(content)
        println("--- End JSON ---\n")

        amazonStateMachineService.createStateMachine(
            roleArn = deployContext.roleArn,
            region = deployContext.region,
            tags = deployContext.tags,
            stateMachineName = deployContext.stateMachineName,
            stateMachineDefinition = content,
        )

        println("$GREEN  ✓$RESET State Machine '${deployContext.stateMachineName}' deployed to AWS Step Functions")
    }

    private fun countInternalFunctions(workflow: Workflow): Int {
        var count = 0
        fun countInSteps(steps: Collection<Step>) {
            for (step in steps) {
                when (val ctx = step.context) {
                    is CallContext -> if (ctx.internalFunction != null) count++
                    is BranchContext -> countInSteps(ctx.steps)
                    is IterationRangeContext -> countInSteps(ctx.steps)
                    is IterationForEachContext -> countInSteps(ctx.steps)
                    is IterationContext -> countInSteps(ctx.steps)
                    is ParallelBranchContext -> ctx.branches.forEach { countInSteps(it.steps) }
                    is ParallelIterationContext -> countInSteps(ctx.iterationContext.steps)
                    else -> Unit
                }
            }
        }
        countInSteps(workflow.steps)
        return count
    }

    private fun bootstrapFunctionRegisterIfMissing(region: String) {
        val store = FunctionRegistryStore(registryPath)

        if (store.exists()) {
            println("$GREEN  ✓$RESET Function-registry found at '$registryPath' (skipping bootstrap)")
            logger.info { "Function Registry found at '$registryPath' (skipping bootstrap)." }
            return
        }

        println("$YELLOW  !$RESET Function-registry not found — bootstrapping from AWS Lambda APIs for region '$region'...")
        logger.warn {
            "Function Registry not found at '$registryPath'. Bootstrapping registry from AWS Lambda APIs for region '$region'..."
        }

        FunctionRegistryBootstrapper(
            store = store,
            catalog = functionsCatalog
        ).bootstrapIfMissing(region)

        println("$GREEN  ✓$RESET Function-registry created and populated at '$registryPath'")
        logger.info { "Function Registry created and populated at '$registryPath'." }
    }

    class Builder {
        private var registryPath: Path = Path.of(System.getProperty("user.dir")).resolve("function-registry.aws.json")
        private var functionsCatalog: CloudFunctionsCatalog = LambdaFunctionsCatalog()
        private var internalFunctionDeployer: InternalFunctionDeployer = NoopInternalFunctionDeployer

        fun registryPath(value: Path) = apply { this.registryPath = value }
        fun internalFunctionDeployer(value: InternalFunctionDeployer) = apply { this.internalFunctionDeployer = value }

        fun build() = AmazonCloudDeployer(
            nodeTraversor = DepthFirstNodeVisitorTraversor(),
            contextVisitor = NodeContextVisitor(createNodeRendererStrategyDecider()),
            amazonStateMachineService = AmazonStateMachineService(),
            registryPath = registryPath,
            functionsCatalog = functionsCatalog,
            internalFunctionDeployer = internalFunctionDeployer
        )
    }
}
