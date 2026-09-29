# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `5c66df7` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-29

### Apêndice B (`appendix-listings.tex`): definições geradas e diagramas de fluxo do caso de estudo

- A secção B.2 "PaymentAuthorization Workflow" (nova etiqueta `app:case-workflow`) junta a
  definição DSL (Listagem B.2), as duas diferenças das execuções (referências com região e stub)
  e, por fornecedor, uma nova subsecção: "Rendered for GCP" (Figura B.1, fluxo do Cloud Workflows,
  e o YAML gerado) e "Rendered for AWS" (Figura B.2, fluxo da state machine com os tipos de estado
  da consola, e o JSON/ASL gerado).
- Os diagramas são novos, em PlantUML (`diagrams/case-flow-gcp.puml`, `diagrams/case-flow-aws.puml`;
  PNG em `images/case-study/`), desenhados a partir das definições geradas à semelhança das
  consolas.
- As listagens YAML e JSON saem da antiga B.4.3 "Rendered Workflow Definitions", que desaparece; a
  secção B.4 passa a "Case-Study Functions and Execution Inputs".
- Introdução do apêndice e cap. 5 (`chapter6.tex`, secção 5.4): as referências às definições
  geradas apontam para a secção B.2.
