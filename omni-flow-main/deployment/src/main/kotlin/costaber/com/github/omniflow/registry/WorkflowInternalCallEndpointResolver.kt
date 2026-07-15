package costaber.com.github.omniflow.registry

import costaber.com.github.omniflow.model.BranchContext
import costaber.com.github.omniflow.model.CallContext
import costaber.com.github.omniflow.model.IterationContext
import costaber.com.github.omniflow.model.IterationForEachContext
import costaber.com.github.omniflow.model.IterationRangeContext
import costaber.com.github.omniflow.model.ParallelBranchContext
import costaber.com.github.omniflow.model.ParallelIterationContext
import costaber.com.github.omniflow.model.Step
import costaber.com.github.omniflow.model.StepContext
import costaber.com.github.omniflow.model.Workflow
import java.net.URI

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
class WorkflowInternalCallEndpointResolver(
    private val registry: FunctionRegistryStore
) {

    /**
     * @param workflow produced the DSL
     * @param internalCallExtractor A function that returns the internal function name for a CallContext
     *  or null if the call is external
     *
     */
    fun resolve(
        workflow: Workflow,
        internalCallExtractor: (CallContext) -> String?
    ): Workflow {
        // Read the registry ONCE per workflow instead of once per internal call.
        // This turns resolution from O(N*M) (N internal calls, each re-reading the whole
        // M-entry registry file) into O(N+M): one read + a lookup per call.
        val registrySnapshot = registry.readAll()
        val updatedSteps = workflow.steps.toList().map { resolveStep(it, internalCallExtractor, registrySnapshot) }
        return workflow.copy(steps = updatedSteps)
    }

    private fun resolveStep(
        step: Step,
        internalCallExtractor: (CallContext) -> String?,
        registrySnapshot: Map<String, FunctionInvocationMetadata>
    ): Step {
        val newCtx = resolveContext(step.context, internalCallExtractor, registrySnapshot)
        return step.copy(context = newCtx)
    }

    private fun resolveContext(
        ctx: StepContext,
        internalCallExtractor: (CallContext) -> String?,
        registrySnapshot: Map<String, FunctionInvocationMetadata>
    ): StepContext{
        return when (ctx) {
            is CallContext -> resolveCall(ctx, internalCallExtractor, registrySnapshot)

            is BranchContext ->
                ctx.copy(steps = ctx.steps.map { resolveStep(it, internalCallExtractor, registrySnapshot) })

            is IterationRangeContext ->
                ctx.copy(steps = ctx.steps.map { resolveStep(it, internalCallExtractor, registrySnapshot) })

            is IterationForEachContext ->
                ctx.copy(steps = ctx.steps.map { resolveStep(it, internalCallExtractor, registrySnapshot) })

            is IterationContext ->
                IterationContext(ctx.value, ctx.steps.map { resolveStep(it, internalCallExtractor, registrySnapshot) })

            is ParallelBranchContext ->
                ctx.copy(
                    branches = ctx.branches.map { b ->
                        b.copy(steps = b.steps.map { resolveStep(it, internalCallExtractor, registrySnapshot) })
                    }
                )

            is ParallelIterationContext -> {
                val itCtx = ctx.iterationContext
                val updatedIt = when (itCtx) {
                    is IterationRangeContext ->
                        itCtx.copy(steps = itCtx.steps.map { resolveStep(it, internalCallExtractor, registrySnapshot) })

                    is IterationForEachContext ->
                        itCtx.copy(steps = itCtx.steps.map { resolveStep(it, internalCallExtractor, registrySnapshot) })

                    else ->
                        IterationContext(itCtx.value, itCtx.steps.map { resolveStep(it, internalCallExtractor, registrySnapshot) })
                }
                ctx.copy(iterationContext = updatedIt)
            }

            else -> ctx
        }
    }

    private fun resolveCall(
        call: CallContext,
        internalCallExtractor: (CallContext) -> String?,
        registrySnapshot: Map<String, FunctionInvocationMetadata>
    ): CallContext {
        val fnName = internalCallExtractor(call) ?: return call // external call

        // Resolve URL against the already-loaded registry snapshot (no per-call file read).
        val url = registry.resolveUrlIn(fnName, registrySnapshot)

        val (host, path) = splitUrl(url)

        //Return a new Call Context with host/path filled so renderers work.
        return call.copy(host = host, path = path)
    }

    private fun splitUrl(url: String): Pair<String, String>{
        val uri = URI(url)
        val host = "${uri.scheme}://${uri.authority}"
        val path = uri.rawPath?.ifBlank {"/"} ?: "/"
        return host to path
    }

}