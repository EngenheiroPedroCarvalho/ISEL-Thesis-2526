# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `1b2fcf5` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-12

### Capítulo 5 (Case Study) — cenário generalizado, sem regulação

- `Chapters/chapter6.tex`: título passou a "Case Study: A Payment Authorization Workflow". O
  cenário deixou de ser bancário: "a bank"/"fraud team" passaram a "an organization"/"the same team
  as the workflow", e "core banking" passou a "a downstream system" (texto, legendas das Figuras
  5.1 e 5.2; o nome `core-banking-stub` é referido uma vez, por aparecer nas capturas).
- Secção 5.1: o requisito "An exit strategy" (DORA, Arts. 28(8) e 29, `\cite{dora2022}`) foi
  substituído por "Portability across providers", sem referência a regulação. Os parágrafos
  "GCP" (5.3) e "Exit strategy" (5.5, agora "Portability") foram ajustados em conformidade.
- Secção 5.5, parágrafo final: "a bank would not accept…" passou a "a production deployment would
  not accept…".
- Capítulo 1, Structure of the Work (`Chapters/chapter1.tex`): descrição do Capítulo 5 atualizada
  para o novo título.
- Glossário (`Chapters/glossary.tex`): removidas as entradas "ICT concentration risk" e "ICT
  services", que citavam a DORA. Acrónimos (`Chapters/acronyms.tex`): removidos DORA e ICT, que só
  serviam essas entradas. A referência `dora2022` deixou de ser citada e sai da bibliografia (fica
  no `.bib`); a numeração das referências foi regenerada.
- Apêndice B (`Chapters/appendix-listings.tex`): legendas "Core banking stub on GCP/AWS" passaram
  a "Downstream system stub on GCP/AWS", e o texto sobre as definições renderizadas (introdução e "Rendered for
  AWS") diz "the downstream system" em vez de "core banking". O código das listagens (nomes,
  hosts, comentários) não mudou, por ser o que correu nos fornecedores.

### Capítulo 4 (Implementation) e Apêndice A — diagrama de classes do registry

- Figura 4.2 (`images/implementation/omniflow-classes-registry.png`) e diagrama completo do
  Apêndice A (`omniflow-integration-classes.png`): acrescentado `tryResolveEntry(ref)` à classe
  `FunctionRegistryStore`, que a Secção 4.1.3 descreve a par de `tryResolveEntryIn()` mas que
  faltava nas figuras. Fontes PlantUML em `../diagrams/`.
- Secção 4.1.3 (`Chapters/chapter5.tex`): "each read re-parses the file" passou a "each read reads
  the whole file and rebuilds the map".
- Secção 4.1.5 Evolution of the Registry Design (`Chapters/chapter5.tex`): o parágrafo com
  "First/Second/Third" passou a lista numerada de três itens; o conteúdo não mudou.

### Capítulo 6 (Evaluation) e Apêndice C — gráficos T1 e T5 no corpo do capítulo

- Secção 6.4 Resolution Overhead and Scalability (`Chapters/chapter7.tex`): os gráficos de T1
  (custo da resolução, chamadas internas vs. externas) e de T5 (leitura única vs. por chamada, em
  função de $R$) passaram do Apêndice C para a secção, como Figuras 6.1 e 6.2. O parágrafo de T1
  remete para a figura e para a tabela do apêndice; o de T2–T5 passou a citar a Figura 6.2.
- Apêndice C (`Chapters/appendix-benchmarks.tex`): ficam só as tabelas de T1 e T5, com uma frase
  a remeter para as figuras na Secção 6.4. As figuras do apêndice passam a ser C.1–C.3 (T2, T10,
  T14).
- Legendas das Figuras 6.1 e 6.2 encurtadas para uma linha. A informação que tinham passou para o
  texto da Secção 6.4: o parágrafo de T1 descreve a experiência (resolver de referência, $N$
  chamadas todas internas ou todas externas, $R{=}1$) e diz que as duas linhas partem do mesmo
  ponto e que a externa fica quase plana; o parágrafo de T2–T5 descreve T5 ($N{=}50$, $F{=}10$,
  resolver de referência) e a correspondência entre "without/with cache" no gráfico e
  per-call/single read.

### Capítulo 1 (Introduction) — textos introdutórios

- `Chapters/chapter1.tex`: acrescentado um parágrafo introdutório às Secções 1.2.1, 1.2.2, 1.2.3,
  1.4, 1.5 e 1.6, antes das respetivas listas. Na 1.2.3 substitui a frase que lá estava ("When a
  referenced function does not exist…"); na 1.4 substitui "Two research questions guide the
  work:" e liga as questões aos desafios (1.2) e à motivação (1.3); na 1.5 substitui "The
  contributions of this dissertation are the following:" e liga as contribuições aos objetivos
  (1.4). O conteúdo das listas não mudou.
