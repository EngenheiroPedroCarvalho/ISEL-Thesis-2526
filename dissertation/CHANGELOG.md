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

### Texto alinhado com as figuras e o código

- Cap. 3, Proposed Solution (`Chapters/chapter3.tex`): o passo 4 do fluxo deixa de dizer que todo o
  metadata resolvido é escrito no registo; só o que vem de descoberta, deploy ou drift.
- Cap. 4, Implementation (`Chapters/chapter5.tex`):
  - Um hit validado sem alterações não escreve no registo; só descoberta, deploy ou drift chamam
    `put()`.
  - O resolver AWS reporta um nome em várias regiões como ambíguo, em vez de o "desambiguar".
  - A redescoberta do Nível 1 percorre todas as regiões, ou só a região da forma `"region/functionRef"`
    (também na legenda da Fig. do Nível 1, `Chapters/appendix-cascade.tex`).
  - O texto e a legenda da Fig. `aws-deploy-sequence` descrevem a sequência completa, com o Nível 3
    expandido, em vez de só o pior caso.
- Cap. 6, Evaluation (`Chapters/chapter7.tex`):
  - T4: o caminho otimizado cresce mais com N do que com F (antes dizia o contrário); a legenda diz
    que as curvas são por F nos dois gráficos.
  - T8: a legenda dá os declives (1,06 a 0,93) em vez de "slope one".
  - T9: o padrão `tryResolveEntry` + `put` é do store; os resolvers de produção usam o snapshot e
    não pagam a leitura extra.
  - T11: texto e legenda referem os saltos isolados de 16–22% em E=10 e E=50.
  - T13: a legenda deixa de dizer que o intervalo de confiança cobre o resto da curva.
