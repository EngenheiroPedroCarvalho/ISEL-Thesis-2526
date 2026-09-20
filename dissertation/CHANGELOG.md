# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `83beba1` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-20

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
