# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `5c66df7` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-29

### Cap. 5 (`chapter6.tex`): excerto do workflow passa para o apêndice

- Sai a Listagem 5.1 (`lst:case-payment-excerpt`, as duas chamadas internas de
  `PaymentAuthorization`); o workflow completo já está na Listagem B.2. A secção 5.2 remete para
  ela e diz em texto que `fraud-check` e `risk-score` declaram `internalFunction` com um descritor.
  As listagens seguintes do cap. 5 descem um número.
- Cap. 4 (`chapter5.tex`, "Invocation" na GCP): a referência passa para a Listagem B.2.

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
