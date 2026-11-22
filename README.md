# Unified Orchestration of Multi-Cloud Serverless Workflows using QuickFaaS and OmniFlow

This repository holds the code and case-study files of the MSc dissertation *Unified Orchestration
of Multi-Cloud Serverless Workflows using QuickFaaS and OmniFlow* (ISEL, 2026).

[OmniFlow](omni-flow-main/) is a Kotlin DSL that defines a cloud workflow once and deploys it to
**AWS Step Functions** or **GCP Cloud Workflows**. [QuickFaaS](omni-flow-main/quickfaas-essentials/QuickFaaS-Deployment/)
deploys serverless functions. This work integrates the two:

- **Unification.** A workflow step can call an *internal function* by name. At deployment time
  OmniFlow resolves each one through a three-level ladder (1: the local function registry,
  validated against the live function; 2: discovery in the provider; 3: deployment with
  QuickFaaS, only when a deployment descriptor is given and the function is confirmed absent)
  and embeds the resolved endpoint in the rendered workflow.
- **AWS support.** QuickFaaS, previously limited to GCP and Azure, can now deploy to
  **AWS Lambda**.

## Repository layout

| Path | Content |
|---|---|
| `omni-flow-main/deployment/` | OmniFlow: DSL, renderers, deployers, function registry, resolution ladder (Maven) |
| `omni-flow-main/quickfaas-essentials/QuickFaaS-Deployment/` | QuickFaaS, with the AWS Lambda provider (Gradle) |
| `omni-flow-main/benchmark/` | JMH benchmarks and their results |
| `case-study-gcp/`, `case-study-aws/` | The dissertation's case study (`PaymentAuthorization`) as run on GCP and AWS, with logs |
| `quickfaas-essentials/` | Reference copy of the original QuickFaaS (not modified) |

## Requirements

- JDK 17 or newer for OmniFlow (Maven wrapper included).
- JDK 17 for QuickFaaS: its Kotlin 1.6.20 compiler does not run on newer JDKs.
- Maven 3.6+ on `PATH` (QuickFaaS uses it to package Java functions).
- Credentials for the target cloud (see [Authentication](#authentication)).

## Build and test

```bash
# OmniFlow
cd omni-flow-main
./mvnw -pl deployment test                 # unit tests
./mvnw clean package                       # build all modules

# QuickFaaS (run with a JDK 17 JAVA_HOME)
cd omni-flow-main/quickfaas-essentials/QuickFaaS-Deployment
./gradlew test                             # unit tests
./gradlew fatJar                           # build/libs/QuickFaaS-Deployment-1.0-fat.jar
```

[`omni-flow-main/TESTING.md`](omni-flow-main/TESTING.md) describes the tests and coverage reports.

## Authentication

OmniFlow and the QuickFaaS subprocess it starts use the same credentials: the identity that runs
the deployment. Two other identities run the workflow: a service account on GCP and an IAM role on
AWS.

### GCP

1. **Log in with Application Default Credentials (ADC):**

   ```bash
   gcloud auth login
   gcloud auth application-default login
   gcloud config set project <project-id>
   ```

   Or use a service account key:
   `export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json`.

   OmniFlow reads ADC to deploy the workflow and to look up Cloud Run services. Leave
   `"accessToken": ""` in each `func-deployment.json`: before each Level 3 deployment, OmniFlow
   gets a fresh token from ADC and passes it to QuickFaaS in a temporary copy of the descriptor.
   The token is never written to the original file. Running QuickFaaS by hand works differently:
   it does not read ADC, so put the output of `gcloud auth print-access-token` in `accessToken`.
   That token expires after one hour.

2. **Enable the APIs:**

   ```bash
   gcloud services enable cloudfunctions.googleapis.com run.googleapis.com \
     cloudbuild.googleapis.com artifactregistry.googleapis.com \
     workflows.googleapis.com storage.googleapis.com
   ```

3. **Grant permissions to the deploying identity.** It must be able to create Cloud Run
   functions and change their IAM policy (Cloud Functions Developer, Cloud Run Admin), write to
   the staging bucket in the descriptor (Storage Object Admin on that bucket), create workflows
   (Workflows Editor), and act as the workflow's service account (Service Account User on it).
   A project Owner has all of these.

4. **Create the service account the workflow runs as**, and pass it in `GoogleDeployContext`
   as `projects/<project>/serviceAccounts/<sa-email>`:

   ```bash
   gcloud iam service-accounts create workflow-sa
   ```

   QuickFaaS makes the functions it deploys public (it grants `roles/run.invoker` to
   `allUsers`), so the workflow can call them with no `auth` block. When `invokerServiceAccount`
   is set in `QuickFaasDeployer`, the deployer also grants `roles/run.invoker` on each new
   function to that account. This means the workflow can still call the function with
   `authentication(type("OIDC"))` if public access is later removed.

### AWS

1. **Export the credentials as environment variables.** OmniFlow and QuickFaaS read the AWS
   credentials only from the environment (`EnvironmentVariableCredentialsProvider`).
   `~/.aws/credentials` profiles and SSO sessions are not read, so export temporary credentials
   too (`aws configure export-credentials --format env` prints them):

   ```bash
   export AWS_ACCESS_KEY_ID=AKIA...
   export AWS_SECRET_ACCESS_KEY=...
   export AWS_SESSION_TOKEN=...      # only for temporary credentials
   export AWS_REGION=eu-west-1
   aws sts get-caller-identity       # check which identity is in use
   ```

   Leave `"accessToken": ""` in each descriptor: AWS does not use it.

2. **Grant permissions to the deploying user.** The user needs to:
   - list, create and update Lambda functions and add permissions to them;
   - list the account's regions (`ec2:DescribeRegions`), for discovery;
   - read and write the S3 bucket in the descriptor, and run `s3:ListAllMyBuckets`;
   - create and get IAM roles and pass them to other services (`iam:PassRole`);
   - create state machines.

   [`case-study-aws/scripts/omniflow-deploy-policy.json`](case-study-aws/scripts/omniflow-deploy-policy.json)
   is the policy used in the case study. Replace the account ID, bucket and region in it.

3. **Create the Step Functions execution role.** It must trust `states.amazonaws.com` and allow
   `lambda:InvokeFunction` (and `execute-api:Invoke`, for external calls through API Gateway).
   Pass its ARN as `roleArn` both to `AmazonDeployContext` and to `AwsLambdaDeployer`. After each
   Level 3 deployment, the deployer adds a resource policy to the new Lambda that allows Step
   Functions to invoke it.
   [`case-study-aws/scripts/setup-aws.sh`](case-study-aws/scripts/setup-aws.sh) creates this
   role and the S3 bucket.

4. **Set up the Lambda execution role.** Put its ARN in the descriptor's `iamRoleArn`. If you
   leave that field empty, the deployer creates `OmniFlowLambdaExecutionRole` with
   `AWSLambdaBasicExecutionRole` attached.

[`omni-flow-main/AWS-Setup-Guide.md`](omni-flow-main/AWS-Setup-Guide.md) has a step-by-step
guide to these steps in the AWS console.

## Usage

### 1. Describe each internal function for QuickFaaS

A deployment descriptor (`func-deployment.json`) next to the function's source:

```json
{
  "cloudProvider": "gcp",
  "accessToken": "",
  "project": "<gcp-project-id>",
  "function": {
    "name": "hello",
    "location": "europe-west1",
    "bucket": "<staging-bucket>",
    "runtime": "java17",
    "trigger": { "type": "http" }
  },
  "functionFile": "./MyFunctionClass.java"
}
```

On AWS, set `"cloudProvider": "aws"`, the account ID as `project`, an S3 bucket, and add
`"iamRoleArn"` with the Lambda execution role (or leave it empty; see
[Authentication](#authentication)).

### 2. Define the workflow

Internal calls use `internalFunction(name, descriptorPath)` instead of `host`/`path`. The name
can be qualified with a region (`"europe-west1/hello"`) to restrict discovery to that
region. Without a descriptor, the function must already exist (Levels 1 and 2 only).

```kotlin
import costaber.com.github.omniflow.dsl.*
import costaber.com.github.omniflow.model.HttpMethod.*

val wf = workflow {
  name("hello-workflow")
  description("Calls one internal function")
  steps(
    step {
      name("say-hello")
      description("Call the hello function")
      context(call {
        method(GET)
        internalFunction("hello", "./functions/hello/func-deployment.json")
        result("greeting")
      })
    }
  )
  result("greeting")
}
```

When this workflow is deployed, `internalFunction("hello", ...)` becomes the endpoint of the
`hello` function: a `run.app` URL on GCP, a Lambda ARN on AWS. If `hello` does not exist yet,
QuickFaaS first deploys it, using the descriptor in `functions/hello/`.
On AWS, internal calls take query parameters only (`query(...)`, no `body(...)`).

A full example, with branching and an external call, is the case study in
[`case-study-gcp/Case.kt`](case-study-gcp/Case.kt).

### 3. Deploy

GCP (Cloud Workflows + Cloud Run functions):

```kotlin
GoogleCloudDeployer.Builder()
  .internalFunctionDeployer(QuickFaasDeployer(
    quickFaasJarPath = Path.of("QuickFaaS-Deployment-1.0-fat.jar"),
    projectId = "<project>", region = "europe-west1",
    invokerServiceAccount = "<sa>@<project>.iam.gserviceaccount.com"))
  .build()
  .deploy(wf, GoogleDeployContext(
    projectId = "<project>", zone = "europe-west1",
    serviceAccount = "projects/<project>/serviceAccounts/<sa>@<project>.iam.gserviceaccount.com",
    workflowId = "hello-workflow",
    workflowDescription = "Hello workflow", workflowLabels = mapOf("app" to "omni-flow")))
```

AWS (Step Functions + Lambda):

```kotlin
AmazonCloudDeployer.Builder()
  .internalFunctionDeployer(AwsLambdaDeployer(
    quickFaasJarPath = Path.of("QuickFaaS-Deployment-1.0-fat.jar"),
    region = "eu-west-1", roleArn = "<step-functions-role-arn>"))
  .build()
  .deploy(wf, AmazonDeployContext(
    stateMachineName = "hello-workflow", roleArn = "<step-functions-role-arn>",
    region = "eu-west-1", tags = mapOf("app" to "omni-flow")))
```

The deployer keeps a per-provider registry in the working directory
(`function-registry.gcp.json` / `function-registry.aws.json`; change it with
`.registryPath(...)`). If the file is missing, it is first populated from the functions that
already exist in the account. Existing functions are never updated, and an error during
discovery aborts the deployment instead of deploying a possibly duplicate function.

Complete, runnable examples are in [`case-study-gcp/`](case-study-gcp/README.md) and
[`case-study-aws/`](case-study-aws/README.md). The full DSL (assign, switch, call steps) is
documented in [`omni-flow-main/README.md`](omni-flow-main/README.md).

## Authors

- **Pedro Carvalho** (Pedro Miguel Pratas de Carvalho) — author.
  ISEL – Instituto Superior de Engenharia de Lisboa, Universidade Politécnica de Lisboa, Portugal.
- **José Simão** — supervisor.
  ISEL – Instituto Superior de Engenharia de Lisboa, Universidade Politécnica de Lisboa, Portugal.
- **Filipe Freitas** — supervisor.
  ISEL – Instituto Superior de Engenharia de Lisboa, Universidade Politécnica de Lisboa, Portugal.

Developed in the Master's programme in Computer Science and Computer Engineering (MEIC),
Department of Electronic Engineering, Telecommunications and Computers, ISEL.

This work builds on two earlier ISEL projects: OmniFlow, by Bernardo Costa (MSc dissertation,
2023), and QuickFaaS, by Pedro Rodrigues, Filipe Freitas and José Simão (2022).

## How to cite

If you use this work, please cite the dissertation:

> P. Carvalho, *Unified Orchestration of Multi-Cloud Serverless Workflows using QuickFaaS and
> OmniFlow*, MSc dissertation, Instituto Superior de Engenharia de Lisboa, Universidade
> Politécnica de Lisboa, Lisbon, Portugal, 2026.

```bibtex
@mastersthesis{carvalho2026unified,
  author  = {Carvalho, Pedro},
  title   = {Unified Orchestration of Multi-Cloud Serverless Workflows using {QuickFaaS} and {OmniFlow}},
  school  = {Instituto Superior de Engenharia de Lisboa, Universidade Polit{\'e}cnica de Lisboa},
  address = {Lisboa, Portugal},
  year    = {2026},
  type    = {Master's dissertation}
}
```

To refer to the code itself:

```bibtex
@misc{carvalho2026repository,
  author = {Carvalho, Pedro},
  title  = {{ISEL-Thesis-2526}: Integration of {QuickFaaS} and {OmniFlow}},
  year   = {2026},
  url    = {https://github.com/EngenheiroPedroCarvalho/ISEL-Thesis-2526},
  note   = {GitHub repository}
}
```

Please also cite the works it builds on:

```bibtex
@article{rodrigues2022quickfaas,
  author  = {Rodrigues, Pedro and Freitas, Filipe and Sim{\~a}o, Jos{\'e}},
  title   = {{QuickFaaS}: Providing Portability and Interoperability between {FaaS} Platforms},
  journal = {Future Internet},
  volume  = {14},
  number  = {12},
  pages   = {360},
  year    = {2022},
  doi     = {10.3390/fi14120360}
}

@mastersthesis{costa2023omniflow,
  author  = {Costa, Bernardo Jos{\'e} Mateus},
  title   = {Function Composition in Function-as-a-Service Platforms},
  school  = {Instituto Superior de Engenharia de Lisboa, Instituto Polit{\'e}cnico de Lisboa},
  address = {Lisboa, Portugal},
  year    = {2023},
  type    = {Master's dissertation}
}
```
