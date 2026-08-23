package costaber.com.github.omniflow.internalfunction

import costaber.com.github.omniflow.builder.ResultType
import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunLocationsV1RestClient
import costaber.com.github.omniflow.cloud.provider.google.service.CloudRunV2ServiceInspector
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.nio.file.Path

/**
 * Unit tests for [WorkflowInternalFunctionResolver].
 *
 * A registry hit is validated against the live Cloud Run service (unless the stored URL is a
 * 1st-gen Cloud Function, which short-circuits before any Cloud Run call), and a registry miss is
 * searched for across every Cloud Run location, so the resolver reaches GCP through the
 * [CloudRunV2ServiceInspector] and [CloudRunLocationsV1RestClient] seams. Both are concrete
 * classes (not interfaces), so they are mocked with MockK here rather than hand-rolled fakes; every
 * assertion still exercises pure local logic only: entries are pre-seeded into a @TempDir registry
 * file, and there are no Cloud Run/HTTP calls, no network.
 */
internal class WorkflowInternalFunctionResolverTest {

    @TempDir
    lateinit var tempDir: Path

    private fun storeWith(vararg entries: Pair<String, FunctionInvocationMetadata>): FunctionRegistryStore {
        val store = FunctionRegistryStore(tempDir.resolve("function-registry.json"))
        store.writeNew(entries.toMap())
        return store
    }

    private fun locationsClient(vararg regions: String): CloudRunLocationsV1RestClient {
        val client = mockk<CloudRunLocationsV1RestClient>()
        every { client.listProjectLocations(any()) } returns regions.toList()
        return client
    }

    /** Inspector that reports exactly what the registry already holds, i.e. no drift. */
    private fun noDriftInspector(vararg entries: Pair<String, FunctionInvocationMetadata>): CloudRunV2ServiceInspector {
        val inspector = mockk<CloudRunV2ServiceInspector>()
        entries.forEach { (_, meta) ->
            every { inspector.lookupByServiceName(meta.serviceName) } returns
                CloudRunV2ServiceInspector.LookupResult.Found(meta.serviceName, meta.url)
        }
        return inspector
    }

    private fun resolverWith(vararg entries: Pair<String, FunctionInvocationMetadata>) =
        WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = storeWith(*entries),
            inspector = noDriftInspector(*entries),
            locationsClient = locationsClient("eu-west-1")
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
        val resolver = resolverWith("seeded" to FunctionInvocationMetadata("seeded", "https://seeded-abc-ew.a.run.app"))
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
    fun `internal call is resolved from registry to Cloud Run host`() {
        val resolver = resolverWith(
            "greeting-fn" to FunctionInvocationMetadata(
                serviceName = "greeting-fn",
                url = "https://greeting-fn-abc123-ew.a.run.app"
            )
        )
        val original = workflow(step("callStep", internalCall("greeting-fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://greeting-fn-abc123-ew.a.run.app")
            get { path }.isEqualTo("")
            get { internalFunction }.isNull()
            // Other fields are preserved.
            get { method }.isEqualTo(HttpMethod.POST)
            get { result }.isEqualTo("internalResult")
            get { timeoutInSeconds }.isEqualTo(15L)
        }
    }

    @Test
    fun `1st-gen Cloud Function registry hit skips Cloud Run validation`() {
        val inspector = mockk<CloudRunV2ServiceInspector>()
        // No `every {}` stub registered for lookupByServiceName -> calling it would throw a
        // MockKException; a passing test proves the short-circuit avoided calling it at all.
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = storeWith(
                "legacy-fn" to FunctionInvocationMetadata(
                    "legacy-fn", "https://us-central1-proj.cloudfunctions.net/legacy-fn"
                )
            ),
            inspector = inspector,
            locationsClient = locationsClient("eu-west-1")
        )
        val original = workflow(step("callStep", internalCall("legacy-fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://us-central1-proj.cloudfunctions.net")
            get { path }.isEqualTo("/legacy-fn")
        }
        verify(exactly = 0) { inspector.lookupByServiceName(any()) }
    }

    @Test
    fun `internal call resolved through regional suffix match`() {
        val resolver = resolverWith(
            "eu-west-1/greeting-fn" to FunctionInvocationMetadata(
                serviceName = "greeting-fn",
                url = "https://greeting-fn-abc-ew.a.run.app"
            )
        )
        val original = workflow(step("callStep", internalCall("greeting-fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://greeting-fn-abc-ew.a.run.app")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside branch is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
        val branch = BranchContext(name = "b1", steps = listOf(step("inner", internalCall("fn"))))
        val original = workflow(step("branchStep", branch))

        val resolved = resolver.resolve(original)

        val branchCtx = resolved.steps.first().context as BranchContext
        expectThat(branchCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://fn-ew.a.run.app")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside iteration range is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
        val iteration = IterationRangeContext(value = "i", steps = listOf(step("inner", internalCall("fn"))), range = Range(1, 5))
        val original = workflow(step("iterStep", iteration))

        val resolved = resolver.resolve(original)

        val iterCtx = resolved.steps.first().context as IterationRangeContext
        expectThat(iterCtx.range).isEqualTo(Range(1, 5))
        expectThat(iterCtx.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://fn-ew.a.run.app")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside iteration forEach is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
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
            get { host }.isEqualTo("https://fn-ew.a.run.app")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside parallel branches is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
        val parallel = ParallelBranchContext(
            branches = listOf(BranchContext(name = "b1", steps = listOf(step("inner", internalCall("fn")))))
        )
        val original = workflow(step("parStep", parallel))

        val resolved = resolver.resolve(original)

        val parCtx = resolved.steps.first().context as ParallelBranchContext
        expectThat(parCtx.branches.first().steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://fn-ew.a.run.app")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `internal call inside parallel iteration is resolved`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
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
            get { host }.isEqualTo("https://fn-ew.a.run.app")
            get { internalFunction }.isNull()
        }
    }

    @Test
    fun `external call keeps its host and path`() {
        val resolver = resolverWith("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
        val external = externalCall()
        val original = workflow(step("callStep", external))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().isEqualTo(external)
    }

    @Test
    fun `drifted URL is refreshed in the registry and used`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://fn-stale-ew.a.run.app"))
        val inspector = mockk<CloudRunV2ServiceInspector>()
        every { inspector.lookupByServiceName("fn") } returns
            CloudRunV2ServiceInspector.LookupResult.Found("fn", "https://fn-current-ew.a.run.app")
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = inspector,
            locationsClient = locationsClient("eu-west-1")
        )
        val original = workflow(step("callStep", internalCall("fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://fn-current-ew.a.run.app")
            get { internalFunction }.isNull()
        }
        expectThat(store.tryResolveEntry("fn")?.second?.url).isEqualTo("https://fn-current-ew.a.run.app")
    }

    @Test
    fun `missing Cloud Run service removes the stale entry and aborts`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
        val inspector = mockk<CloudRunV2ServiceInspector>()
        every { inspector.lookupByServiceName("fn") } returns CloudRunV2ServiceInspector.LookupResult.NotFound
        every { inspector.lookup(any(), any(), any()) } returns CloudRunV2ServiceInspector.LookupResult.NotFound
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = inspector,
            locationsClient = locationsClient("eu-west-1")
        )
        val original = workflow(step("callStep", internalCall("fn")))

        expectThrows<IllegalStateException> { resolver.resolve(original) }
        expectThat(store.tryResolveEntry("fn")).isNull()
    }

    @Test
    fun `permission failure aborts instead of being treated as a miss`() {
        val store = storeWith("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
        val inspector = mockk<CloudRunV2ServiceInspector>()
        every { inspector.lookupByServiceName("fn") } returns
            CloudRunV2ServiceInspector.LookupResult.Forbidden("access denied")
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = inspector,
            locationsClient = locationsClient("eu-west-1")
        )
        val original = workflow(step("callStep", internalCall("fn")))

        expectThrows<IllegalStateException> { resolver.resolve(original) }
        // The entry is a valid binding that could not be checked, so it must survive.
        expectThat(store.tryResolveEntry("fn")?.second?.url).isEqualTo("https://fn-ew.a.run.app")
    }

    @Test
    fun `function found only in a non-preferred region is discovered and registered`() {
        val store = storeWith()
        val inspector = mockk<CloudRunV2ServiceInspector>()
        every { inspector.lookup("proj", "eu-west-1", "fn") } returns CloudRunV2ServiceInspector.LookupResult.NotFound
        every { inspector.lookup("proj", "eu-central-1", "fn") } returns
            CloudRunV2ServiceInspector.LookupResult.Found("fn", "https://fn-ec.a.run.app")
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = inspector,
            locationsClient = locationsClient("eu-west-1", "eu-central-1")
        )
        val original = workflow(step("callStep", internalCall("fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://fn-ec.a.run.app")
            get { internalFunction }.isNull()
        }
        expectThat(store.tryResolveEntry("fn")?.second?.url).isEqualTo("https://fn-ec.a.run.app")
    }

    @Test
    fun `function found in multiple regions is ambiguous`() {
        val store = storeWith()
        val inspector = mockk<CloudRunV2ServiceInspector>()
        every { inspector.lookup("proj", "eu-west-1", "fn") } returns
            CloudRunV2ServiceInspector.LookupResult.Found("fn", "https://fn-ew.a.run.app")
        every { inspector.lookup("proj", "eu-central-1", "fn") } returns
            CloudRunV2ServiceInspector.LookupResult.Found("fn", "https://fn-ec.a.run.app")
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = inspector,
            locationsClient = locationsClient("eu-west-1", "eu-central-1")
        )
        val original = workflow(step("callStep", internalCall("fn")))

        val thrown = expectThrows<IllegalStateException> { resolver.resolve(original) }
        thrown.get { message }.isEqualTo(
            "Internal function 'fn' exists in multiple Cloud Run regions: eu-west-1, eu-central-1. " +
                    "Disambiguate using internalFunction(\"<region>/fn\")"
        )
    }

    @Test
    fun `explicit region-slash-service reference bypasses scanning`() {
        // Regression test for a region/serviceId parsing bug: discoverServiceForRef used to read
        // BOTH region and serviceId via substringBefore("/"), so a qualified ref looked itself up
        // under the region's name instead of the service's. With the fix, "eu-central-1/fn" must
        // resolve to serviceId "fn" in region "eu-central-1" - stubbing only that exact call means
        // the old bug (looking up serviceId "eu-central-1") would hit an unstubbed MockK call and
        // fail the test.
        val store = storeWith()
        val inspector = mockk<CloudRunV2ServiceInspector>()
        every { inspector.lookup("proj", "eu-central-1", "fn") } returns
            CloudRunV2ServiceInspector.LookupResult.Found("fn", "https://fn-ec.a.run.app")
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = null,
            registry = store,
            inspector = inspector,
            locationsClient = locationsClient()
        )
        val original = workflow(step("callStep", internalCall("eu-central-1/fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://fn-ec.a.run.app")
            get { internalFunction }.isNull()
        }
        verify { inspector.lookup("proj", "eu-central-1", "fn") }
    }

    @Test
    fun `all candidate regions forbidden and nothing found raises a distinct error`() {
        val store = storeWith()
        val inspector = mockk<CloudRunV2ServiceInspector>()
        every { inspector.lookup("proj", "eu-west-1", "fn") } returns
            CloudRunV2ServiceInspector.LookupResult.Forbidden("access denied")
        every { inspector.lookup("proj", "eu-central-1", "fn") } returns
            CloudRunV2ServiceInspector.LookupResult.Forbidden("access denied")
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = inspector,
            locationsClient = locationsClient("eu-west-1", "eu-central-1")
        )
        val original = workflow(step("callStep", internalCall("fn")))

        val thrown = expectThrows<IllegalStateException> { resolver.resolve(original) }
        thrown.get { message }.isEqualTo(
            "Internal function 'fn' is not in function-registry and could not be confirmed in Cloud Run. " +
                    "Some regions were not accessible: eu-west-1, eu-central-1. " +
                    "Tip: specify the region explicitly: internalFunction(\"<region>/fn\")."
        )
    }

    @Test
    fun `registry is read once per workflow regardless of the number of internal calls`() {
        val store = spyk(storeWith("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app")))
        val inspector = noDriftInspector("fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = inspector,
            locationsClient = locationsClient("eu-west-1")
        )
        val original = workflow(
            step("call1", internalCall("fn")),
            step("call2", internalCall("fn")),
            step("call3", internalCall("fn"))
        )

        resolver.resolve(original)

        verify(exactly = 1) { store.readAll() }
    }

    @Test
    fun `suffix-matched stale entry rediscovered in a different region replaces the key`() {
        val store = storeWith("eu-west-1/fn" to FunctionInvocationMetadata("fn", "https://fn-ew.a.run.app"))
        val inspector = mockk<CloudRunV2ServiceInspector>()
        every { inspector.lookupByServiceName("fn") } returns CloudRunV2ServiceInspector.LookupResult.NotFound
        every { inspector.lookup("proj", "eu-west-1", "fn") } returns CloudRunV2ServiceInspector.LookupResult.NotFound
        every { inspector.lookup("proj", "eu-central-1", "fn") } returns
            CloudRunV2ServiceInspector.LookupResult.Found("fn", "https://fn-ec.a.run.app")
        val resolver = WorkflowInternalFunctionResolver(
            projectId = "proj",
            preferredRegion = "eu-west-1",
            registry = store,
            inspector = inspector,
            locationsClient = locationsClient("eu-west-1", "eu-central-1")
        )
        val original = workflow(step("callStep", internalCall("fn")))

        val resolved = resolver.resolve(original)

        expectThat(resolved.steps.first().context).isA<CallContext>().and {
            get { host }.isEqualTo("https://fn-ec.a.run.app")
            get { internalFunction }.isNull()
        }
        // The stale "eu-west-1/fn" key (now pointing at the wrong region) is gone, replaced by
        // a plain "fn" key consistent with how a fresh discovery would key it.
        expectThat(store.readAll().keys.toList()).containsExactly("fn")
    }
}
