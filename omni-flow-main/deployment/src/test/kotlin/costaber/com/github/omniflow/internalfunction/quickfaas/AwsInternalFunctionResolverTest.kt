package costaber.com.github.omniflow.internalfunction.quickfaas

import costaber.com.github.omniflow.builder.ResultType
import costaber.com.github.omniflow.cloud.provider.amazon.service.AwsRegionsLister
import costaber.com.github.omniflow.cloud.provider.amazon.service.LambdaFunctionInspector
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
import strikt.api.expectThrows
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.nio.file.Path

/**
 * Unit tests for [AwsInternalFunctionResolver].
 *
 * A registry hit is validated against the live Lambda, and a registry miss is searched for across
 * every AWS region, so the resolver reaches AWS through the [LambdaFunctionInspector] and
 * [AwsRegionsLister] seams. These tests inject [FakeInspector] and [FakeRegionsLister] in their
 * place, so every assertion exercises pure local logic only: entries are pre-seeded into a
 * @TempDir registry file, and there are no AWS SDK calls, no HTTP and no network.
 */
internal class AwsInternalFunctionResolverTest {

    @TempDir
    lateinit var tempDir: Path

    /** Local stand-in for the Lambda API: answers from a canned (region, functionName) map. */
    private class FakeInspector(
        private val results: Map<Pair<String, String>, LambdaFunctionInspector.LookupResult> = emptyMap()
    ) : LambdaFunctionInspector {
        override fun lookup(region: String, functionName: String): LambdaFunctionInspector.LookupResult =
            results[region to functionName] ?: results[ANY_REGION to functionName] ?: LambdaFunctionInspector.LookupResult.NotFound

        companion object {
            private const val ANY_REGION = "*"

            /** Seeds results that match regardless of which region is queried. */
            fun byName(byName: Map<String, LambdaFunctionInspector.LookupResult>): FakeInspector =
                FakeInspector(byName.mapKeys { (name, _) -> ANY_REGION to name })
        }
    }

    /** Local stand-in for AWS region discovery: returns a fixed, injected region list. */
    private class FakeRegionsLister(private val regions: List<String>) : AwsRegionsLister {
        override fun listRegions(bootstrapRegion: String): List<String> = regions
    }

    private fun storeWith(vararg entries: Pair<String, FunctionInvocationMetadata>): FunctionRegistryStore {
        val store = FunctionRegistryStore(tempDir.resolve("function-registry.json"))
        store.writeNew(entries.toMap())
        return store
    }

    /** Inspector that reports exactly what the registry already holds, i.e. no drift. */
    private fun noDriftInspector(vararg entries: Pair<String, FunctionInvocationMetadata>) =
        FakeInspector.byName(
            entries.associate { (_, meta) ->
                meta.serviceName to LambdaFunctionInspector.LookupResult.Found(meta.serviceName, meta.url)
            }
        )

    private fun resolverWith(vararg entries: Pair<String, FunctionInvocationMetadata>) =
        AwsInternalFunctionResolver(
            preferredRegion = "eu-west-1",
            registry = storeWith(*entries),
            inspector = noDriftInspector(*entries),
            regionsLister = FakeRegionsLister(listOf("eu-west-1"))
        )

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
        val resolver = resolverWith("seeded" to FunctionInvocationMetadata("seeded", "arn:aws:lambda:eu-west-1:1:function:seeded"))
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
        val resolver = resolverWith(
            "greeting-fn" to FunctionInvocationMetadata(
                serviceName = "greeting-fn",
                url = "arn:aws:lambda:eu-west-1:123456789012:function:greeting-fn"
            )
        )
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
        val resolver = resolverWith(
            "eu-west-1/greeting-fn" to FunctionInvocationMetadata(
                serviceName = "greeting-fn",
                url = "arn:aws:lambda:eu-west-1:1:function:greeting-fn"
            )
        )
        val original = workflow(step("callStep", internalCall("greeting-fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-west-1:1:function:greeting-fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside branch is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "arn:aws:lambda:eu-west-1:1:function:fn"))
        val branch = BranchContext(name = "b1", steps = listOf(step("inner", internalCall("fn"))))
        val original = workflow(step("branchStep", branch))

        val resolved = resolver.resolve(original)

        val branchCtx = resolved.steps.first().context as BranchContext
        expectThat(branchCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-west-1:1:function:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside iteration range is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "arn:aws:lambda:eu-west-1:1:function:fn"))
        val iteration = IterationRangeContext(value = "i", steps = listOf(step("inner", internalCall("fn"))), range = Range(1, 5))
        val original = workflow(step("iterStep", iteration))

        val resolved = resolver.resolve(original)

        val iterCtx = resolved.steps.first().context as IterationRangeContext
        expectThat(iterCtx.range).isEqualTo(Range(1, 5))
        expectThat(iterCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-west-1:1:function:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside iteration forEach is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "arn:aws:lambda:eu-west-1:1:function:fn"))
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
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-west-1:1:function:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside parallel branches is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "arn:aws:lambda:eu-west-1:1:function:fn"))
        val parallel = ParallelBranchContext(
            branches = listOf(BranchContext(name = "b1", steps = listOf(step("inner", internalCall("fn")))))
        )
        val original = workflow(step("parStep", parallel))

        val resolved = resolver.resolve(original)

        val parCtx = resolved.steps.first().context as ParallelBranchContext
        expectThat(parCtx.branches.first().steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-west-1:1:function:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside parallel iteration is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "arn:aws:lambda:eu-west-1:1:function:fn"))
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
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-west-1:1:function:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `external call keeps its host and path`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "arn:aws:lambda:eu-west-1:1:function:fn"))
        val external = externalCall()
        val original = workflow(step("callStep", external))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().isEqualTo(external)
    }

    @Test
    fun `drifted ARN is refreshed in the registry and used`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "arn:aws:lambda:eu-west-1:1:function:fn-stale"))
        val resolver = AwsInternalFunctionResolver(
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = FakeInspector.byName(
                mapOf("fn" to LambdaFunctionInspector.LookupResult.Found("fn", "arn:aws:lambda:eu-west-1:1:function:fn-current"))
            ),
            regionsLister = FakeRegionsLister(listOf("eu-west-1"))
        )
        val original = workflow(step("callStep", internalCall("fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-west-1:1:function:fn-current")
            get { internalFunction }.isNull()
        }
        expectThat(store.tryResolveEntry("fn")?.second?.url).isEqualTo("arn:aws:lambda:eu-west-1:1:function:fn-current")
    }

    @Test
    fun `missing lambda removes the stale entry and aborts`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "arn:aws:lambda:eu-west-1:1:function:fn"))
        val resolver = AwsInternalFunctionResolver(
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = FakeInspector(),
            regionsLister = FakeRegionsLister(listOf("eu-west-1"))
        )
        val original = workflow(step("callStep", internalCall("fn")))

        expectThrows<IllegalStateException> { resolver.resolve(original) }
        expectThat(store.tryResolveEntry("fn")).isNull()
    }

    @Test
    fun `permission failure aborts instead of being treated as a miss`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "arn:aws:lambda:eu-west-1:1:function:fn"))
        val resolver = AwsInternalFunctionResolver(
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = FakeInspector.byName(
                mapOf("fn" to LambdaFunctionInspector.LookupResult.Forbidden("access denied"))
            ),
            regionsLister = FakeRegionsLister(listOf("eu-west-1"))
        )
        val original = workflow(step("callStep", internalCall("fn")))

        expectThrows<IllegalStateException> { resolver.resolve(original) }
        // The entry is a valid binding that could not be checked, so it must survive.
        expectThat(store.tryResolveEntry("fn")?.second?.url).isEqualTo("arn:aws:lambda:eu-west-1:1:function:fn")
    }

    @Test
    fun `function found only in a non-preferred region is discovered and registered`() {
        val store = storeWith()
        val resolver = AwsInternalFunctionResolver(
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = FakeInspector(
                mapOf(
                    ("eu-west-1" to "fn") to LambdaFunctionInspector.LookupResult.NotFound,
                    ("eu-central-1" to "fn") to LambdaFunctionInspector.LookupResult.Found(
                        "fn", "arn:aws:lambda:eu-central-1:1:function:fn"
                    )
                )
            ),
            regionsLister = FakeRegionsLister(listOf("eu-west-1", "eu-central-1"))
        )
        val original = workflow(step("callStep", internalCall("fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-central-1:1:function:fn")
            get { internalFunction }.isNull()
        }
        expectThat(store.tryResolveEntry("fn")?.second?.url).isEqualTo("arn:aws:lambda:eu-central-1:1:function:fn")
    }

    @Test
    fun `function found in multiple regions is ambiguous`() {
        val store = storeWith()
        val resolver = AwsInternalFunctionResolver(
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = FakeInspector(
                mapOf(
                    ("eu-west-1" to "fn") to LambdaFunctionInspector.LookupResult.Found(
                        "fn", "arn:aws:lambda:eu-west-1:1:function:fn"
                    ),
                    ("eu-central-1" to "fn") to LambdaFunctionInspector.LookupResult.Found(
                        "fn", "arn:aws:lambda:eu-central-1:1:function:fn"
                    )
                )
            ),
            regionsLister = FakeRegionsLister(listOf("eu-west-1", "eu-central-1"))
        )
        val original = workflow(step("callStep", internalCall("fn")))

        val thrown = expectThrows<IllegalStateException> { resolver.resolve(original) }
        thrown.get { message }.isEqualTo(
            "Internal function 'fn' exists in multiple AWS regions: eu-west-1, eu-central-1. " +
                    "Disambiguate using internalFunction(\"<region>/fn\")"
        )
    }

    @Test
    fun `explicit region-slash-name reference bypasses scanning`() {
        val store = storeWith()
        val resolver = AwsInternalFunctionResolver(
            preferredRegion = null,
            registry = store,
            inspector = FakeInspector(
                mapOf(
                    ("eu-central-1" to "fn") to LambdaFunctionInspector.LookupResult.Found(
                        "fn", "arn:aws:lambda:eu-central-1:1:function:fn"
                    )
                )
            ),
            regionsLister = FakeRegionsLister(emptyList())
        )
        val original = workflow(step("callStep", internalCall("eu-central-1/fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("lambda://arn:aws:lambda:eu-central-1:1:function:fn")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `all candidate regions forbidden and nothing found raises a distinct error`() {
        val store = storeWith()
        val resolver = AwsInternalFunctionResolver(
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = FakeInspector(
                mapOf(
                    ("eu-west-1" to "fn") to LambdaFunctionInspector.LookupResult.Forbidden("access denied"),
                    ("eu-central-1" to "fn") to LambdaFunctionInspector.LookupResult.Forbidden("access denied")
                )
            ),
            regionsLister = FakeRegionsLister(listOf("eu-west-1", "eu-central-1"))
        )
        val original = workflow(step("callStep", internalCall("fn")))

        val thrown = expectThrows<IllegalStateException> { resolver.resolve(original) }
        thrown.get { message }.isEqualTo(
            "Internal function 'fn' is not in function-registry and could not be confirmed in AWS Lambda. " +
                    "Some regions were not accessible: eu-west-1, eu-central-1. " +
                    "Tip: specify the region explicitly: internalFunction(\"<region>/fn\")."
        )
    }
}
