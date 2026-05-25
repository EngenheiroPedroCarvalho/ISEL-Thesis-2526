# Diagrama Sequencial — Primeiro Deployment (Exemplo 8)

```mermaid
sequenceDiagram
    actor U as Utilizador
    participant C as AWS Console
    participant L as Máquina Local
    participant O as OmniFlow
    participant Q as QuickFaaS
    participant A as AWS APIs

    rect rgb(220, 235, 255)
        Note over U,A: FASE 1 — Preparação local (uma única vez)
        U->>L: Instala Java 17 e Maven
        U->>L: git clone do repositório
        U->>L: ./gradlew fatJar
        L-->>U: QuickFaaS-Deployment-fat.jar
    end

    rect rgb(255, 235, 200)
        Note over U,A: FASE 2 — Configuração na AWS (uma única vez)
        U->>C: Cria utilizador IAM + access key
        C-->>U: AWS_ACCESS_KEY_ID + AWS_SECRET_ACCESS_KEY
        U->>C: Adiciona política OmniFlowDeployPolicy ao utilizador
        U->>C: Cria bucket S3
        C-->>U: Nome do bucket
        U->>C: Cria role StepFunctionsExecutionRole
        U->>C: Adiciona política AllowInvokeWorkflowTargets ao role
        C-->>U: STEP_FUNCTIONS_ROLE_ARN
    end

    rect rgb(220, 255, 220)
        Note over U,A: FASE 3 — Configuração do projecto (uma única vez)
        U->>L: Preenche func-deployment.json de cada função
        Note right of L: project = account ID<br/>bucket = nome do bucket<br/>location = eu-west-1<br/>iamRoleArn = (vazio)
        U->>L: Exporta variáveis de ambiente
        Note right of L: AWS_ACCESS_KEY_ID<br/>AWS_SECRET_ACCESS_KEY<br/>AWS_REGION<br/>QUICKFAAS_JAR_PATH<br/>STEP_FUNCTIONS_ROLE_ARN
    end

    rect rgb(255, 220, 255)
        Note over U,A: FASE 4 — Execução do OmniFlow
        U->>O: Corre Main.kt → opção 8
        O->>O: Valida credenciais e JAR

        loop Para cada uma das 3 funções
            O->>A: Verifica se Lambda execution role existe
            A-->>O: Não existe
            O->>A: Cria OmniFlowLambdaExecutionRole (IAM)
            A-->>O: Role ARN
            O->>Q: Invoca QuickFaaS com o descriptor
            Q->>L: Compila MyFunctionClass.java → fat JAR
            Q->>A: Upload do JAR para S3
            Q->>A: CreateFunction (Lambda)
            A-->>Q: Função criada
            Q-->>O: Subprocess concluído
            O->>A: GetFunction (polling até estado Active)
            A-->>O: Estado: Active
            O->>A: Obtém ARN da Lambda
            O->>A: Concede permissão de invocação ao Step Functions
        end

        O->>A: CreateStateMachine com os 3 ARNs
        A-->>O: State machine criada
        O-->>U: Deployment concluído
    end

    rect rgb(255, 255, 200)
        Note over U,A: FASE 5 — Verificação e teste
        U->>C: Verifica Lambdas criadas (Lambda → Functions)
        U->>C: Verifica role criado (IAM → Roles)
        U->>C: Abre AwsTextAnalysisPipeline (Step Functions)
        U->>C: Start execution com {"text": "Hello World OmniFlow"}
        C-->>U: Resultado: resumo em português
    end
```

---

## Legenda das fases

| Fase | Quem executa | Frequência |
|---|---|---|
| 1 — Preparação local | Utilizador | Uma única vez por máquina |
| 2 — Configuração AWS | Utilizador (consola AWS) | Uma única vez por conta AWS |
| 3 — Configuração do projecto | Utilizador | Uma única vez por projecto |
| 4 — Execução do OmniFlow | OmniFlow + QuickFaaS (automático) | Cada vez que se faz deployment |
| 5 — Verificação | Utilizador (consola AWS) | Cada vez que se faz deployment |

> As fases 1, 2 e 3 são pré-requisitos que só precisam de ser feitos uma vez. A fase 4 é o que o OmniFlow automatiza — sem ela, o utilizador teria de criar manualmente cada Lambda, fazer o upload do código, configurar os roles e criar a state machine à mão.
