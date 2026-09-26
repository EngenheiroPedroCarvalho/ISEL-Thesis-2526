# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `8e0fd4e` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-26

### Capítulo 4 (Implementação), `Chapters/chapter5.tex`

- §4.1: passam a ser nomeadas no texto as classes de consulta aos providers que constam da
  Figura 4.1 e não eram mencionadas. No item *Bootstrap by discovery* (§4.1.2), a interface
  `CloudFunctionsCatalog` e as implementações `CloudRunV2RestCatalog` (GCP) e
  `LambdaFunctionsCatalog` (AWS). No parágrafo dos resolvers, o `CloudRunV2ServiceInspector` (e a
  forma como valida e descobre serviços) e o `CloudRunLocationsV1RestClient`. A frase sobre a
  descoberta de regiões na AWS passa a nomear a interface `AwsRegionsLister`, como na figura, com
  `Ec2AwsRegionsLister` como implementação.
