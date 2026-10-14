# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `8b8d22a` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-14

### Cap. 7 Conclusions (`Chapters/chapter8.tex`)

- Secções 7.1–7.3 passam de listas a texto corrido, sem mudar factos, números nem referências:
  as contribuições num parágrafo; as RQ e os objetivos em dois parágrafos; as 12 limitações do
  Critical Assessment agrupadas em quatro parágrafos (ciclo de vida, portabilidade, registry,
  operação).
- Secção 7.4 (Future Work): mantém-se em lista, a única do capítulo.
- Secção 7.5 (Final Remarks): passa de um para quatro parágrafos. Os novos descrevem o que muda
  para quem faz o deploy (antes e depois da integração, e o AWS no QuickFaaS) e as duas decisões
  de desenho (ferramentas separadas, um deployer por provider; escada conservadora).

### Cap. 5 Case Study (`Chapters/chapter6.tex`) e Apêndice B (`Chapters/appendix-listings.tex`)

- Secção 5.2 (Workflow Design): nova Figura 5.1 com o fluxo do `PaymentAuthorization` renderizado
  para GCP (`fig:case-flow-gcp`), e uma frase que a refere e nota que no AWS o fluxo tem os mesmos
  passos, sem o `return_output` final.
- Apêndice B: a figura do fluxo GCP sai do apêndice (era a B.1); o texto da secção "Rendered for
  GCP" e a introdução remetem agora para a Figura 5.1. A figura do fluxo AWS passa de B.2 a B.1.

### Cap. 4 Implementation (`Chapters/chapter5.tex`)

- Secção 4.2 (Function Deployment and Invocation): removido o parágrafo de introdução às
  subsecções ("On GCP, deployment required moving QuickFaaS to second-generation functions…"),
  que também dizia que o endpoint fica fixo no workflow e o registry não é consultado.
- Secção 4.2: novo parágrafo "One deployer per provider",
  a seguir à Figura `fig:omniflow-classes-deployers`. Explica que o `QuickFaasDeployer` (GCP) foi
  feito e testado primeiro e que o AWS entrou como segunda implementação (`AwsLambdaDeployer`)
  para não mexer no deployer GCP; que uma só classe podia ter servido os dois providers; e as
  vantagens (GCP intocado, configuração por provider verificada pelo compilador, um novo
  provider como o Azure é uma classe nova sem alterar as existentes). Mantém, a pedido, a
  história do desenvolvimento, apesar da convenção de descrever só o estado final.

## 2026-10-13

### Capa (`Config/_cover.tex`, `Config/_department.tex`, `Config/_institution.tex`)

- O nome do mestrado em inglês passa de "Computer Science and Engineering" (valor do template para
  `meic`) para "Computer Science and Computer Engineering", com `\ncmajorfield(en)` a seguir a
  `\iselthesis@setcourse{meic}`.
- A universidade passa de "Instituto Politécnico de Lisboa" para "Universidade Politécnica de
  Lisboa", em PT e EN (`Config/_department.tex` e `Config/_institution.tex`).
