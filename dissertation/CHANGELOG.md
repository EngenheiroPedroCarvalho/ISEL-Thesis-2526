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
