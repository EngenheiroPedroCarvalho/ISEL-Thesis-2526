# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `13430da` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-11

### Cap. 6 (Evaluation), `Chapters/chapter7.tex`: Tabelas 6.1 e 6.2 mais claras

- **Tabela 6.1** (`tab:eval-correctness`): colunas renomeadas para *Situation set up by the test*
  e *Expected outcome*; cada célula passa a ser uma frase completa (o que existe no registo, o que
  o provider responde, o que acontece). As linhas que juntavam várias situações separadas por `;`
  foram divididas (chamadas aninhadas, chamadas externas, leitura única, registo em falta, entrada
  `cloudfunctions.net`). Legenda encurtada: "Resolver unit tests by ladder level (`--`: whole workflow), on AWS and GCP
  unless marked otherwise. Every test passes."
- **Tabela 6.2** (`tab:eval-summary`): coluna *Varies* substituída por *What is measured*, que diz
  o que cada experiência mede; os resultados dizem o que comparam (p.ex. "single read 38–58×
  faster"). Legenda encurtada: remete para a Tabela C.1 (notação) e para o Apêndice C. Números
  inalterados.

### Cap. 6 (Evaluation), `Chapters/chapter7.tex`: tabelas junto da sua referência

- As Tabelas 6.1–6.3 passam a `[!htbp]`, e o capítulo sobe `\topfraction` (0.9),
  `\textfraction` (0.1) e `\floatpagefraction` (0.8), repostos no fim do capítulo. A Tabela 6.1
  deixa de ficar sozinha numa página de floats e fica no topo da p. 37, logo a seguir à referência
  (p. 36), com texto por baixo. A Tabela 6.2 passa da p. 40 para o topo da p. 39, logo a seguir à
  referência (p. 38).
- Colunas da Tabela 6.2 ligeiramente mais estreitas (0.39/0.42), para eliminar um overfull de 5 pt.

### Cap. 6 (Evaluation), `Chapters/chapter7.tex`: correspondência por sufixo (T10)

- Secção 6.4, parágrafo T2–T5: "A suffix match ("region/name") scans the loaded map, but remains
  cheap" reescrito. Explica que o varrimento acontece quando as chaves do registo incluem a região
  e a chamada usa só o nome, e dá o custo em vez de "cheap": no máximo 1,95× a procura exata,
  391 µs para 50 procuras com R=1000 (menos de 8 µs por chamada).
- Tabela 6.2, linha T10: deixa claro que a chamada usa o nome simples e que o que muda é a forma
  das chaves do registo (`name` vs `region/name`).

### Cap. 6 (Evaluation), `Chapters/chapter7.tex`: título da Secção 6.5

- "Production Resolvers and Write Path" passa a "Cost of the Production Resolvers and of Registry
  Writes". Label `sec:eval-real-resolvers` inalterado.
- Apêndice C (`Chapters/appendix-benchmarks.tex`): "Registry Write Path (T8, T9)" passa a
  "Registry Writes (T8, T9)".
- "write path" substituído por "registry writes" no resto do texto: abertura da Secção 6.4
  (`chapter7.tex`), título de parágrafo "Registry writes (T8, T9)" na 6.5 e lista de contribuições
  do Cap. 1 (`Chapters/chapter1.tex`).
- Na mesma frase da Secção 6.4, "the registry read path" passa a "registry reads".

### Cap. 6 (Evaluation), `Chapters/chapter7.tex`: custo das gravações no registo (T8)

- Secção 6.5, parágrafo "Registry writes": "K writes cost K times one write, and one write grows
  with the file" reescrito. Explica que cada `put()` lê e reescreve o ficheiro inteiro (uma
  gravação: 45 µs com o registo vazio, 490 µs com 1 000 entradas) e que as gravações não são
  agrupadas, pelo que o tempo cresce linearmente com o número delas (100 gravações: 5,9 ms com o
  registo vazio, 36 ms com 1 000 entradas). A igualdade "K vezes uma gravação" não batia com os
  números da Tabela C.8.
- Reposta a comparação com a resolução, agora com números: uma leitura com uma procura custa
  10,5 µs com uma entrada e 183 µs com 1 000 entradas (T2), contra 45 µs e 490 µs de uma gravação.
- "Writes are not batched" substituído: são os resolvers que registam cada função com o seu próprio
  `put()` (K funções = K leituras e reescritas do ficheiro); o bootstrap grava todas as entradas de
  uma vez (`writeNew`).

### Cap. 6 (Evaluation), `Chapters/chapter7.tex`: validação por chamada (Secção 6.6)

- "A registry hit is validated on every internal call, not once per function: the snapshot removes
  repeated file reads, not live lookups…" reescrito sem jargão ("registry hit", "live lookups"):
  explica que, se o registo tem entrada para a função chamada, o resolver confirma junto do provider
  que a função existe e lê o endpoint atual, em cada chamada interna, que o snapshot evita reler o ficheiro mas não regista que funções o provider já confirmou, e que $I$
  chamadas à mesma função enviam $I$ pedidos de validação.
- "The region list is fetched at most once per deployment, lazily, and only calls that miss pay a
  region scan" reescrito sem jargão ("lazily", "miss"): a lista de regiões só é pedida quando uma
  chamada precisa de procurar nas regiões, no máximo uma vez por deployment; só pagam a procura
  (até um pedido por região) as chamadas sem entrada no registo e as que têm uma entrada obsoleta,
  que antes ficavam de fora.

### Cap. 6 (Evaluation), `Chapters/chapter7.tex`: legenda da Tabela 6.4

- Legenda encurtada para "Developer steps to make one function available to one workflow on GCP."
  A definição de "carried by hand" saiu da legenda; o texto da Secção 6.7 já a explica e enumera os
  quatro valores.

### Cap. 6 (Evaluation), `Chapters/chapter7.tex`: Secções 6.7 e 6.8 juntas

- "Manual Effort Before and After" (6.7) e "Packaging Overhead (S1)" (6.8) passam a uma só Secção
  6.7, "Developer Effort and Packaging Overhead", com dois parágrafos com título: "Manual effort
  (Table 6.4)" e "Packaging overhead on AWS (S1)". Texto e tabelas inalterados.
- Label `sec:eval-bundle` removido; a secção mantém `sec:eval-manual-effort`. Discussion e Threats
  to Validity passam a 6.8 e 6.9.
- Secção 6.1: os dois últimos pontos da lista juntos num só, que aponta para a 6.7.
- Tabelas 6.4 e 6.5 passam a `[!htbp]`: ficam juntas no topo da p. 42, logo a seguir à secção que
  as refere (p. 41); a 6.5 deixa de ficar no fundo da página, a meio da Discussion.

### Cap. 6 (Evaluation), `Chapters/chapter7.tex`: Threats to Validity, "Later sessions"

- Reescrito o ponto "Later sessions": volta a dizer de onde vem o "about 10%" (T1 corrida duas
  vezes: uma leitura do registo custou 9,8 µs e depois 10,7 µs; o dado tinha saído da Secção 6.3 em
  `4ca8bc7`) e troca "their comparison … is an order-of-magnitude one" por "differences of that size
  … may come from the change of session rather than from the code being measured".
- Ponto "Abort paths not run live" passa a "Aborts not tested on the real providers": diz que as
  situações em que a resolução aborta foram testadas só com test doubles, não contra as APIs reais.
