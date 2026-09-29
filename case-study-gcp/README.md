# Case study run on GCP (2026-09-28)

The `PaymentAuthorization` workflow of the dissertation's case study (Chapter 5), deployed and
executed against a live GCP project (`tfm26-509910`, region `europe-west1`) on 2026-09-28. This
folder holds what was run and what it produced, as evidence for the chapter.

## What is here

| Path | Content |
|---|---|
| `Case.kt` | The workflow as run, and a small runner around `GoogleCloudDeployer` + `QuickFaasDeployer` |
| `functions/` | The three QuickFaaS hooks and their deployment descriptors |
| `scripts/run-case-study-gcp.sh` | Phases 0–4: stub, first deployment, executions, second deployment, executions |
| `scripts/fix-stub-and-run.sh` | Replaces the stub and runs the three transactions (see "What happened") |
| `logs/` | Output of each phase, the registry after each deployment, and the deployed YAML |

The logs had their ANSI colour codes removed; nothing else was changed.

## How it differs from Listing B.2

- Internal calls use region-qualified references (`"europe-west1/fraud-check"`). In this project
  the region `me-central2` refuses Cloud Run lookups (403 `LOCATION_POLICY_VIOLATED`), so a bare
  reference that misses the registry would abort at Level 2.
- The scoring calls pass `amount` and `country` as query parameters: Cloud Workflows does not
  accept a map (`transaction`) as a query value.
- `report-decision` calls `core-banking-stub`, a QuickFaaS function deployed outside the ladder,
  through a literal `https://` URL (the GCP renderer adds no scheme to a literal host).

## What happened

1. **Stub** (`logs/0-stub.log`): `core-banking-stub` deployed with QuickFaaS alone.
2. **First deployment, no registry** (`logs/1-first-deploy.log`): the bootstrap listed the
   project's Cloud Run services (skipping `me-central2`) and registered the two already present
   (`quickfaas-test-fn`, `core-banking-stub`). Both scoring functions missed at Levels 1 and 2 and
   were deployed at Level 3 (about a minute each); the workflow was created with their `run.app`
   URLs (`logs/rendered-first.yaml`, `logs/registry-after-first.json`).
3. **Executions** (`logs/2-executions.log`): all `FAILED` at `report-decision`. The stub returned
   500: QuickFaaS's `GcpHttpRequest.getFromJsonBody` requires the content type to equal
   `application/json`, and Cloud Workflows sends `application/json; charset=utf-8`.
4. **Second deployment** (`logs/3-second-deploy.log`): the workflow was deleted first, because the
   deployer only creates workflows (`ALREADY_EXISTS` otherwise). Both calls were Level 1 hits,
   validated against Cloud Run; nothing was deployed and the registry file was left byte-identical
   (`logs/registry-after-second.json`). The execution in `logs/4-executions.log` failed as in 3.
5. **Stub replaced** (`logs/5-stub-redeploy.log`): the stub now reads the body with `getBody()` and
   parses it itself. Its `run.app` URL depends only on the name and project, so the workflow was
   not redeployed.
6. **Executions** (`logs/6-executions.log`): `tx-001` (120, PT) → `approved`, `tx-002`
   (4800, ES) → `declined`, `tx-003` (300, PT) → `approved`; all `SUCCEEDED`.

Two other observations: QuickFaaS exits with code 1 after a successful GCP deployment (OmniFlow
accepts it when the success marker is present), and the committed
`omni-flow-main/QuickFaaS-Deployment-1.0-fat.jar` predates the move to second-generation functions,
so the run used a jar built from the current QuickFaaS source.

## Reproducing it

The scripts were run from a scratch directory with this layout: the scripts and `Case.kt` compiled
to `out/`, a `work/` directory holding `functions/` and `qf/` (the QuickFaaS fat jar, built with
`./gradlew fatJar` under JDK 17, next to a copy of `omni-flow-main/function-deployment`), and
`../cp.txt`, OmniFlow's classpath from
`./mvnw -pl deployment compile dependency:build-classpath -Dmdep.outputFile=cp.txt`. `Case.kt` was
compiled with the Kotlin 2.1.10 compiler against `deployment/target/classes` and that classpath.
They need Application Default Credentials for an account that can deploy to the project, and
create public Cloud Run functions and a workflow.
