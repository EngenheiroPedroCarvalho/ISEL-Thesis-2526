package costaber.com.github.omniflow.metrics

import costaber.com.github.omniflow.model.CallContext
import costaber.com.github.omniflow.model.Workflow
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import java.net.URI

/**
 * Benchmark-only NAIVE counterpart of
 * [costaber.com.github.omniflow.registry.WorkflowInternalCallEndpointResolver].
 *
 * The production resolver reads the registry ONCE per workflow (`readAll()` +
 * `resolveUrlIn` per call, Θ(N+R)). This one instead calls
 * [FunctionRegistryStore.resolveUrl] on EVERY internal call, so it re-reads and
 * re-parses the whole registry file per call (Θ(N·R)) - the legacy pre-optimization
 * path. Used by P8/P9 to measure workflow resolution with and without the
 * registry-read optimization.
 *
 * It only walks a FLAT list of CALL steps (which the P8/P9 generators produce) and
 * fills host/path exactly like the production resolver (same [URI]-based split, keeping
 * `internalFunction`), so the resolved workflow is identical to the optimized path.
 * Only the read strategy differs.
 */
object NaiveEndpointResolver {

    fun resolve(
        workflow: Workflow,
        store: FunctionRegistryStore,
        internalCallExtractor: (CallContext) -> String?
    ): Workflow {
        val updatedSteps = workflow.steps.map { step ->
            val ctx = step.context
            if (ctx is CallContext) {
                val fnName = internalCallExtractor(ctx)
                if (fnName != null) {
                    // Re-reads + re-parses the whole registry file on every call.
                    val (host, path) = splitUrl(store.resolveUrl(fnName))
                    step.copy(context = ctx.copy(host = host, path = path))
                } else step
            } else step
        }
        return workflow.copy(steps = updatedSteps)
    }

    // Mirrors WorkflowInternalCallEndpointResolver.splitUrl so both paths produce
    // byte-identical host/path (and thus identical rendered output).
    private fun splitUrl(url: String): Pair<String, String> {
        val uri = URI(url)
        val host = "${uri.scheme}://${uri.authority}"
        val path = uri.rawPath?.ifBlank { "/" } ?: "/"
        return host to path
    }
}
