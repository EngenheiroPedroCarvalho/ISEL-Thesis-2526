# Diagramas de Implementação — AWS Lambda no QuickFaaS + OmniFlow

---

## 1. Arquitectura Geral (antes vs depois)

```mermaid
graph TB
    subgraph ANTES["Estado Actual"]
        direction TB
        U1[Utilizador] --> OF1[OmniFlow DSL]
        OF1 --> GCD[GoogleCloudDeployer]
        OF1 --> ACD1[AmazonCloudDeployer]
        GCD --> WIFR[WorkflowInternalFunctionResolver]
        WIFR --> QFD[QuickFaasDeployer]
        QFD -->|subprocess| QFJAR[QuickFaaS JAR]
        QFJAR -->|GCP only| GCPFN[GCP Cloud Functions]
        GCD -->|YAML| GCW[Google Cloud Workflows]
        ACD1 -->|JSON| AWSSF[AWS Step Functions]
        ACD1 -.->|NoopDeployer ✗| X[❌ Sem deploy Lambda]
    end

    subgraph DEPOIS["Estado Final"]
        direction TB
        U2[Utilizador] --> OF2[OmniFlow DSL]
        OF2 --> GCD2[GoogleCloudDeployer]
        OF2 --> ACD2[AmazonCloudDeployer ★]
        GCD2 --> WIFR2[WorkflowInternalFunctionResolver]
        WIFR2 --> QFD2[QuickFaasDeployer]
        QFD2 -->|subprocess| QFJAR2[QuickFaaS JAR ★]
        QFJAR2 -->|GCP| GCPFN2[GCP Cloud Functions]
        QFJAR2 -->|AWS ★| AWSL[AWS Lambda + Function URL]
        GCD2 -->|YAML| GCW2[Google Cloud Workflows]
        ACD2 --> AWSIFR[AwsInternalFunctionResolver ★]
        AWSIFR --> ALD[AwsLambdaDeployer ★]
        ALD -->|subprocess| QFJAR2
        ACD2 -->|JSON| AWSSF2[AWS Step Functions]
        AWSSF2 -->|HTTPS IAM| AWSL
    end

    style X fill:#ff6b6b,color:#fff
    style AWSL fill:#ff9900,color:#fff
    style ALD fill:#ffd700,color:#000
    style AWSIFR fill:#ffd700,color:#000
    style ACD2 fill:#ffd700,color:#000
    style QFJAR2 fill:#ffd700,color:#000
```

---

## 2. Hierarquia de Classes — QuickFaaS JAR (novos artefactos)

```mermaid
classDiagram
    class CloudProvider {
        <<interface>>
        +companion: CloudCompanion
        +projects: List~ProjectData~
        +project: CloudProject
        +cloudSpecifics: CloudSpecifics?
        +requestProjects() List~ProjectData~
        +setProjectData(name: String)
    }

    class CloudCompanion {
        <<interface>>
        +name: String
        +shortName: String
        +cloudRequests: CloudRequests
        +newCloudProvider() CloudProvider
    }

    class CloudRequests {
        <<interface>>
        +setBearerToken(token: String)
    }

    class GcpProvider {
        +companion: GcpCompanion
        +project: GcpProject
        +cloudSpecifics: null
    }

    class MsAzureProvider {
        +companion: AzureCompanion
        +project: MsAzureProject
        +cloudSpecifics: MsAzureSpecifics
    }

    class AwsProvider {
        <<novo>>
        +companion: AwsCompanion
        +project: AwsProject
        +cloudSpecifics: AwsSpecifics
        +requestProjects() List~ProjectData~
    }

    class AwsSpecifics {
        <<novo>>
        +awsAccessKeyId: String
        +awsSecretAccessKey: String
        +awsRegion: String
        +setSpecifics(data: DeploymentData)
    }

    class AwsRequests {
        <<novo>>
        -lambdaClient: LambdaClient
        -s3Client: S3Client
        +setCredentials(keyId, secret, region)
        +listLambdaFunctions(region) List
        +uploadZipToS3(bucket, key, bytes)
        +createLambdaFunction(config) LambdaArn
        +updateLambdaFunctionCode(name, s3Bucket, s3Key)
        +createFunctionUrl(name, region) String
        +addResourcePolicy(name, region, principal)
    }

    class AwsProject {
        <<novo>>
        +projectData: AwsProjectData
        +function: AwsLambdaFunction
        +buckets: List~BucketData~
        +requestBuckets()
        +setBucketData(name: String)
    }

    class AwsLambdaFunction {
        <<novo>>
        +name: String
        +location: String
        +iamRoleArn: String
        +runtimes: Array~RuntimeVersion~
        +triggers: List~Trigger~
        +deployZip(zipFilePath, projData)
        +getTriggerUrl(projData) Pair~String,String~
        -waitForActive(name, region)
        -createFunctionUrl(name, region) String
    }

    class AwsS3Bucket {
        <<novo>>
        +bucketData: BucketData
        +uploadToBucket(zipFilePath, function) String
        -createBucketIfAbsent(name, region)
    }

    CloudProvider <|.. GcpProvider
    CloudProvider <|.. MsAzureProvider
    CloudProvider <|.. AwsProvider
    CloudRequests <|.. AwsRequests
    AwsProvider --> AwsSpecifics
    AwsProvider --> AwsProject
    AwsProvider --> AwsRequests
    AwsProject --> AwsLambdaFunction
    AwsLambdaFunction --> AwsS3Bucket
```

---

## 3. Hierarquia de Classes — OmniFlow (novos artefactos)

```mermaid
classDiagram
    class InternalFunctionDeployer {
        <<interface>>
        +deployOrUpdate(name, descriptorPath) FunctionInvocationMetadata
    }

    class NoopInternalFunctionDeployer {
        +deployOrUpdate() ← lança UnsupportedOperationException
    }

    class QuickFaasDeployer {
        -quickFaasJarPath: Path
        -projectId: String
        -region: String
        -invokerServiceAccount: String?
        +deployOrUpdate(name, path) FunctionInvocationMetadata
        -waitForFunctionReady(project, region, name)
        -grantInvokerPermission(...)
    }

    class AwsLambdaDeployer {
        <<novo>>
        -quickFaasJarPath: Path
        -region: String
        -iamRoleArn: String
        -readinessTimeoutSeconds: Long
        +deployOrUpdate(name, path) FunctionInvocationMetadata
        -waitForLambdaActive(name, region)
        -addStepFunctionsPermission(name, region)
        -buildLambdaFunctionUrl(name, region) String
    }

    class QuickFaasDescriptorLoader {
        +load(path) QuickFaasDescriptor
        +validate(descriptor, expectedProvider)
        -GCP_VALID_RUNTIMES: Set
        -AWS_VALID_RUNTIMES: Set ★novo
    }

    class QuickFaasProcessInvoker {
        -quickFaasJarPath: Path
        +invoke(descriptorPath, accessToken?)
    }

    class WorkflowInternalFunctionResolver {
        -projectId: String
        -preferredRegion: String?
        -registry: FunctionRegistryStore
        -inspector: CloudRunV2ServiceInspector
        -internalFunctionDeployer: InternalFunctionDeployer
        +resolve(workflow) Workflow
        -resolveOrDiscoverInternal(ref, internal) String
    }

    class AwsInternalFunctionResolver {
        <<novo>>
        -region: String
        -registry: FunctionRegistryStore
        -lambdaClient: LambdaClient
        -internalFunctionDeployer: InternalFunctionDeployer
        +resolve(workflow) Workflow
        -resolveOrDeploy(ref, internal) String
        -checkLambdaExists(name, region) String?
        -buildFunctionUrl(name, region) String
    }

    class AmazonCloudDeployer {
        -internalFunctionDeployer: InternalFunctionDeployer
        -registryPath: Path
        +deploy(workflow, context)
        class Builder {
            +internalFunctionDeployer(InternalFunctionDeployer)
            +build()
        }
    }

    class AwsLambdaIamHelper {
        <<novo>>
        +addStepFunctionsInvokePermission(functionName, region, roleArn)
        -buildPolicyDocument(roleArn) String
    }

    InternalFunctionDeployer <|.. NoopInternalFunctionDeployer
    InternalFunctionDeployer <|.. QuickFaasDeployer
    InternalFunctionDeployer <|.. AwsLambdaDeployer
    QuickFaasDeployer --> QuickFaasProcessInvoker
    QuickFaasDeployer --> QuickFaasDescriptorLoader
    AwsLambdaDeployer --> QuickFaasProcessInvoker
    AwsLambdaDeployer --> QuickFaasDescriptorLoader
    AwsLambdaDeployer --> AwsLambdaIamHelper
    AmazonCloudDeployer --> AwsInternalFunctionResolver
    AmazonCloudDeployer --> InternalFunctionDeployer
    AwsInternalFunctionResolver --> InternalFunctionDeployer
```

---

## 4. Sequência de Deploy — AWS Workflow com funções internas

```mermaid
sequenceDiagram
    actor Dev as Desenvolvedor
    participant Main as Main.kt
    participant ACD as AmazonCloudDeployer
    participant AWSIFR as AwsInternalFunctionResolver
    participant Reg as FunctionRegistryStore
    participant ALD as AwsLambdaDeployer
    participant DCL as QuickFaasDescriptorLoader
    participant PI as QuickFaasProcessInvoker
    participant QFJAR as QuickFaaS JAR (AwsProvider)
    participant S3 as AWS S3
    participant Lambda as AWS Lambda
    participant IAM as AwsLambdaIamHelper
    participant SF as AmazonStateMachineService

    Dev->>Main: deployAwsExample(scan)
    Main->>ACD: deploy(workflow, AmazonDeployContext)

    Note over ACD: ★ Novo: resolver internas antes de renderizar
    ACD->>AWSIFR: resolve(workflow)

    loop Para cada internalFunction no workflow
        AWSIFR->>Reg: tryResolveEntry("my-fn")
        Reg-->>AWSIFR: null (não encontrada)

        AWSIFR->>Lambda: GetFunction("my-fn", region)
        Lambda-->>AWSIFR: 404 Not Found

        Note over AWSIFR: Descriptor disponível → invocar deployer
        AWSIFR->>ALD: deployOrUpdate("my-fn", "./functions/my-fn/func-deployment.json")

        ALD->>DCL: load(path)
        DCL-->>ALD: QuickFaasDescriptor
        ALD->>DCL: validate(descriptor, "aws")
        DCL-->>ALD: OK

        Note over ALD: Não há ADC token no AWS — credenciais via env vars
        ALD->>PI: invoke(descriptorPath, accessToken=null)

        PI->>QFJAR: java -jar QuickFaaS.jar

        Note over QFJAR: AwsProvider seleccionado (cloudProvider="aws")
        QFJAR->>S3: PutObject(bucket, "my-fn.zip")
        S3-->>QFJAR: 200 OK

        QFJAR->>Lambda: CreateFunction(name, runtime, handler, role, s3Key)
        Lambda-->>QFJAR: FunctionArn

        QFJAR->>Lambda: CreateFunctionUrlConfig(AuthType=AWS_IAM)
        Lambda-->>QFJAR: FunctionUrl

        QFJAR-->>PI: "Deployment finished successfully!"
        PI-->>ALD: retorna (processo terminou)

        ALD->>Lambda: GetFunction("my-fn") — polling até State=Active
        Lambda-->>ALD: State=Active

        ALD->>IAM: addStepFunctionsInvokePermission("my-fn", region, roleArn)
        IAM->>Lambda: AddPermission(Principal=states.amazonaws.com)
        Lambda-->>IAM: 200 OK

        ALD-->>AWSIFR: FunctionInvocationMetadata(url="https://abc.lambda-url.eu-west-1.on.aws")

        AWSIFR->>Reg: put("my-fn", metadata)
        AWSIFR-->>ACD: workflow resolvido (host+path preenchidos)
    end

    Note over ACD: Renderizar DSL → Step Functions JSON
    ACD->>ACD: nodeTraversor.traverse(workflow)

    ACD->>SF: createStateMachine(name, roleArn, definition)
    SF-->>ACD: StateMachineArn

    ACD-->>Main: deploy concluído
    Main-->>Dev: ✓ Step Function + Lambda deployed
```

---

## 5. Sequência de Invocação em Runtime (Step Functions → Lambda)

```mermaid
sequenceDiagram
    actor User as Utilizador
    participant SF as AWS Step Functions
    participant IAM as IAM (SigV4)
    participant LFU as Lambda Function URL
    participant Lambda as AWS Lambda Runtime
    participant Handler as MyHandler.java

    User->>SF: StartExecution(input)

    Note over SF: Task state com http:invoke
    SF->>IAM: assinar pedido com roleArn (SigV4)
    IAM-->>SF: Authorization header

    SF->>LFU: GET https://abc.lambda-url.eu-west-1.on.aws/?param=value\nAuthorization: AWS4-HMAC-SHA256...

    Note over LFU: AuthType=AWS_IAM → valida assinatura
    LFU->>Lambda: invocação interna

    Lambda->>Handler: handleRequest(input, context)
    Handler-->>Lambda: response JSON

    Lambda-->>LFU: 200 { "result": "..." }
    LFU-->>SF: HTTP 200 body

    SF->>SF: próximo estado (resultado disponível em $.body)
    SF-->>User: ExecutionSucceeded
```

---

## 6. Estrutura do Descriptor — Comparação GCP vs AWS

```mermaid
graph LR
    subgraph GCP["func-deployment.json (GCP — actual)"]
        G1["cloudProvider: 'gcp'"]
        G2["accessToken: '&lt;OAUTH_TOKEN&gt;'"]
        G3["project: 'workflow-test-omniflow'"]
        G4["function.name: 'my-fn'"]
        G5["function.location: 'europe-west1'"]
        G6["function.bucket: 'my-gcs-bucket'"]
        G7["function.runtime: 'java17'"]
        G8["function.trigger.type: 'http'"]
        G9["functionFile: './MyFunctionClass.java'"]
    end

    subgraph AWS["func-deployment.json (AWS — novo)"]
        A1["cloudProvider: 'aws'"]
        A2["project: '123456789012'  ← account ID"]
        A3["function.name: 'my-lambda-fn'"]
        A4["function.location: 'eu-west-1'"]
        A5["function.bucket: 'my-s3-bucket'"]
        A6["function.runtime: 'java17'"]
        A7["function.trigger.type: 'http'"]
        A8["functionFile: './MyHandler.java'"]
        A9["iamRoleArn: 'arn:aws:iam::...::role/lambda-role'  ★ novo"]
    end

    subgraph DIFF["Diferenças chave"]
        D1["❌ accessToken removido\n(AWS usa env vars)"]
        D2["★ iamRoleArn obrigatório\n(Lambda precisa de role de execução)"]
        D3["★ project = AWS Account ID\n(não um nome de projecto GCP)"]
    end
```

---

## 7. Fluxo de Validação do Descriptor por Provider

```mermaid
flowchart TD
    START([validate descriptor]) --> CP{cloudProvider?}

    CP -->|gcp| GCP_CHECK[Verificar GCP_VALID_RUNTIMES]
    GCP_CHECK --> GCP_NAME[Validar nome: a-z0-9- max 63]
    GCP_NAME --> GCP_BUCKET[Avisar se bucket em falta]
    GCP_BUCKET --> GCP_TOKEN[Verificar token não é placeholder]
    GCP_TOKEN --> OK1([✓ válido])

    CP -->|aws| AWS_ROLE{iamRoleArn presente?}
    AWS_ROLE -->|não| ERR1([✗ erro: iamRoleArn obrigatório])
    AWS_ROLE -->|sim| AWS_CHECK[Verificar AWS_VALID_RUNTIMES]
    AWS_CHECK --> AWS_NAME[Validar nome: a-zA-Z0-9-_ max 64]
    AWS_NAME --> AWS_BUCKET[Verificar bucket S3 em falta]
    AWS_BUCKET --> AWS_CREDS[Avisar se AWS_ACCESS_KEY_ID não definida]
    AWS_CREDS --> OK2([✓ válido])

    CP -->|azure| AZ_FLOW[... fluxo existente ...]
    AZ_FLOW --> OK3([✓ válido])

    CP -->|outro| ERR2([✗ erro: provider desconhecido])

    style ERR1 fill:#ff6b6b,color:#fff
    style ERR2 fill:#ff6b6b,color:#fff
    style OK1 fill:#51cf66,color:#fff
    style OK2 fill:#51cf66,color:#fff
    style OK3 fill:#51cf66,color:#fff
```

---

## 8. Mapa de Ficheiros — O que criar e o que modificar

```mermaid
graph TD
    subgraph QF["QuickFaaS JAR (quickfaas-essentials/QuickFaaS-Deployment)"]
        QF1["★ model/AwsProvider.kt"]
        QF2["★ model/specifics/AwsSpecifics.kt"]
        QF3["★ model/requests/AwsRequests.kt"]
        QF4["★ model/projects/AwsProject.kt"]
        QF5["★ model/resources/buckets/AwsS3Bucket.kt"]
        QF6["★ model/resources/functions/AwsLambdaFunction.kt"]
        QF7["✏️  model/DeploymentSerializables.kt  ← +iamRoleArn"]
        QF8["✏️  controller/Main.kt  ← registar AwsProvider"]
    end

    subgraph OF["OmniFlow (deployment module)"]
        OF1["★ internalfunction/quickfaas/AwsLambdaDeployer.kt"]
        OF2["★ internalfunction/quickfaas/AwsInternalFunctionResolver.kt"]
        OF3["★ cloud/provider/amazon/deployer/AwsLambdaIamHelper.kt"]
        OF4["✏️  internalfunction/quickfaas/QuickFaasDescriptorLoader.kt  ← +AWS_VALID_RUNTIMES"]
        OF5["✏️  cloud/provider/amazon/deployer/AmazonCloudDeployer.kt  ← chamar resolver"]
        OF6["✏️  Main.kt  ← exemplo 7 AWS auto-deploy"]
    end

    subgraph FN["Funções de Exemplo"]
        FN1["★ functions/hello-lambda-fn/MyHandler.java"]
        FN2["★ functions/hello-lambda-fn/func-deployment.json  (aws)"]
    end

    QF --> JAR["QuickFaaS-Deployment-1.0-fat.jar  (rebuild)"]
    JAR --> OF1
    JAR --> OF2

    style QF1 fill:#ffd700,color:#000
    style QF2 fill:#ffd700,color:#000
    style QF3 fill:#ffd700,color:#000
    style QF4 fill:#ffd700,color:#000
    style QF5 fill:#ffd700,color:#000
    style QF6 fill:#ffd700,color:#000
    style OF1 fill:#ffd700,color:#000
    style OF2 fill:#ffd700,color:#000
    style OF3 fill:#ffd700,color:#000
    style FN1 fill:#ffd700,color:#000
    style FN2 fill:#ffd700,color:#000
```

---

**Legenda:** `★` = ficheiro novo &nbsp;|&nbsp; `✏️` = ficheiro modificado
