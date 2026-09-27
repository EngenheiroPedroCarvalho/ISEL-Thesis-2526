# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `a90e0c1` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-27

### Estado da arte: novos trabalhos e tabela revista

- Cap. 2 (`chapter2.tex`), §2.2: cinco famílias em vez de quatro. Novos: AWS CDK (família IaC);
  AFCL/xAFCL (família de workflows — definição independente do fornecedor, mas com os endpoints
  escritos à mão no YAML, funções implantadas antes e execução no motor próprio); nova §2.2.5
  "Service Registries" (AWS Cloud Map, Consul: resolução em tempo de execução, sem implantação,
  registo partilhado). O parágrafo da ligação em tempo de implantação passa a dizer que Terraform
  e Pulumi podem procurar uma função existente por *data source*, mas só pelo nome e na região
  configurada.
- Cap. 2, nova §2.2.6 "Positioning of This Work" com o resumo e a Tabela 2.1. A tabela ganha a
  coluna *Discov.* e as linhas AWS CDK, AFCL/xAFCL e AWS Cloud Map/Consul; Terraform e Pulumi
  passam a P em *Discov.*; este trabalho passa a P em portabilidade de workflow (dois fornecedores)
  e em *Discov.* (uma conta/projeto, sem registo partilhado), com notas de rodapé. O resumo passa a
  três observações e diz os limites deste trabalho.
- Cap. 1 (`chapter1.tex`), §1.6: a descrição do Cap. 2 inclui os registos de serviços.
- Bibliografia: `ristov2021afcl`, `ristov2023xafcl`, `awsCloudMap`, `consulServiceDiscovery`,
  `awsCdk`, `terraformLambdaDataSource`, `pulumiLambdaGetFunction`.

### Estudo de caso: listagens que compilam e artefactos gerados

- Cap. 5 (`chapter6.tex`), Listing 5.1: `params` em vez de `input` (o DSL não tem `input`),
  `description` em cada passo (obrigatória no `StepBuilder`), e corrigido um erro de lógica — o
  ramo de aprovação caía em `decline-transaction`, e uma transação aprovada acabava `"declined"`.
  Os dois ramos registam agora a decisão e juntam-se em `report-decision`, a chamada externa ao
  core banking (corpo = transação, decisão em *query*). A listagem foi compilada e executada.
  Texto de §5.1, §5.2 e §5.5 ajustado.
- Cap. 5, nova §5.4 "What the Deployments Produce": registos AWS e GCP gerados pelos resolvers e
  renderers reais, com o fornecedor substituído por *test doubles* (Listings 5.2 e 5.3); a segunda
  implantação não chama o deployer, não altera o registo e produz o mesmo state machine.
- Cap. 2, Listing 2.3, e Cap. 3, Listings 3.3–3.4: `params` em vez de `input`, `description` nos
  passos (e no workflow `FraudPipeline`), e `variables(variable(...) equalTo ...)` no `assign`.
  Verificadas por compilação.

### Alinhamento com o código do merge de `experiment/gcf-gen2` (`9521625`)

- Resumos EN e PT: "Forty-five"/"Quarenta e cinco" passam a "Forty-six"/"Quarenta e seis" testes.
- Cap. 6 (`chapter7.tex`), §6.2: 46 testes dos resolvers (23 AWS, 23 GCP); nova linha na tabela de
  correção — chamada interna com corpo (só AWS) recusada antes da cascata, nada implantado.
- Cap. 4 (`chapter5.tex`), §4.3: corrigida a afirmação de que a Lambda recebe "the step's
  payload". O `AwsHttpTemplate` só lê `queryStringParameters`; uma chamada interna na AWS passa as
  entradas por `query(...)`, e o `AwsInternalFunctionResolver` recusa uma chamada com corpo.
- Cap. 5 (`chapter6.tex`), Listing 5.1: as chamadas de pontuação passam a transação por `query`;
  as condições usam `withKey` (`fraudResult.body.flagged` no GCP, `$.fraud-check.fraudResult.flagged`
  na AWS). Novo parágrafo em §5.2 a explicar as duas escolhas. §5.6: nova limitação — no renderer
  AWS do OmniFlow, um `assign` gera um estado `Pass` que substitui o estado inteiro, e
  `report-decision` perde a transação.
