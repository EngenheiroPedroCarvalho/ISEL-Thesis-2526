# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `65d5f3a` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-28

### Modelo de classes do OmniFlow dividido em partes (cap. 4, `chapter5.tex`)

- A Figura 4.1 (modelo de classes completo, `fig:omniflow-integration-classes`) passa para o
  Apêndice A, agora chamado "Complete Diagrams".
- No cap. 4 o modelo aparece em quatro partes, cada uma junto do texto que a descreve (novos
  `diagrams/omniflow-classes-{entry,registry,resolvers,deployers}.puml` e PNG):
  - parte 1, pontos de entrada (`fig:omniflow-classes-entry`): novo parágrafo "Entry points" na
    introdução da secção 4.1;
  - parte 2, registo e bootstrap (`fig:omniflow-classes-registry`): no início de "Creation,
    Bootstrap and Updates", que passa a apresentar a store e o `FunctionInvocationMetadata`;
  - parte 3, resolvers e consultas ao fornecedor (`fig:omniflow-classes-resolvers`): em "Registry
    Store and Resolver Internals";
  - parte 4, estratégia de deployment (`fig:omniflow-classes-deployers`): na introdução da secção
    4.2, que passa a descrever as três implementações de `InternalFunctionDeployer`.
