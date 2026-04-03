package costaber.com.github.omniflow.model

import costaber.com.github.omniflow.builder.ResultType

data class CallContext(
    val method: HttpMethod,
    /**
     * For EXTERNAL calls, these are defined explicitly in the DSL
     * For INTERNAL calls, OmniFlow fills these (from the registry) before render/deploy
     */
    val host: String,
    val path: String,

    val authentication: Authentication? = null,
    val body: Map<String, Any> = emptyMap(),
    val bodyRaw: String = "",
    val header: Map<String, Term<*>> = emptyMap(),
    val query: Map<String, Term<*>> = emptyMap(),
    val timeoutInSeconds: Long? = null,
    val result: String,
    val resultType: ResultType = ResultType.BODY,

    /**
     * New field (internal call).
     * Rule:
     * - if internalFunction != null -> host/path MUST NOT be provided in DSL
     * - else -> host/path MUST be provided in DSL
     */
    val internalFunction: InternalFunction? = null
) : StepContext {

    override fun childNodes() = emptyList<Node>()
}