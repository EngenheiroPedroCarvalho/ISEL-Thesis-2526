# AWS Setup Guide — OmniFlow + QuickFaaS

This guide walks you through everything you need to prepare on AWS before running any of the OmniFlow AWS examples (options **6**, **7** and **8** of `Main.kt`). Each step explains **what** to do and **why** it is required, so the manual can be followed end-to-end by a user who has never used OmniFlow before.

---

## 1. Overview — what OmniFlow does on AWS

OmniFlow translates a Kotlin DSL workflow into an **AWS Step Functions** state machine. Each task in the workflow is either:

- an **HTTP call** to an existing endpoint (API Gateway, public REST, etc.), or
- an **internalFunction** — Java code that OmniFlow hands over to **QuickFaaS**, which packages it as a fat JAR, uploads it to **S3**, and creates the corresponding **AWS Lambda** function.

Once all Lambdas exist, OmniFlow deploys the **state machine** and links each step to the right Lambda ARN.

To make any of this work, AWS must allow:

1. **You (the IAM user running OmniFlow)** — to create/update Lambdas, S3 objects, IAM roles and Step Functions state machines.
2. **The Step Functions execution role** — to invoke the Lambdas (or API Gateway endpoints) used by the state machine.
3. **The Lambda execution role** — to write logs to CloudWatch (and any other AWS service the function needs).

The rest of this guide configures exactly these three identities.

---

## 2. Prerequisites on your machine

| Requirement | Why |
|---|---|
| **Java 17 JDK** on `PATH` | OmniFlow and QuickFaaS run on the JVM. Lambda runtime is `java17`, so the generated JAR must also be compiled with Java 17. |
| **Maven 3.6+** on `PATH` | QuickFaaS shells out to `mvn package` to build each Lambda fat JAR. |
| **AWS CLI** (optional) | Useful to verify credentials and inspect resources (`aws sts get-caller-identity`, `aws lambda list-functions`, …). Not required by OmniFlow itself. |
| **QuickFaaS fat JAR built locally** | Required for examples 7 and 8. Build with: `cd quickfaas-essentials/QuickFaaS-Deployment && ./gradlew fatJar`. The artifact is at `build/libs/QuickFaaS-Deployment-fat.jar`. |

---

## 3. Create the IAM user that will run OmniFlow

OmniFlow needs programmatic credentials (Access Key ID / Secret) to call AWS APIs. We create a dedicated IAM user instead of using the root account so its permissions can be scoped precisely.

1. **IAM** → **Users** → **Create user**
2. Name: `omniflow-deploy-user`
3. **Do not** check "Provide user access to the AWS Management Console" (this user is for the SDK only).
4. **Create user**, open it, go to **Security credentials** → **Create access key** → **Command Line Interface (CLI)** → confirm.
5. Copy and save:
   ```
   AWS_ACCESS_KEY_ID=AKIA...
   AWS_SECRET_ACCESS_KEY=wJalr...
   ```
   These are only displayed once.

### 3.1 Attach the deployment policy

OmniFlow + QuickFaaS calls a wide set of services during a single run: S3 (upload package), Lambda (create / update / wait until Active), IAM (create Lambda execution role and attach policies), Step Functions (create state machine). The simplest correct policy is to grant full access to those four services for this user only.

**IAM** → **Users** → `omniflow-deploy-user` → **Add permissions** → **Create inline policy** → **JSON**.

Policy name: `OmniFlowDeployPolicy`

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "LambdaFullAccess",
      "Effect": "Allow",
      "Action": "lambda:*",
      "Resource": "*"
    },
    {
      "Sid": "StepFunctionsFullAccess",
      "Effect": "Allow",
      "Action": "states:*",
      "Resource": "*"
    },
    {
      "Sid": "S3Access",
      "Effect": "Allow",
      "Action": [
        "s3:ListAllMyBuckets",
        "s3:ListBucket",
        "s3:GetBucketLocation",
        "s3:PutObject",
        "s3:GetObject"
      ],
      "Resource": "*"
    },
    {
      "Sid": "IAMRoleManagement",
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole",
        "iam:GetRole",
        "iam:AttachRolePolicy",
        "iam:PutRolePolicy",
        "iam:UpdateAssumeRolePolicy",
        "iam:PassRole"
      ],
      "Resource": "*"
    }
  ]
}
```

**Why each block exists:**

- **`lambda:*`** — QuickFaaS calls `CreateFunction`, `UpdateFunctionCode`, `GetFunction`, and uses the SDK *waiter* which internally calls `GetFunctionConfiguration` until the function reaches the `Active` state.
- **`states:*`** — OmniFlow calls `CreateStateMachine` / `UpdateStateMachine` / `DescribeStateMachine`.
- **`s3:ListAllMyBuckets`** — QuickFaaS validates that the target bucket exists by listing buckets *before* uploading.
- **`s3:PutObject` / `GetObject`** — uploading and (occasionally) re-downloading the Lambda package.
- **`s3:GetBucketLocation`** — OmniFlow auto-detects the bucket's region, since Lambda must live in the same region as its package.
- **`iam:CreateRole` + `AttachRolePolicy`** — when the `iamRoleArn` field of `func-deployment.json` is left blank, OmniFlow auto-creates the role `OmniFlowLambdaExecutionRole` and attaches `AWSLambdaBasicExecutionRole`.
- **`iam:PassRole`** — required to *hand* a role to another AWS service (Lambda, Step Functions). Without it, `CreateFunction` and `CreateStateMachine` are rejected even if the user can otherwise create them.
- **`iam:UpdateAssumeRolePolicy`** — used to ensure the Step Functions role trusts the regional Step Functions principal (`states.<region>.amazonaws.com`).

> The policy uses `Resource: "*"` for simplicity. In a production setting you would restrict resources to specific ARNs.

---

## 4. Create the Step Functions execution role

The state machine itself runs as an AWS service and needs its **own** IAM role to invoke Lambdas (or API Gateway). This role is **different** from the user's policy: the user defines the state machine, but Step Functions executes it.

1. **IAM** → **Roles** → **Create role**
2. **Trusted entity type**: AWS service
3. **Use case**: **Step Functions** → **Next**
4. On the *Permissions* page click **Next** without adding any managed policy (we will add an inline one in the next step).
5. Role name: `StepFunctionsExecutionRole`
6. **Create role**
7. Save the resulting ARN: `arn:aws:iam::<account-id>:role/StepFunctionsExecutionRole`.

### 4.1 Inline policy for the role

Open the role → **Add permissions** → **Create inline policy** → **JSON**.

Policy name: `AllowInvokeWorkflowTargets`

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "InvokeLambdas",
      "Effect": "Allow",
      "Action": "lambda:InvokeFunction",
      "Resource": "*"
    },
    {
      "Sid": "InvokeApiGateway",
      "Effect": "Allow",
      "Action": "execute-api:Invoke",
      "Resource": "arn:aws:execute-api:*:*:*"
    }
  ]
}
```

**Why:**

- **`lambda:InvokeFunction`** — needed by any step that uses the `arn:aws:states:::lambda:invoke` resource (examples 7 and 8). OmniFlow grants this automatically via `AwsLambdaIamHelper.grantStepFunctionsInvoke`, but having it on the role from the start avoids race conditions on the first run.
- **`execute-api:Invoke`** — needed when a step calls an API Gateway endpoint (example 6).

> If your account enforces a **Permissions Boundary** on roles, edit the boundary so it also allows these two actions; otherwise the inline policy will be silently denied.

---

## 5. Lambda execution role (auto-created)

Every Lambda needs a role that it *assumes* at runtime (trust principal `lambda.amazonaws.com`). OmniFlow handles this automatically:

- If `iamRoleArn` in `func-deployment.json` is **blank**, OmniFlow creates `OmniFlowLambdaExecutionRole` and attaches the AWS-managed `AWSLambdaBasicExecutionRole` policy (CloudWatch Logs).
- If you set `iamRoleArn` to a specific ARN, OmniFlow uses it as-is.

For the examples in this repo, leaving the field blank is the simplest option.

---

## 6. The `func-deployment.json` descriptor

QuickFaaS reads one descriptor per Lambda. OmniFlow patches **only** the `iamRoleArn` at runtime; every other field must be filled in advance.

```jsonc
{
  "cloudProvider": "aws",
  "accessToken": "",                         // not used by AWS — leave blank
  "iamRoleArn": "",                          // blank ⇒ auto-create OmniFlowLambdaExecutionRole
  "project": "025064823406",                 // your 12-digit AWS Account ID, no hyphens
  "function": {
    "name": "aws-summary-fn",                // Lambda function name (must be unique per region)
    "location": "eu-west-1",                 // AWS region for the Lambda
    "bucket": "omniflow-quickfaas-packages", // existing S3 bucket for the deployment ZIP
    "runtime": "java17",
    "trigger": { "type": "http" }
  },
  "functionFile": "./MyFunctionClass.java"   // path to the Java handler, relative to the descriptor
}
```

**Field justifications:**

| Field | Why it matters |
|---|---|
| `cloudProvider` | Tells QuickFaaS which deployer to use; OmniFlow asserts `"aws"`. |
| `project` | AWS Account ID. Used to compose ARNs. **Must be 12 digits, no hyphens.** |
| `iamRoleArn` | Lambda's execution role. Blank ⇒ auto-create. |
| `function.name` | Becomes the Lambda function name and is referenced by `lambda://name` in the OmniFlow DSL. |
| `function.location` | AWS region. Must match the region where the S3 bucket lives, otherwise the upload-then-create chain fails. OmniFlow detects bucket region with `GetBucketLocation` and overrides this if needed. |
| `function.bucket` | S3 bucket where the fat JAR is uploaded before `CreateFunction` references it. The bucket must already exist. |
| `function.runtime` | Pinned to `java17` because the generated handler uses Lambda Java 17 APIs. |
| `accessToken` | GCP-only; leave blank. |

### 6.1 Create the S3 bucket

QuickFaaS does **not** create the bucket. Create it once:

1. **S3** → **Create bucket**
2. Region: same as `function.location` (e.g. `eu-west-1`).
3. Name: globally unique, e.g. `omniflow-quickfaas-packages-<account-id>`.
4. Keep "Block all public access" enabled.
5. Update every `func-deployment.json` with the chosen bucket name.

---

## 7. Environment variables

The JVM must be started **with these variables already exported**; the AWS SDK reads them at boot.

```bash
# Credentials of the IAM user from §3
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=wJalr...

# Default region (overrideable through the interactive prompt)
export AWS_REGION=eu-west-1

# Path to the fat JAR produced by ./gradlew fatJar (only needed for examples 7 and 8)
export QUICKFAAS_JAR_PATH=/abs/path/to/QuickFaaS-Deployment-fat.jar

# Step Functions execution role from §4 (only needed for examples 7 and 8;
# example 6 prompts for it interactively)
export STEP_FUNCTIONS_ROLE_ARN=arn:aws:iam::<account-id>:role/StepFunctionsExecutionRole

# Optional — overrides the default state machine name
export STATE_MACHINE_NAME=AwsTextAnalysisPipeline
```

**Why env vars instead of prompts?** The AWS SDK reads credentials only **once** during static initialization via `EnvironmentVariableCredentialsProvider`. Setting them after the JVM starts has no effect.

---

## 8. Run an example

1. From the repo root: `cd omni-flow-main/deployment && mvn compile exec:java -Dexec.mainClass=costaber.com.github.omniflow.MainKt`
   *(or run `Main.kt` from your IDE.)*
2. Pick the desired option from the menu.
3. Fill in any prompts the program shows (some default to the env vars above).
4. Watch the staged output — OmniFlow logs each phase: descriptor validation, S3 upload, Lambda creation, readiness check, IAM permission grant, state machine deployment.

When the program finishes you can verify in the AWS Console:

- **Lambda** → the new function(s) exist in the chosen region and are **Active**.
- **Step Functions** → the new state machine exists. Click **Start execution** with input `{}` and inspect the output.

---

## 9. Troubleshooting

| Error | Root cause | Fix |
|---|---|---|
| `is not authorized to perform: lambda:GetFunctionConfiguration` | User policy missing Lambda waiter permission | Use `lambda:*` in the user policy (§3.1). |
| `is not authorized to perform: s3:ListAllMyBuckets` | QuickFaaS validates the bucket before upload | Add `s3:ListAllMyBuckets` (already in §3.1). |
| `iam:PassRole` denied when creating Step Functions / Lambda | User can create the resource but not assign a role to it | Add `iam:PassRole` (already in §3.1). |
| `cannot be assumed by Lambda` during `CreateFunction` | Role just created; IAM not yet globally consistent | OmniFlow retries automatically up to 3× with 20 s back-off. If it still fails, run the example again. |
| `execute-api:Invoke not authorized` | Step Functions role missing API Gateway permission | Add the inline policy from §4.1. |
| `<REPLACE_WITH_S3_BUCKET>` or `<REPLACE_WITH_AWS_ACCOUNT_ID>` placeholder error | Descriptor still contains template values | Fill `bucket` and `project` in every `func-deployment.json`. |
| `bucket … is in 'us-east-1'` warning | Bucket region differs from `function.location` | OmniFlow follows the bucket; either move the bucket or change `function.location`. |
| `Lambda reached Failed state` | Handler code threw on `CreateFunction` | Inspect CloudWatch Logs for the function. |
| `StateMachineAlreadyExists` | A previous run created the same state machine | OmniFlow updates it on subsequent runs; if you want a clean slate, delete it from the console or via CLI: `aws stepfunctions delete-state-machine --state-machine-arn …`. |

---

## 10. Cleanup (optional)

```bash
# State machine
aws stepfunctions delete-state-machine \
  --state-machine-arn arn:aws:states:eu-west-1:<account-id>:stateMachine:<name> \
  --region eu-west-1

# Lambdas
aws lambda delete-function --function-name aws-preprocess-fn --region eu-west-1
aws lambda delete-function --function-name aws-char-stats-fn --region eu-west-1
aws lambda delete-function --function-name aws-summary-fn    --region eu-west-1

# S3 deployment objects (bucket itself can be kept)
aws s3 rm s3://<bucket>/aws-preprocess-fn/ --recursive
```

Roles and the S3 bucket can be reused across runs — there is no need to delete them between executions.
