# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `a02a536` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-28

### Invocação junto de cada fornecedor (cap. 4, `chapter5.tex`)

- A secção 4.3 "Runtime Invocation" deixa de existir. A parte da GCP (chamada HTTP, autenticação
  OIDC) passa a ser o parágrafo "Invocation" no fim da 4.2.1; a parte da AWS (`lambda:invoke`, só
  query parameters, renderer, Listagem 4.2) passa a ser o parágrafo "Invocation" no fim da 4.2.2,
  com a etiqueta `sec:runtime-invocation`.
- A 4.2 passa a chamar-se "Function Deployment and Invocation", e a 4.2.2 "AWS Lambda Functions".
  As antigas 4.2.3 (QuickFaaS AWS provider) e 4.2.4 (OmniFlow-side AWS deployer) passam a
  parágrafos da 4.2.2, com as mesmas etiquetas.
- As Listagens 4.1 e 4.2 passam a flutuantes, para não se partirem entre páginas.
- Referências: a introdução do cap. 4 e a estrutura do cap. 1 descrevem a nova organização; no
  cap. 7, a limitação "Broad, automatic IAM" aponta para a 4.2.1, e o objetivo 3 aponta só para a
  4.2.2.
