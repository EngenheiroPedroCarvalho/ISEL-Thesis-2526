# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `85555e4` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-28

### "Resolution cascade" passa a "resolution ladder"

O termo foi substituído em todo o texto, incluindo as ocorrências isoladas de "cascade" que se
referem ao mesmo conceito ("the cascade" → "the ladder", "cascade's" → "ladder's"):
`abstract-en.tex`, capítulos 1, 2, 3, 5, 6, 7 e 8, `glossary.tex` (entrada "Resolution ladder"),
`appendix-cascade.tex` (título do Apêndice A: "Resolution Ladder Sequence Diagrams") e o comentário
em `Config/_files.tex`. Títulos renomeados: §3 "The Resolution Ladder" e §7 "Correctness of the
Resolution Ladder". Labels (`sec:resolution-cascade`, `fig:resolution-cascade-level*`,
`app:cascade-sequences`), a chave do glossário, os nomes de ficheiros e as imagens ficaram iguais.

### Cortes de informação a mais (cap. 2 e 6)

Revisão à procura de pormenor excessivo; aplicados só os cortes de prioridade alta.

- **Cap. 2 (`chapter2.tex`), §2.1.3 FaaS Deployment Model:** os dois parágrafos sobre o pipeline
  ZIP (lista dos JARs, `pom.xml`, `function-configs.json`, Cloud Resource Manager API) passam a um
  parágrafo curto: hook + template + bibliotecas, empacotamento Maven, ZIP, upload para o bucket.
- **Cap. 2, §2.2.1 Infrastructure-as-Code:** o parágrafo sobre a ligação em tempo de deployment
  (que desmontava `!GetAtt`, `Fn::GetAtt` e `${aws_lambda_function.lambda.arn}` campo a campo)
  fica com um exemplo (AWS SAM) e remissões para os restantes; as citações mantêm-se todas.
- **Cap. 6 (`chapter7.tex`), §6.3 Methodology:** a proveniência das sessões de medição de T6/T7
  reduzida a uma frase; retirada a descrição das experiências T15–T18 e S2, que não são reportadas.
- **Cap. 6, §6.5:** retirado o parágrafo sobre a saída para consola que inflacionava T6/T7 e a
  decomposição do custo por chamada no GCP (0,35 / 0,22 / 0,6 µs), substituída por uma frase; as
  legendas das Tabelas T6/T7 deixam de mencionar a saída para consola.
- **Cap. 6, §6.7:** a lista das chamadas API de um deployment Level 3 (AWS e GCP) passa a remissão
  para §4.1 e §4.2.
- **Cap. 6, figuras:** retiradas as figuras de largura total que repetiam a tabela (T3a/b, T4a/b,
  T6, T7, T8, T9, T11); T12 e T13 perdem tabela e figura e ficam só no texto. T1, T2, T5, T10 e T14
  (tabela e figura lado a lado) ficam iguais. Os ficheiros PNG continuam em `images/benchmarks/`.

### Cortes de informação a mais, prioridade média (cap. 2, 3 e 4)

- **Cap. 2 (`chapter2.tex`), §2.1.3 QuickFaaS:** a descrição MVC (Controller/Model/View) passa a uma
  frase, que passa a referir o entry point headless usado pela integração. O parágrafo sobre as
  gerações das Cloud Functions encurta (fica o URL de 1.ª geração, a nota sobre os nomes novos da
  Google e a razão da mudança para a 2.ª geração). No exemplo de deployment, deixam de se explicar
  `dependenciesFile`/`configurationsFile` campo a campo.
- **Cap. 2, §2.1.4 Example: Function Chaining:** a explicação linha a linha da listagem passa a um
  parágrafo curto.
- **Cap. 3 (`chapter3.tex`), §3.1.1:** o item do Level 1 perde o suffix match e o argumento de custo
  da validação; o parágrafo "Provider and region scope" encurta para cerca de metade e remete para
  a subsecção do deployer AWS (The OmniFlow-Side Deployer) em vez do cap. 4.
- **Cap. 3, §3.3:** a explicação do formato do registo fica mais curta e passa a incluir a
  localização do ficheiro; a explicação linha a linha da Listing do passo `internalFunction` fica só
  com o essencial (sem `host`/`path`, o que dá `internalFunction`).
- **Cap. 4 (`chapter5.tex`), §4.1:** retirada a subsecção "Registry File Format", que repetia §3.3
  (as subsecções seguintes sobem um número). A subsecção Cloud Run Functions on GCP perde os pormenores do
  pedido v2 (`buildConfig`/`serviceConfig`, `functionId`, Eventarc, template de storage).
  "Evolution of the Registry Design" perde o parágrafo "A metadata record per function" (passa de
  quatro para três fios), o nome da fixture `OptimizedEndpointResolver` e a última frase.
- **Cap. 4, §4.3 Runtime Invocation:** retiradas a menção a `AmazonConstantsUtils` e a explicação de
  `InputPath`/`Next`.

### Cortes de informação a mais, prioridade baixa (cap. 1, 5 e 6)

- **Cap. 1 (`chapter1.tex`), abertura:** a definição da IBM e o parágrafo sobre a história da cloud
  (Parkhill, Vaquero, Armbrust, Buyya, EC2) passam a um parágrafo de duas frases, com as mesmas
  citações. No objetivo 4, deixa de se enumerar cada situação testada (estão na Tabela da §6.2).
- **Cap. 5 (`chapter6.tex`), §5.2 Workflow Design:** retirados os caminhos exatos que `withKey` gera
  em Cloud Workflows e Step Functions.
- **Cap. 6 (`chapter7.tex`), §6.1 Scope and Goals:** a lista das "seis partes" passa a uma frase com
  remissões para as secções. Em Threats to Validity, retirada a explicação dos dois testes de
  deployment completo do código.

### Pontos e vírgulas desnecessários

Retirados 72 ";" do texto, substituídos por ponto final, vírgula com conjunção ("and",
"so", "since", "which") ou dois pontos, conforme a relação entre as orações: `abstract-en.tex`,
`abstract-pt.tex`, capítulos 1, 2, 3, 5, 6, 7 e 8, `appendix-cascade.tex` e `glossary.tex`
(inclui legendas de figuras e tabelas). A legenda de T11 foi reescrita ("with $I$ internal and $E$
external calls, $R{=}F{=}10$ fixed"). Ficaram os ";" que separam itens de listas, as células
abreviadas da tabela de correção (§6.2), as séries com vírgulas internas (grupos do Related Work,
níveis no glossário, agradecimentos) e o ";" antes da remissão entre parênteses em §3.1.1.

### Abstract em português: "cascata" passa a "escada"

`abstract-pt.tex`: as duas ocorrências de "cascata" passam a "escada", para acompanhar o termo
"resolution ladder" do texto em inglês.

### Caso de estudo executado no GCP (cap. 5) e evidência (cap. 6 e 7)

O `PaymentAuthorization` foi implantado e executado no projeto GCP `tfm26-509910` a 28/09; os
artefactos (workflow, funções, scripts, logs) estão em `case-study-gcp/` na raiz do repositório.

- **Cap. 5 (`chapter6.tex`):**
  - Abertura: a implantação no GCP foi executada; as do AWS são descritas e produzidas com test
    doubles.
  - Listing 5.1: as chamadas de pontuação passam `amount` e `country` como query parameters em vez
    do mapa `transaction` (um valor de query no Cloud Workflows não pode ser um mapa); o texto que a
    justifica foi atualizado.
  - §5.4: o parágrafo inicial passa a referir só o AWS; o parágrafo do GCP foi reescrito com a
    execução real (referências com região, stub do core banking, bootstrap, Level 3, segunda
    implantação com Level 1 e registo idêntico, workflow apagado entre as duas). A Listing 5.3 passa
    a ser o registo real (quatro entradas). Nova Tabela `tab:case-executions` com as três execuções.
  - §5.6 Discussion: novo parágrafo com os quatro limites que a execução revelou (referências sem
    região e regiões fechadas, deployer só cria workflows, host literal em formas diferentes nos dois
    renderers, `getFromJsonBody` do QuickFaaS e o charset do Cloud Workflows).
- **Cap. 6 (`chapter7.tex`), Threats to Validity:** a evidência passa a ser "mostly local", com a
  execução do caso de estudo no GCP; no AWS continua por verificar.
- **Cap. 7 (`chapter8.tex`):** objetivo 4 (evidência "mostly local"), dois itens novos no Critical
  Assessment ("Workflow redeployment", "Bare references and restricted regions"), trabalho futuro
  (validar no AWS e no GCP para além de um workflow) e Final Remarks ("evidence against AWS").

### Conformidade com as Normas do TFM (CTC do ISEL, 2025/26)

- **Resumos** (`abstract-en.tex`, `abstract-pt.tex`): encurtados para 290 e 297 palavras (limite
  300; tinham 375 e 411). Juntam-se a descrição do problema e os três níveis da escada numa só
  frase cada, e passa a referir-se o caso de estudo executado no Google Cloud. Palavras-chave
  passam de 7 a 5 (saem AWS Lambda e Google Cloud).
- **Ordem dos resumos** (`template.tex`): o resumo em português passa a vir antes do inglês
  (`\abstractorder(en):={pt,en}`).
- **Nota de rodapé retirada** (`chapter6.tex`, §5.4): a referência à pasta `case-study-gcp` passa
  para o texto.
- **Títulos e legendas** (`template.tex`, sem mexer nos ficheiros do template): títulos de
  capítulo, secção e subsecção a preto (eram laranja e roxo); secções em 12 pt e subsecções em
  11 pt (eram ~14 pt e 12 pt); legendas em 9 pt (eram 10 pt).
- **Capa** (`Config/_cover.tex`, `template.xmpdata`): "Omniflow" → "OmniFlow"; grau do candidato
  "(Graduated)" → "(BSc)"; orientadores com "Prof. Doutor"; título em português ("Orquestração
  Unificada de Workflows Serverless Multi-Cloud com QuickFaaS e OmniFlow"), que aparece na página
  do Resumo.
- **Declaração de integridade** (`statement-en.tex`): data no formato "Lisbon, 30 September 2026".

### Referências

- **`Bibliography/bibliography.bib` reescrito** (as chaves citadas ficam iguais):
  - Autores institucionais entre chavetas duplas (`{{Google Cloud}}` imprimia "G. Cloud", e
    "Serverless Workflow Community" imprimia "S. W. Community").
  - `carvalho2026towards` passa de `@proceedings` a `@inproceedings`, com `booktitle` do CISTI
    (faltam páginas, editora e DOI).
  - `vaquero2008breaks`: *ACM SIGCOMM Computer Communication Review* 39(1), pp. 50–55, 2009 (era
    um *workshop* de 2008, pp. 39–50, que não corresponde ao DOI).
  - `jonas2019cloudProgramming`: relatório técnico UCB/EECS-2019-3, com os 14 autores (faltavam seis
    e "R. Stoica" era Ion Stoica).
  - `armbrust2010view`: acrescentado Matei Zaharia; retirados os URL de versões de acesso livre que
    não correspondiam ao DOI (também em Buyya e Vaquero).
  - `serverlessComputing`: DOI 10.1145/3510611 e número do artigo (239).
  - `sebsflow_arxiv`: título completo ("SeBS-Flow: …") e passa a `@online` arXiv (era `@article`
    sem revista); o mesmo para Spillner, Spillner–Dorodko e Zhao.
  - Páginas web e documentação uniformizadas como `@online` com autor, `url` e `urldate` (havia
    três formatos de data de acesso e várias entradas sem autor); `pulumi` tinha a data 2022.
  - Datas de acesso em formato ISO (`template.tex`, `urldate=iso`): o formato por defeito,
    MM/DD/AAAA, lia-se "09/11/2026" como 9 de novembro.
  - Retiradas 15 entradas não citadas, incluindo as duplicadas `afcl` e `lopez2020triggerflow`, e
    `ibmCloudComputing`, que deixou de ser citada.
- **Cap. 1 (`chapter1.tex`):** a página da IBM deixa de sustentar a formalização da *cloud*; a frase
  sobre o EC2 perde a data sem fonte e passa a citar Armbrust et al.; a frase sobre Jonas et al.
  passa a dizer o que o relatório de Berkeley defende (deixa de lhe atribuir *edge-cloud
  integration*).
- **Cap. 2 (`chapter2.tex`), §2.2.2:** "referred to in the surveyed text as Lambada" → "with a
  tool its author calls Lambada".
- **Cap. 6 (`chapter7.tex`), §6.8:** a comparação com a avaliação original do QuickFaaS passa a
  citá-la.

### Afirmações corrigidas face ao código

- **Cap. 1 (`chapter1.tex`), §1.2.2:** o URL de uma função no GCP deixa de "conter a região e o
  projeto": é atribuído pela Google na implantação, com um *hash* do projeto e o código da região,
  e não se pode escrever antes de a função existir (remete para §4.1.4 em vez de §2.1.3).
- **Cap. 1, contribuição 4 e "Structure of the Work":** o caso de estudo foi executado no GCP; a
  implantação AWS foi produzida pelo *resolver* e pelo *renderer* com o fornecedor simulado (dizia
  "end to end on both providers" / "deployed to both AWS and GCP").
- **Cap. 2 (`chapter2.tex`), Listing 2.1:** valores válidos para uma função de 2.ª geração, iguais
  aos do projeto usado no caso de estudo (`tfm26-509910`, `quickfaas-test-fn`,
  `tfm26-quickfaas-deploy`); "project name" → "project ID".
- **Cap. 3 (`chapter3.tex`), §3.4.1:** o erro de função irresolúvel "names the function and tells
  the developer to deploy it first" (só a mensagem AWS sugere o `deploymentDescriptorPath`).
- **Cap. 4 (`chapter5.tex`):**
  - §4.1.1: o *bootstrap* salta as regiões que não consegue listar e só falha se nenhuma responder.
  - §4.1.4: `QuickFaasDeployer` só concede `roles/run.invoker` quando recebe a conta de serviço do
    *workflow*, e uma falha não interrompe a implantação.
  - §4.2.2: o subprocesso do QuickFaaS é repetido só quando o papel IAM ainda não se propagou, até
    três tentativas no total (dizia "retried up to three times").
  - §4.3: Listing 4.2 — explicados `InputPath` e `Next`.
- **Cap. 5 (`chapter6.tex`), §5.1:** "`updatedAt`-stamped entries" → o registo guarda a ligação
  atual de cada função e a data da última alteração do ficheiro, não que implantação escreveu cada
  entrada.
- **Cap. 6 (`chapter7.tex`):** T6/T7 — "well under a microsecond" → "at most about a microsecond"
  (o GCP mede ≈0,95 µs); tabela de pedidos ao fornecedor — a paragem ao segundo resultado vale
  também para o GCP.
- **Cap. 7 (`chapter8.tex`), "Bare references and restricted regions":** uma região recusada só
  aborta a resolução quando a função não é encontrada noutra região.
- **Glossário (`glossary.tex`), "Stale entry":** acrescentada a procura nas outras regiões antes de
  a entrada ser removida.
- **Figuras** (fontes em `../diagrams/`, PNG regenerados com PlantUML 1.2026.8):
  - Figs. A.1–A.3: título "Resolution ladder" (dizia "Resolution cascade").
  - Fig. 3.3: acrescentado `bodyTerm` a `CallContext`; a multiplicidade 0..1 passa para o lado de
    `InternalFunction`.
  - Fig. 4.3: regenerada com o mesmo estilo das outras figuras.
- **Bibliografia:** `serverlessStepFunctionsPlugin` com `titleaddon` mais curto (a linha saía da
  margem).

### Informação repetida e redação

- **Cap. 1 (`chapter1.tex`):** abertura sem a série de benefícios sem fonte; §1.1 passa a "The
  Emergence of Serverless Computing" (o texto não tratava arquiteturas orientadas a eventos), com
  "workload frequency" e "extensive survey" reescritos; "Currently," → "Without the integration,";
  "The remainder of this dissertation…" → "The dissertation is organised as follows:".
- **Cap. 2 (`chapter2.tex`):**
  - §2.1.1: "represents a shift", "defining characteristic", "granular billing" e "lift control-flow"
    reescritos.
  - §2.1.3: a subsubsecção "Components Overview" do QuickFaaS (duas frases) junta-se à abertura;
    *comma splice* nos templates; "most common denominator" → "lowest common denominator"; no
    exemplo, "current QuickFaaS" → "original QuickFaaS", "authorized"/"authorizedJavaScript" →
    "authorised JavaScript", URL em `\url{}`, sem "successfully" nem "effectively".
  - §2.1.4: "Domain Specific" → "Domain-Specific"; três `\cite{silva2025advanced}` repetidos no
    mesmo item retirados; "composed by" → "composed of"; os dois parágrafos que repetiam as quatro
    categorias passam a uma frase sobre o nome, a descrição e o contexto de cada passo.
  - §2.2: "inherently tied", "widely used", "effectively" e "deployment surface" reescritos; o
    quarto grupo deixa de se chamar "A final family" (vinha ainda a §2.2.5).
  - Legenda da Listing 2.1 com ponto final, como as outras.
- **Cap. 3 (`chapter3.tex`):** a lista do fluxo deixa de partir uma frase entre os itens 2 e 3
  (o item 3 continua a ser o que a §3.1.1 cita); "idempotent at the level that matters" →
  "idempotent with respect to functions"; "Why not two independent communicating services" passa
  de quatro razões a duas frases; "pragmatic", "cleaner alternative" e "familiar HTTP vocabulary"
  retirados; dois pontos duplos e *comma splice* em §3.3 corrigidos.
- **Cap. 4 (`chapter5.tex`):** retirada a remissão repetida para a justificação do subprocesso
  (§4.2); "miss-path scan" substituído por uma descrição; §4.1.5 deixa de repetir os modos de falha
  de §3.4.1 e fica só com os dois pormenores próprios; §4.1.6 sem "worth recounting" nem "cheap".
- **Cap. 5 (`chapter6.tex`), §5.6:** reescrita a transição sobre as atualizações do modelo de
  fraude.
- **Cap. 6 (`chapter7.tex`):**
  - Tabela de notação com $K$ e $R_0$ (usados em T8/T9) e remissão para $G$ (§6.6); deixa de dizer
    "five symbols".
  - Resposta à RQ2 com cinco conclusões: sai o tamanho do *bundle* (S1), que não responde à RQ2 (fica
    em §6.9 e no objetivo 3).
  - "proved in T5" → "shown in T5"; "The first workflows to mix" → "Workflows that mix"; "keep the
    comparison honest" → "apply"; "is free in practice" → "adds no measurable cost"; "surcharge" →
    "extra cost"; "well under 0.1% of a real function" (sem suporte) → "proportionally less for a
    function with real dependencies".
- **Cap. 7 (`chapter8.tex`):** novo item "Renderer limits" no Critical Assessment (o `Pass` do
  *renderer* AWS e o *host* literal em formatos diferentes, vistos no cap. 5); retirada a menção à
  PSD2 nas Final Remarks (sem fonte nem introdução); "stated plainly" e "convenient for a demo"
  reescritos.
- **Siglas (`acronyms.tex`):** "Function-as-a-Service" e "Infrastructure-as-Code" com hífen, como
  no texto; retiradas GCS, PaaS, SaaS, UI e JDK (não usadas); acrescentadas CDK, CISTI, ICT, ML,
  OAuth, OIDC, RQ e SAM.

### Estrutura

Todas as `\label` existentes mantêm-se; só a numeração muda.

- **Cap. 3 (`chapter3.tex`):** os conceitos passam a vir antes da escada que os usa. Ordem nova:
  3.1 Components Architecture → 3.2 DSL Extensions (era 3.3) → 3.3 The Resolution Ladder (era a
  subsecção 3.1.1, passa a secção) → 3.4 Design Rationale (era 3.2) → 3.5 Developer Procedure. O
  roteiro da introdução foi reescrito, e a Listing 3.2 remete para a secção da escada.
- **Cap. 4 (`chapter5.tex`):**
  - 4.1 passa a "Function Registry and Resolvers" e perde a subsecção do GCP: 4.1.1 Creation,
    Bootstrap and Updates; 4.1.2 Deployment-Time Endpoint Resolution; 4.1.3 Registry Store and
    Resolver Internals; 4.1.4 Validation and Error Semantics; 4.1.5 Evolution of the Registry Design.
  - Nova 4.2 "Function Deployment at Level~3" (`sec:function-deployment`, antes "AWS Lambda Support
    in QuickFaaS", sem etiqueta), com um parágrafo de abertura: 4.2.1 Cloud Run Functions on GCP
    (era 4.1.4); 4.2.2 Deployment on AWS (nova subsecção `subsec:aws-lambda-deployment`, com a
    Fig. 4.2 e o parágrafo das duas camadas); 4.2.3 The QuickFaaS AWS Provider; 4.2.4 The
    OmniFlow-Side AWS Deployer.
  - Roteiro da introdução reescrito.
- **Cap. 5 (`chapter6.tex`):** a secção "Runtime Invocation" (um parágrafo, e mesmo título que a
  §4.3) passa ao fim da §5.4; o capítulo fica com cinco secções e a etiqueta `sec:case-runtime`
  desaparece (só a introdução a usava). A §5.3 diz que a implantação de produção no AWS é só
  descrita, e a introdução passa a dizer que a §5.4 mostra duas das três implantações.
- **Cap. 7 (`chapter8.tex`), §7.1 Summary of Contributions:** passa a enumerar as cinco
  contribuições da §1.5, cada uma com a secção onde é tratada.
