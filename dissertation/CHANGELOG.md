# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `c1b3f6e` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-09

### Cores dos títulos e da capa (`template.tex`)

- Os títulos passam a usar as cores do modelo Word do TFM (`Template_TFM_20251217`), em vez de
  preto: capítulos, títulos não numerados (Resumo, Abstract, índices, Acronyms, Glossary) e o
  filete do capítulo a laranja `#FF3C28`; secções e subsecções a bordeaux `#5F1437`. Mantêm-se os
  tamanhos dos capítulos (14 pt) e das secções (12 pt).
- Capa: o texto saía a roxo `#622B72`, porque `Config/_packages.tex` redefine `iselpurple` para o
  diagrama de Gantt. `template.tex` repõe o bordeaux `#5F1437` depois de esse ficheiro ser
  carregado.
- Subsecções passam de 11 pt para 10,5 pt, como no modelo Word.
