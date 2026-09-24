# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `0e158a7` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-24

### Quarta revisão de informação repetida (grupo A)

- **Cap. 4 Implementation (`chapter5.tex`):** retiradas as subsecções §4.1.1 Motivation e §4.1.2
  Concept and Conventions (repetiam §3.3 e o item "Mutually exclusive forms" de §3.4.1), substituídas
  por um parágrafo de ligação no fim da abertura de §4.1; as subsecções seguintes de §4.1 sobem dois
  números. O item "Drift handling" e o parágrafo do resolver GCP em §4.1.6 passam a remeter para o
  Nível 1 de §3.1.1 em vez de o voltarem a descrever. A leitura única para snapshot fica explicada
  numa só frase de §4.1.9 (com o `OptimizedEndpointResolver`), e a correção da limitação da 1.ª
  geração passa a ser só uma remissão para o trabalho futuro.
- **Cap. 3 Proposed Solution (`chapter3.tex`):** retiradas duas das três menções à 1.ª geração
  (no "Static binding" e no item "Stale binding"); fica a do Nível 1.
- **Cap. 5 Case Study (`chapter6.tex`):** §5.4 Runtime Invocation reduzida ao que difere entre as
  duas implantações (repetia §4.3). O parágrafo final da Discussion passa a remeter para os limites
  de §7.3 em vez de os descrever.
- **Cap. 6 Evaluation (`chapter7.tex`):** a ressalva sobre o hardware fica só em Threats to Validity
  (a Methodology remete para lá); a nota das duas sessões em Threats encurtada; retirada a frase que
  repetia, após a Tabela 6.1, que os testes simulam o provider; T6/T7 e a terceira conclusão da RQ2
  passam a remeter para o snapshot de §4.1 e deixam de narrar a história da otimização.
- **Cap. 7 Conclusions (`chapter8.tex`):** retirada a secção §7.4 Applicability (repetia o estudo de
  caso); a menção à PSD2 passa para as Final Remarks, e §7.5–§7.6 passam a §7.4–§7.5. A limitação
  da 1.ª geração encurtada no objetivo 2 e na Critical Assessment.

### Correção da chave do registry no GCP

- **Cap. 4 Implementation (`chapter5.tex`, §4.1.1 Registry File Format):** o texto dizia que um
  serviço Cloud Run encontrado por descoberta fica sempre sob uma chave com região
  (`"europe-west1/risk-score"`), o que contradizia §3.3 e o código. Corrigido: a chave é o
  `functionRef` simples, e só leva a região quando a chamada usa essa forma ou quando o bootstrap
  encontra o mesmo nome em mais de uma região (`CloudRunV2RestCatalog.kt`,
  `WorkflowInternalFunctionResolver.kt`). Mantém-se que o `serviceName` guardado é o nome completo
  do recurso.

### Quarta revisão de informação repetida (grupo B)

- **Cap. 1 Introduction (`chapter1.tex`):** a Motivation deixa de recontar a sequência manual e
  remete para o item "Manual pre-deployment"; "non-secret" sai do objetivo 1 e da contribuição 2
  (fica no desafio "Metadata synchronization" e em §3.3); a contribuição 3 perde a frase que
  repetia o objetivo 3.
- **Cap. 2 (`chapter2.tex`):** o âmbito de um só provider passa a remeter para §1.2; retirada a
  frase final do parágrafo das ferramentas IaC, que antecipava a conclusão da tabela comparativa.
- **Cap. 3 Proposed Solution (`chapter3.tex`):**
  - a introdução deixa de repetir que o endpoint só existe depois do deployment;
  - a descrição da Fig. 3.2 e o parágrafo da interface de deployment deixam de nomear as classes
    (ficam no cap. 4);
  - a propriedade "safe by default" passa a remeter para §4.2.2 em vez de listar as permissões;
  - "Provider and region scope" remete para §1.2 no âmbito de um só provider e deixa de repetir
    `DescribeRegions` e a forma `"region/functionRef"`;
  - retirado o remate do segundo argumento contra os dois serviços (repetia o fecho do cap. 2);
  - o walkthrough reduz a remissão ao static binding a uma frase;
  - o parágrafo do descritor QuickFaaS continua a agrupar os campos em três grupos, mas já não os
    define um a um (estão definidos com a Listing 2.1).
- **Cap. 4 Implementation (`chapter5.tex`):** retirados "non-secret" em §4.1.1, a frase que repetia
  a forma `"region/functionRef"` em §4.1.4, e a primeira das duas menções a que a Function URL não
  é usada (fica o parágrafo "A deliberate design choice").
- **Cap. 5 Case Study (`chapter6.tex`):** frase sobre o redeploy de `fraud-check` encurtada.
- **Cap. 6 Evaluation (`chapter7.tex`):** retirada a frase das "dezenas de milissegundos" em T8/T9
  (fica no Global framing).

### Quinta revisão de informação repetida (grupo A)

- **Cap. 2 (`chapter2.tex`):** a §2.1.4.3 "Workflow Development Cycle" perde o parágrafo que
  recontava as três fases (definir, renderizar, implantar) já descritas em §2.1.4 e §2.1.4.1, e
  passa a chamar-se "Example: Function Chaining", que é o que resta nela.
- **Cap. 3 Proposed Solution (`chapter3.tex`):**
  - a abertura de §3.3 deixa de recapitular o modelo de steps do cap. 2 e remete para §2.1.4;
  - retirada a lista de dois itens ("literal endpoint" / "automatically resolved endpoint"), que
    repetia o parágrafo anterior, e a menção à exclusão mútua na introdução do capítulo;
  - retirada da descrição da Fig. 3.2 a frase sobre a invocação nativa pelo ARN (fica no static
    binding e no registry);
  - corrigida a propriedade "idempotent": dizia que o cascade não volta a implantar uma função
    *inalterada*, o que dava a entender que implantaria uma alterada; passa a dizer que não volta a
    implantar uma função que já existe.
- **Cap. 4 Implementation (`chapter5.tex`, §4.1.4):** o tratamento de permissões recusadas deixa de
  ser descrito no parágrafo do resolver AWS e na frase seguinte; fica em §4.1.6, com uma remissão.
  Retirada a frase que resumia o que distingue os dois resolvers.
- **Cap. 5 Case Study (`chapter6.tex`, Discussion):** o primeiro parágrafo deixa de resumir §5.3 e
  remete para lá.
- **Cap. 6 Evaluation (`chapter7.tex`):** retirado da Scope and Goals o parágrafo que anunciava a
  contagem de chamadas à API (já está na lista das seis partes); a abertura de §6.6 encurtada.
- **Cap. 7 Conclusions (`chapter8.tex`):** §7.1 Summary of Contributions reduzida a um parágrafo
  (repetia os objetivos 1 e 3 de §7.2 e a lista do trabalho do deployer AWS).
