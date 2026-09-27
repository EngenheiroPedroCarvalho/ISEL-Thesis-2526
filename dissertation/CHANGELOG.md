# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `1810f90` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-27

### RQ2 como custo local, autenticação no GCP e registo por máquina

- Resumos EN e PT (`abstract-en.tex`, `abstract-pt.tex`): o custo em µs passa a ser "o trabalho
  local" da resolução; acrescentado o limite de pedidos ao fornecedor (um por chamada interna com o
  registo atualizado) e a implantação na cloud "medida em minutos". Retirada a afirmação de que o
  custo não cresce com o tamanho do registo (cresce, pela leitura única; T5).
- Cap. 1 (`chapter1.tex`): RQ2 reformulada (trabalho local, crescimento e número de pedidos ao
  fornecedor); o objetivo 4 passa a incluir a contagem de pedidos; o desafio "No shared record"
  passa a "No verified link".
- Cap. 6 (`chapter7.tex`), §6.1: a exclusão das operações na cloud passa a ser justificada — a
  latência é conhecida (minutos, dominada pelo fornecedor, com as ferramentas separadas ou
  integradas) e medi-la de novo mediria o fornecedor; a pergunta de RQ2 inclui os pedidos,
  contados a partir da implementação. §6.10: a resposta a RQ2 inclui a contagem de pedidos; o
  "Global framing" diz que os valores são só a parte local e que a implantação leva minutos (antes
  "seconds"). §6.11: a falta de evidência contra os fornecedores reais passa a ser escolha de
  âmbito, remetendo para §6.1.
- Cap. 4 (`chapter5.tex`), §4.3 Runtime Invocation: novo parágrafo sobre o GCP — chamada
  `http.<verbo>` com o URL `run.app`, autenticação OIDC declarada no passo
  (`GoogleCallRenderer`), `run.invoker` dado à conta de serviço do workflow, e a redundância
  enquanto o QuickFaaS der o mesmo papel a `allUsers`.
- Cap. 7 (`chapter8.tex`): resposta a RQ2 com a contagem de pedidos; objetivo 4 diz que os tempos
  de ponta a ponta não foram medidos por opção; na avaliação crítica, novo ponto "A per-machine
  index, not a shared store" e, em "Broad, automatic IAM", a concessão a `allUsers` no GCP.

### Revisão de estilo (sem alteração de conteúdo)

- Todos os capítulos, o apêndice A e os dois resumos: grafia unificada em inglês britânico
  (*optimised*, *optimisation*, *organisations*, *authorisation*, *standardisation*, *modelled*,
  *data centres*, *catalogue*…). Não mudam identificadores de código nem etiquetas
  (`tab:eval-t4-optimized` mantém-se), nem o texto dentro das listagens.
- Remissões: `\S\ref{}` passa a `Section~\ref{}` em todo o texto (a remissão para o estudo de caso no
  Cap. 6, Evaluation, passa a `Chapter~\ref{cha:case-study}`); retiradas duas remissões duplicadas
  seguidas (Cap. 4, §4.1.3; Cap. 5, §5.3).
- Frases longas partidas (cerca de 40) nos Caps. 1–6 e nos dois resumos. No Cap. 4, a interface
  do `FunctionRegistryStore` passa a lista; no Cap. 6, a sequência de implantação do Nível 3
  (AWS e GCP) passa a duas listas numeradas.
- Cap. 2 (`chapter2.tex`), §2.1.3: corrigida a frase incompleta "Lastly, the location of the
  cloud-agnostic function file…" (passa a nomear o campo `functionFile`) e a remissão vaga "as
  shown in the configuration context" (passa a apontar o campo `bucket` da Listing 2.1).

### Diagramas: correções de coerência com o código e o texto

- Cap. 3 (`chapter3.tex`), Fig. `call-step-model`: a nota passa a dizer `host = path = ""` (string
  vazia, como no `CallContextBuilder`) em vez de `host = path = e`.
- Cap. 3, Fig. `architecture-after`: a nuvem GCP passa a "Cloud Run functions (GCP)" (2.ª geração);
  o QuickFaaS passa a "(+ AwsProvider, GCP v2 API)"; a legenda mostra «new» e «extended» nas
  amostras de cor, que antes ficavam quase invisíveis.
- Cap. 4 (`chapter5.tex`), Fig. `aws-deploy-sequence`: `path = ""` em vez de `path = e`; a nota do
  Nível 1 remete para o diagrama do Apêndice A em vez de um ficheiro `.puml`.
- Cap. 4, texto da Fig. `aws-deploy-sequence`: a referência aos três níveis da cascata aponta para
  `sec:resolution-cascade` (Cap. 3) em vez de `subsec:endpoint-resolution`.
- Apêndice A (`appendix-cascade.tex`), Figs. A.1–A.3: as notas que citavam ficheiros `.puml`
  passam a remeter para "the Level N diagram" ou para o diagrama de sequência AWS do Cap. 4.
- PNG regenerados com PlantUML 1.2026.8 e copiados para `images/`.

### Diagramas: pontos menores da revisão

- Cap. 3 (`chapter3.tex`), Nível 2 da cascata: a consulta única com `"region/functionRef"` passa a
  valer para os dois fornecedores (antes lia-se só como caso AWS).
- Cap. 3, Fig. `architecture-after`: nova seta Workflows → Cloud Run functions ("HTTP call (run.app
  URL)"), simétrica da seta Step Functions → Lambda.
- Cap. 4 (`chapter5.tex`), Fig. `aws-deploy-sequence`: o bootstrap passa pelo
  `FunctionRegistryBootstrapper` (`LambdaFunctionsCatalog`); o passo IAM inclui a reparação da
  *trust policy* (`UpdateAssumeRolePolicy`); os passos do QuickFaaS incluem `GetBucketLocation`, o
  `GetFunction` inicial e o *waiter* do SDK.
- Apêndice A (`appendix-cascade.tex`), Fig. A.1 (Nível 1, GCP e AWS): novo ramo para a exceção de
  sufixo ambíguo no registo; a nova procura diz que consulta todas as regiões ou só a de
  `"region/functionRef"`.
- Apêndice A, legenda da Fig. A.2: a forma `"region/functionRef"` descrita para os dois fornecedores.
- Apêndice A, Fig. A.3 (Nível 3, GCP): nota com o que o QuickFaaS faz no subprocesso (criação v2,
  espera por `ACTIVE`, `roles/run.invoker` para `allUsers`).
