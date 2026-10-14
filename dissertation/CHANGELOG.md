# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `727a8f0` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-14 — Cap. 4, Listagem 4.3

- `chapter5.tex`: a Listagem 4.3 (estado de Step Functions para uma chamada Lambda interna) deixa
  de ser *float*: sozinha numa página de *floats* ficava centrada na vertical; agora fica no fim da
  secção, dentro de uma `minipage`, e abre a pág. 29 no topo. Um `\newpage` antes dela deixa o
  espaço livre no fundo da pág. 28, em vez de esticar o intervalo entre a Listagem 4.2 e o
  parágrafo "Invocation".
