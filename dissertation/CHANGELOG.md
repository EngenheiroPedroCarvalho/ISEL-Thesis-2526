# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `39e2394` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-29

### Cap. 5 (`chapter6.tex`), secção 5.1: ICT por extenso

- Requisito "An exit strategy": "ICT services" passa a "information and communication technology
  (ICT) services" (primeira e única ocorrência no texto).

### Glossário (`glossary.tex`): nova entrada "ICT services"

- Definição do DORA, art. 3.º, alínea 21 (citação literal, com `\cite[Art.~3(21)]{dora2022}`), e
  uma frase a dizer que os serviços cloud do caso de estudo são serviços ICT.

### Cap. 5 (`chapter6.tex`): as contas de teste e de produção passam para a discussão

- Sai o requisito "One workflow, several accounts" (secção 5.1); ficam dois requisitos (estratégia
  de saída e auditabilidade).
- Secção 5.3 "Deploying Across Accounts and Providers" passa a "Deploying to AWS and GCP": o
  parágrafo "AWS test account" passa a "AWS." (como o da GCP) e sai o parágrafo "AWS production
  account". A introdução do capítulo deixa de falar em conta de teste e de produção.
- Secção 5.5 (Discussão): novo parágrafo "Several accounts" com o conteúdo que saiu (endpoint
  diferente por conta ou projeto, só os descritores mudam, um registry por conta, não partilhar o
  registry), agora também para projetos GCP, e a nota de que nenhuma promoção foi executada.
