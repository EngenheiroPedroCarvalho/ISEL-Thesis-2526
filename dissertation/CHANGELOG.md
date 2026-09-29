# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `39e2394` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-29

### Agradecimentos (`acknowledgments.tex`): texto novo, em inglês

- Os agradecimentos foram reescritos (ISEL e orientadores, Nuno Bartolomeu, Santander e equipa de
  Enterprise Architecture, Bernardo Cardoso, amigos, família, Mariana) e traduzidos para inglês,
  a língua da dissertação.

### Cap. 6 (`chapter7.tex`): tabelas depois da primeira referência

- Tabelas 6.2 (`tab:eval-summary`) e 6.6 (`tab:eval-s1`): `[tbp]` passa a `[hbp]`. Com `[tbp]`,
  as duas iam para o topo da página, antes do parágrafo que as refere (pp. 35 e 38); agora ficam
  logo a seguir a ele. As Tabelas 6.1, 6.3, 6.4 e 6.5 já ficavam depois da referência.
- As mesmas duas tabelas: `\captionsetup{belowskip=0pt}`. O template põe 1 cm acima da legenda
  das tabelas (`packages.clo`), que no meio do texto se somava ao espaço do float (cerca de 42 pt
  entre o parágrafo e a legenda); agora ficam cerca de 13 pt.

### Cap. 6 (`chapter7.tex`), secção 6.3 (Methodology): reference resolver e production resolvers

- Novo parágrafo no fim da metodologia: T1, T2, T4, T5 e T10–T14 usam um reference resolver
  escrito para os benchmarks (duas variantes: lê o registry a cada chamada interna ou uma só vez),
  que não valida os hits no fornecedor; T3 chama o registry store diretamente; T6 e T7 medem os
  production resolvers (`AwsInternalFunctionResolver` e `WorkflowInternalFunctionResolver`), as
  classes que o OmniFlow corre num deployment. Os dois termos eram usados sem definição.
- Secção 6.2: o parêntese de "Each production resolver (...)", primeiro uso do termo no capítulo,
  acrescenta "the classes OmniFlow runs when it deploys a workflow".

### Cap. 6 (`chapter7.tex`), secção 6.2: o registry nos testes dos resolvers

- "The registry, in contrast, is a real JSON file in a temporary directory" passa a dizer que o
  registry não é um test double (os testes usam a mesma classe, `FunctionRegistryStore`, que o
  OmniFlow usa num deployment) e que o diretório temporário é criado vazio pelo JUnit
  (`@TempDir`) antes de cada teste e apagado depois, sem tocar nos registries do diretório de
  trabalho.

### Glossário (`glossary.tex`): nova entrada "Unit test"

- Teste automático que exercita uma unidade de código (classe ou função) isolada, com as
  dependências externas substituídas por test doubles; corre localmente, depressa e de forma
  determinista. O termo aparece no resumo em inglês e no Cap. 6 (`chapter7.tex`) sem definição.

### Glossário (`glossary.tex`): nova entrada "Test double"

- Objeto que substitui uma dependência real num teste; nos testes dos resolvers, os test doubles
  fazem de fornecedor (lookup com respostas fixas por região, lista fixa de regiões, deployer que
  regista o que lhe pedem para implantar). O termo é usado no Cap. 6
  (`chapter7.tex`) sem definição.

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
