# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `c8cc48c` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-23

### Repetições retiradas (`Chapters/chapter3.tex`, `chapter5.tex`)

- Cap. 3, §DSL Extensions: apagado o parágrafo que reintroduzia
  `internalFunction(name, deploymentDescriptorPath)` como "alternative construction path …
  mutually exclusive with host/path" logo a seguir ao parágrafo da Figura 3.3, que já o diz. O
  parágrafo seguinte ("By introducing internalFunction directly in the call execution step, the DSL
  remains close to the original OmniFlow design…"), que repetia o fecho do mesmo parágrafo da
  figura, foi fundido com a frase de introdução da lista de conceitos.
- Cap. 4 (`chapter5.tex`), §AWS Lambda Support in QuickFaaS: o parágrafo da Figura 4.3 deixa de
  enumerar o trabalho do `AwsLambdaDeployer` (role IAM, cópia temporária do descritor, região do
  bucket S3, 3 tentativas com 20 s de espera, *polling* do `GetFunction` até 180 s, `AddPermission`),
  que §4.2.2 descreve a seguir, e remete para lá.
