# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `ff13abf` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-16 — Portabilidade das funções no AWS

- `chapter2.tex` (Cap. 2, Related Work): a frase de posicionamento deixa de dizer que este trabalho
  usa "functions written against a provider-agnostic model"; passa a falar de funções implantadas
  por uma ferramenta portável e acrescenta aos limites que o código das funções é portável entre
  GCP e Azure, mas não para o AWS. Na Tabela 2.1, a coluna "Func. portab." deste trabalho passa de
  Y para P, e a nota (a) explica porquê.
- `chapter5.tex` (Cap. 4): o parágrafo do provider AWS do QuickFaaS diz que o `AwsHttpTemplate`,
  ao contrário dos templates GCP e Azure, não usa um adaptador para `HttpRequestQf`/`HttpResponseQf`,
  pelo que o hook AWS tem assinatura própria.
- `chapter8.tex` (Cap. 7, Future Work): novo item, um adaptador AWS para os tipos genéricos do
  QuickFaaS sobre o payload do `lambda:invoke` (um hook para todos os fornecedores e body nas
  chamadas internas no AWS).
- `../article/article.tex`: a mesma frase de posicionamento, FP = P com nota (c) na tabela, e o
  adaptador AWS no trabalho futuro.

## 2026-10-16 — Afirmações técnicas alinhadas com a implementação

Revisão do texto contra o código atual; só texto, o código não mudou.

- `chapter3.tex` (Cap. 3): o parágrafo do descritor diz que `function.location` fixa a região só no
  GCP (no AWS manda a região do bucket S3); o Nível 1 diz que a redescoberta procura só na região
  de uma referência `"region/functionRef"`; "observable" passa a dizer o que é de facto
  reportado (descoberta e deployment na consola, entrada atualizada com aviso, acerto sem
  alterações só em debug).
- `chapter5.tex` (Cap. 4): o `AwsHttpTemplate` é "the AWS template" (não "provider-agnostic") e
  passa os query parameters ao hook como mapa; o `AwsLambdaDeployer` recebe uma região e o role do
  Step Functions, que ainda não usa; a permissão de invocação é uma resource-based policy para
  `states.amazonaws.com`, não restrita à state machine nem ao role.
- `chapter6.tex` (Cap. 5): o limite 1 diz que a referência sem região só aborta se a função não for
  encontrada nas regiões acessíveis.
- `chapter7.tex` (Cap. 6): na Tabela 6.1, a entrada stale não procura noutras regiões com uma
  referência `region/name`; o S1 fala de "QuickFaaS layer" em vez de "provider-agnostic layer"
  (texto, legenda e linhas da tabela S1, e o item do âmbito).
- `chapter8.tex` (Cap. 7): "provider-agnostic layer" → "QuickFaaS layer".
- `glossary.tex`: *Hook function* e *Template function* distinguem GCP/Azure (tipos genéricos do
  QuickFaaS) de AWS (mapa de query parameters); *Stale entry* ressalva a referência com região.
- `abstract-en.tex` / `abstract-pt.tex`: "tens of microseconds" → "tens to hundreds of
  microseconds" ("dezenas a centenas de microssegundos"), porque o resolver GCP leva 199 µs; no
  Resumo saem "assim" e "mínimo," para manter o limite de palavras.
- `../article/article.tex`: "AWS template `AwsHttpTemplate`" e "QuickFaaS layer", como na
  dissertação.

