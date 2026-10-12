# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `e4c761e` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-12

### Capítulo 6 (Evaluation) — porque se usa um resolver de referência

- Secção 6.3 Methodology (`Chapters/chapter7.tex`): acrescentadas as duas razões para T1, T2, T4,
  T5 e T10–T14 usarem o resolver de referência: os resolvers de produção validam cada hit no
  provider (medi-los exigiria pedidos de rede ou test doubles, e o ponto de substituição muda o
  trabalho local medido, como em T6/T7); e só leem o registo uma vez, pelo que comparar as duas
  formas de leitura exige dois resolvers que diferem apenas nisso e produzem o mesmo workflow.
- Posição das Figuras 6.1 e 6.2: estavam ambas no topo da página a seguir à Tabela 6.2, longe do
  texto que as discute. Passaram a `[!htb]` com 70% da largura do texto e foram colocadas junto
  aos parágrafos: a 6.1 logo após a lista das três operações do parágrafo de T1, a 6.2 logo após a
  frase que a cita no parágrafo de T2–T5 (o texto seguinte passou a `\noindent`).
