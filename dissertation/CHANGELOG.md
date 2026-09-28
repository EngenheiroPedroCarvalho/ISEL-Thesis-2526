# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `1d04544` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-28

### Caso de estudo: consola GCP, código e definições geradas (cap. 5 e Apêndice B)

- **Cap. 5 (`chapter6.tex`), secção 5.4, parágrafo GCP:** nova Figura 5.1
  (`fig:case-gcp-console`) com as duas capturas da consola Google Cloud (`images/GCP/img.png`: as
  três funções Cloud Run, públicas, em `europe-west1`; `images/GCP/img_1.png`: o workflow
  `payment-authorization`), como subfiguras. O texto remete para a nova secção B.4 e, nas
  execuções, para as entradas (Listagem B.13).
- **Apêndice B (`appendix-listings.tex`):** nova secção B.4 "Case-Study Functions and Rendered
  Workflows" (`app:case-code`), gerada a partir de `case-study-gcp/` e `case-study-aws/`:
  - funções na GCP (hooks Java da QuickFaaS: `fraud-check`, `risk-score`, stub);
  - funções na AWS (Java para `fraud-check` e `risk-score`; stub em Python atrás do API Gateway);
  - definições geradas completas: YAML do Cloud Workflows e JSON (ASL) do Step Functions;
  - entradas das três execuções em cada fornecedor (na AWS dentro da chave `transaction`).
