# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `83beba1` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-15

### Capítulos 2 e 3 juntos num só capítulo

- **Cap. 2 (`chapter2.tex`):** os capítulos "Background" e "Related Work" passam a ser um único
  capítulo, com o título **"Serverless Portability: Background and State of the Art"**.
  - "Related Work" passa a ser a secção 2.5. As suas quatro secções passam a subsecções (2.5.1 a
    2.5.4): Infrastructure-as-Code…, Portable Execution Layers…, Assessing Portability…, Workflow
    Composition….
  - Mantêm-se as etiquetas `cha:background` (capítulo) e `ch:related-works` (agora na secção
    2.5).
  - Introdução do capítulo: "Chapter~\ref{ch:related-works} reviews…" passa a "Finally,
    Section~\ref{ch:related-works} reviews…".
  - Introdução da secção Related Work: "This chapter reviews…" e "The remainder of this chapter…"
    passam a "This section…".
- **Renumeração:** os capítulos seguintes descem um número: Proposed Solution 4→3,
  Implementation 5→4, Case Study 6→5, Evaluation 7→6, Conclusions 8→7.
- **Cap. 1 (`chapter1.tex`), "Structure of the Work":** os pontos dos capítulos 2 e 3 passam a um
  só ponto, e os números dos capítulos seguintes foram atualizados.
- **Cap. 3 (`chapter3.tex`, Proposed Solution):** a referência "(Chapter~\ref{ch:related-works})"
  passa a "(Section~\ref{ch:related-works})".
- **`Config/_files.tex`:** atualizados os comentários com os números dos capítulos.
