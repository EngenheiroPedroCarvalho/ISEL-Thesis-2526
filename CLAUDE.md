# CLAUDE.md

Guidance for Claude Code when working in this repository. Open work (thesis and code) is tracked
in `TODO.md`.

## Behavioral guidelines (Karpathy-inspired)

General coding-agent guidelines, merged in from
[multica-ai/andrej-karpathy-skills](https://github.com/multica-ai/andrej-karpathy-skills) (MIT
license), itself derived from Andrej Karpathy's public observations on common LLM coding pitfalls.
Apply these before the project-specific guidance below.

1. **Think Before Coding.** Don't assume — state assumptions explicitly. If a request is
   ambiguous, present the alternative interpretations instead of silently picking one. Stop and ask
   when confused rather than guessing; push back if a simpler approach exists.
2. **Simplicity First.** Write the minimum code that solves the problem — nothing speculative. No
   unrequested features, no abstractions for single-use code, no error handling for scenarios that
   can't happen. Test: would a senior engineer call this overcomplicated?
3. **Surgical Changes.** Touch only what the request requires; clean up only the mess your own
   edit creates. Don't refactor, reformat, or "fix" adjacent code while making a requested change;
   match the existing style even if you'd prefer another. Test: every changed line should trace
   directly to the request.
4. **Goal-Driven Execution.** Define success criteria and loop until verified. Convert vague asks
   ("fix the bug," "add validation") into explicit, testable steps, each paired with how it's
   verified.

These bias toward caution over speed — for trivial tasks (typo fixes, obvious one-liners), use
judgement; not every change needs the full rigor.

## What this project is

Master's thesis (ISEL) — **OmniFlow + QuickFaaS integration**. OmniFlow is a Kotlin DSL that
defines cloud workflows and renders/deploys them to **AWS Step Functions** and **GCP Cloud
Workflows**. QuickFaaS is a companion tool that deploys serverless **functions**.

**The contribution** has two halves:
- **(B) Unification (core):** incorporate QuickFaaS into OmniFlow so that, from a single workflow
  definition, internal functions are auto-deployed and their endpoints resolved and wired into the
  workflow — instead of two separate manual steps.
- **(A) AWS support (complement):** extend QuickFaaS (previously GCP/Azure only) to deploy to
  **AWS Lambda**.

## Repository layout

```
ISEL-Thesis-2526/
├── omni-flow-main/                          # main project (Maven multi-module)
│   ├── deployment/                          # OmniFlow core: DSL, model, renderers, registry
│   │   └── src/{main,test}/kotlin/costaber/com/github/omniflow/
│   ├── benchmark/                           # JMH performance benchmarks
│   │   └── .../metrics/ , .../generator/
│   ├── quickfaas-essentials/QuickFaaS-Deployment/   # QuickFaaS deployer (Gradle, Kotlin 1.6.20)
│   │   └── src/{main,test}/kotlin/model/    # cloud providers: AwsProvider, GcpProvider, ...
│   └── TESTING.md                           # full testing documentation (read this)
├── dissertation/                            # MSc dissertation LaTeX sources (canonical; see its CLAUDE.md)
├── diagrams/                                # PlantUML sources and PNGs for the thesis figures
├── thesis/                                  # older split copy of the chapters (superseded; do not edit)
├── quickfaas-essentials/                    # reference clone of the original QuickFaaS (do not edit)
└── TODO.md                                  # open work on the thesis and the code
```

Two build systems: `deployment/` and `benchmark/` are **Maven**; `QuickFaaS-Deployment/` is
**Gradle**.

## Key packages (the contribution's code)

Under `omni-flow-main/deployment/src/main/kotlin/costaber/com/github/omniflow/`:

- `registry/` — the Function Registry: `FunctionRegistryStore` (JSON file I/O and lookups),
  `FunctionRegistryBootstrapper` and `CloudFunctionsCatalog` (populate a missing registry from the
  provider), `FunctionInvocationMetadata`.
- `internalfunction/` — `WorkflowInternalFunctionResolver` (GCP cascade) and the
  `InternalFunctionDeployer` strategy (with `NoopInternalFunctionDeployer`).
- `internalfunction/quickfaas/` — `AwsInternalFunctionResolver` (AWS cascade), the Level 3
  deployers `QuickFaasDeployer` (GCP) and `AwsLambdaDeployer` (AWS), `QuickFaasDescriptor` /
  `QuickFaasDescriptorLoader`, and `QuickFaasProcessInvoker` (runs the QuickFaaS jar as a
  subprocess).
- `cloud/provider/{amazon,google}/` — renderers; the entry-point deployers `AmazonCloudDeployer`
  and `GoogleCloudDeployer` (bootstrap the registry, run the resolver, deploy the workflow); and
  provider lookups (`LambdaFunctionInspector`, `LambdaFunctionsCatalog`, `AwsRegionsLister`;
  `CloudRunV2ServiceInspector`, `CloudRunV2RestCatalog`, `CloudRunLocationsV1RestClient`).

Under `omni-flow-main/quickfaas-essentials/QuickFaaS-Deployment/src/main/kotlin/model/`: the AWS
provider added to QuickFaaS (`AwsProvider`, `AwsLambdaFunction`, `AwsRequests`, `AwsS3Bucket`, …).

## How the resolution cascade behaves (verified 2026-09-11)

The dissertation describes this behaviour, so keep the code and `dissertation/` consistent.

- **Levels:** 1 = registry hit, validated against the live function; 2 = provider discovery
  across regions (or one region with the `"region/name"` form); 3 = QuickFaaS deployment, only
  when the call supplies a `deploymentDescriptorPath`.
- **Registries are per provider:** `function-registry.gcp.json` and `function-registry.aws.json`
  in the working directory (override with the deployer builders' `registryPath(...)`).
- **Single read:** both resolvers read the registry once per `resolve()` into a snapshot, look
  calls up with `tryResolveEntryIn`, and keep the snapshot in sync with `put`/`remove`.
- **No deployment without confirmed absence:** a discovery error (for example, inaccessible
  regions) aborts; it never falls through to Level 3. A stale registry entry also aborts, even when
  a descriptor is present.
- **No updates:** Level 3 runs only when the function is absent; the cascade never updates an
  existing function.
- **Static binding:** the resolved endpoint is embedded in the rendered workflow at deployment
  time.
- **GCP limitation:** `QuickFaasDeployer` creates first-generation Cloud Functions
  (`cloudfunctions.net` URLs), but validation, discovery and bootstrap query only Cloud Run. Bindings
  to first-gen functions skip validation and can't be rediscovered. This is documented in the
  thesis; the fix idea is in `TODO.md`.

## Build, test, coverage

```bash
# Part B — deployment (Maven)
cd omni-flow-main
./mvnw -pl deployment test                    # run unit tests
./mvnw -pl deployment test jacoco:report      # + coverage -> deployment/target/site/jacoco/index.html

# Part A — QuickFaaS AWS provider (Gradle)
cd omni-flow-main/quickfaas-essentials/QuickFaaS-Deployment
./gradlew test                                # run unit tests
./gradlew jacocoTestReport                    # + coverage -> build/reports/jacoco/test/html/index.html
```

The code targets Java 17. On this Mac, IntelliJ runs the tests with its bundled JDK 25, but in a
terminal `/usr/bin/java` is only the macOS placeholder: Maven then hangs silently. Set `JAVA_HOME`
first, and add `-o` to run offline:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@26/libexec/openjdk.jdk/Contents/Home ./mvnw -o -pl deployment test
```

## Performance benchmarks (JMH)

The benchmark jar's Main-Class is a custom launcher; to use JMH CLI flags, invoke the JMH runner
directly on the classpath:

```bash
cd omni-flow-main
./mvnw -pl benchmark -am clean package                 # builds benchmark/target/benchmarks.jar
java -cp benchmark/target/benchmarks.jar org.openjdk.jmh.Main \
  "costaber\.com\.github\.omniflow\.metrics\.Benchmark(Rendering|InternalCall).*" \
  -f 1 -wi 3 -i 5 -w 1 -r 1 -rf csv -rff benchmark/results/jmh-results.csv
python3 benchmark/results/plot_benchmarks.py           # regenerate the P1-P5 graphs
```

Results and graphs live in `benchmark/results/` (`RESULTS.md`, `P1_*.png … P5_*.png`).
For thesis-grade numbers use `-f 3` on a dedicated machine.

## Testing conventions

- **Scope:** tests exercise **only local operations** (pure logic, local file I/O, rendering,
  resolution). **Do NOT write tests that call — or mock — AWS/GCP SDKs or HTTP.** Cloud-calling
  classes (`AwsLambdaDeployer`, `AwsRequests`, `CloudFunctionsIamHelper`, `deployZip`, etc.) are
  out of unit-test scope by design.
- **Assertion stack:** `deployment/` uses **JUnit 5 + Strikt + MockK** (and `@TempDir`);
  `QuickFaaS-Deployment/` has **only `kotlin.test`** on the classpath (no Strikt/MockK).
- Mirror existing tests for import style (`QuickFaasDescriptorTest.kt`, `AmazonRendererTest.kt`).
- Benchmarks: JMH `@BenchmarkMode(AverageTime)`, consume outputs with `Blackhole`.

## Gotchas (learned the hard way)

- **`junit-vintage-engine`** must stay in `deployment/pom.xml`: many existing tests use
  `kotlin-test-junit` (JUnit 4) and won't run under Surefire (JUnit 5) without it.
- **Tests run on JDK 21 or newer** (bytecode targets 17). `System.setSecurityManager` is
  unavailable, so code paths that call `kotlin.system.exitProcess` (e.g. `logPropertyMissing` via
  `setProjectData("")`) are **not unit-testable in-process** — leave them untested and documented.
- **`FunctionRegistryStore` has no cache.** `readAll`, `tryResolveEntry`, `resolveEntry` and
  `resolveUrl` re-read and re-parse the whole file on every call, and every `put`/`remove` rewrites
  it. The production resolvers avoid the read cost by reading once per `resolve()` and using the
  pure `tryResolveEntryIn`/`resolveUrlIn`. Benchmarks P6/P7 measure the per-call read path, P10/P11
  the resolvers, and P13/P17 the write path.
- **`GoogleAccessTokenProvider`'s default constructor arg calls `GoogleCredentials.getApplicationDefault()`
  eagerly** — and so do `CloudRunV2ServiceInspector()`/`CloudRunLocationsV1RestClient()`, which
  default-construct a `GoogleAccessTokenProvider` themselves. Just *instantiating* either class
  with no-arg defaults tries to resolve real Application Default Credentials and fails/hangs
  without `gcloud auth application-default login` configured — even if no method that actually
  needs a token is ever called. Local-only tests/benchmarks that construct these classes (e.g. P11)
  must pass an explicit `GoogleAccessTokenProvider(credentials = GoogleCredentials.create(AccessToken(...)))`
  to avoid touching ADC.
- `.gradle/` is (unfortunately) tracked in the repo; avoid committing its churn — `git checkout --`
  it before committing.

## Conventions

- Never push to `main`. Development happens on feature branches
  (current: `claude/progress-report-compliance-e4damf`).
- Commit only when the user asks, and treat a commit as published: commits on this branch have been
  pushed to GitHub automatically (probably by IntelliJ).
- Don't edit `quickfaas-essentials/` at the repo root — it's a read-only reference clone; the
  active QuickFaaS code is under `omni-flow-main/quickfaas-essentials/QuickFaaS-Deployment/`.
- Prefer minimal production changes; confirm before altering behaviour of colleagues' code.
