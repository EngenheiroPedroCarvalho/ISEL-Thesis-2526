# Guia de Apresentação — Exemplo 8
### Pipeline de Análise de Texto com AWS Lambda + Step Functions

---

## Antes de começar

- [ ] Variáveis de ambiente exportadas no terminal
- [ ] `func-deployment.json` das 3 funções preenchidos
- [ ] Browser aberto na consola AWS (Lambda + Step Functions + IAM)
- [ ] IDE ou terminal pronto para correr `Main.kt`
- [ ] Corrida de teste feita no dia anterior — **não demonstrar pela primeira vez ao vivo**

---

## 1. Contextualizar

> *"Vou mostrar como o OmniFlow descreve e faz o deployment de um pipeline serverless na AWS, de forma completamente automática, a partir de um DSL em Kotlin."*

**Mostrar:** `Example8Workflow` em `Main.kt`

```kotlin
private val Example8Workflow = workflow {
    name("AwsTextAnalysisPipeline")
    steps(
        step { name("call-preprocess")  ... internalFunction("aws-preprocess-fn", ...) }
        step { name("call-char-stats")  ... internalFunction("aws-char-stats-fn", ...) }
        step { name("call-summary")     ... internalFunction("aws-summary-fn",    ...) }
    )
}
```

> *"O utilizador declara 3 passos sequenciais. Cada um referencia uma função interna pelo nome. O OmniFlow trata do resto — não há configuração manual na consola AWS."*

---

## 2. Mostrar o código das funções

**Mostrar:** `functions/aws-summary-fn/MyFunctionClass.java`

> *"Este é o código Java da terceira função. Recebe um texto e uma língua, e devolve um resumo estatístico. O utilizador escreve apenas a lógica de negócio — não há nenhum detalhe de AWS aqui."*

**Mostrar:** `functions/aws-preprocess-fn/func-deployment.json`

> *"Este ficheiro diz ao OmniFlow onde fazer o deployment — a região, o bucket S3, o runtime. O campo `iamRoleArn` está vazio porque o OmniFlow cria o role de execução automaticamente."*

---

## 3. Correr o exemplo

**Executar:** `Main.kt` → opção `8`

O programa vai pedir confirmação das credenciais e depois avançar pelos estágios.

---

## 4. Narrar o que acontece no terminal

### Estágio 1 — Configuração
> *"Valida que as credenciais AWS estão disponíveis e que o JAR do QuickFaaS existe."*

### Estágio 2 — Deployment das Lambdas

Para cada uma das 3 funções, o terminal mostra:

```
→ Loading deployment descriptor...
✓ Descriptor validated (provider=aws, runtime=java17)
→ Invoking QuickFaaS subprocess...
✓ QuickFaaS subprocess completed
→ Waiting for Lambda to become Active...
✓ Lambda is Active
✓ Lambda ARN: arn:aws:lambda:eu-west-1:...
```

> *"O QuickFaaS compila o código Java, cria um fat JAR, faz upload para S3, e chama a API da AWS para criar a Lambda. O OmniFlow aguarda até a função estar activa antes de avançar."*

> *"Reparem que este processo ocorre para as 3 funções de forma sequencial e automática — nenhuma foi criada manualmente."*

### Estágio 3 — State machine criada

> *"Com os ARNs das 3 Lambdas em mãos, o OmniFlow gera a Amazon States Language e cria a state machine no Step Functions."*

---

## 5. Mostrar na consola AWS

### Lambda → Functions

> *"As 3 funções foram criadas. Nenhuma existia antes de correr o exemplo."*

Clicar numa função → mostrar o separador **Code** com o JAR uploaded.

### IAM → Roles → `OmniFlowLambdaExecutionRole`

> *"Este role foi criado automaticamente pelo OmniFlow. É o papel que cada Lambda assume quando executa — dá-lhe permissão para escrever logs no CloudWatch. O utilizador não o configurou."*

### Step Functions → `AwsTextAnalysisPipeline`

> *"Esta é a state machine gerada. O OmniFlow traduziu o DSL Kotlin para Amazon States Language — o formato JSON nativo do Step Functions."*

Clicar em **Definition** → mostrar o JSON.

> *"Reparem que cada step invoca a Lambda directamente pelo ARN, sem API Gateway — é uma chamada directa entre serviços AWS."*

---

## 6. Executar a state machine

**Step Functions → Start execution**

Input:
```json
{ "text": "Hello World OmniFlow" }
```

> *"O workflow recebe um texto e passa-o pelas 3 funções em sequência."*

Enquanto executa, mostrar o grafo a ficar verde passo a passo.

Após terminar, abrir o resultado do último passo:

```json
{
  "summary": "O texto tem 3 palavras e 5.7 caracteres em média.",
  "language": "pt",
  "wordCount": 3,
  "avgWordLength": 5.7
}
```

> *"O resultado final é da função `aws-summary-fn`. O parâmetro `lang=pt` foi definido directamente no DSL — a função gerou o resumo em português."*

---

## 7. Fechar a demonstração

> *"O que acabámos de ver foi: código Java local + descrição de workflow em Kotlin → 3 Lambdas deployed + state machine criada e executada, tudo de forma automática."*

> *"O mesmo DSL funciona para GCP sem alterações — o utilizador escolhe o provider e o OmniFlow adapta-se."*

---

## Notas rápidas para perguntas frequentes

| Pergunta | Resposta curta |
|---|---|
| *O QuickFaaS é vosso?* | O QuickFaaS é uma ferramenta existente. A integração com o OmniFlow para AWS foi o que foi desenvolvido. |
| *O que é o Step Functions?* | É o serviço de orquestração da AWS — equivalente ao Cloud Workflows no GCP. Executa a lógica do workflow e chama as funções na ordem certa. |
| *O que é um role IAM?* | É uma identidade temporária na AWS. A Lambda assume um role quando executa, o que lhe dá permissão para aceder a outros serviços. |
| *Porque é que o campo `iamRoleArn` pode ficar vazio?* | O OmniFlow detecta que está vazio e cria automaticamente um role com as permissões mínimas necessárias. |
| *Funciona com qualquer código Java?* | Qualquer classe Java que implemente a interface `RequestHandler` do Lambda. O OmniFlow gera o wrapper automaticamente. |
| *E se a função já existir?* | O QuickFaaS faz update do código. O OmniFlow também faz update da state machine se já existir. |
