# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `5f0d1c4` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-21

### Figuras alinhadas com o código (`../diagrams/*.puml`, PNG em `images/`)

- Fig. 3.1 (`architecture-before`, cap. 3): a nota deixa de citar o `NoopInternalFunctionDeployer`,
  que só existe a partir deste trabalho.
- Fig. 3.2 (`architecture-after`, cap. 3): resolvers, deployers e registos marcados como novos; DSL,
  `GoogleCloudDeployer`, `AmazonCloudDeployer` e jar do QuickFaaS como estendidos. A legenda já não
  diz que os componentes sem marca são "unmodified, pre-existing".
- Figura do modelo do `call` (`call-step-model`, cap. 3): acrescentado `bodyRaw`; o invariante XOR
  exige `host` definido em vez de `host != ε`.
- Diagrama de classes (`omniflow-integration-classes`, cap. 4): atributos e métodos atualizados
  (`preferredRegion`, `discoverFunctionForRef`, `readAll`/`tryResolveEntryIn`, `bootstrapIfMissing(scope)`),
  acrescentados os inspectors, os listers de regiões e o `CloudFunctionsCatalog`; sem o estereótipo
  `<<new in this work>>`; disposição da esquerda para a direita.
- Sequência AWS (`aws-deploy-sequence`, cap. 4): bootstrap do registo, leitura única para o snapshot,
  validação no Nível 1, descoberta em todas as regiões no Nível 2 e falha por ambiguidade ou regiões
  inacessíveis.
- Apêndice A, Nível 1 GCP e AWS: pesquisa sobre o snapshot, redescoberta com `remove(key)` seguido de
  `put(functionRef, …)`, ramo de erro por ambiguidade; no GCP, o ramo das funções de 1.ª geração.

### Capítulo 4, Implementation (`Chapters/chapter5.tex`)

- Secção Function Registry: o `AwsLambdaDeployer` deixa de ser o único descrito como "added in this
  work", já que toda a camada de integração é deste trabalho.
- Secção AWS Lambda Support: a descrição da Fig. `aws-deploy-sequence` passa a referir um `alt` com um
  ramo por nível e um quarto para as falhas, em vez de três fragmentos `alt`.
