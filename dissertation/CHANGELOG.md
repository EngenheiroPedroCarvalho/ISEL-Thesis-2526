# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `39e2394` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-29

### Glossário (`glossary.tex`): nova entrada "ICT concentration risk"

- Definição do DORA, art. 3.º, alínea 29 (citação literal, com `\cite[Art.~3(29)]{dora2022}`), e
  uma frase a ligá-la ao caso de estudo (um workflow e as suas funções num único fornecedor).

### Cap. 4 (`chapter5.tex`), secção 4.2.2: espaçamento da p. 25

- Figura 4.6 (`fig:aws-runtime-invocation`): `[H]` passa a `[htbp]`. Com `[H]`, a figura e a
  Listagem 4.3 não cabiam na p. 25 e o LaTeX esticava os espaços à volta da Listagem 4.2; agora as
  duas ficam juntas na p. 26 e a p. 25 tem o espaçamento normal.
- Listagem 4.2 (`lst:aws-descriptor`): deixa de flutuar (sai `float=htbp`) e fica no texto, sem
  o espaço extra dos floats entre o parágrafo e a listagem.

### Cap. 5 (`chapter6.tex`): revisão do texto, mais direto e simples

- Todo o capítulo reescrito em frases mais curtas, sem mudar factos, números, referências,
  figuras, listagem nem tabela.
- Secção 5.5: o parágrafo sem título sobre a atualização do modelo passa a "Updating a function."
  e perde a frase "Promoting the workflow across accounts and providers needs no edit", que
  contradizia a saída parcial para a GCP.
- Secção 5.5: novo parágrafo "Auditability." (o requisito da secção 5.1 remetia para a discussão,
  que não o tratava): cumprido só em parte; a definição implantada mostra os endpoints e o registry
  a ligação atual, mas sem histórico nem registo de que deployment escreveu cada entrada.
- Secção 5.5, "Renderer defects fixed.": nova Listagem 5.2 (`lst:case-renderer-fix`) com o antes
  e o depois dos dois defeitos (estado `Pass` de `approve-transaction` e parâmetro `amount` de
  `fraud-check`); o texto diz que o `Pass` tinha `Result` sem `ResultPath`.

### Cap. 5 (`chapter6.tex`), secção 5.4: resultados das execuções e Figuras 5.1 e 5.2

- Parágrafo "AWS.": as seis funções que o bootstrap encontrou passam a ser identificadas (o stub
  do core banking e cinco de testes anteriores, uma em `eu-central-1`, que o workflow não usa), e
  diz-se que nenhuma das funções de scoring estava entre elas.
- Parágrafo "GCP.": sai a menção aos dois serviços que o bootstrap encontrou no projeto; os tempos
  e o segundo deployment (Nível 1, registry byte-idêntico) ficam.
- Figuras 5.1 e 5.2: passam a `[htbp]`; cada subfigura tem a largura da sua imagem, pelo que as
  legendas (a) e (b) ficam alinhadas com a imagem, e há um `\bigskip` entre as subfiguras.
- Listagem 5.1 (`lst:case-registry-new`): passa a `float=htbp`, como as outras listagens
  flutuantes; deixa de ficar partida entre as pp. 28 e 30 com a página das figuras no meio.

### Cap. 5 (`chapter6.tex`), secção 5.4: sai a remissão para as pastas do repositório

- Sai a frase "The workflow, functions, scripts and logs are in the repository folders
  `case-study-aws` and `case-study-gcp`"; a secção abre com as diferenças das execuções em
  relação à Listagem B.2.

### Cap. 5 (`chapter6.tex`): a GCP como ilustração da estratégia de saída

- Secção 5.3, parágrafo "GCP.": "For the exit strategy, ..." passa a "To illustrate the
  exit-strategy requirement (Section 5.1), the same workflow is also deployed to a second
  provider, ...".
- Secção 5.5: novo parágrafo "Exit strategy." antes dos limites: a definição do workflow passa
  para a GCP sem alterações, mas a saída é parcial (código das funções, referências com região e
  chamadas externas adaptados por fornecedor; limites 1, 3 e 4).

### Cap. 5 (`chapter6.tex`), secção 5.1: ICT por extenso

- Requisito "An exit strategy": "ICT services" passa a "information and communication technology
  (ICT) services" (primeira e única ocorrência no texto).

### Glossário (`glossary.tex`): nova entrada "ICT services"

- Definição do DORA, art. 3.º, alínea 21 (citação literal, com `\cite[Art.~3(21)]{dora2022}`), e
  uma frase a dizer que os serviços cloud do caso de estudo são serviços ICT.

### Cap. 5 (`chapter6.tex`): as contas de teste e de produção passam para a discussão

- Sai o requisito "One workflow, several accounts" (secção 5.1); ficam dois requisitos (estratégia
  de saída e auditabilidade).
- Secção 5.3 "Deploying Across Accounts and Providers" passa a "Deploying to AWS and GCP": o
  parágrafo "AWS test account" passa a "AWS." (como o da GCP) e sai o parágrafo "AWS production
  account". A introdução do capítulo deixa de falar em conta de teste e de produção.
- Secção 5.5 (Discussão): novo parágrafo "Several accounts" com o conteúdo que saiu (endpoint
  diferente por conta ou projeto, só os descritores mudam, um registry por conta, não partilhar o
  registry), agora também para projetos GCP, e a nota de que nenhuma promoção foi executada.
