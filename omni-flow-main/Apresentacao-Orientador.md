# Towards Cloud-Agnostic Serverless Applications
## Unifying Function Deployment and Workflow Orchestration

**Pedro Carvalho, José Simão, Filipe Freitas**
Instituto Superior de Engenharia de Lisboa (ISEL), Politécnico de Lisboa

---

## Slide 1 — Motivation

The current serverless ecosystem is significantly fragmented: function definition, deployment, and workflow orchestration are managed by separate tools that are often specific to a single cloud provider. This fragmentation complicates reasoning about execution semantics, state management, and portability, increasing vendor lock-in in applications that span multiple cloud platforms.

The problem extends across the entire development lifecycle: teams describe functions in one toolchain, deploy provider-specific artefacts in another, and orchestrate workflows in a third model with different assumptions about retries, data flow, and error handling. Over time, these mismatches increase maintenance effort and make cross-cloud migration costly, because invocation metadata and orchestration logic must be manually synchronised.

A concrete example of this problem: when a developer wants to define a workflow that calls a function that has not yet been deployed, they must first deploy the function to discover its endpoint, and only then return to the workflow to complete it. This manual synchronisation is a recurring source of errors and rework.

---

## Slide 2 — Solution Overview

The proposed solution is a unified framework that combines two existing tools developed within the ISEL research group: **QuickFaaS**, for portable serverless function deployment on GCP, and **OmniFlow**, a Kotlin-based DSL for workflow orchestration. Their integration allows developers to describe the entire pipeline — functions and workflow — in a single definition, with automatic and coordinated deployment to the target cloud platform.

```plantuml
@startuml
skinparam backgroundColor #FAFAFA
skinparam defaultFontSize 13

package "Developer" {
  artifact "Kotlin DSL\n(Workflow + Functions)" as DSL
}

package "OmniFlow" {
  component "DSL Parser" as Parser
  component "Internal Model" as Model
  component "Renderer" as Renderer
  component "AwsLambdaDeployer" as ALD
}

package "QuickFaaS" {
  component "GCP Function Deployer" as Deployer
}

package "Cloud Providers" {
  component "AWS Step Functions" as AWS
  component "GCP Cloud Workflows" as GCP
  component "AWS Lambda" as Lambda
  component "GCP Cloud Functions" as CF
}

DSL --> Parser
Parser --> Model
Model --> Renderer
Model --> Deployer
Model --> ALD
Renderer --> AWS
Renderer --> GCP
Deployer --> CF
ALD --> Lambda

@enduml
```

The architecture follows a layered model: the Kotlin workflow definition is transformed by provider-specific renderers into native platform artefacts (Amazon States Language for AWS, YAML for GCP). Internal functions are deployed before the workflow is created — via QuickFaaS for GCP Cloud Functions, and via OmniFlow's `AwsLambdaDeployer` for AWS Lambda.

---

## Slide 3 — QuickFaaS: Portable Function Deployment

QuickFaaS is a tool that addresses serverless function portability by allowing developers to implement function logic once and deploy it to GCP Cloud Functions with minimal code changes.

Conceptually, QuickFaaS separates cloud-agnostic business logic from provider-specific integration logic. Developers implement a *hook function* using generic request/response abstractions, while provider-specific templates serve as entry points that adapt native event formats and invocation contracts to the hook interface.

The deployment pipeline works as follows: it creates a temporary project, integrates the user's hook function, compiles the code and required libraries, and bundles everything into a deployable ZIP archive. This artefact is then uploaded and deployed using the GCP Cloud Functions API.

```plantuml
@startuml
skinparam backgroundColor #FAFAFA
skinparam defaultFontSize 13

start
:Developer provides\nhook function + descriptor;
:Create temporary project;
:Integrate provider-specific template;
:Compile code + dependencies;
:Package into ZIP / fat JAR;
:Upload to GCP\n(Cloud Functions API);
:Invoke DeployFunction API;
:Wait for ACTIVE state;
:Register metadata in Function Registry;
stop
@enduml
```

---

## Slide 4 — OmniFlow: Portable Workflow Orchestration

OmniFlow is a Kotlin library and DSL for defining workflows independently of provider-specific workflow schemas. Developers specify workflow logic once in a provider-agnostic form, and OmniFlow renders and deploys the corresponding artefact for the target platform.

The framework follows a three-stage pipeline: first, the DSL captures the workflow structure (metadata, inputs, steps, and output); second, the DSL constructs are converted into a provider-agnostic internal model organised as a hierarchical tree; third, a rendering layer traverses that model and generates provider-specific artefacts, which are then submitted through deployment adapters.

```plantuml
@startuml
skinparam backgroundColor #FAFAFA
skinparam defaultFontSize 13

rectangle "Phase 1\nKotlin DSL" as F1 #D6EAF8
rectangle "Phase 2\nInternal Model\n(Agnostic tree)" as F2 #D5F5E3
rectangle "Phase 3\nRendering +\nDeployment" as F3 #FCF3CF

F1 -right-> F2 : parse
F2 -right-> F3 : traverse

note bottom of F1
  workflow { }
  step { }
  call { }
  assign { }
  switch { }
end note

note bottom of F3
  AWS: JSON (ASL)
  GCP: YAML
end note
@enduml
```

The workflow model supports execution and control-flow steps — HTTP calls, variable assignments, conditionals, loops, and parallel branches — allowing developers to combine external service invocations with internal data transformations in a single definition.

---

## Slide 5 — Related Work

This work is positioned at the intersection of workflow portability and function portability, a space that existing solutions cover only partially.

Deployment-oriented Infrastructure-as-Code tools such as AWS CloudFormation, Terraform, Serverless Framework, and Pulumi reduce operational effort, but function configuration and triggers remain strongly provider-shaped. They do not generate native workflow artefacts for managed orchestrators.

Portable execution layer approaches such as OpenFaaS and code transformation tools (Python-to-FaaS, Java-to-Lambda) reduce migration effort but are often platform-constrained or require an additional runtime layer.

For workflow composition, solutions such as FaaSFlow, Triggerflow, Serverless Workflow Specification, Synapse, and Temporal advance orchestration portability, but typically through a dedicated runtime model — they do not delegate execution to the managed orchestrators native to each provider (Step Functions, Cloud Workflows).

The gap addressed by this work is the combination of portable function deployment with native artefact generation for managed orchestrators, without introducing an intermediate execution layer.

---

## Slide 6 — Unified Model: Internal and External Functions

The central contribution of this work is the extension of OmniFlow's call model to distinguish between two types of functions:

An **internal function** is a serverless function owned and maintained by the workflow developer, who controls its source code and is responsible for its deployment and evolution. Internal functions on GCP are deployed via QuickFaaS; on AWS, deployment is handled by OmniFlow's `AwsLambdaDeployer`. In both cases, OmniFlow resolves the invocation metadata automatically during workflow generation.

An **external function** is a function not owned by the workflow developer — for example, a third-party API or a managed cloud service. The developer cannot deploy, update, or otherwise manage its lifecycle. External functions are treated as unmanaged HTTP dependencies, and their invocation parameters must be defined manually.

```plantuml
@startuml
skinparam backgroundColor #FAFAFA
skinparam defaultFontSize 13

rectangle "call { internalFunction(\"passengerFlowAnalysis\") }" as A #D5F5E3
note right of A : Internal function — already in the registry.\nOmniFlow resolves the endpoint automatically.

rectangle "call { internalFunction(\"occupancyPredictor\", \"/path/descriptor.json\") }" as B #D5F5E3
note right of B : Internal function — not yet deployed.\nOmniFlow triggers deployment before generating the workflow.

rectangle "call { host(\"gtfs-broker\"); path(\"/v1/realtime-feed\") }" as C #FADBD8
note right of C : External function — third-party API.\nEndpoint defined manually by the developer.

@enduml
```

This distinction is enforced by a mutual exclusion rule: a call step cannot simultaneously contain `internalFunction` and `host`/`path`, preventing conflicts about the origin of the invoked function. Both modes of `internalFunction` — name only and name with descriptor path — are fully implemented and functional for both GCP and AWS.

---

## Slide 7 — Function Registry

The Function Registry is a local JSON file (`function-registry.json`) that serves as the source of truth for the invocation metadata of all deployed internal functions. It is automatically populated by the framework after each successful deployment, and is consulted during workflow generation to resolve the endpoints of referenced functions.

```plantuml
@startuml
skinparam backgroundColor #FAFAFA
skinparam defaultFontSize 13

object "function-registry.json" as Registry {
  updatedAt = "2026-02-15T00:00:00Z"
  ---
  passengerFlowAnalysis:
    serviceName = "passengerFlowAnalysis"
    url = "arn:aws:lambda:eu-west-1:..."
  ---
  sensorDataNormalize:
    serviceName = "sensorDataNormalize"
    url = "https://europe-west1-project.cloudfunctions.net/..."
}
@enduml
```

The registry decouples workflow authoring from endpoint management: instead of hard-coding invocation URLs in each call step, the workflow references stable logical names, while the registry maps those names to the currently deployed endpoints. This indirection reduces manual edits when functions are redeployed, renamed at the platform layer, or moved across providers.

The registry supports resolution by exact name and by suffix (e.g., `region/functionName`), and exposes operations including `put`, `remove`, `readAll`, `resolveEntry`, and `resolveUrl`. The `updatedAt` field supports freshness checks and synchronisation policies — for example, validating entries only when a configurable staleness threshold is exceeded.

---

## Slide 8 — Internal Function Resolution Flow

The process of resolving an internal function during workflow deployment follows a deterministic validation logic with fail-fast behaviour: deployment only proceeds when metadata is consistent between the local registry and the remote state on the cloud platform.

```plantuml
@startuml
skinparam backgroundColor #FAFAFA
skinparam defaultFontSize 13

actor "OmniFlow" as O
participant "Function Registry\n(local)" as R
participant "FaaS Platform\n(GCP / AWS)" as P
participant "InternalFunctionDeployer" as Q

O -> R : resolveEntry(functionName)

alt Found in registry
  R --> O : local metadata
  O -> P : GetFunction / GetService
  alt Exists remotely
    P --> O : remote metadata
    alt Endpoints match
      O -> O : proceed to workflow deployment
    else Endpoints differ
      O -> R : put(functionName, remoteMeta)
      O -> O : proceed with updated metadata
    end
  else Does not exist remotely
    O -> R : remove(functionName)
    O -> O : stop — redeployment required
  end
else Not in registry
  alt Descriptor available
    O -> Q : deployOrUpdate(name, descriptorPath)
    Q --> O : FunctionInvocationMetadata
    O -> R : put(functionName, meta)
    O -> O : proceed to workflow deployment
  else No descriptor
    O -> O : stop — function not found
  end
end
@enduml
```

This mechanism preserves fail-fast behaviour: deployment proceeds only when invocation metadata is consistent across the local and remote views. As a result, the rendered workflow remains reproducible, and operational errors caused by stale endpoints are detected before runtime.

---

## Slide 9 — Next Steps

The work identifies three main directions for future development, all motivated by limitations observed in the current implementation.

**Selective redeployment.** Currently, any change to a workflow or function triggers a full redeployment, introducing unnecessary overhead for small updates. The planned solution is to use content hashing at the function and workflow levels to detect modified components and redeploy only what has changed.

**Locality-aware placement.** In multi-cloud workflows, cross-provider data movement increases latency and transfer cost. The plan is to add placement hints so developers can express co-location constraints (e.g., keeping functions close to data). The renderer can then optimise deployment decisions while balancing portability and performance.

**Cross-provider benchmarking and ML inference.** Comparative benchmarks of workflow engines using identical workloads (latency, cost, cold-start) are planned, as well as support for adaptive scheduling for serverless ML model inference, where variable model loading and request patterns create optimisation challenges.

---

## Slide 10 — Conclusion

This work presents a unified framework that addresses serverless ecosystem fragmentation at the level of the complete lifecycle: from function definition to deployment, workflow composition, and execution on native managed orchestrators.

The main contributions are: the integration between QuickFaaS and OmniFlow that eliminates manual synchronisation between deployment and workflow definition; the extended call model distinguishing internal and external functions; the Function Registry as a decoupling mechanism between logical names and physical endpoints; and full support for AWS Lambda and Step Functions through OmniFlow's `AwsLambdaDeployer`, including automatic IAM management.

The framework advances cloud portability by decoupling serverless application development from vendor-specific orchestration services, while preserving execution on each platform's native managed orchestrators without introducing an intermediate runtime layer.
