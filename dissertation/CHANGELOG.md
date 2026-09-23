# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `475e21f` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-23 — Cortar repetições (análise de informação repetida, pontos A1, A3, A5, A11)

**A1 — limitação das Cloud Functions de 1.ª geração no GCP.** A explicação fica em §4.1.6; nos
outros sítios passou a uma oração com referência cruzada.
- Cap. 2 (`chapter2.tex`), §2.1.3.3: deixa de antecipar as consequências para o registo; mantém o
  facto (reconhecimento pelo domínio `cloudfunctions.net`) e a ressalva de que é uma heurística.
- Cap. 3 (`chapter3.tex`): encurtadas as menções no Nível 1 de §3.1.1, na segunda execução do
  walkthrough (§3.4.1) e no ponto *Stale binding*.
- Cap. 4 (`chapter5.tex`): encurtada a menção no ponto *Drift handling* (§4.1.4) e retirada a
  repetição no fim de §4.1.6.
- Cap. 7 (`chapter8.tex`), Future Work 1: a ressalva sobre o domínio passa a remeter para §2.1.3.3.

**A3 — criação e atualização do registo.** A lista fica em §4.1.4 (nova etiqueta
`subsec:registry-updates`).
- Cap. 3, §3.3 "Function Registry": retirada a frase "These values are not written manually…",
  repetida no fim da leitura da listagem (e que falava de funções "updated", contra a regra de não
  haver atualizações).
- Cap. 4: §4.1.1 remete para §4.1.4 em vez de repetir a lista; retirada de §4.1.6 a frase sobre o
  `updatedAt`; §4.1.10 "From a manually edited file…" remete para §4.1.4 em vez de repetir a lista.

**A5 — passos do `AwsLambdaDeployer`.** A descrição fica em §4.2.2.
- Cap. 4: §4.1.7 e a abertura de §4.2 deixam de enumerar os passos (papel IAM, região do bucket,
  subprocesso, polling).
- Cap. 5 (`chapter6.tex`), §5.3: remete para a Figura da sequência AWS sem voltar a enumerar.

**A11 — lacuna do Related Work (§2.2).**
- Fundidos os dois parágrafos finais de §2.2.1 ("What none of them offers…" e "Overall, this
  category…") num só.
- O parágrafo final de §2.2.4 deixa de reenunciar a lacuna já dita no texto da tabela; mantém o
  alinhamento com os serviços nativos do fornecedor e a implantação a pedido.

## 2026-09-23 — Cortar repetições (pontos A2, A4 e C1)

**A2 — o problema de copiar endpoints à mão.** Fica contado no cap. 1 (§1.2); os outros sítios
remetem para lá.
- Cap. 1 (`chapter1.tex`), §1.2.2: retirada do ponto "The endpoint exists only after deployment" a
  frase sobre a ordem entre as duas ferramentas, que repetia o ponto "Manual pre-deployment" de
  §1.2.1.
- Cap. 2 (`chapter2.tex`), fim de §2.1.4.3: o parágrafo "A practical limitation…" passa a remeter
  para §1.2 em vez de recontar o problema.
- Cap. 4 (`chapter5.tex`), §4.1.1 Motivation: reduzida a duas frases, sem a frase quase igual à da
  abertura do cap. 3.
- Cap. 4, §4.1.10: retirado o parágrafo "From literal endpoints to logical indirection" (repetia
  §4.1.1); a abertura passa de cinco para quatro linhas de evolução.

**A4 — formato do ficheiro de registo.** §4.1.3 deixa de repetir a descrição de §3.3 (campos,
`serviceName`/`url`, HTTPS vs ARN, `updatedAt`) e remete para ela e para a listagem; ficam as duas
convenções que só aparecem num registo em uso.

**C1 — incoerência sobre atualizações.** Cap. 3 (`chapter3.tex`), §3.3: "To deploy (or update) a
function managed this way" passou a "To deploy a function managed this way", de acordo com a regra
de que a cascata nunca atualiza uma função existente (§7.3).

## 2026-09-23 — Cortar repetições (pontos A6–A10, A12, A13, C2 e C3)

**A6 — Function URL e invocação nativa.** Cap. 4 (`chapter5.tex`), §4.2.1: diz só que a
integração não usa a Function URL; o porquê fica em §4.2.2. Cap. 7 (`chapter8.tex`), §7.1:
retirado "keeping the invocation within the AWS control plane", já argumentado em §4.3.

**A7 e C2 — stale e permissões.**
- Cap. 4, §4.1.8: a abertura passa a dizer que são três as restrições já enunciadas em §3.4.1
  (função por resolver, referência ambígua, entrada *stale*) e que resta uma de implementação (antes
  dizia "remaining two", contando *stale*, que já estava em §3.4.1). Retirado o parágrafo "Stale
  registry entries", que repetia §3.4.1.
- Cap. 4, §4.1.6: a exclusão de regiões sem permissão durante a varredura passa a remeter para
  §4.1.8 em vez de a descrever outra vez.
- As remissões para *stale* em §4.1.4 (Drift handling) e §5.3 (`chapter6.tex`) passam a apontar
  para §3.4.1; a de §6.2 (`chapter7.tex`) aponta para §3.4.1 e §4.1.8.

**A8 — dupla verificação da exclusividade.** Cap. 3 (`chapter3.tex`), §3.3: retirada a frase
"This rule is enforced twice…", que fica em §4.1.2 e na lista de erros de §3.4.1.

**A9 — pipeline do OmniFlow.** Cap. 2 (`chapter2.tex`), §2.1.4: a lista Definition / Rendering /
Deployment de §2.1.4.3 passou a uma frase que remete para os componentes de §2.1.4.1; a disciplina
begin/end do rendering, que só estava ali, passou para o item Renderer. Retirada de §2.1.4.1 a
enumeração dos campos do workflow (fica em §2.1.4.2) e retirado o parágrafo "Once a workflow is
defined, OmniFlow renders it…", que o repetia pela terceira vez.

**A10 — Background.**
- Cap. 2, §2.1.1: o ZIP como denominador comum passa a remeter para §2.1.3.3, onde é explicado.
- Cap. 2, §2.1.2: a frase sobre o orquestrador deixa de repetir o argumento da *statelessness* de
  §2.1.1.
- Cap. 3, §3.4: retirada a terceira enumeração dos campos do descritor QuickFaaS (ficam §2.1.3.4 e
  §3.3).

**A12 — endurecimento no caso de estudo.** Cap. 5, §5.5: a lista (registo com controlo de acesso,
histórico de escritas atribuível, IAM de menor privilégio) deixa de ser repetida e remete para
§7.4.

**A13 — limite dos testes.** Cap. 6, §6.2: a frase quase igual à de §6.11 passou a uma remissão.

**C3.** Cap. 4, §4.1.6: `"<region> /<functionRef>"` corrigido para `"region/functionRef"`.

## 2026-09-23 — Cortar repetições (secção B da análise)

- **Cap. 1 (`chapter1.tex`), §1.5, contribuição 3:** deixa de repetir a justificação do objetivo 3
  (a sobreposição dos dois fornecedores só no GCP).
- **Cap. 3 (`chapter3.tex`):**
  - §3.1: retirado "on a single cloud provider" da frase de objetivo; o âmbito é enunciado em §3.1.1.
  - §3.1.1, "Provider and region scope": "a pesquisa começa na região do workflow" aparecia duas
    vezes no mesmo parágrafo; fica uma.
  - §3.3: o parágrafo "Function Ownership Determines Resolution" fica só com o critério (quem é dono
    da função decide a forma da chamada) e absorve a regra de que as chamadas externas não entram no
    registo; retiradas as outras duas menções a essa regra (parágrafo "Function Registry" e fim da
    leitura da listagem) e a menção à posse na lista das duas formas.
  - §3.4: retirado "and use it consistently in the workflow and in the QuickFaaS descriptor" (a
    regra fica em §3.3 e a consequência em §3.4.1).
- **Cap. 4 (`chapter5.tex`):**
  - Abertura: retirado o parágrafo sobre a "two-part structure", que o parágrafo de organização
    seguinte repetia.
  - §4.1.2: retirada a frase sobre as chamadas externas, dita em §4.1.5.
  - §4.1.3: absorve a localização do ficheiro; retirada a subsecção "Registry Location".
  - §4.1.6: a leitura única deixa de ser anunciada antes de ser descrita; retirada a frase "symmetric
    in shape, differing only…", repetida no fim da subsecção.
  - §4.1.10: retirada a menção ao snapshot, descrito em §4.1.6.
  - Retirada a subsecção "Summary" do fim de §4.1, que repetia §3.1.1 e §4.1.5.
- **Cap. 5 (`chapter6.tex`), §5.3 e §5.5:** a primeira implantação na AWS remete para o walkthrough
  de §3.4.1 em vez de o recontar; retirado "Because PaymentAuthorization names no provider" (dito em
  §5.2); a atualização in-place que mantém o ARN/URL remete para §3.1.1.
- **Cap. 6 (`chapter7.tex`):** retirada da abertura a frase que antecipava as partes da avaliação
  (listadas em §6.1); em §6.10, "Global framing" deixa de repetir os 29/81 µs do ponto anterior; em
  §6.11, retirado "The two sessions agree on the quantity they share" (dito em §6.3).
