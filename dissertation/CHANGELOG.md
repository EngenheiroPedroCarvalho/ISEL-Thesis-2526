# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `0e254e8` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-28

### Caso de estudo executado também no AWS (cap. 5, `chapter6.tex`)

O deploy na conta AWS de teste deixou de ser descrito com test doubles: foi executado numa conta
real (evidência em `case-study-aws/`), depois de corrigido o renderer AWS (merge de
`experiment/aws-pass-state`).

- **Introdução e §5.3:** os deploys na conta AWS de teste e no GCP foram executados; só o deploy na
  conta de produção fica descrito.
- **§5.4 "What the Deployments Produce":** reescrita. O parágrafo sobre os test doubles e o
  segundo deploy simulado saíram. Há agora um parágrafo com os desvios comuns às duas execuções
  (referências com região e stub do core banking) e outro sobre a execução no AWS (stub em Lambda
  atrás de uma HTTP API do API Gateway, bootstrap com seis funções já existentes, Nível 3 com cerca
  de 17 s por função e 48 s no total, segundo deploy no Nível 1 em cerca de 1,5 s, registry igual
  byte a byte). O parágrafo do GCP foi encurtado para não repetir os desvios. O Listing
  `lst:case-registry-aws` passou a ser o registry real (oito entradas, conta `025064823406`), em
  vez do registry com conta fictícia. Novo parágrafo sobre as execuções: o input no Step Functions
  leva a transação na chave `transaction`, e o stub devolveu o `id` de cada transação.
- **Tabela `tab:case-executions`:** passa a mostrar as três transações nos dois providers (colunas
  AWS e GCP); todas terminaram em `SUCCEEDED`.
- **§5.5 Discussion:** o parágrafo sobre o `Pass` que substituía o estado inteiro ("which this
  work did not change") passa a descrever os dois defeitos do renderer AWS e a correção: um
  `assign` de uma variável escreve só essa variável (`Result`/`InputPath` + `ResultPath`), um de
  várias variáveis continua a substituir o estado; os query parameters das Lambdas deixam de ir
  em `States.Array`. Os "four limits" do GCP passam a "five limits" das duas execuções, com um novo
  quarto limite: o código das funções não é portável entre providers (os hooks QuickFaaS do GCP e
  do AWS têm assinaturas diferentes).

### Cap. 1 e Conclusões alinhados com a execução no AWS (`chapter1.tex`, `chapter8.tex`)

- **Cap. 1:** na lista de contribuições, o caso de estudo passa de "deployed and run on GCP, and
  its AWS deployment was produced … with the provider simulated" a "deployed and run on both AWS
  and GCP"; na descrição da estrutura, o Cap. 5 passa de "run on GCP and resolved for AWS" a "run
  on AWS and on GCP".
- **Conclusões (cap. 7):**
  - Contribuições: o caso de estudo é "run on AWS and on GCP".
  - Objetivo 4: a evidência não local passa a incluir os deploys do caso de estudo no AWS e no
    GCP; saiu "nothing ran against AWS".
  - Avaliação crítica: "Renderer limits" deixa de dizer que o caso de estudo não corre no AWS e
    que o renderer não foi alterado; fica a limitação dos assigns com várias variáveis e a dos
    hosts literais. Novo ponto "Provider-specific function source": os hooks QuickFaaS do GCP e o
    template Lambda do AWS têm assinaturas diferentes, por isso cada função é escrita uma vez por
    provider.
  - Trabalho futuro: "Validate the ladder against live providers" deixa de destacar o AWS e passa
    a "beyond the single workflow" do caso de estudo.
  - Considerações finais: "evidence against AWS" passa a "live evidence beyond a single workflow".

### Resumos e Avaliação alinhados com a execução no AWS (`abstract-en.tex`, `abstract-pt.tex`, `chapter7.tex`)

- **Resumos:** o caso de estudo foi "deployed and run on AWS and on Google Cloud" / "implantado e
  executado no AWS e no Google Cloud" (antes, só no Google Cloud).
- **Cap. 6, limitações da evidência de correção:** o caso de estudo passa a verificar os
  pressupostos dos test doubles nos dois providers (bootstrap, Níveis 2 e 3, hits validados no
  Nível 1); saiu "On AWS it remains unchecked". Os caminhos que continuam sem verificação contra as
  APIs reais passam a ser nomeados: os abortos por entradas obsoletas, nomes ambíguos e regiões
  inacessíveis.
