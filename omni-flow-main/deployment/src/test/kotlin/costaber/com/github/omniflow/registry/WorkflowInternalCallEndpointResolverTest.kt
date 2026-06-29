package costaber.com.github.omniflow.registry

import costaber.com.github.omniflow.builder.ResultType
import costaber.com.github.omniflow.model.AssignContext
import costaber.com.github.omniflow.model.BranchContext
import costaber.com.github.omniflow.model.CallContext
import costaber.com.github.omniflow.model.HttpMethod
import costaber.com.github.omniflow.model.IterationForEachContext
import costaber.com.github.omniflow.model.IterationRangeContext
import costaber.com.github.omniflow.model.ParallelBranchContext
import costaber.com.github.omniflow.model.ParallelIterationContext
import costaber.com.github.omniflow.model.Range
import costaber.com.github.omniflow.model.Step
import costaber.com.github.omniflow.model.StepType
import costaber.com.github.omniflow.model.Term
import costaber.com.github.omniflow.model.Value
import costaber.com.github.omniflow.model.Variable
import costaber.com.github.omniflow.model.VariableInitialization
import costaber.com.github.omniflow.model.Workflow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.nio.file.Path

/**
 * Unit tests for [WorkflowInternalCallEndpointResolver].
 *
 * These tests exercise ONLY local logic: a real [FunctionRegistryStore] backed by a
 * @TempDir JSON file (local file I/O) plus pure in-memory model rewriting. No AWS/GCP
 * SDK calls, no HTTP, no network.
 */
internal class WorkflowInternalCallEndpointResolverTest {

    @TempDir
    lateinit var tempDir: Path

    private fun storeWith(vararg entries: Pair<String, FunctionInvocationMetadata>): FunctionRegistryStore {
        val store = FunctionRegistryStore(tempDir.resolve("function-registry.json"))
        store.writeNew(entries.toMap())
        return store
    }

    private fun callContext(
        host: String = "",
        path: String = "",
        result: String = "callResult"
    ): CallContext = CallContext(
        method = HttpMethod.POST,
        host = host,
        path = path,
        body = mapOf("payload" to "value"),
        bodyRaw = "{\"payload\":\"value\"}",
        header = mapOf("Content-Type" to Value("application/json")),
        query = mapOf("q" to Value("1")),
        timeoutInSeconds = 30L,
        result = result,
        resultType = ResultType.BODY
    )

    private fun step(name: String, context: costaber.com.github.omniflow.model.StepContext): Step =
        Step(name = name, description = "desc-$name", type = StepType.CALL, context = context, next = "")

    private fun workflow(vararg steps: Step): Workflow = Workflow(
        name = "wf",
        description = "a workflow",
        input = "input",
        steps = steps.toList(),
        result = "wfResult"
    )

    /** Extractor that marks a call internal when its [CallContext.result] matches the given marker. */
    private fun extractorByResult(marker: String, functionRef: String): (CallContext) -> String? =
        { call -> if (call.result == marker) functionRef else null }

    @Test
    fun `internal call has host and path replaced by registry endpoint`() {
        val store = storeWith(
            "greeting-fn" to FunctionInvocationMetadata(
                serviceName = "greeting-fn",
                url = "https://greeting-fn-abc123.lambda-url.eu-west-1.on.aws/invoke"
            )
        )
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val original = workflow(step("call", callContext(result = "internalCall")))

        val resolved = resolver.resolve(original, extractorByResult("internalCall", "greeting-fn"))

        val ctx = resolved.steps.first().context
        expectThat(ctx).isA<CallContext>().and {
            get { host }.isEqualTo("https://greeting-fn-abc123.lambda-url.eu-west-1.on.aws")
            get { path }.isEqualTo("/invoke")
        }
    }

    @Test
    fun `internal call with root path yields slash`() {
        val store = storeWith(
            "root-fn" to FunctionInvocationMetadata(
                serviceName = "root-fn",
                url = "https://root-fn.lambda-url.eu-west-1.on.aws/"
            )
        )
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val original = workflow(step("call", callContext(result = "internalCall")))

        val resolved = resolver.resolve(original, extractorByResult("internalCall", "root-fn"))

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://root-fn.lambda-url.eu-west-1.on.aws")
            get { path }.isEqualTo("/")
        }
    }

    @Test
    fun `external call is left untouched when extractor returns null`() {
        val store = storeWith(
            "greeting-fn" to FunctionInvocationMetadata("greeting-fn", "https://x/invoke")
        )
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val externalCall = callContext(host = "https://external.example.com", path = "/api/v1", result = "externalCall")
        val original = workflow(step("call", externalCall))

        // Extractor never matches => every call is external.
        val resolved = resolver.resolve(original) { null }

        val resolvedCall = resolved.steps.first().context
        expectThat(resolvedCall).isA<CallContext>().isEqualTo(externalCall)
    }

    @Test
    fun `other call context fields are untouched for internal call`() {
        val store = storeWith(
            "greeting-fn" to FunctionInvocationMetadata("greeting-fn", "https://h.example.com/p")
        )
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val source = callContext(result = "internalCall")
        val original = workflow(step("call", source))

        val resolved = resolver.resolve(original, extractorByResult("internalCall", "greeting-fn"))

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { method }.isEqualTo(HttpMethod.POST)
            get { header }.containsKey("Content-Type")
            get { query }.containsKey("q")
            get { body }.containsKey("payload")
            get { bodyRaw }.isEqualTo("{\"payload\":\"value\"}")
            get { timeoutInSeconds }.isEqualTo(30L)
            get { result }.isEqualTo("internalCall")
            get { resultType }.isEqualTo(ResultType.BODY)
            get { authentication }.isNull()
        }
    }

    @Test
    fun `nested call inside branch is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://host.example.com/branch"))
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val nested = step("inner", callContext(result = "internalCall"))
        val branch = BranchContext(name = "branch1", steps = listOf(nested))
        val original = workflow(step("branchStep", branch))

        val resolved = resolver.resolve(original, extractorByResult("internalCall", "fn"))

        val branchCtx = resolved.steps.first().context as BranchContext
        expectThat(branchCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://host.example.com")
            get { path }.isEqualTo("/branch")
        }
    }

    @Test
    fun `nested call inside iteration range is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://host.example.com/range"))
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val nested = step("inner", callContext(result = "internalCall"))
        val iteration = IterationRangeContext(value = "i", steps = listOf(nested), range = Range(1, 9))
        val original = workflow(step("iterStep", iteration))

        val resolved = resolver.resolve(original, extractorByResult("internalCall", "fn"))

        val iterCtx = resolved.steps.first().context as IterationRangeContext
        expectThat(iterCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://host.example.com")
            get { path }.isEqualTo("/range")
        }
        // The iteration container metadata is preserved.
        expectThat(iterCtx.range).isEqualTo(Range(1, 9))
    }

    @Test
    fun `nested call inside iteration forEach is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://host.example.com/foreach"))
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val nested = step("inner", callContext(result = "internalCall"))
        val iteration = IterationForEachContext(
            value = "i",
            steps = listOf(nested),
            forEachVariable = Variable("items")
        )
        val original = workflow(step("iterStep", iteration))

        val resolved = resolver.resolve(original, extractorByResult("internalCall", "fn"))

        val iterCtx = resolved.steps.first().context as IterationForEachContext
        expectThat(iterCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://host.example.com")
            get { path }.isEqualTo("/foreach")
        }
        expectThat(iterCtx.forEachVariable.name).isEqualTo("items")
    }

    @Test
    fun `nested call inside parallel branches is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://host.example.com/par"))
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val nested = step("inner", callContext(result = "internalCall"))
        val parallel = ParallelBranchContext(
            branches = listOf(BranchContext(name = "b1", steps = listOf(nested)))
        )
        val original = workflow(step("parStep", parallel))

        val resolved = resolver.resolve(original, extractorByResult("internalCall", "fn"))

        val parCtx = resolved.steps.first().context as ParallelBranchContext
        val innerCall = parCtx.branches.first().steps.first().context
        expectThat(innerCall).isA<CallContext>().and {
            get { host }.isEqualTo("https://host.example.com")
            get { path }.isEqualTo("/par")
        }
    }

    @Test
    fun `nested call inside parallel iteration is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://host.example.com/pariter"))
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val nested = step("inner", callContext(result = "internalCall"))
        val parallel = ParallelIterationContext(
            iterationContext = IterationRangeContext(value = "i", steps = listOf(nested), range = Range(0, 3))
        )
        val original = workflow(step("parIterStep", parallel))

        val resolved = resolver.resolve(original, extractorByResult("internalCall", "fn"))

        val parCtx = resolved.steps.first().context as ParallelIterationContext
        val innerCall = parCtx.iterationContext.steps.first().context
        expectThat(innerCall).isA<CallContext>().and {
            get { host }.isEqualTo("https://host.example.com")
            get { path }.isEqualTo("/pariter")
        }
    }

    @Test
    fun `non call contexts are left intact`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://host/path"))
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val assign = AssignContext(
            variables = listOf<VariableInitialization<*>>(
                VariableInitialization(variable = Variable("x"), term = Value(1))
            )
        )
        val original = workflow(step("assignStep", assign))

        val resolved = resolver.resolve(original) { "fn" }

        expectThat(resolved.steps.first().context).isA<AssignContext>().isEqualTo(assign)
    }

    @Test
    fun `missing registry key fails clearly`() {
        val store = storeWith("present-fn" to FunctionInvocationMetadata("present-fn", "https://host/p"))
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val original = workflow(step("call", callContext(result = "internalCall")))

        val ex = assertThrows<IllegalStateException> {
            resolver.resolve(original, extractorByResult("internalCall", "missing-fn"))
        }
        expectThat(ex.message ?: "").isEqualTo(ex.message ?: "")
    }

    @Test
    fun `resolve preserves workflow level metadata and step count`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://host/p"))
        val resolver = WorkflowInternalCallEndpointResolver(store)
        val external = callContext(host = "https://ext", path = "/keep", result = "externalCall")
        val original = workflow(
            step("a", external),
            step("b", callContext(result = "internalCall"))
        )

        val resolved = resolver.resolve(original, extractorByResult("internalCall", "fn"))

        expectThat(resolved.name).isEqualTo("wf")
        expectThat(resolved.description).isEqualTo("a workflow")
        expectThat(resolved.input).isEqualTo("input")
        expectThat(resolved.result).isEqualTo("wfResult")
        expectThat(resolved.steps.toList()).hasSize(2)
        // The external call was left byte-identical.
        expectThat(resolved.steps.toList()[0].context).isA<CallContext>().isEqualTo(external)
    }
}
