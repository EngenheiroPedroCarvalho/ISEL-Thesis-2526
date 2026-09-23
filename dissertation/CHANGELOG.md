# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `3ea629f` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-23

### Repetições retiradas do capítulo 4 (`Chapters/chapter5.tex`)

- §4.1 (parágrafo da Figura 4.1) e §"A metadata record per function": deixam de descrever os campos
  do `FunctionInvocationMetadata` (`serviceName` + `url`, HTTPS no GCP / ARN na AWS), que
  §"Registry File Format" define; ficam com o nome do record e uma remissão.
- §4.1.6 e §4.1.8 deixam de se citar em círculo sobre "permissão negada ≠ ausência": a regra
  completa (excluída do conjunto de candidatos durante a varredura, distinguida no fim, diagnóstico
  a nomear as regiões inacessíveis) fica só em §4.1.8, "Insufficient permissions".
- Bullet "Drift handling" (§"Creation, Bootstrap and Updates"): fica com a deriva (validar e
  atualizar a entrada) e remete para §4.1.8 quanto ao recurso que desapareceu, em vez de repetir o
  tratamento das entradas obsoletas.
- §4.3 "Runtime Invocation": o parágrafo da Figura 4.4 deixa de contrastar com o
  `apigateway:invoke` e de descrever o `ResultSelector`, que o parágrafo do renderer trata a
  seguir; a referência ao `AmazonConstantsUtils` passou para esse parágrafo.
