# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `8b8d22a` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-14

### Cap. 4 Implementation (`Chapters/chapter5.tex`)

- Secção 4.2 (Function Deployment and Invocation): novo parágrafo "Why one deployer per provider",
  a seguir à Figura `fig:omniflow-classes-deployers`. Explica que o `QuickFaasDeployer` (GCP) foi
  feito e testado primeiro e que o AWS entrou como segunda implementação (`AwsLambdaDeployer`)
  para não mexer no deployer GCP; porque não se juntou tudo numa classe (só o subprocesso e o
  descriptor são comuns, e já estão em `QuickFaasProcessInvoker` e `QuickFaasDescriptorLoader`);
  e as vantagens (GCP intocado, configuração por provider verificada pelo compilador, um novo
  provider como o Azure é uma classe nova sem alterar as existentes). Mantém, a pedido, a
  história do desenvolvimento, apesar da convenção de descrever só o estado final.

## 2026-10-13

### Capa (`Config/_cover.tex`, `Config/_department.tex`, `Config/_institution.tex`)

- O nome do mestrado em inglês passa de "Computer Science and Engineering" (valor do template para
  `meic`) para "Computer Science and Computer Engineering", com `\ncmajorfield(en)` a seguir a
  `\iselthesis@setcourse{meic}`.
- A universidade passa de "Instituto Politécnico de Lisboa" para "Universidade Politécnica de
  Lisboa", em PT e EN (`Config/_department.tex` e `Config/_institution.tex`).
