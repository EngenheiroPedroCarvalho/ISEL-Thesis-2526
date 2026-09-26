# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `1f82c01` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-26

### Limitação da 1.ª geração de Cloud Functions: o Level 3 atualiza a função existente

- Capítulo 4 (`Chapters/chapter5.tex`, §4.1.4, parágrafo "Limitation: first-generation Cloud
  Functions on GCP"): passa a dizer o que acontece quando o Level 3 volta a invocar o QuickFaaS
  para uma função que já existe. O QuickFaaS encontra-a pela API do Cloud Functions e atualiza-a
  com o código do descritor, ou seja, o cascade substitui uma função existente.
- Capítulo 7 (`Chapters/chapter8.tex`, limitações, ponto "No update path"): acrescentada a exceção
  do caso da 1.ª geração no GCP, com referência à §4.1.4.
