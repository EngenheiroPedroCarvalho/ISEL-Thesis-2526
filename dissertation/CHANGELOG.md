# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `1b2fcf5` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-12

### Capítulo 4 (Implementation) e Apêndice A — diagrama de classes do registry

- Figura 4.2 (`images/implementation/omniflow-classes-registry.png`) e diagrama completo do
  Apêndice A (`omniflow-integration-classes.png`): acrescentado `tryResolveEntry(ref)` à classe
  `FunctionRegistryStore`, que a Secção 4.1.3 descreve a par de `tryResolveEntryIn()` mas que
  faltava nas figuras. Fontes PlantUML em `../diagrams/`.
- Secção 4.1.3 (`Chapters/chapter5.tex`): "each read re-parses the file" passou a "each read reads
  the whole file and rebuilds the map".
