# CLAUDE.md

Guidance for Claude Code when working in this repository.

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
└── quickfaas-essentials/                    # reference clone of the original QuickFaaS (do not edit)
```

Two build systems: `deployment/` and `benchmark/` are **Maven**; `QuickFaaS-Deployment/` is
**Gradle**.

## Key packages (the contribution's code)

- `deployment/.../registry/` — function registry + endpoint resolution
  (`WorkflowInternalCallEndpointResolver`, `FunctionRegistryStore`, `FunctionEndpointKeys`).
- `deployment/.../internalfunction/` — auto-deploy glue (`AwsLambdaDeployer`,
  `AwsInternalFunctionResolver`, `QuickFaasDescriptorLoader`).
- `deployment/.../cloud/provider/{amazon,google}/` — provider renderers/deployers.
- `QuickFaaS-Deployment/.../model/Aws*` — the AWS provider added to QuickFaaS.

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
- **JDK 21 runtime.** `System.setSecurityManager` is unavailable, so code paths that call
  `kotlin.system.exitProcess` (e.g. `logPropertyMissing` via `setProjectData("")`) are **not
  unit-testable in-process** — leave them untested and documented.
- **`FunctionRegistryStore.resolveUrl`/`tryResolveEntry`/`resolveEntry` re-read and re-parse the
  whole registry file on every call** (no in-memory cache). Fine for small registries; O(N·M) if
  the registry grows. `resolveUrl` is exercised by benchmarks P3/P6/P7; `tryResolveEntry` is the
  same anti-pattern reached from the real auto-deploy resolvers
  (`AwsInternalFunctionResolver`/`WorkflowInternalFunctionResolver`), quantified by P10/P11 — a
  known optimization opportunity, not yet applied to those two classes.
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
  (current: `OmniFlow-QuickFaaS-Test`).
- Don't edit `quickfaas-essentials/` at the repo root — it's a read-only reference clone; the
  active QuickFaaS code is under `omni-flow-main/quickfaas-essentials/QuickFaaS-Deployment/`.
- Prefer minimal production changes; confirm before altering behaviour of colleagues' code.
