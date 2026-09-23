# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `a30c525` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-23

### Termo "XOR invariant" retirado (`Chapters/chapter3.tex`, `chapter5.tex`, `glossary.tex`)

- Cap. 3, §DSL Extensions: "as construction paths are *mutually exclusive*, as captured by the XOR
  invariant" passa a "are *mutually exclusive*"; a frase seguinte diz "This rule is enforced twice"
  em vez de "This invariant". O conteúdo (exclusividade entre `internalFunction` e
  `host`/`path`, verificada no builder e nos resolvers) mantém-se.
- Cap. 4 (`chapter5.tex`), §Concept and Conventions: "the XOR invariant that forbids combining it
  with a literal `host`/`path`" passa a "the rule that…", e "where that invariant is enforced" a
  "where that rule is enforced".
- Glossário: removida a entrada `xor` ("XOR invariant"), já sem ocorrências no texto.
- Figura do modelo do `call` (`../diagrams/call-step-model.puml`, PNG em
  `images/dsl-extension/`, cap. 3): a nota passa a começar por "Mutually exclusive (enforced at DSL
  build time, re-checked by the resolvers)"; PNG regerado.
