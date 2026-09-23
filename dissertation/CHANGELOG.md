# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `7c3d1da` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-23

### Repetições entre capítulos (`Chapters/chapter2.tex`, `chapter3.tex`, `chapter5.tex`)

- Cap. 2, §"Infrastructure-as-Code and Deployment-Oriented Tooling": o ARN deixa de ser escrito por
  extenso e explicado campo a campo pela segunda vez (já o é em §1.2, que o parágrafo cita); fica a
  remissão e o que interessa ali, que só os primeiros campos são conhecidos quando o template é
  escrito.
- Cap. 3, §"Components Architecture" (parágrafo da Figura 3.1): a sincronização manual deixa de ser
  re-descrita ("the endpoint is known only after the function is deployed … finding and replacing
  every endpoint by hand", de §1.2) e passa a remeter para lá.
- Cap. 4, §"From literal endpoints to logical indirection": deixa de re-derivar o acoplamento do
  `host`/`path` literal, já exposto em §4.1 "Motivation"; fica com o que é próprio da narrativa de
  evolução, o primeiro passo de design.
