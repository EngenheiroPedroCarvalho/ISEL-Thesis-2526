# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `72810b0` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-23 — Cinco imprecisões (terceira análise, pontos C1–C5)

- **Cap. 6 (`chapter7.tex`), §6.8:** o texto dizia "three values" carregados à mão e enumerava
  quatro (client ID, secret, `access_token`, URL), como conta a Tabela 6.9; passou a "four". Dizia
  também que cada um desses erros produz um workflow que se implanta e só falha em execução, o que só
  vale para o URL: um erro nos outros três impede o deployment da função. Reescrito nesse sentido.
- **Cap. 6, §6.7 "The deployment itself":** "On AWS the resolver resolves the execution role"
  passou a "the deployer" (é o `AwsLambdaDeployer`, §4.2.2).
- **Cap. 4 (`chapter5.tex`), §4.1.9 "From a trusted cache…":** "a hit is now confirmed against
  `GetFunction` on both providers" — no GCP a validação é contra o Cloud Run. Passou a "confirmed
  live on both providers, through `GetFunction` on AWS", e o tratamento de drift e de função apagada
  remete para §4.1.6.
- **Cap. 3 (`chapter3.tex`), §3.1.1, Nível 1:** "a deleted one removes it and aborts the
  deployment" omitia a redescoberta noutras regiões (§3.4.1, §4.1.6, Tabela 6.1, Fig. A.1 e o
  código). Passou a dizer que a função é procurada nas outras regiões, a entrada substituída se for
  encontrada e removida caso contrário, abortando então a implantação.

## 2026-09-23 — Cortar repetições locais (terceira análise, pontos A1–A8)

- **Cap. 3 (`chapter3.tex`), §3.4.1 (A7):** a segunda execução do walkthrough deixa de recontar a
  ligação estática (revalidação no Nível 1, exceção da 1.ª geração, workflow antigo a chamar o
  endpoint velho) e remete para "Static binding" em §3.1.1, que citava duas vezes na mesma frase.
- **Cap. 4 (`chapter5.tex`):**
  - §4.1.6 (A2): a frase "What distinguishes the two resolvers…" deixa de repetir a diferença na
    descoberta de regiões, dita na frase anterior.
  - §4.1.9 "A metadata record per function" (A3): a mesma frase dizia duas vezes que a fixture de
    benchmark não faz validação na cloud; fica uma.
- **Cap. 6 (`chapter7.tex`):**
  - §6.7 (A1): a frase sobre T6/T7 acrescentada na correção anterior repetia a abertura da secção;
    reduzida a uma oração.
  - §6.4, T3 (A4, A5): retirada a frase "T4 and T5 confirm the same trend on real Workflow objects…",
    que o parágrafo de T4 repete; "eliminates that call-times-registry-size product: the cost no
    longer multiplies the two factors together" passou a "removes that product".
  - §6.10 (A5, A6): "call-times-registry-size cost they used to pay" passou a "per-call reads they
    used to pay for"; o parágrafo "Global framing" deixa de repetir que a leitura única está nos dois
    resolvers e de recontar a abertura de §6.7, ficando com a ideia nova (os valores são um limite
    inferior).
- **Cap. 7 (`chapter8.tex`), §7.2 (A8):** a resposta à RQ1 deixa de repetir as duas ressalvas (testes
  locais, 1.ª geração no GCP), que os objetivos 2 e 4 dão logo abaixo, e remete para eles; a resposta
  à RQ2 deixa de repetir que as chamadas ao fornecedor são contadas e não medidas (objetivo 4).
