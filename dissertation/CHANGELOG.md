# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `f818f36` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-28

### Texto encurtado em todos os capítulos (cap. 1–7)

- `chapter1.tex`–`chapter8.tex` reescritos para frases mais curtas e diretas, sem repetições
  (o texto dos capítulos passou de ~24 000 para ~13 700 palavras; PDF de 88 para 78 páginas).
  Mantêm-se todas as secções, etiquetas, citações e números.
- **Cap. 1:** os objetivos passam a ser quatro, com "Extend QuickFaaS to AWS Lambda" (o cap. 7 já
  avaliava quatro objetivos e a introdução só listava três). A estrutura do trabalho menciona os
  apêndices.
- **Cap. 4:** "Evolution of the Registry Design" reduzida a um parágrafo; "Automatic registration"
  e "Post-deployment updates" fundidos em "Registration".
- **Cap. 5:** os cinco limites encontrados nos fornecedores passam a lista numerada; os defeitos
  do renderer AWS ficam num parágrafo próprio.

### Diagramas e listagens: completos em apêndice, partes no texto

- **Novo diagrama** `aws-deploy-sequence-level3` (`diagrams/*.puml`, `images/implementation/`):
  só o ramo de Nível 3 da sequência AWS, mostrado no cap. 4 (`fig:aws-deploy-level3`). A sequência
  completa (`fig:aws-deploy-sequence`) passa para o Apêndice A, que se chama agora "Sequence
  Diagrams". A nota do diagrama `resolution-cascade-level3-aws` aponta para o apêndice.
- **Novo Apêndice B** (`appendix-listings.tex`, `app:listings`): workflow `FraudPipeline` completo
  (`lst:wf-fraudpipeline-full`), `PaymentAuthorization` completo (`lst:case-payment-workflow`) e os
  dois registos completos do caso de estudo. No texto ficam excertos: `lst:call-step-internal`
  (passa a ser o passo `fraud-check` do FraudPipeline; sai `lst:wf-internal-part`),
  `lst:wf-external-part`, `lst:case-payment-excerpt` (as duas chamadas internas) e
  `lst:case-registry-new` (só as entradas novas de cada registo).
- **Novo Apêndice C** (`appendix-benchmarks.tex`, `app:benchmarks`): tabela de notação e as
  tabelas/figuras completas de T2, T4–T11 e T14, com as mesmas etiquetas. Os pontos com ruído
  (T1 N=10, T9 K=10/R0=10, T13 profundidade 2) são assinalados no início do apêndice.
- `Config/_files.tex`: inclui os dois novos apêndices.

### Avaliação simplificada (cap. 6, `chapter7.tex`)

- Tabela de correção reduzida de 18 para 11 linhas, agrupadas por nível.
- Nova tabela-resumo das experiências (`tab:eval-summary`); no texto ficam só T1 (tabela e
  figura) e T3. T6/T7 e T8/T9 descritos em texto. Secção "Match Strategy, Mixed Workflows, and
  Structure" (`sec:eval-secondary`) removida (resumida na tabela e no Apêndice C).
- A metodologia define a notação em texto; a tabela de notação vai para o Apêndice C.

### Referência ao JMH

- Nova entrada `jmh` (OpenJDK, JMH) em `bibliography.bib`, citada na metodologia do cap. 6.
