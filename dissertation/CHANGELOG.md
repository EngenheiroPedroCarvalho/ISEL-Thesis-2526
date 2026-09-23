# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `7a9f2e4` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-23 — Três incoerências (segunda análise de repetições, pontos C1–C3)

- **Cap. 6 (`chapter7.tex`), §6.7 "What the count grows with":** a frase dizia que as *I*
  validações ao fornecedor eram "the mechanism behind the growth in N, flat in F, that T6 and T7
  show", mas §6.5 diz que T6 e T7 não chegam ao fornecedor (*fake* local na AWS, validação saltada
  no GCP). Passa a dizer que T6 e T7 não incluem esses pedidos e que, com um fornecedor real, cada
  um acrescenta uma ida e volta de rede por chamada interna.
- **Cap. 4 (`chapter5.tex`), §4.2.2:** o descritor AWS era descrito como diferente do GCP "mainly
  in cloudProvider, the S3 bucket, the Lambda runtime, and the iamRoleArn field", mas as duas
  listagens têm o mesmo `runtime` (`java17`). Reescrito: tem os mesmos campos, com valores AWS
  (`cloudProvider`, `project`, `location`, `bucket`), `accessToken` vazio, e acrescenta `iamRoleArn`.
- **Cap. 4, Listing 4.1:** acrescentado `"accessToken": ""`. O QuickFaaS declara o campo como
  obrigatório (`DeploymentData.accessToken: String`) e o `AwsLambdaDeployer` não o injeta, por
  isso um descritor sem ele não seria lido; fica também de acordo com §3.3 ("on AWS the field is
  left empty") e com o `AWS-Setup-Guide.md`.

## 2026-09-23 — Cortar repetições locais (segunda análise, pontos A1–A8)

- **Cap. 2 (`chapter2.tex`):**
  - §2.1.4.2 (A7): o parágrafo "The specialisation is disjoint…" deixa de repetir que as categorias
    são quatro e disjuntas e de voltar a enumerar os construtos; o parágrafo seguinte diz "for the
    other categories" em vez de as nomear outra vez.
  - §2.2 (A8): retirada do texto da tabela a frase sobre as marcas de portabilidade herdadas, que a
    nota ᵃ da Tabela 2.1 diz por palavras quase iguais.
- **Cap. 3 (`chapter3.tex`):**
  - §3.1.1, "Provider and region scope" (A5): deixa de recontar a pesquisa por regiões e a forma
    `"region/functionRef"`, já descritas no Nível 2.
  - §3.3 (A1): a lista "two distinct ways" deixa de repetir a exclusividade `host`/`path` vs.
    `internalFunction` do parágrafo da Fig. 3.3 e fica com o que acrescenta (há ou não deployment).
  - §3.3 (A4): retirada da leitura da Listing 3.1 a explicação de porque a validação custa uma só
    chamada, dada no Nível 1 de §3.1.1.
  - §3.4 (A3): o fim do parágrafo das Listings 3.3/3.4 deixa de resumir o que o walkthrough conta e
    remete para §3.4.1.
  - §3.4.1 (A2): "QuickFaaS deploys the function under the descriptor's name" aparecia em duas frases
    seguidas; fica uma. Retirada a frase final "The two names must therefore be kept equal" (a regra
    está em §3.3).
- **Cap. 4 (`chapter5.tex`), §4.1.6 (A6):** o resolver AWS deixa de repetir, passo a passo, o que é
  igual ao GCP (validação, drift, redescoberta, pesquisa e ambiguidade); fica o que é próprio da
  AWS. O parágrafo das escritas no registo remete para os pontos de §4.1.4 e fica com o que é novo
  (a escrita acontece antes da injeção; um hit sem alteração não escreve).
  No parágrafo reescrito, `"region/functionRef"` passou a partir com `\allowbreak` (saía 25 pt pela
  margem); os avisos de *overfull hbox* voltaram aos 8 do template.

## 2026-09-23 — Cortar repetições moderadas (segunda análise, secção B)

- **Cap. 1 (`chapter1.tex`):** §1.2.3 "Metadata synchronization" deixa de antecipar "so that the
  endpoint is never carried across by hand" (dito em §1.3); o ponto do cap. 6 em §1.6 deixa de
  repetir a lista de medições da contribuição 5 e passa a nomear as duas perguntas.
- **Cap. 2 (`chapter2.tex`), §2.1.3.4:** `function-configs.json` já não é explicado outra vez (fica
  em §2.1.3.3).
- **Cap. 3 (`chapter3.tex`):** o objetivo *workflow-first* deixa de ser reenunciado na abertura de
  §3.1 e no início de §3.2 (fica na abertura do capítulo).
- **Cap. 4 (`chapter5.tex`):** retirada a frase de abertura de §4.1, que dizia o mesmo que §4.1.1;
  §4.1.5 deixa de resumir os três níveis logo antes de dizer que não os repete.
- **Cap. 5 (`chapter6.tex`):** a abertura deixa de antecipar que as duas funções de scoring passam a
  internas (dito em §5.2); a chamada de liquidação externa deixa de ser explicada em §5.2 e §5.4
  (fica em §5.1); em §5.3, a implantação no GCP deixa de repetir que o workflow não muda; §5.5
  remete para §7.5 (onde está o endurecimento proposto) em vez de §7.4.
- **Cap. 6 (`chapter7.tex`):** em §6.10 retirada a explicação da diferença AWS/GCP (dada em §6.5);
  em §6.11 retirado o parágrafo "The evaluation is also intentionally local-only…" (dito em §6.1 e
  §6.10), com a remissão de §6.10 a passar para §6.1 e o parágrafo seguinte a abrir com "is local
  as well" em vez de "has a similar limit".
- **Cap. 7 (`chapter8.tex`):** §7.1 deixa de repetir a frase sobre a estratégia
  `InternalFunctionDeployer` (está em §4.1); §7.4 remete para o endurecimento de §7.5 em vez de o
  listar, e o item "Harden the registry" de §7.5 passa a dizer "access-controlled", que só estava em
  §7.4; o item "Replace the subprocess boundary" remete para §3.2 em vez de repetir a
  justificação.
