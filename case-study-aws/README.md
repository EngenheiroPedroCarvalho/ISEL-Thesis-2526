# Case study run on AWS (2026-09-28)

The `PaymentAuthorization` workflow of the dissertation's case study (Chapter 5), deployed and
executed against a live AWS account (`025064823406`, region `eu-west-1`) on 2026-09-28, as the
counterpart of the GCP run in `case-study-gcp/`. It ran with the renderer from branch
`experiment/aws-pass-state` (commit `ad7f52e`); see "Renderer fixes" below.

## What is here

| Path | Content |
|---|---|
| `CaseAws.kt` | The workflow as run, and a small runner around `AmazonCloudDeployer` + `AwsLambdaDeployer` |
| `functions/` | The two QuickFaaS hooks and their deployment descriptors, and the core-banking stub (Python) |
| `scripts/setup-aws.sh` | Creates the QuickFaaS bucket and the Step Functions execution role |
| `scripts/run-case-study-aws.sh` | Phases 0–4: stub, first deployment, executions, second deployment, executions |
| `scripts/omniflow-deploy-policy.json` | The IAM policy the deploying user needed |
| `logs/` | Output of each phase, the registry after each deployment, and the deployed state machine |

The logs had their ANSI colour codes removed; `rendered.asl.json` was pretty-printed. Nothing else
was changed.

## How it differs from Listing 5.1

- Internal calls use region-qualified references (`"eu-west-1/fraud-check"`), as on GCP.
- The scoring calls pass `amount` and `country` as query parameters: on AWS an internal call takes
  query parameters only (the QuickFaaS Lambda template reads only `queryStringParameters`).
- The execution input is `{"transaction": {...}}`: in Step Functions the input is the state itself.
- The hooks use the signature the QuickFaaS AWS template calls,
  `handleRequest(Map<String, String>, Context)`, and return a `Map` that Lambda serialises to JSON.
- `report-decision` calls `core-banking-stub`, a Python Lambda behind an API Gateway HTTP API,
  created with the AWS CLI outside the ladder. The renderer emits `apigateway:invoke` for external
  calls, so the stub needs API Gateway, and the QuickFaaS Java template drops the request body the
  stub has to read.

## Renderer fixes

A local render of the workflow before the run showed two defects in the AWS renderer, fixed on
`experiment/aws-pass-state`:

- An `assign` rendered as a `Pass` state whose `Result` replaced the whole state, so
  `report-decision` would no longer find `$.transaction`. A single-variable assign now writes only
  that variable (`"Result": "approved", "ResultPath": "$.decision"`); assigns of several variables
  and the loop counters keep the previous form.
- Query parameters of a Lambda call were wrapped in `States.Array`, so the hooks received `"[120]"`.
  The Lambda payload now carries plain strings; API Gateway calls keep the arrays they require.

## What happened

1. **Stub** (`logs/0-stub.log`): Lambda execution role, `core-banking-stub` and its HTTP API; a
   probe request returned `{"recorded": true, "id": "probe", "decision": "probe"}`.
2. **First deployment, no registry** (`logs/1-first-deploy.log`, 15:18): the bootstrap listed the
   account's Lambdas (about 12 s) and registered the six already present, including one in
   `eu-central-1`. Both scoring functions missed at Levels 1 and 2 and were deployed at Level 3
   (about 17 s each); the state machine was created with their ARNs (`logs/rendered.asl.json`,
   `logs/registry-after-first.json`). The whole deployment took about 48 s.
3. **Executions** (`logs/2-executions.log`): `tx-001` (120, PT) → `approved`, `tx-002`
   (4800, ES) → `declined`; both `SUCCEEDED`, and the stub echoed each transaction's `id`.
4. **Second deployment** (`logs/3-second-deploy.log`, 15:27): the state machine was deleted first,
   because the deployer only creates. Both calls were Level 1 hits, validated against Lambda;
   nothing was deployed, the whole deployment took about 1.5 s, and the registry file was left
   byte-identical (`logs/registry-after-second.json`).
5. **Executions** (`logs/4-executions.log`): `tx-003` (300, PT) → `approved`, `SUCCEEDED`.

Other observations: QuickFaaS exits with code 1 after a successful AWS deployment too (OmniFlow
accepts it when the success marker is present), and Step Functions invoked the HTTP API's
`$default` stage with no stage in the path. The first attempts stopped on missing IAM permissions
(`s3:ListAllMyBuckets`, used by QuickFaaS) and on a quoting bug in the run script; the logs here
are from the runs after those were fixed (phase 1 at 15:18, phases 2–4 at 15:27, resumed with
`run-case-study-aws.sh from2`).

## Reproducing it

The scripts were run from a scratch directory holding `aws-case.env` (written by `setup-aws.sh`),
`cp.txt` (OmniFlow's classpath from
`./mvnw -pl deployment compile dependency:build-classpath -Dmdep.outputFile=cp.txt`), `CaseAws.kt`
compiled to `out/` against `deployment/target/classes` and that classpath, and a `work/` directory
with `functions/`, `stub/handler.py` and `qf/` (the QuickFaaS fat jar built with `./gradlew fatJar`
under JDK 17, next to a copy of `omni-flow-main/function-deployment`). They need the AWS CLI and
credentials in `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`, for a user with the permissions in
`scripts/omniflow-deploy-policy.json`.
