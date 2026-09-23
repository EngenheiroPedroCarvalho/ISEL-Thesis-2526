# Alterações feitas após a revisão de setembro de 2026

Nota de acompanhamento do PDF novo (`template.pdf`, 94 páginas, compilado a 23/09/2026).

Responde às **14 anotações** do PDF revisto `pedro-mst 1.pdf`: as 7 da primeira leitura (17/09) e
as 7 acrescentadas na segunda (20/09). Estão todas aplicadas.

---

## Antes de mais: a numeração dos capítulos mudou

A primeira nota pedia que o Background e o Related Work passassem a um só capítulo. Feito — e por
isso **todos os capítulos seguintes descem um número**. Ao procurar no PDF novo o sítio de uma
nota, é preciso traduzir:

| No PDF revisto | No PDF novo |
|---|---|
| 2 Background **+** 3 Related Work | **2** Serverless Portability: Background and State of the Art (§2.1 Background, §2.2 Related Work) |
| 4 Proposed Solution | **3** |
| 5 Implementation | **4** |
| 6 Case Study | **5** |
| 7 Evaluation | **6** |
| 8 Conclusions | **7** |

As etiquetas internas (`cha:background`, `ch:related-works`) não foram renomeadas, para não partir
referências cruzadas.

### Depois de 21/09: duas subsecções a menos em §4.1

A 23/09 a dissertação foi revista duas vezes à procura de informação repetida, e cada assunto
passou a ser explicado num só sítio, com remissões nos outros. Os capítulos mantêm a numeração, mas §4.1 perdeu
duas subsecções, que não traziam nada que não estivesse noutro lado:

| PDF de 21/09 | PDF de 23/09 |
|---|---|
| §4.1.9 Registry Location | integrada em §4.1.3 Registry File Format |
| §4.1.10 Evolution of the Registry Design | **§4.1.9** |
| §4.1.11 Summary | retirada (repetia §3.1.1 e §4.1.5) |

Com estes cortes o PDF passou de 98 para 94 páginas, por isso as páginas indicadas abaixo são as do
PDF de 23/09.

---

## As 14 notas, uma a uma

### Primeira leitura (17/09)

| # | Nota (pág. no PDF revisto) | O que foi feito | Onde ver agora |
|---|---|---|---|
| 1 | p. 27 — "Juntar background com related work" | Os dois capítulos passaram a um só, com o título "Serverless Portability: Background and State of the Art". As quatro secções de background passaram a subsecções de §2.1; o Related Work e as suas quatro subsecções ficaram em §2.2. | Cap. 2, **p. 7** |
| 2 | p. 27 — "Alterar parágrafo inicial e ter duas secções, uma de background e outra de related work" | O capítulo tem hoje exatamente duas secções. O parágrafo de abertura foi reescrito: abre com "This chapter has two parts" e anuncia as duas, em vez de anunciar um capítulo só de background. | Cap. 2, abertura, **p. 7** |
| 3 | p. 30 — "Rever isto" | Revisto o parágrafo sobre o que o QuickFaaS provisiona no GCP, contra o código e a documentação atual da Google. Registada a renomeação das duas gerações (hoje *Cloud Run function (1st gen)* e *Cloud Run function*) e explicado porque é que a dissertação mantém os nomes por geração: é o que distingue as duas APIs que o código chama. URL corrigido para `<project-id>`. Acrescentada a ressalva de que o domínio `cloudfunctions.net` não prova que a função é de 1.ª geração. | §2.1.3.3 "FaaS Deployment Model: The ZIP Strategy", **p. 10** |
| 4 | p. 36 — "Relê o texto todo, há partes que não se está a perceber bem, vê o CODE" | Secção relida por inteiro. O parágrafo de abertura passou a nomear os quatro grupos antes de os discutir (antes dizia "each group" sem nunca dizer quais). O parágrafo do CODE foi reescrito contra o artigo original (Ristov et al., FGCS 160, 2024): alvo, hierarquia de três níveis, redução de até 9,23× em linhas de código, biblioteca de armazenamento unificada, e o posicionamento face à tese sustentado pelo próprio artigo. Corrigidos erros que quebravam frases e uma família sem numeração em §2.2.3. | §2.2, **p. 14**; parágrafo do CODE em §2.2.1, **p. 15** |
| 5, 6, 7 | p. 37 — "??", "Section 4.1.1??", "????" | Reescrito o parágrafo sobre a resolução de referências entre recursos em tempo de *deployment*. Frase introdutória nova a dizer o que os mecanismos têm em comum; cada fragmento de sintaxe passou a ser identificado (`!GetAtt MyFunction.Arn`, `Fn::GetAtt: [hello, Arn]`, `${aws_lambda_function.lambda.arn}`); e o ARN passou a ser decomposto campo a campo. As referências cruzadas soltas deixaram de ser um `§` isolado e passaram a nomear o que referenciam. | §2.2.1, **p. 16**; referência cruzada corrigida em §2.1.3.3, **p. 10** |

### Segunda leitura (20/09)

| # | Nota (pág. no PDF revisto) | O que foi feito | Onde ver agora |
|---|---|---|---|
| 8 | p. 41 — "Dizer também como está organizado o capítulo" | Acrescentado um parágrafo de roteiro no fim da introdução, no mesmo formato do que o capítulo seguinte já tinha. Nomeia as quatro secções e as duas subsecções. | Cap. 3, introdução, **p. 20** |
| 9 | p. 41 — "Todas as figuras têm de ter texto que as descreve, no código o mesmo, não pode ser apenas como se vê na listing 2.1" | Ver a secção seguinte: a nota estava marcada no cap. 3, mas foi aplicada a **todos** os capítulos. | Ver abaixo |
| 10 | p. 42 — "Step 3 in section xxx, em vez de above" | "Step (3) above" passou a "Step (3) of the end-to-end flow listed in Section 3.1", que nomeia a lista em vez de depender da posição na página. | §3.1.1 "The Resolution Cascade", **p. 22** |
| 11 | p. 48 — "Explica com algum detalhe" | A listagem dos ficheiros de registry era introduzida por uma só frase. Tem hoje dois parágrafos que a leem: os dois campos de topo, a chave (e quando é promovida a `"region/functionRef"`), os dois campos de cada valor, e o contraste entre os dois exemplos (URL HTTPS no GCP, ARN na AWS). Fica dito também o que *não* está no ficheiro. | Listing 3.1, **p. 26–27** |
| 12 | p. 49 — "Tens de explicar o código" | O parágrafo do descritor QuickFaaS remetia para a Listing 2.1 sem dizer o que lá está. Descreve hoje os campos em três grupos (onde, o quê, e o código), seguidos dos dois que importam à integração: `function.name` tem de ser igual ao `functionRef`, e `function.location` fixa a região. Acrescentada a ressalva de que na AWS o `accessToken` fica vazio, porque o *provider* lê as credenciais do ambiente. | Parágrafo "QuickFaaS Deployment Descriptor and Code File", **p. 28** |
| 13, 14 | p. 52 — "Rever" (×2) | Eram dois defeitos de composição na mesma frase: o nome de uma classe impresso partido com hífen entre linhas (como se a classe se chamasse `Google-CloudDeployer`) e outro identificador a entrar pela margem. Ambos corrigidos, e a frase — que acumulava dois parêntesis encaixados — foi dividida em duas. Sem alteração de conteúdo. | §4.1, abertura, **p. 31** |

---

## Nota 9 em detalhe: figuras e código explicados

A nota estava marcada sobre o título do capítulo Proposed Solution, mas aplica-se ao documento
todo, por isso foram revistas **todas** as figuras e listagens de todos os capítulos.

Quase todas já eram lidas pelo texto. Havia cinco lacunas, todas fechadas:

| Onde | O que faltava |
|---|---|
| Listing 2.1 (`func-deployment.json`), **p. 11** | Quatro campos visíveis na listagem ficavam por explicar. Novo parágrafo sobre `function.location`, `function.bucket`, e os dois opcionais `dependenciesFile` e `configurationsFile`. |
| Listing 3.1 (ficheiros de registry), **p. 26–27** | Nota 11, acima. |
| Listing 3.2 (call step com `internalFunction`), **p. 27** | Só tinha a frase "Listing 3.2 shows a call step". Novo parágrafo a explicar o código: o vocabulário comum a qualquer step, e sobretudo a ausência de `host`/`path`, substituídos pelo `functionRef` e pelo caminho do descritor. |
| Parágrafo do descritor QuickFaaS, **p. 28** | Nota 12, acima. |
| Listing 4.2 (estado Step Functions gerado), **p. 40** | O texto explicava quatro campos, mas a listagem mostra também `InputPath` e `Next`. Acrescentada a frase que os cobre. |

As restantes já tinham texto descritivo: o encadeamento da Listing 2.3 tem três parágrafos; o
modelo de classes, a sequência de *deployment* e a invocação em execução do cap. 4 têm um parágrafo
cada; e cada figura T do cap. 6 tem o seu parágrafo de discussão.

---

## Alterações não marcadas na revisão

Trabalho de consistência que saiu da revisão, mas que não estava assinalado:

- **Simetria no parágrafo do *deployment-time binding*** (§2.2.1, p. 16). Ao explicar os
  fragmentos de sintaxe da nota 7, ficou à vista que o CloudFormation era o único dos quatro
  exemplos a quem não se atribuía a dependência implícita, e que a referência do Terraform estava
  apenas glosada enquanto a do CloudFormation tinha decomposição completa. Ambas corrigidas: o
  `!GetAtt` passa a dizer que também fixa a ordem do *deployment*, e a interpolação do Terraform
  passa a ser lida pelas três partes (tipo de recurso, etiqueta, atributo).

- **Varredura de *overfull hbox*** (documento todo). As duas notas "rever" marcavam defeitos de
  composição, o que motivou uma passagem pelo log de compilação inteiro. Os avisos passaram de
  **23 para 8**. Os 8 que restam são do cabeçalho de capítulo do template e são espaço em branco:
  o título não chega a entrar na margem.

- **Pontuação** (documento todo). Removidos os 184 travessões (`---`) da prosa, substituídos pela
  pontuação que nomeia a relação entre as duas metades da frase (parênteses, dois pontos, vírgula
  ou ponto e vírgula, conforme o caso). Os meios-travessões ficaram, por serem uso correto:
  intervalos numéricos, o composto `OmniFlow–QuickFaaS`, e o marcador de "não" da tabela
  comparativa, que a própria legenda declara.

- **Referência do artigo do CISTI** (bibliografia). Reduzida ao que já se sabe — autores, título e
  ano — enquanto não houver local, páginas e DOI.

- **Cap. 5, parágrafo da Listing 5.1** (p. 42). Uma frase contrastava as chamadas internas com "the
  external `risk-score` variant" da Listing 3.4, que não mostra variante nenhuma com esse nome:
  mostra o passo `external-risk-api`, uma API de um parceiro. Reescrita.

---

## Estado da compilação

94 páginas, sem referências por resolver, 48 entradas na bibliografia para 48 citadas, e os 8
avisos de *overfull hbox* do template referidos acima.

## Limitação conhecida, documentada e não fechada

No GCP, a validação e a descoberta cobrem apenas serviços Cloud Run, enquanto o QuickFaaS implanta
funções de 1.ª geração. Uma ligação a uma dessas funções é aceite sem verificação em tempo real e
não pode ser redescoberta se sair do registry. Está descrita como limitação no cap. 4 e como
trabalho futuro no cap. 7.

O caminho para a fechar (migrar o QuickFaaS para a API v2 das Cloud Functions, o que torna cada
função um serviço Cloud Run e a faz cair na cobertura que já existe) está implementado e passa nos
testes locais, mas **nunca pôde ser validado contra o GCP**, porque as contas de faturação do
projeto estão fechadas. Por isso fica como trabalho futuro e não é reclamado na dissertação.
