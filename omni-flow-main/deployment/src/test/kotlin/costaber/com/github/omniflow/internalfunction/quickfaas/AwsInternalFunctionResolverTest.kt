package costaber.com.github.omniflow.internalfunction.quickfaas

import costaber.com.github.omniflow.builder.ResultType
import costaber.com.github.omniflow.model.AssignContext
import costaber.com.github.omniflow.model.BranchContext
import costaber.com.github.omniflow.model.CallContext
import costaber.com.github.omniflow.model.HttpMethod
import costaber.com.github.omniflow.model.InternalFunction
import costaber.com.github.omniflow.model.IterationForEachContext
import costaber.com.github.omniflow.model.IterationRangeContext
import costaber.com.github.omniflow.model.ParallelBranchContext
import costaber.com.github.omniflow.model.ParallelIterationContext
import costaber.com.github.omniflow.model.Range
import costaber.com.github.omniflow.model.Step
import costaber.com.github.omniflow.model.StepContext
import costaber.com.github.omniflow.model.StepType
import costaber.com.github.omniflow.model.Value
import costaber.com.github.omniflow.model.Variable
import costaber.com.github.omniflow.model.VariableInitialization
import costaber.com.github.omniflow.model.Workflow
import costaber.com.github.omniflow.registry.FunctionInvocationMetadata
import costaber.com.github.omniflow.registry.FunctionRegistryStore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.nio.file.Path

/**
 * Unit tests for [AwsInternalFunctionResolver].
 *
 * The resolver short-circuits on a registry hit (local file lookup via
 * [FunctionRegistryStore.tryResolveEntry]) and never reaches the AWS Lambda API in
 * these tests. Every entry is pre-seeded into a @TempDir registry file, so all
 * assertions exercise pure local logic only: no AWS SDK calls, no HTTP, no network.
 */
internal class AwsInternalFunctionResolverTest {

    @TempDir
    lateinit var tempDir: Path

    private fun storeWith(vararg entries: Pair<String, FunctionInvocationMetadata>): FunctionRegistryStore {
        val store = FunctionRegistryStore(tempDir.resolve("function-registry.json"))
        store.writeNew(entries.toMap())
        return store
    }

    private fun externalCall(result: String = "callResult"): CallContext = CallContext(
        method = HttpMethod.GET,
        host = "https://external.example.com",
        path = "/api",
        header = mapOf("Accept" to Value("application/json")),
        query = mapOf("q" to Value("1")),
        body = mapOf("k" to "v"),
        bodyRaw = "{\"k\":\"v\"}",
        timeoutInSeconds = 15L,
        result = result,
        resultType = ResultType.BODY
    )

    private fun internalCall(functionName: String, result: String = "internalResult"): CallContext = CallContext(
        method = HttpMethod.POST,
        host = "",
        path = "",
        header = mapOf("Accept" to Value("application/json")),
        query = mapOf("q" to Value("1")),
        body = mapOf("k" to "v"),
        bodyRaw = "{\"k\":\"v\"}",
        timeoutInSeconds = 15L,
        result = result,
        resultType = ResultType.BODY,
        internalFunction = InternalFunction(name = functionName)
    )

    private fun step(name: String, context: StepContext): Step =
        Step(name = name, description = "desc-$name", type = StepType.CALL, context = context, next = "")

    private fun workflow(vararg steps: Step): Workflow = Workflow(
        name = "wf",
        description = "a workflow",
        input = "input",
        steps = steps.toList(),
        result = "wfResult"
    )

    @Test
    fun `workflow without internal functions is unchanged in content`() {
        val store = storeWith("seeded" to FunctionInvocationMetadata("seeded", "arn:aws:lambda:eu-west-1:1:function:seeded"))
        val resolver = AwsInternalFunctionResolver(region = "eu-west-1", registry = store)
        val external = externalCall()
        val assign = AssignContext(
            variables = listOf<VariableInitialization<*>>(
                VariableInitialization(variable = Variable("x"), term = Value(1))
            )
        )
        val original = workflow(
            step("callStep", external),
            step("assignStep", assign)
        )

        val resolved = resolver.resolve(original)

        expectThat(resolved.name).isEqualTo("wf")
        expectThat(resolved.result).isEqualTo("wfResult")
        expectThat(resolved.steps.toList()).hasSize(2)
        expectThat(resolved.steps.toList()[0].context).isA<CallContext>().isEqualTo(external)
        expectThat(resolved.steps.toList()[1].context).isA<AssignContext>().isEqualTo(assign)
    }

    @Test
    fun `internal call is resolved from registry to lambda host`() {
        val store = storeWith(
            "greeting-fn" to FunctionInvocationMetadata(
                serviceName = "greeting-fn",
                url = "arn:aws:lambda:eu-west-1:123456789012:function:greeting-fn"
            )
        )
        val resolver = AwsInternalFunctionResolver(region = "eu-west-1", registry = store)
        val original = workflow(step("callStep", internalCall("greeting-fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-west-1:123456789012:function:greeting-fn")
            get { path }.isEqualTo("")
            get { internalFunction }.isNull()
            // Other fields are preserved.
            get { method }.isEqualTo(HttpMethod.POST)
            get { result }.isEqualTo("internalResult")
            get { timeoutInSeconds }.isEqualTo(15L)
        }
    }

    @Test
    fun `internal call resolved through regional suffix match`() {
        val store = storeWith(
            "eu-west-1/greeting-fn" to FunctionInvocationMetadata(
                serviceName = "greeting-fn",
                url = "arn:aws:lambda:eu-west-1:1:function:greeting-fn"
            )
        )
        val resolver = AwsInternalFunctionResolver(region = "eu-west-1", registry = store)
        val original = workflow(step("callStep", internalCall("greeting-fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-west-1:1:function:greeting-fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside branch is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "arn:fn"))
        val resolver = AwsInternalFunctionResolver(region = "eu-west-1", registry = store)
        val branch = BranchContext(name = "b1", steps = listOf(step("inner", internalCall("fn"))))
        val original = workflow(step("branchStep", branch))

        val resolved = resolver.resolve(original)

        val branchCtx = resolved.steps.first().context as BranchContext
        expectThat(branchCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside iteration range is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "arn:fn"))
        val resolver = AwsInternalFunctionResolver(region = "eu-west-1", registry = store)
        val iteration = IterationRangeContext(value = "i", steps = listOf(step("inner", internalCall("fn"))), range = Range(1, 5))
        val original = workflow(step("iterStep", iteration))

        val resolved = resolver.resolve(original)

        val iterCtx = resolved.steps.first().context as IterationRangeContext
        expectThat(iterCtx.range).isEqualTo(Range(1, 5))
        expectThat(iterCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside iteration forEach is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "arn:fn"))
        val resolver = AwsInternalFunctionResolver(region = "eu-west-1", registry = store)
        val iteration = IterationForEachContext(
            value = "i",
            steps = listOf(step("inner", internalCall("fn"))),
            forEachVariable = Variable("items")
        )
        val original = workflow(step("iterStep", iteration))

        val resolved = resolver.resolve(original)

        val iterCtx = resolved.steps.first().context as IterationForEachContext
        expectThat(iterCtx.forEachVariable.name).isEqualTo("items")
        expectThat(iterCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside parallel branches is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "arn:fn"))
        val resolver = AwsInternalFunctionResolver(region = "eu-west-1", registry = store)
        val parallel = ParallelBranchContext(
            branches = listOf(BranchContext(name = "b1", steps = listOf(step("inner", internalCall("fn")))))
        )
        val original = workflow(step("parStep", parallel))

        val resolved = resolver.resolve(original)

        val parCtx = resolved.steps.first().context as ParallelBranchContext
        expectThat(parCtx.branches.first().steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside parallel iteration is resolved`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "arn:fn"))
        val resolver = AwsInternalFunctionResolver(region = "eu-west-1", registry = store)
        val parallel = ParallelIterationContext(
            iterationContext = IterationForEachContext(
                value = "i",
                steps = listOf(step("inner", internalCall("fn"))),
                forEachVariable = Variable("items")
            )
        )
        val original = workflow(step("parIterStep", parallel))

        val resolved = resolver.resolve(original)

        val parCtx = resolved.steps.first().context as ParallelIterationContext
        expectThat(parCtx.iterationContext.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `external call keeps its host and path`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "arn:fn"))
        val resolver = AwsInternalFunctionResolver(region = "eu-west-1", registry = store)
        val external = externalCall()
        val original = workflow(step("callStep", external))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().isEqualTo(external)
    }
}
