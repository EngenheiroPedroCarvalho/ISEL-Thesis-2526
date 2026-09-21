# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `83beba1` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-21

### Referência do artigo do CISTI reduzida ao que é sabido (`bibliography.bib`)

A entrada `carvalho2026towards` era um `@inproceedings` com `booktitle` ("2026 21st Iberian
Conference on Information Systems and Technologies (CISTI)"), `publisher` (IEEE) e uma nota
`TODO: local, páginas e DOI`. Passa a um `@proceedings` com apenas os três campos que já se
conhecem --- `author`, `title` e `year` --- seguindo o template `proceedings` da BibTeX.com. Os
dados que ainda faltam deixam de estar prometidos numa nota visível.

**Consequência conhecida e aceite:** o biblatex, no tipo `@proceedings` (que descreve o volume de
atas, não um artigo dentro dele), imprime `editor` e não `author`, por isso os três autores ficam
no `.bib` mas **não aparecem na bibliografia**. A referência [7] sai como "Towards Cloud-Agnostic
Serverless Applications: Unifying Function Deployment and Workflow Orchestration. 2026". O `author`
continua a servir de `labelname`, por isso a entrada mantém-se ordenada sob "C" (Carvalho), entre
Buyya e Cloud. A alternativa --- voltar a `@inproceedings`, que imprime o autor --- foi ponderada e
recusada.

A citação no cap. 1 (`chapter1.tex`, §1.4) não foi tocada: o texto já nomeia a conferência e o
título do artigo, por isso a frase continua a identificar a publicação mesmo com a referência
reduzida.

Compilado: 98 páginas, sem referências por resolver, 48 entradas numeradas para 48 citadas e os
mesmos 8 avisos de *overfull hbox* do template.

## 2026-09-20

### Sintaxe de interpolação do Terraform explicada (`chapter2.tex`)

No parágrafo do *deployment-time binding* da secção 2.4, a referência
`${aws_lambda_function.lambda.arn}` aparecia só glosada como "o atributo `arn` do recurso Lambda
chamado `lambda`", ao contrário do `!GetAtt` do CloudFormation, que tem decomposição completa
(atributo, anatomia do ARN, porquê uma expressão e não um literal). Corrigida a assimetria:

- Dito que a definição da state machine é uma string onde o Terraform interpola expressões da forma
  `${...}`, avaliadas no momento do *apply*.
- A expressão passa a ser lida pelas três partes, por ordem: tipo de recurso
  `aws_lambda_function`, etiqueta `lambda` dada a um recurso desse tipo na mesma configuração, e
  atributo `arn` que esse recurso exporta.
- Acrescentado o paralelo com o `MyFunction` do parágrafo anterior --- `lambda` é a etiqueta interna
  da configuração, não o nome que a função tem na AWS --- e que o atributo só tem valor depois de a
  função ser criada.
- A dependência implícita (a função criada antes do workflow) mantém-se, agora em frase própria.

Sem entradas novas na bibliografia (`terraformSfnStateMachine` e `terraformReferences` já
estavam citadas). Compilado: 98 páginas, sem referências por resolver e com os mesmos 8 avisos
de *overfull hbox* do template.

### Nota "explica as figuras e o código" alargada aos restantes capítulos

A nota dos orientadores tinha sido aplicada só ao cap. 3, onde estava marcada. Revistas agora todas
as figuras e listagens dos outros capítulos: 3 listagens no cap. 2, 3 figuras e 2 listagens no
cap. 4 (`chapter5.tex`), 1 listagem no cap. 5 (`chapter6.tex`) e, no cap. 6 (`chapter7.tex`), as 14
figuras T1–T14 mais as tabelas de correção, de chamadas à API, de esforço manual e a S1.

Estavam quase todas já lidas pelo texto (o encadeamento da Listing 2.3 tem três parágrafos; o
modelo de classes, a sequência de deployment e a invocação em runtime do cap. 4 têm um parágrafo
cada; cada figura T do cap. 6 tem o seu parágrafo de discussão e é referida uma vez por `\ref`).
Só havia duas lacunas, ambas corrigidas:

- **Cap. 2, Listing 2.1 (`func-deployment.json`)** — o item "Deployment Definition" nomeava
  `cloudProvider`, `project`, `accessToken`, `runtime`, `trigger` e `functionFile`, mas deixava por
  explicar quatro campos visíveis na listagem. Acrescentado um parágrafo: `function.location` (a
  região) e `function.bucket` (o bucket de Cloud Storage onde a fonte empacotada é estacionada
  antes do provisionamento); e os dois campos opcionais que na listagem aparecem a vazio —
  `dependenciesFile`, cujo conteúdo é injetado no bloco de dependências do `pom.xml` gerado, e
  `configurationsFile`, que fornece o `function-configs.json` empacotado com a função e lido em
  execução pelas bibliotecas QuickFaaS. Verificado em `GcpBuildScripts.kt:18,24`,
  `JavaUtils.setPomDependencies` e `Utils.CONFIGS_FILE`; os dois campos caem num valor por omissão
  vazio quando não são dados (`Main.kt:65-70`, `Runtime.kt:22`).
- **Cap. 4 (`chapter5.tex`), Listing 4.3 (estado Step Functions gerado)** — o texto explicava
  `Resource`, `Parameters`/`FunctionName`, `ResultSelector` e `ResultPath`, mas a listagem mostra
  também `InputPath` e `Next`, sem referência. Acrescentada uma frase: `InputPath`, fixo em `"$"`,
  entrega o documento de estado inteiro à task, e `Next` nomeia o passo seguinte — ambos emitidos
  igualmente para uma task `apigateway:invoke`. Confirmado em `AmazonCallRenderer.kt:44,63`
  (os dois ramos emitem o mesmo `InputPath`) e `:110` (`Next`).

Corrigida ainda a concordância na frase imediatamente anterior à adição do cap. 2 (*"the function
file the developer wish to deploy"* → *"wishes to deploy"*).

Compilado: 98 páginas (mais uma, do parágrafo do cap. 2), sem referências por resolver e com os
mesmos 8 avisos de *overfull hbox* do template.

### Dependência implícita do `!GetAtt` (assimetria no parágrafo de *deployment-time binding*)

Ainda no parágrafo do cap. 2, §2.5.1: o texto dizia que a referência do Terraform *"also creates an
implicit dependency between the two resources, so the function is created before the workflow that
names it"* e que o Pulumi *"likewise records a dependency"*, mas não dizia o mesmo do
CloudFormation, que é o primeiro exemplo da lista e faz exatamente isso. Lido em sequência, dava a
entender que a ordenação era uma vantagem do Terraform/Pulumi sobre o SAM.

Acrescentada uma frase no fim do bloco do SAM: a referência fixa a ordem do deployment, porque o
CloudFormation lê um `!GetAtt` a outro recurso como dependência implícita e cria a função antes da
state machine que substitui o ARN. Redigida de forma a não repetir textualmente a frase do
Terraform que vem três linhas abaixo.

Nova entrada na bibliografia, `awsCloudFormationDependsOn` (*DependsOn attribute*, AWS
CloudFormation Template Reference Guide), verificada na fonte: *"Dependent stacks also have
implicit dependencies in the form of target properties `!Ref`, `!GetAtt`, and `!Sub` … Resource B is
created before resource A."* Por ser entrada nova, foi preciso o procedimento do `defernumbers`
descrito em `CLAUDE.md` (apagar `.aux`/`.bbl`/`.fdb_latexmk` e refazer o ciclo pdflatex/bibtex),
senão era impressa como `[0]`: agora são 48 entradas citadas e 48 numeradas, 1 a 48 sem repetições.

Compilado: 97 páginas, sem referências por resolver e com os mesmos 8 avisos de *overfull hbox* do
template.

### Decomposição do ARN na nota `????` da pág. 37

Seguimento da nota `????` sobre `!GetAtt MyFunction.Arn` (cap. 2, §2.5.1): a frase acrescentada na
passagem anterior dizia *o que é* a expressão (uma intrínseca do CloudFormation que lê o atributo
`Arn`) mas não dizia *que valor* produz. Acrescentadas duas frases a seguir a essa:

- o valor é o ARN da função, com a forma
  `arn:aws:lambda:<region>:<account-id>:function:<name>` e a leitura campo a campo (prefixo fixo,
  serviço, região e conta onde a função foi criada, nome que lá recebeu);
- a razão de ser uma expressão e não um literal: a conta e o nome atribuído não são conhecidos
  quando o template é escrito, e `MyFunction` é o identificador lógico do recurso dentro do
  template, não o nome que a função acaba por ter.

A forma do ARN já estava no cap. 1 (§1.2, "Provider-Specific Function Identification and Invocation
Binding"), pelo que a passagem do cap. 2 remete para lá (`\S\ref{sec:int_cha_int_uni_multi}`) em
vez de reintroduzir o acrónimo. Mesma tipografia do cap. 1 (`\allowbreak` entre os campos).

Compilado: 97 páginas, 0 avisos novos de *overfull hbox* (mantêm-se os 8 do template, todos de
18,0 pt e já documentados abaixo) e nenhuma referência por resolver.

### Segunda ronda de revisão dos orientadores (`pedro-mst 1.pdf`, 7 notas novas)

O PDF entregue à tarde é o mesmo da manhã com sete notas acrescentadas, todas nos capítulos
Proposed Solution e Implementation (páginas 41, 42, 48, 49 e 52 do PDF; capítulos 4 e 5 na
numeração desse PDF, hoje 3 e 4 depois da fusão Background/Related Work).

- **Cap. 3 (`chapter3.tex`), introdução** — nota "dizer também como está organizado o capítulo":
  acrescentado um parágrafo de roteiro no fim da introdução, no mesmo formato do que o cap. 4 já
  tinha. Nomeia as quatro secções e o que cada uma faz, incluindo as duas subsecções (a cascata,
  §3.1.1, e o walkthrough, §3.4.1).
- **Cap. 3, §3.1.1 "The Resolution Cascade"** — nota "Step 3 in section xxx, em vez de above": a
  frase de abertura dizia "Step~(3) above"; passa a "Step~(3) of the end-to-end flow listed in
  Section~\ref{sec:components-architecture}", que nomeia a lista referida em vez de depender da
  posição na página.
- **Cap. 3, §3.3, Listing 3.1 (ficheiros de registry)** — nota "explica com algum detalhe": a
  listagem era introduzida por uma só frase ("A minimal JSON-based structure … is illustrated
  in…"). Passa a ter dois parágrafos que a leem: os dois campos de topo (`updatedAt`, `functions`
  como mapa), a chave (a referência simples, promovida a `"region/functionRef"` quando o mesmo
  nome existe em mais do que uma região) e os dois campos de cada valor (`serviceName`, `url`);
  depois o contraste entre os dois exemplos — `url` é o endpoint HTTPS no GCP, dividido em
  `host`/`path`, e o ARN na AWS, usado como recurso de uma invocação nativa, sendo a região
  contida no ARN o que mantém a validação de Nível 1 numa única chamada. Fica também dito o que
  *não* está no ficheiro (credenciais, parâmetros de deployment, chamadas com endpoint literal).
- **Cap. 3, §3.3, Listing 3.2 (call step com `internalFunction`)** — nota geral sobre figuras e
  código: a listagem só tinha a frase "Listing 4.2 shows a call step…". Acrescentado o parágrafo
  que explica o código linha a linha (o vocabulário comum a qualquer step, `method` e `result`
  mantidos, e sobretudo a ausência de `host`/`path`, substituídos pelo `functionRef` e pelo
  caminho do descritor, lido só se a cascata chegar ao Nível 3).
- **Cap. 3, §3.3, "QuickFaaS Deployment Descriptor and Code File"** — nota "tens de explicar o
  código": o parágrafo remetia para a Listing 2.1 sem dizer o que lá está. Passa a descrever os
  campos do descritor em três grupos (onde é feito o deployment: `cloudProvider`, `project`,
  `accessToken`; o que é criado: `function.name`, `location`, `runtime`, `trigger`, `bucket`; e o
  código: `functionFile`), seguidos dos dois campos que importam à integração — `function.name`
  tem de ser igual ao `functionRef` (com a remissão para a consequência de divergirem, no fim de
  §3.4.1) e `function.location` fixa a região que a entrada do registry vai apontar. Verificado
  contra `QuickFaasDescriptor.kt` e contra os descritores de exemplo em
  `omni-flow-main/functions/`; acrescentada a ressalva de que na AWS o `accessToken` fica vazio,
  porque `AwsRequests.kt:33` usa `EnvironmentVariableCredentialsProvider`.
- **Cap. 4 (`chapter5.tex`), §4.1** — as duas notas "rever" marcavam dois defeitos de composição
  na mesma frase: `\texttt{Google-\linebreak[4]CloudDeployer}` imprimia o nome da classe partido
  com hífen entre linhas (como se a classe se chamasse `Google-CloudDeployer`), e
  `\texttt{FunctionInvocationMetadata}` transbordava para a margem (*overfull hbox*). Os dois
  nomes passam a usar `\allowbreak`, como o resto do capítulo, e a frase — que acumulava dois
  parêntesis encaixados — foi dividida em duas: a primeira nomeia os dois colaboradores, a segunda
  descreve o que o store persiste. Sem alteração de conteúdo.

Nota de âmbito: a nota sobre figuras e código está marcada sobre o título do capítulo Proposed
Solution e foi aplicada aí (as três figuras do capítulo já tinham texto descritivo; faltava-o nas
duas listagens). As figuras dos restantes capítulos não foram revistas nesta passagem.

### Varredura de *overfull hbox* em todo o documento

Na sequência das duas notas "rever" (que marcavam um nome de classe partido com hífen e um
identificador a entrar pela margem), varri o log de compilação inteiro. Havia 23 avisos de
*overfull hbox*; ficaram 8. Todos os casos de texto foram corrigidos; os 8 que restam são do
template e não imprimem nada na margem (ver no fim).

- **`template.tex` (zona "USER CUSTOMIZATION")** — duas definições novas, que são o remédio de
  fundo:
  - `\setlength{\emergencystretch}{1em}`: dá ao TeX uma passagem final mais folgada nos parágrafos
    que não consegue quebrar de outra maneira, em vez de deixar a linha entrar pela margem.
    Resolve sozinho quatro parágrafos. Testei 0/1/1,5/2/3~em: 1~em é o valor mais pequeno que
    resolve estes casos sem aumentar o número de linhas *underfull* (mantém-se em 6, como antes;
    com 3~em subia para 9).
  - `biburllcpenalty`/`biburlucpenalty`/`biburlnumpenalty`: deixam o biblatex quebrar URLs longos
    dentro das palavras e não só na pontuação. Corrige a entrada `openFaaSTriggers`, cujo URL
    terminado em `#cloudevents` passava 1,9~pt da margem.
- **Pontos de quebra em identificadores longos** (`\allowbreak`, como no resto do texto):
  `https://<region>-<project-id>.cloudfunctions.net/<name>`, `body(variable("args.data"))` e
  `result("firstResult")` no cap. 2; `InternalFunctionDeployer` e
  `internalFunction("fraud-check", "./deploy/fraud-check.json")` no cap. 3;
  `internalFunction(name, deploymentDescriptorPath)`, `"region/functionRef"` e `AwsLambdaDeployer`
  no cap. 4; `AverageTime` e `DescribeRegions` no cap. 6; `internalFunction(name,
  deploymentDescriptorPath)` no cap. 7. Onde o `\emergencystretch` já resolvia, não acrescentei
  quebra, para não partir nomes ao meio sem necessidade.
- **Cap. 6 (`chapter7.tex`), bloco do T5:** a tabela de quatro colunas era mais larga do que a
  `minipage` de `0.31\textwidth` que a contém (7,9~pt a mais). Reduzido o `\tabcolsep` para 4~pt
  dentro dessa `minipage`, mantendo a geometria igual à dos outros blocos T.
- **Entradas da lista de figuras:** as legendas da figura do modelo do *call step* (cap. 3) e da
  T12 (cap. 6) transbordavam na *List of Figures*, por causa de `internalFunction` e do grupo
  matemático `$I{=}40$/$E{=}10$/$F{=}10$`. As duas passam a ter legenda curta
  (`\caption[...]{...}`), o que encurta a entrada da lista e deixa a legenda da figura intacta.

Os 8 avisos que ficam são todos do cabeçalho de capítulo do template (`chapstyle=isel`): a
`tabular` do título em `ISELthesis-files/Chap-Styles/isel.ldf` arrasta 18~pt de `\tabcolsep` para
além da caixa de texto. São espaço em branco — o título não chega a entrar na margem — e o ficheiro
é do template, que não se edita.

### Correções da revisão dos orientadores (notas da pág. 37 do PDF revisto)

- **Cap. 2 (`chapter2.tex`), secção 2.5.1 "Infrastructure-as-Code and Deployment-Oriented
  Tooling":** reescrito o parágrafo sobre a resolução de referências entre recursos em tempo de
  deployment, em resposta a três notas ("????", "??" e "Section 4.1.1??") que assinalavam
  fragmentos de sintaxe sem explicação e uma referência cruzada opaca.
  - Frase introdutória nova a dizer o que estes mecanismos têm em comum (uma expressão que o motor
    de deployment substitui pelo identificador da função) antes de mostrar exemplos.
  - `!GetAtt MyFunction.Arn` passa a ser identificado como expressão CloudFormation que lê o
    atributo `Arn` do recurso `MyFunction`; `Fn::GetAtt: [hello, Arn]` como a forma longa da mesma
    intrínseca; `${aws_lambda_function.lambda.arn}` como o atributo `arn` do recurso Lambda
    `lambda`. Acrescentada a consequência da dependência implícita no Terraform (a função é criada
    antes do workflow).
  - A referência final deixa de ser um `\S\ref` isolado: passa a nomear o que referencia ("the
    resolution cascade proposed in this dissertation and described in §3.1.1").

### Releitura da secção 2.5 "Related Work" (nota 3 da revisão: "relê o texto todo, vê o CODE")

- **Cap. 2 (`chapter2.tex`), parágrafo de abertura da 2.5:** acrescentado o roteiro da secção — os
  quatro grupos (IaC/deployment, camadas de execução portáveis e transformação de código, métodos
  que medem portabilidade, frameworks de composição e orquestração) passam a ser nomeados antes das
  subsecções. Antes o texto dizia "each group" sem nunca ter dito quais eram. A sigla IaC passa a
  ser expandida na primeira ocorrência.
- **Parágrafo do CODE (2.5.1):** reescrito contra o artigo original (Ristov et al., FGCS 160, 2024,
  open access). A descrição anterior ("integração com sistemas de armazenamento e automação do
  movimento de pacotes de deployment") era vaga e não dizia qual é a ideia central. Passa a
  descrever: o alvo (*federated FaaS* — a mesma função em várias regiões de vários fornecedores), a
  hierarquia de três níveis função → fornecedores → regiões, a redução de até 9,23× em linhas de
  código face ao Terraform e ao Serverless Framework, e a biblioteca de storage unificada que copia
  o pacote entre AWS e GCP. O posicionamento face à tese passa a ser sustentado pelo próprio artigo:
  o CODE faz deployment das funções que um workflow invoca, não do workflow, e os autores listam o
  deployment automático de workflows da sua linguagem de coreografia como trabalho futuro.
- **Erros que quebravam as frases:** "packag" → "package" (Pulumi); "language template"/"trigger
  mechanism" → plural (OpenFaaS); "into FaaS deployment" → "deployments"; "into AWS Lambda
  function" → "functions".
- **Repetição:** no parágrafo da ligação em tempo de deployment, a segunda frase começava outra vez
  por "These tools also"; passa a "They also".
- **2.5.3 (SEAPORT):** as famílias estavam numeradas "first", "second", …, "final", sem terceira.
  Acrescentada a frase de abertura ("A third group steps back from enabling portability to
  *measuring* it") e fundida com a frase seguinte, que repetia o sujeito e a ideia de "measuring
  portability".

### Via para fechar a limitação da 1.ª geração no GCP (trabalho futuro)

- **Cap. 7 (`chapter8.tex`), secção 7.2 "Future Work", ponto 1:** o ponto propunha um *cliente
  Cloud Functions v1* para validação, descoberta e bootstrap. Passa a propor o oposto: mover o
  provider GCP do QuickFaaS para a **API Cloud Functions v2**. O argumento é estrutural — uma
  função de 2.ª geração *é* um serviço Cloud Run, logo a validação, a descoberta e o bootstrap já
  construídos sobre a API do Cloud Run passam a cobri-la sem código novo, e a heurística que
  identifica a 1.ª geração pelo URL (que o endpoint `cloudfunctions.net` da 2.ª geração derrota,
  `gcpFunctionsVersionComparison`) deixa de ser precisa. O ponto passa a nomear também o custo, que
  recai todo no QuickFaaS: corpo do pedido reestruturado, URL `run.app` lido da função em vez de
  composto a partir do projeto e da região, permissões de invocação na Cloud Run Admin API, e o
  template do trigger de storage a adoptar a interface CloudEvents. A via do cliente v1 fica
  referida como alternativa, com a sua desvantagem (obrigaria a distinguir as duas gerações em
  tempo de resolução).
- **Cap. 4 (`chapter5.tex`), parágrafo "Limitation: first-generation Cloud Functions on GCP":** a
  frase final nomeava a mesma via v1; passa a nomear a via v2, para não contradizer o ponto de
  trabalho futuro que referencia.
- Sem alterações ao código: a limitação descrita continua a ser a do código actual. A via v2 chegou
  a ser implementada e testada localmente num ramo à parte (`experiment/gcf-gen2`), mas não foi
  possível validá-la contra o GCP (faturação encerrada), pelo que permanece trabalho futuro.

### Geração das Cloud Functions no GCP (nota 2 da revisão: "rever isto")

- **Cap. 2 (`chapter2.tex`), secção 2.3.3 "FaaS Deployment Model: The ZIP Strategy":** revisto o
  parágrafo sobre o que o QuickFaaS provisiona no GCP. Confirmado contra o código
  (`GcpRequests.kt:48-70` usa `cloudfunctions.googleapis.com/v1`; `GcpFunction.kt:62` constrói
  `https://$location-$projectId.cloudfunctions.net/$name`) e contra a documentação actual da Google.
  - **Nomes desactualizados:** o texto dizia que o Cloud Run é "also marketed as second-generation
    Cloud Functions". A Google renomeou ambas as gerações: a primeira é hoje *Cloud Run function
    (1st gen)* e a segunda é *Cloud Run function*, ou seja, um serviço Cloud Run feito a partir de
    código-fonte. O parágrafo passa a registar a renomeação e a justificar porque é que a
    dissertação mantém os nomes por geração (é o que distingue as duas APIs que o código chama).
  - **URL mais preciso:** `<project>` → `<project-id>`, que é o que o código usa (`projectId`).
  - **Ressalva nova:** o domínio `cloudfunctions.net` não prova que a função é de 1.ª geração — uma
    função criada pela API v2 é servida em `run.app` mas mantém também um endpoint
    `cloudfunctions.net`. Isto é relevante porque o resolver identifica a 1.ª geração exactamente
    por esse domínio.
- **`Bibliography/bibliography.bib`:** nova entrada `gcpFunctionsVersionComparison` (documentação
  "Compare Cloud Run functions"). Como o biblatex corre com `defernumbers=true`, a entrada saiu
  como `[0]`; reconstruída a bibliografia de raiz (apagar `.aux`/`.bbl`/`.fdb_latexmk` + pdflatex,
  bibtex, pdflatex ×2). Ficam 47 entradas citadas, nenhuma a `[0]`.

### Referência cruzada solta no cap. 2 (mesmo defeito da nota 6)

- **Cap. 2 (`chapter2.tex`), §2.1.3.3:** "This distinction becomes relevant later in the
  dissertation (§4.1.6)" passa a "…relevant later, where the registry store and resolver internals
  are described (§4.1.6)". Era a última ocorrência no capítulo do padrão que o orientador marcou
  com "Section 4.1.1??": um número de subsecção para a frente sem dizer o que lá está.

### Cap. 2 reestruturado em duas secções (nota 1b da revisão)

- **Cap. 2 (`chapter2.tex`):** o capítulo passa a ter exactamente **duas secções**, como pedido:
  **2.1 Background** (nova, `\label{sec:background}`) e **2.2 Related Work**. As quatro secções de
  background passaram a subsecções de 2.1 (Serverless Computing and FaaS; Workflows and
  Orchestration; QuickFaaS; OmniFlow) e as 7 subsecções que tinham passaram a subsubsecções
  (2.1.3.1 … 2.1.4.3). A Related Work e as suas quatro subsecções ficaram como estavam, agora
  numeradas 2.2.x. Nenhum rótulo foi renomeado.
- **Parágrafo inicial do capítulo reescrito.** O `6377afa` tinha feito a junção dos capítulos mas
  não tocou no parágrafo de abertura (só trocou "Chapter~\ref" por "Finally, Section~\ref"), que
  continuava a anunciar um capítulo só de background apesar do título prometer "Background **and
  State of the Art**". Passa a abrir com "This chapter has two parts" e a anunciar as duas secções.
- **Referências cruzadas:** três ocorrências de `Section~\ref{sec:background_quickfaas}` (caps. 2,
  5 e 6) passaram a `\S\ref{...}`, por o alvo ser agora uma subsecção. As outras referências para
  o capítulo 2 já usavam `\S\ref` ou apontam para a 2.2, que continua a ser secção.
- **Índice:** não foi preciso mexer. A classe já lista até ao nível de parágrafo, por isso as
  subsubsecções novas aparecem no índice sem qualquer alteração de configuração (confirmado por
  compilação com e sem `\settocdepth`); `template.tex` ficou intacto.

## 2026-09-16

### Nova secção 1.5 "Contributions" no capítulo 1

- **Cap. 1 (`chapter1.tex`):** nova secção **1.5 "Contributions"** (`sec:int_contributions`),
  entre "Research Questions and Objectives" e "Structure of the Work". Enumera as cinco
  contribuições (modelo de chamada interna/externa; registo de funções e cascata de resolução;
  suporte AWS Lambda no QuickFaaS; protótipo e caso de estudo; avaliação) e termina com um
  parágrafo sobre o artigo publicado com os orientadores, "Towards Cloud-Agnostic Serverless
  Applications: Unifying Function Deployment and Workflow Orchestration"
  (`\cite{carvalho2026towards}`), distinguindo o que o artigo cobre do que a dissertação
  acrescenta.
  O parágrafo nomeia a conferência: Iberian Conference on Information Systems and Technologies
  (CISTI).
- **`Bibliography/bibliography.bib`:** nova entrada `carvalho2026towards` (@inproceedings), CISTI
  2026, IEEE. Falta preencher o `note` (local, páginas e DOI), marcado com `TODO`.

## 2026-09-15

### Capítulos 2 e 3 juntos num só capítulo

- **Cap. 2 (`chapter2.tex`):** os capítulos "Background" e "Related Work" passam a ser um único
  capítulo, com o título **"Serverless Portability: Background and State of the Art"**.
  - "Related Work" passa a ser a secção 2.5. As suas quatro secções passam a subsecções (2.5.1 a
    2.5.4): Infrastructure-as-Code…, Portable Execution Layers…, Assessing Portability…, Workflow
    Composition….
  - Mantêm-se as etiquetas `cha:background` (capítulo) e `ch:related-works` (agora na secção
    2.5).
  - Introdução do capítulo: "Chapter~\ref{ch:related-works} reviews…" passa a "Finally,
    Section~\ref{ch:related-works} reviews…".
  - Introdução da secção Related Work: "This chapter reviews…" e "The remainder of this chapter…"
    passam a "This section…".
- **Renumeração:** os capítulos seguintes descem um número: Proposed Solution 4→3,
  Implementation 5→4, Case Study 6→5, Evaluation 7→6, Conclusions 8→7.
- **Cap. 1 (`chapter1.tex`), "Structure of the Work":** os pontos dos capítulos 2 e 3 passam a um
  só ponto, e os números dos capítulos seguintes foram atualizados.
- **Cap. 3 (`chapter3.tex`, Proposed Solution):** a referência "(Chapter~\ref{ch:related-works})"
  passa a "(Section~\ref{ch:related-works})".
- **`Config/_files.tex`:** atualizados os comentários com os números dos capítulos.
