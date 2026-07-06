# Testes da Integração OmniFlow + QuickFaaS (com suporte AWS)

Este documento descreve em detalhe os testes adicionados para validar a **minha contribuição** à
tese: a **unificação** do OmniFlow com o QuickFaaS (deixar de ser um processo disjunto — definir o
workflow e instalar as funções passou a ser um só fluxo) e, como complemento, a **extensão do
QuickFaaS para AWS** (antes só GCP/Azure).

> **Princípio de âmbito (decidido com os orientadores):** os testes exercitam **apenas operações
> no computador local** — lógica pura, I/O de ficheiros local, **resolução** de endpoints e
> **renderização** (DSL → Amazon States Language / GCP Workflows YAML). **Não há testes que chamem
> (nem mockem) a AWS/GCP.** Fica de fora qualquer tempo/lógica de *deployment*: upload do zip,
> criação de Lambda/Cloud Function, espera por disponibilidade na cloud, IAM.

Os três pilares: **(1) testes unitários**, **(2) cobertura de código** e **(3) testes de
desempenho** (renderização/resolução, com JMH).

---

## 1. Como correr

### Parte B — Integração no OmniFlow (módulo Maven `deployment/`)
```bash
cd omni-flow-main
./mvnw -pl deployment test                 # corre os testes unitários
./mvnw -pl deployment test jacoco:report   # + relatório de cobertura
# Relatório: deployment/target/site/jacoco/index.html
```

### Parte A — Provider AWS do QuickFaaS (módulo Gradle `QuickFaaS-Deployment/`)
```bash
cd omni-flow-main/quickfaas-essentials/QuickFaaS-Deployment
./gradlew test                 # corre os testes unitários
./gradlew jacocoTestReport     # + relatório de cobertura
# Relatórios: build/reports/tests/test/index.html  e  build/reports/jacoco/test/html/index.html
```

### Desempenho (módulo Maven `benchmark/`, JMH)
```bash
cd omni-flow-main
./mvnw -pl benchmark -am clean package
java -jar benchmark/target/benchmarks.jar -rf csv -rff results.csv   # todos os benchmarks
# Filtrar um experimento, ex.: java -jar benchmark/target/benchmarks.jar "BenchmarkRenderingScalability.*"
```

> **Pré-requisito de build (Parte B):** foi adicionada a dependência `junit-vintage-engine` ao
> `deployment/pom.xml`. Sem ela, os testes existentes escritos em JUnit 4 (`kotlin-test-junit`)
> **não corriam** no Surefire (JUnit 5). Com ela, a contagem de testes do módulo passou a refletir
> a suíte completa.

---

## 2. Testes unitários — Parte B (unificação, `deployment/`)

Stack: **JUnit 5 + Strikt** (e `@TempDir` para I/O local). Localização:
`deployment/src/test/kotlin/costaber/com/github/omniflow/`.

| Ficheiro | O que valida |
|---|---|
| `registry/WorkflowInternalCallEndpointResolverTest.kt` | Núcleo da unificação. Uma chamada **interna** tem `host`/`path` substituídos pelo endpoint do registo (URL dividido em `scheme://authority` + `path`, com `/` quando vazio); uma chamada **externa** (extractor devolve `null`) fica **byte-a-byte intacta**; resolução recursiva dentro de `BranchContext`, `IterationRangeContext`, `IterationForEachContext`, `ParallelBranchContext`, `ParallelIterationContext`; chave inexistente no registo → exceção clara; restantes campos da `CallContext` (método, headers, query, body, timeout, result, authentication) preservados. |
| `internalfunction/quickfaas/AwsInternalFunctionResolverTest.kt` | Mesma natureza de transformação de árvore para o resolver AWS: sem funções internas → workflow inalterado; com funções internas → endpoints resolvidos em todos os tipos de contexto e aninhamentos. |
| `registry/FunctionRegistryStoreTest.kt` | Persistência local do registo: round-trip `writeNew()` → `readAll()` com `@TempDir`; registo inexistente → mapa vazio (sem exceção); `serviceName`/`url` corretos; JSON inválido/sem o nó `functions` tolerado. |
| `registry/FunctionRegistryKeysAndMetadataTest.kt` | Geração das chaves de endpoint (`<ref>.host` / `<ref>.path`) e mapeamento de `FunctionInvocationMetadata`. |
| `internalfunction/quickfaas/QuickFaasDescriptorLoaderTest.kt` (casos AWS acrescentados) | Parsing/validação de `func-deployment.json` para AWS: runtimes válidos (`java17`, `java21`, `nodejs20.x`) passam; runtime inválido → `IllegalArgumentException`; provider `aws` com `expected=gcp` → `IllegalStateException`; round-trip com `iamRoleArn` presente e vazio. |
| `internalfunction/quickfaas/AwsLambdaDeployerTest.kt` | Lógica **local** do deployer: `patchIamRoleArn` (substitui/preenche o `iamRoleArn`, preserva os restantes campos, tolera espaços à volta do `:`, e o ficheiro temporário escrito é relido corretamente via `QuickFaasDescriptorLoader`). O deploy real para AWS está num teste `@Disabled` (requer credenciais). |
| `renderer/AmazonRendererTest.kt` (casos Lambda acrescentados) | Renderização **local** específica da invocação de Lambda: chamada interna → `arn:aws:states:::lambda:invoke` com `ResultSelector` a usar `$.Payload`; chamada externa (API Gateway) mantém `$.ResponseBody`. |

## 3. Testes unitários — Parte A (provider AWS do QuickFaaS, `QuickFaaS-Deployment/`)

Stack: **`kotlin.test`** (única biblioteca de teste no classpath deste módulo Gradle; sem Strikt/
MockK). Localização: `QuickFaaS-Deployment/src/test/kotlin/model/`. **19 testes.**

| Ficheiro | O que valida |
|---|---|
| `model/AwsProviderTest.kt` | `setProjectData(accountId)` define o Account ID em `AwsProjectData.name`, repõe o bucket e **propaga o `iamRoleArn`** de `AwsSpecifics` para o `AwsLambdaFunction`; `requestProjects()` devolve a lista em memória **sem chamada à cloud**; metadados do companion (`name`, `shortName`, `newCloudProvider()`). |
| `model/specifics/AwsSpecificsTest.kt` | `setSpecifics(...)` resolve a **região** e o **`iamRoleArn`** a partir do descriptor; `iamRoleArn` em branco produz o aviso. (Testadas as ramificações fornecidas pelo descriptor; as que dependem de variáveis de ambiente não são exercitadas por não serem definíveis em processo.) |
| `model/resources/functions/AwsLambdaFunctionTest.kt` | Propriedades locais: lista de `locations`, `runtimes` (`JAVA11`/`JAVA17`), formatação do runtime e `handler` (`AwsHttpTemplate`). **Não** invoca `deployZip` (esse chama o SDK). |
| `model/resources/functions/runtimes/scripts/AwsBuildScriptsTest.kt` | Geração **local** de artefactos: o POM gerado para a Lambda contém o `maven-shade-plugin` e injeta as dependências do utilizador; seleção do fat-jar e erro claro quando não há JAR. **Não** corre o build Maven real. |
| `model/resources/functions/AwsLambdaFunctionBuildIntegrationTest.kt` | **Passa pelo código real do QuickFaaS, sem cloud** (ver secção 3.1). |

> **Limitação documentada (Parte A):** o ramo de nome em branco de `setProjectData("")` chama
> `logPropertyMissing(...)` → `exitProcess(1)`, que termina a JVM. Esse caminho não é testável em
> processo (capturar `System.exit` exigiria um `SecurityManager`, indisponível nas JVM modernas),
> pelo que fica como limitação conhecida e não é testado.

### 3.1. Teste que passa pelo QuickFaaS real, mas sem cloud

Todos os outros testes chamam apenas `resolver.resolve(...)` / `traversor.traverse(...)` — nunca o
QuickFaaS. Este teste faz o oposto: invoca o **código de produção real** do QuickFaaS
(`AwsLambdaFunction.buildAndZip("aws")` → `AwsBuildScripts.javaBuildScript` → `JavaUtils.mavenBuild`,
um **`mvn package` real** via Maven Invoker, usando o Maven já empacotado no repo em
`function-deployment/java/`), e para **exatamente** antes da fronteira com a cloud:

```
parse descriptor → provider setup (local, p/ AWS) → function.buildAndZip(...) → function.deployZip(...)
                                                      ^^^^^^^^^^^^^^^^^^^^^^^     ^^^^^^^^^^^^^^^^^^^^^
                                                      testado (mvn real)          NUNCA chamado (S3+Lambda)
```

Reutiliza o mesmo `functions/hello-lambda-fn/MyFunctionClass.java` do Exemplo 8. Produz um zip real
e confirma o seu tamanho (**14 685 bytes**, determinístico entre execuções).

**Descoberta feita através deste teste:** a primeira execução produziu um zip de apenas 3988 bytes
(sem as classes de `aws-lambda-java-core` incluídas) — revelando um **bug real** em
`AwsBuildScripts.copyFatJarAsZip`. Como o POM não define `<build><finalName>` ao nível do projeto,
o Maven produz **dois** jars em `target/` (`function-1.0.jar`, fino, do plugin `jar` por defeito, e
`function.jar`, gordo, do `shade`); o filtro antigo (`extension=="jar" && !startsWith("original-")`)
aceitava **ambos**, e `listFiles().firstOrNull()` escolhia entre eles de forma **não
determinística** (a ordem do sistema de ficheiros não é garantida) — em produção, isto podia
deployar uma Lambda **quebrada** (sem as suas dependências) só por azar na ordem de listagem.
**Corrigido** para procurar o jar pelo nome exato (`function.jar`, o `finalName` configurado no
shade), eliminando a ambiguidade. Confirmado determinístico em execuções repetidas após a correção.

> **Nota de ambiente:** requer que `function-deployment/java/apache-maven-3.8.6/bin/{mvn,mvnDebug,mvnyjp}`
> tenham o bit de execução (corrigido neste commit — não estava definido no repo) e que
> `tasks.test.workingDir` aponte para `omni-flow-main/` (adicionado ao `build.gradle.kts`), para os
> paths relativos do Maven empacotado resolverem.

---

## 4. Cobertura de código

Medida com **JaCoCo** em ambos os módulos, **restrita às classes de lógica local** da
contribuição (as classes que chamam AWS/GCP estão fora de âmbito — de teste **e** do denominador
da cobertura — por desenho).

- **Parte B (`deployment/`)** — incluídos `registry/**` e `internalfunction/**`, com `excludes`
  das classes cloud (`AwsLambdaDeployer`, `QuickFaasDeployer`, `CloudFunctionsIamHelper`,
  `*FunctionRegistryBootstrapper`, `WorkflowInternalFunctionResolver`, etc.).
  - Cobertura do núcleo de **resolução/registo**: **78,4% de instruções** no conjunto da lógica
    local. Por classe: `FunctionEndpointKeys` e `FunctionInvocationMetadata` 100%,
    `QuickFaasDescriptorLoader` 94%, `FunctionRegistryStore` 92%,
    `WorkflowInternalCallEndpointResolver` 80%, `AwsInternalFunctionResolver` 61%.
- **Parte A (`QuickFaaS-Deployment/`)** — incluídas as classes `model/Aws*` de lógica local
  (excluído `AwsRequests`, que chama o SDK).
  - As classes de **lógica local pura** ficam bem cobertas: `AwsProvider` **84%**, `AwsSpecifics`
    **73%**. O total do conjunto `Aws*` (~36%) é puxado para baixo por `AwsLambdaFunction.deployZip`,
    `AwsS3Bucket` (upload) e código de serialização gerado — todos fora de âmbito por chamarem a
    cloud / não serem lógica testável localmente.

Como gerar: ver secção 1. Na dissertação, reportar a % de instruções/ramos das classes de lógica
local e explicar que as classes de *deployment* na cloud ficam naturalmente fora.

---

## 5. Testes de desempenho (JMH) — P1 a P8

Reutilizam o módulo `benchmark/` (JMH 1.37) e os geradores de workflows
(`generator/WorkflowGenerator`, `StepGenerator`, `StepContextGenerator`, estendidos com
`withParameterizedCalls`, `withInternalCalls`, `withExternalCalls`, `withNestedSteps`,
`withLambdaCalls`, `callWithParameters`, `internalCall`, `externalCall`, `lambdaCall`). **Todos
medem apenas renderização/resolução (CPU local)** — os benchmarks de *deployment* na cloud não
são usados.

**Metodologia JMH** (aplicada a todos): `@BenchmarkMode(AverageTime)`, *warmup* + medição com
*forks*, e o resultado é consumido com `Blackhole` para evitar *dead-code elimination*. Exportar
CSV (`-rf csv`) e ajustar uma curva aos pontos.

> **Resultados já executados:** ver `benchmark/results/RESULTS.md` (tabelas + conclusões) e os
> gráficos `benchmark/results/P1_*.png … P8_*.png`. Gerados com
> `java -cp benchmark/target/benchmarks.jar org.openjdk.jmh.Main "...metrics.Benchmark(Rendering|InternalCall|Resolution).*" -f 1 -wi 3 -i 5 -w 1 -r 1 -rf csv -rff benchmark/results/jmh-results.csv`
> e `python3 benchmark/results/plot_benchmarks.py`.

| Benchmark | Pergunta / objetivo | Variável | O que se espera |
|---|---|---|---|
| `metrics/BenchmarkRenderingScalability.kt` (**P1**) | Como degrada o tempo de **renderização** com o número de funções? | `@Param n ∈ {1..200}` | Tempo cresce com N; caracterizar linear vs. supralinear (atenção a O(N²) caso haja concatenação de strings). |
| `metrics/BenchmarkRenderingByParameterCount.kt` (**P2**) | O número de **inputs da função** (materializados como argumentos passados na chamada) afeta a renderização? | `@Param p ∈ {0..20}`, N fixo | Crescimento linear em p (~15 µs por input). |
| `metrics/BenchmarkInternalCallResolution.kt` (**P3**) | Qual o **custo da unificação** (passo de resolução de funções internas)? | `@Param n`, interno vs. externo | Linear e pequeno; internas > externas pelo custo das *lookups* + cópia da árvore (registo pré-carregado em memória — sem I/O no caminho quente). |
| `metrics/BenchmarkRenderingAwsVsGcp.kt` (**P4**) | O renderizador **AWS** tem custo comparável ao **GCP**? | renderer AWS vs. GCP, ao longo de N | Mesma ordem de grandeza; observação, não veredicto. |
| `metrics/BenchmarkRenderingByNesting.kt` (**P5**) | O que degrada: o **número** de funções ou a **complexidade estrutural**? | profundidade de aninhamento, total de passos fixo | Distinguir efeito do nº total de nós vs. profundidade; aninhamento muito profundo pode revelar limites de recursão. |
| `metrics/BenchmarkRegistryScaling.kt` (**P6**) | `FunctionRegistryStore.resolveUrl` não tem cache: cada chamada relê e reparsa o ficheiro do registo inteiro. Qual o custo real disso à medida que o **registo cresce**, independentemente do nº de chamadas do workflow? | `@Param n` (chamadas internas) × `@Param m` (funções já registadas), `resolveAllExternal` como controlo (nunca toca o registo) | Custo cresce com `n` **e** com `m` (O(N·M)); `resolveAllExternal` deve manter-se ~constante em `m`, confirmando que o efeito é especificamente do I/O+parsing do registo. Quantifica o problema já identificado nos "Gotchas" do `CLAUDE.md` e serve de baseline para justificar uma futura cache em memória. |
| `metrics/BenchmarkResolutionOptimization.kt` (**P7**) | Qual o ganho real de ler o registo **uma só vez** por `resolve(workflow)` em vez de por chamada? | `@Param n × @Param m`, naive vs. optimized, na mesma execução | *Speedup* de ~200×; a resolução passa de Θ(N·M) para Θ(N+M). |
| `metrics/BenchmarkRenderingLambdaVsApiGateway.kt` (**P8**) | Renderizar uma chamada **interna** (`lambda:invoke`, já resolvida) é mais barato que uma **externa** (`apigateway:invoke`)? Nenhum benchmark anterior renderizava chamadas internas — P1/P2/P4/P5 só renderizam externas; P3/P6/P7 só medem `resolve()`, nunca renderizam. | `@Param n ∈ {1..200}`, AWS apenas (GCP não distingue) | `lambda:invoke` mais barato (menos campos por chamada); confirmado: ~1,75× mais rápido a N=200. |

---

## 6. Resumo do estado

| Pilar | Módulo | Resultado |
|---|---|---|
| Testes unitários | `deployment/` (Parte B, Maven) | **148 testes**, 0 falhas (5 `@Ignore`/`@Disabled` por exigirem cloud) |
| Testes unitários | `QuickFaaS-Deployment/` (Parte A, Gradle) | **18 testes**, 0 falhas |
| Cobertura | ambos | JaCoCo, focada na lógica local (ver secção 4) |
| Desempenho | `benchmark/` (JMH) | P1–P8 compilam e correm localmente |

**Fora de âmbito (trabalho futuro):** testes de integração reais contra AWS/GCP, tempo de
deployment end-to-end, mutation testing, property-based, fuzzing, métricas estáticas e concorrência.
