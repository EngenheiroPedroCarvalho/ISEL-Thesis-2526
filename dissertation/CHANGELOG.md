# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `1810f90` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-27

### Diagramas: correções de coerência com o código e o texto

- Cap. 3 (`chapter3.tex`), Fig. `call-step-model`: a nota passa a dizer `host = path = ""` (string
  vazia, como no `CallContextBuilder`) em vez de `host = path = e`.
- Cap. 3, Fig. `architecture-after`: a nuvem GCP passa a "Cloud Run functions (GCP)" (2.ª geração);
  o QuickFaaS passa a "(+ AwsProvider, GCP v2 API)"; a legenda mostra «new» e «extended» nas
  amostras de cor, que antes ficavam quase invisíveis.
- Cap. 4 (`chapter5.tex`), Fig. `aws-deploy-sequence`: `path = ""` em vez de `path = e`; a nota do
  Nível 1 remete para o diagrama do Apêndice A em vez de um ficheiro `.puml`.
- Cap. 4, texto da Fig. `aws-deploy-sequence`: a referência aos três níveis da cascata aponta para
  `sec:resolution-cascade` (Cap. 3) em vez de `subsec:endpoint-resolution`.
- Apêndice A (`appendix-cascade.tex`), Figs. A.1–A.3: as notas que citavam ficheiros `.puml`
  passam a remeter para "the Level N diagram" ou para o diagrama de sequência AWS do Cap. 4.
- PNG regenerados com PlantUML 1.2026.8 e copiados para `images/`.
