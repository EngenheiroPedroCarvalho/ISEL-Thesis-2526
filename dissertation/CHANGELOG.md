# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `82235e2` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)


## 2026-09-27

### Funções GCP de 2.ª geração (merge de `experiment/gcf-gen2`)

O QuickFaaS passou a publicar no GCP funções de 2.ª geração (Cloud Run functions), e a limitação
da 1.ª geração deixou de existir. O texto foi alinhado com o código:

- **Cap. 2** (`chapter2.tex`, "FaaS Deployment Model: The ZIP Strategy"): o parágrafo sobre a
  1.ª geração descreve agora o QuickFaaS *original* e diz que este trabalho move o provider GCP
  para a API Cloud Functions v2. Saiu a frase sobre a heurística do domínio `cloudfunctions.net`.
  A frase sobre a gestão das funções de 1.ª geração diz agora "Cloud Functions API rather than the
  Cloud Run Admin API" (antes: "that same first-generation API", mas a Google também as gere pela
  v2), com citação.
- **Cap. 3** (`chapter3.tex`): o Nível 1 deixa de ter a exceção da 1.ª geração; o exemplo do
  registo GCP (Listing `lst:registry-structure` e o texto antes dele) usa um URL `run.app` e o
  nome completo do serviço Cloud Run como `serviceName`.
- **Cap. 4** (`chapter5.tex`):
  - nova subsecção **"Cloud Run Functions on GCP"** (`subsec:gcp-cloud-run-functions`), que
    substitui o parágrafo "Limitation: first-generation Cloud Functions on GCP": pedido v2
    (`buildConfig`/`serviceConfig`, `functionId`), espera pelo estado `ACTIVE`, URL lido de
    `serviceConfig.uri`, `roles/run.invoker` no serviço Cloud Run, o que o `QuickFaasDeployer`
    regista, e o que continua fora (funções de 1.ª geração criadas fora da integração; o template
    de storage ainda é de 1.ª geração);
  - "Registry File Format": todas as entradas GCP têm o nome completo do serviço e um URL
    `run.app`;
  - "Registry Store and Resolver Internals": saiu a exceção da 1.ª geração;
  - introdução do capítulo e "Evolution of the Registry Design" (onde fica o histórico da
    exceção) atualizados.
- **Cap. 5** (`chapter6.tex`): o Nível 3 no GCP publica Cloud Run functions; as chamadas usam
  URLs `run.app`; saiu a referência à limitação.
- **Cap. 6** (`chapter7.tex`):
  - tabela de correção: a linha da 1.ª geração passa a ser "entrada com URL `cloudfunctions.net`
    validada contra o Cloud Run";
  - tabela de pedidos à API: saiu a linha "first-generation Cloud Function in the registry";
  - deploy de Nível 3 no GCP descrito com a API v2 e o IAM no serviço Cloud Run;
  - resposta à RQ1 com uma só ressalva (testes que simulam o provider);
  - T7 re-medido (ver abaixo).
- **Cap. 7** (`chapter8.tex`): saiu a exceção da 1.ª geração em "No update path"; "Partial GCP
  coverage" passa a "Cloud Run services only on GCP" (só funções de 1.ª geração criadas fora da
  integração); o Objetivo 2 passa de "partially met on GCP" a "met"; a RQ1 já não remete para o
  Objetivo 2; saiu o item 1 do trabalho futuro (feito); o resumo das contribuições menciona a
  passagem para a 2.ª geração.
- **Apêndice A** (`appendix-cascade.tex`): legendas dos Níveis 1 e 3 sem a 1.ª geração; figuras
  GCP dos Níveis 1 e 3 regeradas a partir de `diagrams/resolution-cascade-level{1,3}-gcp.puml`
  (sem o atalho `cloudfunctions.net`; API v2, `state = ACTIVE`, `roles/run.invoker`).
- **Glossário** (`glossary.tex`): entradas "Cloud Run" e "First-generation Cloud Function".

### T7 re-medido (Cap. 6, `chapter7.tex`; Cap. 7, `chapter8.tex`)

Sem o atalho da 1.ª geração, o T7 passou a validar cada *hit* no Cloud Run. O benchmark usa agora
o `CloudRunV2ServiceInspector` real com um `HttpClient` local, e foi medido de novo com as mesmas
opções (`-f 3 -wi 3 -i 5 -w 1 -r 1`).

- **Metodologia:** o T7 vem de uma terceira sessão; seis pontos do T6 medidos de novo nessa sessão
  ficaram 1–7% acima dos da segunda. Saiu a comparação da coluna N=1 do T7 com o T2.
- **T6/T7 ("Production resolvers"):** descrição do que substitui o provider em cada um; custo por
  chamada no GCP ~0,35 → ~0,95 µs; F=10/N=200: 81,3 → 199,1 µs. Novo parágrafo sobre a diferença
  entre fornecedores (parsing de URI + cliente Cloud Run, que o *fake* do T6 dispensa). O
  parágrafo sobre o *logging* indica agora os valores com que se compara.
- **Tabela e figura do T7:** valores novos; legendas atualizadas.
- **Discussão (RQ2)** e **Cap. 7 (RQ2):** 81 → 199 µs; **Threats to Validity:** "later measurement
  sessions".
