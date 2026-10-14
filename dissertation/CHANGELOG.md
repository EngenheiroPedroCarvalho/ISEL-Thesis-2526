# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `25017ec` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-14 — Diagramas sem título

- Os 17 diagramas PlantUML que tinham título (`../diagrams/*.puml`) deixam de o ter, já que a
  legenda de cada figura o repete; os PNG em `images/architecture/`, `images/dsl-extension/` e
  `images/implementation/` foram regenerados. Nenhum diagrama mudou de conteúdo.

## 2026-10-14 — Resumo com *deployment*

- `abstract-pt.tex`: "implantar"/"implantação" passam a *deployment* (em itálico, como *workflow*
  e *endpoint*), também na palavra-chave "Ligação de *endpoints* em tempo de *deployment*". A
  frase sobre o AWS passa a "O QuickFaaS, que até aqui suportava apenas o Google Cloud e o Azure,
  ganhou também suporte para o AWS Lambda, pelo que a integração abrange as duas *clouds* do
  OmniFlow".
