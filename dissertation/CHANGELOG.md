# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `3743131` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-10

### Cap. 6 Evaluation (`Chapters/chapter7.tex`): parágrafos longos passados a listas

- **Scope and Goals:** o parágrafo de abertura foi encurtado e o mapeamento pergunta → evidência →
  secção passou a uma lista.
- **Correctness of the Resolution Ladder:** os quatro casos de registry alterado fora do OmniFlow
  passaram a uma lista; o conteúdo não mudou.
- **Methodology:** as definições do JMH (modo, `Blackhole`, forks, warm-up, medição, máquina,
  intervalo de confiança) passaram a uma lista. Foi removido o parágrafo que dizia que T1, T6 e T7
  vieram de uma corrida posterior e que T1 foi repetido (10,7 µs vs. 9,8 µs, concordância de ~10%).
  A definição dos símbolos $N$, $I$, $E$, $F$, $R$, $K$ e $R_0$ (e a nota "R is not F") foi
  substituída por uma remissão para a tabela de notação do Apêndice C (`tab:eval-notation`).
- **Resolution Overhead and Scalability:** o parágrafo introdutório foi reescrito: diz quais
  experiências a secção cobre (T1–T5, T10–T14) e quais ficam para a seguinte (T6–T9), e anuncia as
  duas conclusões (antes dizia "Three findings matter", mas só se seguiam dois parágrafos).
- **Resolution Overhead and Scalability:** a tabela e a figura de T1 (antes Tabela 6.3 e Figura 6.1)
  passaram para o Apêndice C (`Chapters/appendix-benchmarks.tex`), numa secção nova "Resolution
  Overhead (T1)" antes de T2–T5, com a tabela por cima do gráfico como nas outras experiências. O
  parágrafo de T1 no capítulo remete agora para o apêndice. As tabelas seguintes do capítulo
  passaram de 6.4–6.6 para 6.3–6.5, e o capítulo deixou de ter figuras.
- **T1 (capítulo e Apêndice C):** "before walking the tree" passou a "before it traverses the
  workflow's steps", para não pressupor que o leitor sabe que o workflow é uma árvore.
  Na Methodology, "the walk over the workflow" passou a "the traversal of the workflow's steps".
  No parágrafo de T1, "the production deployers" passou a "the provider deployers
  (`GoogleCloudDeployer` and `AmazonCloudDeployer`, Chapter 4)", o termo usado no Cap. 4.
- **Resolution Overhead and Scalability, parágrafo T2–T5:** "costs $N$ reads of an $R$-entry file;
  a single read per resolution makes the cost flat in $N$" passou a "$I$ reads" (só as chamadas
  internas leem o registry, `NaiveEndpointResolver.kt`) e "replaces them with one read plus a small
  constant per call" (o custo não é constante em $N$: T1 mostra ~0,23 µs por chamada interna).
- **Resolution Overhead and Scalability, parágrafo T1:** o parêntese "(snapshot lookup, URL split,
  rebuilt call node)" passou a uma lista que explica as três operações em memória por chamada
  interna (procura no mapa, independente de $R$; divisão do URL em `host`/`path`; cópia do nó, porque
  o modelo é imutável). Diz também que os 0,23 µs são a inclinação da reta e porque é que uma chamada
  externa custa só 0,005 µs.
- **Production Resolvers and Write Path, T6/T7:** "A 200-call workflow with ten functions" passou
  a "A workflow of 200 internal calls to ten distinct functions" (todas as chamadas são internas,
  $F{=}10$).
  O mesmo no parágrafo de T1 ("A 200-call workflow costs 56.4 µs" → "A workflow of 200 internal
  calls costs 56.4 µs") e na resposta à RQ2 da Discussion ("for a 200-call workflow" → "for a
  workflow of 200 internal calls").
- **Production Resolvers and Write Path, T6/T7:** a descrição dos fakes passou a dizer que T6
  substitui o inspector da Lambda inteiro e por isso exclui o trabalho local do SDK, enquanto T7
  mantém o inspector real do Cloud Run e substitui só o cliente HTTP.
- **Threats to Validity:** os dois parágrafos passaram a uma lista com as mesmas quatro ameaças.
  O item "Later sessions" passou a dizer que T1, T6 e T7 usaram as mesmas definições e a mesma
  máquina e que um T1 repetido concorda em cerca de 10% (informação antes na Methodology).

### Cap. 4 Implementation (`Chapters/chapter5.tex`): construção dos inspectors

- No parágrafo que apresenta os dois resolvers, acrescentada a frase "The Cloud Run inspector takes
  its HTTP client as a constructor argument; the Lambda inspector is an interface whose
  implementation builds an SDK client per lookup.", que suporta a explicação de T6/T7 no Cap. 6.
