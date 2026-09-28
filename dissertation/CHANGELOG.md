# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `e8e40a0` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-28

### Excerto do YAML da GCP (cap. 4, `chapter5.tex`)

- Nova Listagem 4.1 (`lst:rendered-gcp`) no parágrafo "Invocation" da 4.2.1: o passo
  `fraud-check` tal como o renderer da GCP o gerou no caso de estudo
  (`case-study-gcp/logs/rendered-first.yaml`), com o URL `run.app` resolvido e os `query`.
  As listagens seguintes do cap. 4 passam a 4.2 e 4.3.
- O texto sobre autenticação diz agora que o renderer acrescenta `auth: type: OIDC` dentro de
  `args` quando o passo a declara, e que o caso de estudo não a declarou, porque as funções eram
  públicas.
