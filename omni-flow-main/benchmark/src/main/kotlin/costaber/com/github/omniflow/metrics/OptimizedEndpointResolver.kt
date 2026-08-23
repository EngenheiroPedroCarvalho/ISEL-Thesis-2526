package costaber.com.github.omniflow.metrics

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
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import java.net.URI

/**
 * Benchmark-only ALREADY-OPTIMIZED counterpart of [NaiveEndpointResolver]: reads the registry
 * ONCE per workflow (`readAll()` + `resolveUrlIn` per call, Θ(N+R)) instead of once per call.
 *
 * This carries the same logic the production `deployment` module used to expose as
 * `WorkflowInternalCallEndpointResolver` before that class was removed as superseded once the
 * single-read optimization was propagated into the real production resolvers
 * (`AwsInternalFunctionResolver`/`WorkflowInternalFunctionResolver`). It is kept here, relocated
 * rather than deleted, because P3/P8/P9/P14/P15/P16/P18/P19 rely on it as a pure-local-logic
 * reference implementation, deliberately decoupled from any provider-specific live-validation
 * step (unlike the production resolvers, which always validate a registry hit against a live
 * inspector) - see each benchmark's own doc comment for why that isolation matters.
 */
object OptimizedEndpointResolver {

    fun resolve(
        workflow: Workflow,
        store: FunctionRegistryStore,
        internalCallExtractor: (CallContext) -> String?
    ): Workflow {
        val registrySnapshot = store.readAll()
        val updatedSteps = workflow.steps.toList().map { resolveStep(it, store, internalCallExtractor, registrySnapshot) }
        return workflow.copy(steps = updatedSteps)
    }

    private fun resolveStep(
        step: Step,
        store: FunctionRegistryStore,
        internalCallExtractor: (CallContext) -> String?,
        registrySnapshot: Map<String, FunctionInvocationMetadata>
    ): Step {
        val newCtx = resolveContext(step.context, store, internalCallExtractor, registrySnapshot)
        return step.copy(context = newCtx)
    }

    private fun resolveContext(
        ctx: StepContext,
        store: FunctionRegistryStore,
        internalCallExtractor: (CallContext) -> String?,
        registrySnapshot: Map<String, FunctionInvocationMetadata>
    ): StepContext {
        return when (ctx) {
            is CallContext -> resolveCall(ctx, store, internalCallExtractor, registrySnapshot)

            is BranchContext ->
                ctx.copy(steps = ctx.steps.map { resolveStep(it, store, internalCallExtractor, registrySnapshot) })

            is IterationRangeContext ->
                ctx.copy(steps = ctx.steps.map { resolveStep(it, store, internalCallExtractor, registrySnapshot) })

            is IterationForEachContext ->
                ctx.copy(steps = ctx.steps.map { resolveStep(it, store, internalCallExtractor, registrySnapshot) })

            is IterationContext ->
                IterationContext(ctx.value, ctx.steps.map { resolveStep(it, store, internalCallExtractor, registrySnapshot) })

            is ParallelBranchContext ->
                ctx.copy(
                    branches = ctx.branches.map { b ->
                        b.copy(steps = b.steps.map { resolveStep(it, store, internalCallExtractor, registrySnapshot) })
                    }
                )

            is ParallelIterationContext -> {
                val itCtx = ctx.iterationContext
                val updatedIt = when (itCtx) {
                    is IterationRangeContext ->
                        itCtx.copy(steps = itCtx.steps.map { resolveStep(it, store, internalCallExtractor, registrySnapshot) })

                    is IterationForEachContext ->
                        itCtx.copy(steps = itCtx.steps.map { resolveStep(it, store, internalCallExtractor, registrySnapshot) })

                    else ->
                        IterationContext(itCtx.value, itCtx.steps.map { resolveStep(it, store, internalCallExtractor, registrySnapshot) })
                }
                ctx.copy(iterationContext = updatedIt)
            }

            else -> ctx
        }
    }

    private fun resolveCall(
        call: CallContext,
        store: FunctionRegistryStore,
        internalCallExtractor: (CallContext) -> String?,
        registrySnapshot: Map<String, FunctionInvocationMetadata>
    ): CallContext {
        val fnName = internalCallExtractor(call) ?: return call

        val url = store.resolveUrlIn(fnName, registrySnapshot)

        val (host, path) = splitUrl(url)

        return call.copy(host = host, path = path)
    }

    // Mirrors the removed WorkflowInternalCallEndpointResolver.splitUrl so both paths produce
    // byte-identical host/path (and thus identical rendered output).
    private fun splitUrl(url: String): Pair<String, String> {
        val uri = URI(url)
        val host = "${uri.scheme}://${uri.authority}"
        val path = uri.rawPath?.ifBlank { "/" } ?: "/"
        return host to path
    }
}
