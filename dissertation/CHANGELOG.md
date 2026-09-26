# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `cc3aca2` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-26

### Capítulo 4 (Implementação), `Chapters/chapter5.tex`

- §4.1.2, item *Drift handling*: a frase dizia que uma entrada cuja função já não existe é
  "substituída ou removida". Corrigido para o comportamento real (e o do §3, Nível 1, e da
  Tabela da avaliação): a entrada só é substituída se a função for encontrada noutra região; caso
  contrário é removida e a implantação aborta.
