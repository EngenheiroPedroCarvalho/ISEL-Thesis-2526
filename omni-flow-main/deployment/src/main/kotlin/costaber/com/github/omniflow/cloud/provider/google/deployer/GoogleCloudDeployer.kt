package costaber.com.github.omniflow.cloud.provider.google.deployer

import costaber.com.github.omniflow.cloud.provider.google.provider.GoogleDefaultStrategyDeciderProvider.createNodeRendererStrategyDecider
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleRenderingContext
import costaber.com.github.omniflow.cloud.provider.google.renderer.GoogleTermContext
import costaber.com.github.omniflow.cloud.provider.google.service.GoogleWorkflowService
import costaber.com.github.omniflow.deployer.CloudDeployer
import costaber.com.github.omniflow.model.*
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import costaber.com.github.omniflow.registry.WorkflowEndpointResolver
import costaber.com.github.omniflow.resource.util.joinToStringNewLines
import costaber.com.github.omniflow.traversor.DepthFirstNodeVisitorTraversor
import costaber.com.github.omniflow.visitor.NodeContextVisitor
import mu.KotlinLogging
import java.nio.file.Path

class GoogleCloudDeployer internal constructor(
    private val nodeTraversor: DepthFirstNodeVisitorTraversor,
    private val contextVisitor: NodeContextVisitor,
    private val googleWorkflowService: GoogleWorkflowService,
    private val functionRegistryFile: Path = Path.of(System.getProperty("user.dir")).resolve("function-registry.json")
) : CloudDeployer<GoogleDeployContext> {

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun deploy(workflow: Workflow, deployContext: GoogleDeployContext) {
        logger.info { "Starting to convert Workflow into a Workflow" }
        val workflow = resolveInternalEndpoints(workflow)

        val renderingContext = GoogleRenderingContext(termContext = GoogleTermContext())
        val content = nodeTraversor.traverse(contextVisitor, workflow, renderingContext)
            .filterNot(String::isEmpty)
            .joinToStringNewLines()
        googleWorkflowService.deploy(
            projectId = deployContext.projectId,
            zone = deployContext.zone,
            serviceAccount = deployContext.serviceAccount,
            workflowId = deployContext.workflowId,
            workflowDescription = deployContext.workflowDescription,
            workflowLabels = deployContext.workflowLabels,
            workflowSourceContents = content,
        )
    }

    private fun resolveInternalEndpoints(workflow: Workflow): Workflow {
        val functionRefs = collectFunctionRefs(workflow.steps)

        //Ensure registry exists + placeholders for all refs used by this workflow
        if (functionRefs.isNotEmpty()) {
            FunctionRegistryStore(functionRegistryFile).ensurePlaceholders(functionRefs)
        }

        val resolvedSteps = resolveSteps(workflow.steps.toList())
        return workflow.copy(steps = resolvedSteps)
    }

    private fun collectFunctionRefs(steps: Collection<Step>): Set<String>{
        val refs = mutableSetOf<String>()

        fun walkSteps(list: List<Step>){
            list.forEach { step ->
                when(val ctx = step.context){
                    is CallContext -> {
                        WorkflowEndpointResolver.inferFunctionRefFromregistryKeys(ctx.host, ctx.path)
                            ?.let {refs.add(it)}
                    }

                    is BranchContext -> walkSteps(ctx.steps)
                    is IterationContext -> walkSteps(ctx.steps)

                    is ParallelBranchContext -> ctx.branches.forEach { b -> walkSteps(b.steps)}
                    is ParallelIterationContext -> walkSteps(ctx.iterationContext.steps)

                    else -> {/* AssignContext, ConditionalContext, etc -> nothing to resolve*/}
                }
            }
        }
        walkSteps(steps.toList())
        return refs
    }

    private fun resolveSteps(steps: List<Step>): List<Step> =
        steps.map { step ->
            when (val ctx = step.context){
                is CallContext -> {
                    val (host, path) = WorkflowEndpointResolver.resolveHostAndPath(
                        host = ctx.host,
                        path = ctx.path,
                        registryFile = functionRegistryFile
                    )
                    if (host == ctx.host && path == ctx.path) step
                    else step.copy(context = ctx.copy(host = host, path = path))
                }

                is BranchContext -> {
                    val updated = ctx.copy(steps = resolveSteps(ctx.steps))
                    step.copy(context = updated)
                }

                is IterationRangeContext -> {
                    val updated = ctx.copy(steps = resolveSteps(ctx.steps))
                    step.copy(context = updated)
                }

                is IterationForEachContext -> {
                    val updated = ctx.copy(steps = resolveSteps(ctx.steps))
                    step.copy(context = updated)
                }

                is IterationContext -> {
                    val updated = IterationContext(ctx.value, resolveSteps(ctx.steps))
                    step.copy(context = updated)
                }

                is ParallelIterationContext -> {
                    val itCtx = ctx.iterationContext
                    val updatedIteration = when (itCtx) {
                        is IterationRangeContext -> itCtx.copy(steps = resolveSteps(itCtx.steps))
                        is IterationForEachContext -> itCtx.copy(steps = resolveSteps(itCtx.steps))
                        else -> IterationContext(itCtx.value, resolveSteps(itCtx.steps))
                    }
                    step.copy(context = ctx.copy(iterationContext = updatedIteration))
                }
                else -> step
            }
        }


    class Builder {
        private var functionRegistryFile: Path = Path.of("function-registry.json")

        fun functionRegistryFile(path: Path) = apply { this.functionRegistryFile = path }
        fun build() = GoogleCloudDeployer(
            nodeTraversor = DepthFirstNodeVisitorTraversor(),
            contextVisitor = NodeContextVisitor(createNodeRendererStrategyDecider()),
            googleWorkflowService = GoogleWorkflowService(),
            functionRegistryFile = functionRegistryFile,
        )
    }
}