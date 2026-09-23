# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `49f4102` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-23

### Mais repetições retiradas (`Chapters/chapter5.tex`, `chapter7.tex`)

- Cap. 4, §"Evolution of the Registry Design" / "A metadata record per function": deixa de repetir
  o que os resolvers fazem com o snapshot (já dito em §4.1.6) e remete para lá; fica com o que lhe
  é próprio, o `FunctionInvocationMetadata` e a fixture `OptimizedEndpointResolver`.
- Cap. 4, §4.1.7 "Resolution Cascade Sequences": os três parágrafos que voltavam a narrar os
  Níveis 1–3 (validação, redescoberta antes de remover a entrada, varredura de regiões, regiões
  inacessíveis) — conteúdo de §4.1.6 e das legendas do Apêndice A — dão lugar às remissões para as
  três figuras; mantém-se o apontador para a Figura 4.3 quanto ao detalhe operacional da AWS.
- Cap. 6, §"Threats to Validity": os dois primeiros parágrafos deixam de repetir os factos da
  §"Methodology" (portátil ocioso mas não dedicado, três forks, o I/O de ficheiro a dominar, a
  proveniência de T6/T7 e a grandeza em que as duas sessões concordam) e ficam só com as
  consequências, remetendo para a metodologia.
