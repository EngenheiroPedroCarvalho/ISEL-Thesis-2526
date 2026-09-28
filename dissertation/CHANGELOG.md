# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `cce3b3c` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-28

### Leitura única do registo (cap. 5, `chapter5.tex`)

- **"Store internals":** a frase sobre a store não guardar estado em memória passa a dizer que
  os resolvers evitam esse custo lendo o registo uma vez por resolução (explicado mais abaixo na
  mesma secção), e que o cap. 7 analisa as consequências de desempenho "of both paths". Antes, a
  frase dava a entender que a releitura por chamada continuava por resolver.
