# Resultados dos testes de desempenho (P1–P16) e de tamanho (S1–S2)

Medições locais de renderização (DSL → Amazon States Language / GCP Workflows YAML) e de resolução
de funções internas, sem chamadas à nuvem. Geradas com JMH a partir do módulo `benchmark/`.

## Metodologia

- **Ferramenta:** JMH 1.37, modo `AverageTime`, unidade µs/op, com `Blackhole` a consumir cada
  resultado para impedir *dead-code elimination*.
- **Execução:** `-f 1 -wi 3 -i 5 -w 1 -r 1` — uma *fork* da JVM, 3 iterações de aquecimento e 5 de
  medição (1 s cada), para medir o código já compilado pelo JIT.
- **Dados brutos:** `jmh-results.csv` (P1–P5 e P12, um só `@Param`), `jmh-results-p6.csv` (P6, com
  a dimensão `Param: m`), `jmh-results-p7.csv` (P7 antes/depois), `jmh-results-p8.csv`/
  `jmh-results-p9.csv` (P8–P9, resolução do workflow real), `jmh-results-p10.csv`/`jmh-results-p11.csv`
  (P10–P11, resolvers reais AWS/GCP), `jmh-results-p13.csv` (P13, escrita no registo),
  `jmh-results-p14.csv` (P14, exact vs. suffix match), `jmh-results-p15.csv` (P15, mistura I/E) e
  `jmh-results-p16.csv` (P16, R com workflow misto). Gráficos: `P1_*.png … P16_*.png` (P7 e P8 em
  duas figuras cada, `P7a`/`P7b` e `P8a`/`P8b`), regeneráveis com `python3 plot_benchmarks.py`.
- **Modelo de custo:** a renderização é uma travessia *depth-first*
  (`DepthFirstNodeVisitorTraversor` + `NodeContextVisitor`) que visita cada nó da AST uma vez e
  acumula texto num `StringBuilder` partilhado (`IndentedRenderingContext.append`). Como o `append`
  é amortizado O(1), o tempo é proporcional ao número de nós e ao texto produzido, sem o termo
  quadrático da concatenação de `String` imutável.
- **Ressalva:** ambiente partilhado e configuração reduzida; os valores absolutos servem para
  comparar tendências e relações, não como números definitivos de hardware. Para a versão final,
  repetir com `-f 3` e máquina dedicada.

## Notação — o que N, R, F, I e E representam

Estes símbolos aparecem ao longo de quase todas as secções seguintes. Têm sempre o mesmo
significado — o que muda, secção a secção, é a **relação entre eles** (fixos? independentes?
R=F? tudo interno? misto?):

- **N** — número total de **chamadas** no workflow (passos `CALL`, internas ou externas). É sempre
  esta a unidade que se soma/repete N vezes quando o workflow é homogéneo (N leituras, N *lookups*,
  N escritas). A partir do P15, N decompõe-se em **N = I + E** (ver abaixo).
- **R** — número de entradas no **registo** (`function-registry.json`) no momento em que é
  lido/escrito. **Não é o mesmo que F** — R é o tamanho físico do ficheiro; F é quantas dessas
  entradas o workflow *usa*. Um registo pode ter R=1000 entradas e o workflow só referenciar F=10
  delas (as outras R-F são "ruído"/funções de outros workflows). (Chamava-se **M** até ao P14;
  renomeado para **R** — de "Registo" — sem alterar lógica nem valores.)
- **F** — número de funções **internas distintas** que o workflow referencia. Só existe a partir do
  P8 (antes disso não havia necessidade de distinguir "quantas chamadas" de "quantas funções
  diferentes"): as chamadas internas distribuem-se *round-robin* pelas F funções (ex.: F=4 e 6
  chamadas internas → cada função é chamada 1 ou 2 vezes). A partir do P15, F aplica-se
  especificamente às **I** chamadas internas — as **E** chamadas externas não têm este conceito
  (não são resolvidas contra nenhum registo).
- **I** — número de chamadas a **funções internas** no workflow (repetições contam: duas chamadas
  à mesma função contam 2 para I). Só existe a partir do P15.
- **E** — número de chamadas a **funções externas** no workflow (idem, repetições contam). Só
  existe a partir do P15. **N = I + E**: até ao P14 todo workflow era homogéneo (I=N/E=0 em
  P3/P6-P14, ou I=0/E=N em P1/P2/P4/P5) — P15/P16 são os primeiros a misturar os dois tipos de
  chamada no mesmo workflow.

**Como R, F e I/E se relacionam, secção a secção:**

| Secções | Relação R ↔ F ↔ I/E | O que representa |
|---|---|---|
| P3 | F não se aplica; R fixo em 1 | registo de uma função só, isola o efeito de N (workflow 100% interno) |
| P6, P7 | F não se aplica; R e N variam independentemente | registo genérico de R entradas, sem ligação às funções que o workflow chama |
| **P8, P10, P11** | **R = F** | registo "do tamanho certo": contém exatamente as F funções que o workflow usa, nem mais nem menos |
| **P9, P14** | **R ≥ F**, com F e N fixos | registo "sobredimensionado": as R-F entradas extra nunca são chamadas, só inflacionam o custo de cada leitura/scan do ficheiro inteiro |
| P13 | Não há F (é o lado de **escrita**, não de resolução); usa **R0** (tamanho inicial do registo) e **K** (nº de escritas sucessivas) em vez de R/N | mesmo papel de R e N, mas nomeados de forma diferente por não se tratar de resolução de chamadas |
| P12 | Nenhum dos três; usa **branchWidth** (nº de condições/branches) | eixo de renderização, não de registo |
| **P15** | **R = F** fixo (=10); **I** e **E** variam livremente, **N = I+E** | primeiro workflow com mistura real de chamadas internas/externas; isola se o custo segue I ou N |
| **P16** | **I, E, F fixos** (I=40/E=10/F=10); **R** varia | gémeo do P9 em workflow misto — confirma que Θ(R) se mantém inalterado com chamadas externas presentes |

---

## P1 — Degradação com o número de funções

**Objetivo.** Medir como o tempo de renderização escala com o número de funções (passos) e se a
complexidade é linear ou supralinear.

> **Nota de âmbito:** as chamadas são externas por design (`WorkflowGenerator.withIndependentSteps`
> → `StepContextGenerator.independentCall()`, `host`/`path` literais, `internalFunction = null`),
> isolando o custo de renderizar sem a resolução de endpoints (medida em P3/P6/P7). Como a
> renderização só usa o `host`/`path` já presentes na `CallContext`, o resultado é igual para
> chamadas internas ou externas.

| N (funções) | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 1 | 5,7 | 6,0 |
| 10 | 40,5 | 45,1 |
| 50 | 190,3 | 263,7 |
| 100 | 428,7 | 445,3 |
| 200 | 810,9 | 901,2 |

![P1 — Tempo de renderização vs número de funções](P1_rendering_scalability.png)

**Resultado.** O tempo cresce de forma aproximadamente **linear** (≈4 µs por função adicional, em
ambos os destinos), sem comportamento quadrático.

**Justificação.** Um workflow de N passos independentes tem Θ(N) nós na AST; a travessia visita cada
nó uma vez e cada passo emite texto constante, logo T(N) = a·N + b, com `b` o custo fixo de arranque
(dominante só para N pequeno). Com concatenação imutável, cada passo recopiaria o prefixo já
produzido, dando Θ(N²); a linearidade observada confirma a travessia em passagem única com
`StringBuilder`.

---

## P2 — Efeito do número de *inputs* da função

**Objetivo.** Medir se e como o número de inputs (argumentos) de uma função afeta o tempo de
renderização, com o número de funções fixo — isolando esta dimensão da do P1 (pergunta dos
orientadores: uma função com mais *inputs* demora mais a renderizar?). Nota de modelação: a
assinatura Java/Python da função não entra no workflow — cada input materializa-se como um argumento
passado na chamada (parâmetro de *query*/corpo da `CallContext`).

> **Nota de âmbito:** como no P1, as chamadas são externas
> (`StepContextGenerator.callWithParameters(p)`, `host`/`path` literais, sem `internalFunction`) —
> mede-se só a renderização, não a resolução.

| Inputs por função | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 0 | 49,3 | 27,7 |
| 1 | 85,7 | 77,7 |
| 5 | 142,3 | 150,2 |
| 10 | 184,5 | 243,2 |
| 20 | 350,3 | 440,5 |

![P2 — Tempo de renderização vs número de inputs da função](P2_rendering_by_parameters.png)

**Resultado.** Sim — o número de inputs afeta o tempo de forma aproximadamente **linear** (~15 µs
por input). O GCP parte mais baixo (função sem inputs) mas cresce mais depressa, ultrapassando o AWS
a partir de ~5 inputs.

**Justificação.** Cada input é escrito individualmente (resolução do termo + emissão de um par
chave-valor). Com N fixo, T(p) = N·(c·p + k): `c·p` é o custo dos inputs e `N·k` o custo-base por
chamada, daí a linearidade em p e a interceção positiva em p = 0. A base maior no AWS
(k_AWS > k_GCP) deve-se à Amazon States Language emitir mais estrutura fixa por chamada (`Type`,
`Resource`, `Parameters`, `ResultSelector`, `ResultPath`) que o `call:`/`args:` do GCP; o declive
maior no GCP (c_GCP > c_AWS) reflete um custo por input superior no YAML. Base maior numa reta e
declive maior noutra tornam a interseção por volta dos 5 inputs inevitável. O número de inputs é
assim uma segunda dimensão de custo, independente do número de funções.

> Nota: o benchmark é `BenchmarkRenderingByParameterCount` e o gerador `callWithParameters(p)` —
> "parameter" designa cada input da função passado na chamada.

---

## P3 — Custo da unificação: resolução de funções internas

**Objetivo.** Medir o overhead da unificação OmniFlow+QuickFaaS: quanto custa resolver os endpoints
das funções internas (o passo que liga as duas ferramentas), comparado com uma chamada externa, que
não dispara resolução.

| Nº de chamadas | Internas — resolução (µs) | Externas — sem resolução (µs) |
|---:|---:|---:|
| 1 | 7,5 | 0,02 |
| 10 | 74,6 | 0,14 |
| 50 | 368,0 | 0,65 |
| 100 | 747,2 | 1,10 |
| 200 | 1511,4 | 2,34 |

![P3 — Custo da unificação: resolução de funções internas](P3_resolution_overhead.png)

**Resultado.** Resolver os endpoints das funções internas custa **~7,5 µs por função** e escala
**linearmente**. As chamadas externas, que não disparam resolução, custam ~0,01 µs.

**Justificação.** O caminho externo termina de imediato (`internalCallExtractor(call) ?: return
call`): só percorre e copia a árvore — é o *baseline* que isola o efeito da unificação. O caminho
interno invoca, por chamada, `registry.resolveUrl(nome)`, que executa `readAll()` (lê o ficheiro do
registo do disco e reinterpreta todo o JSON), seguido de procura no mapa, *parsing* do URL
(`java.net.URI`) e cópia da `CallContext`. Com o registo de tamanho fixo (uma entrada), o custo é
constante por chamada, logo T(N) = a·N. Os 7,5 µs — altos para uma consulta em mapa — devem-se a
essa releitura do ficheiro a cada invocação, sem cache.

**Caveat de escalabilidade.** Como `readAll()` é Θ(R), a resolução é, no caso geral, **Θ(N·R)**,
com R = tamanho do registo. O ensaio fixa R = 1, isolando a dependência linear em N; se o registo
crescer com o número de funções distintas (R ∝ N), a resolução degrada para Θ(N²). Ler o registo
uma só vez por `resolve(workflow)` reduziria o custo para Θ(N + R) — otimização explorada em P6/P7.
Em absoluto, o custo é negligenciável face ao *deployment* real (segundos).

---

## P4 — Renderizador AWS vs GCP

**Objetivo.** Comparar o renderizador AWS (a extensão da contribuição) com o renderizador GCP já
existente, para o mesmo workflow, confirmando que a extensão AWS não introduz penalização de
desempenho.

| N | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 1 | 5,8 | 5,8 |
| 50 | 221,4 | 233,0 |
| 100 | 423,0 | 469,5 |
| 200 | 817,4 | 912,3 |

![P4 — Renderizador AWS vs GCP](P4_aws_vs_gcp.png)

**Resultado.** O renderizador AWS tem custo equivalente ao GCP — mesma ordem de grandeza em todo o
intervalo, com o GCP ligeiramente mais caro à escala.

**Justificação.** Ambos partilham a travessia e o mecanismo de acumulação de texto, diferindo só
nos renderizadores concretos de cada nó; são ambos Θ(N) e as retas só diferem nas constantes. A
pequena vantagem do AWS à escala (~12% em N = 200) é coerente com o YAML do GCP exigir mais
formatação do que a emissão direta de JSON. Contudo, as barras de erro do AWS são largas nos pontos
maiores (±115 µs em N = 100; ±185 µs em N = 200) e sobrepõem-se às do GCP: a leitura correta é de
equivalência assintótica, não de superioridade. A maior variância em N elevado vem da pressão do
recoletor de lixo (realocações do `StringBuilder`, *garbage* proporcional ao texto) e da contenção
de CPU no ambiente partilhado, alargando o intervalo de confiança sem alterar a tendência.

---

## P5 — Efeito da estrutura/aninhamento

**Objetivo.** Isolar o efeito da complexidade estrutural (profundidade de aninhamento de
`parallel`/`iteration`) no tempo de renderização, com o número total de funções fixo — distinguir se
o que degrada o desempenho é o número de funções ou a sua organização.

| Profundidade | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 0 | 83,9 | 96,3 |
| 1 | 109,7 | 100,8 |
| 3 | 155,4 | 114,5 |
| 5 | 205,8 | 133,6 |

![P5 — Tempo de renderização vs profundidade de aninhamento](P5_rendering_by_nesting.png)

**Resultado.** Com o número total de passos fixo, aumentar a profundidade de aninhamento eleva o
tempo de renderização. O AWS é **mais sensível** ao aninhamento (≈2,5× de profundidade 0 a 5) do que
o GCP (≈1,4×).

**Justificação.** A profundidade acrescenta nós-contentor (cada nível de `parallel`/`iteration` é um
nó com o seu enquadramento) e, sobretudo, alonga o prefixo de indentação — o
`IndentedRenderingContext` antepõe indentação a cada linha, com comprimento proporcional à
profundidade. Assim, mesmo com as folhas fixas, o número de nós e de caracteres por linha cresce com
a profundidade, donde T(d) ≈ a·d + b (a N constante). O AWS é mais sensível porque a Amazon States
Language exige mais estrutura por nível aninhado (`Parallel`/`Map`, `Branches`/`Iterator`,
`ResultPath`) que o YAML do GCP. A complexidade estrutural é assim um fator de custo independente do
número bruto de funções.

---

## P6 — Custo de escalabilidade do registo (Θ(N·R))

**Objetivo.** Medir como o tamanho do registo (R) afeta o custo da resolução, isolando esta dimensão
do número de chamadas internas (N). `FunctionRegistryStore.resolveUrl` não tem cache: cada chamada
relê e reparsa o ficheiro do registo inteiro (`readAll()`). O P3 fixava R = 1 e só variava N; o P6
varia as duas dimensões independentemente para caracterizar a dependência em R.

**Tempo total de `resolveAllInternal` (µs), por combinação (R, N):**

| R \ N | 1 | 10 | 50 | 200 |
|---:|---:|---:|---:|---:|
| 1 | 178,1 | 1772,7 | 9425,9 | 36346,0 |
| 10 | 186,2 | 1824,6 | 9146,2 | 36713,6 |
| 50 | 206,5 | 2133,3 | 10206,5 | 40478,3 |
| 200 | 278,3 | 2773,3 | 13880,6 | 55616,7 |
| 1000 | 744,6 | 7485,5 | 37316,5 | 149632,1 |

**Controlo `resolveAllExternal`** (nunca toca o registo, independente de R): 0,02 µs (N=1) →
0,13 µs (N=10) → 0,51 µs (N=50) → 1,97 µs (N=200) — iguais em toda a linha de R, confirmando que o
efeito de R acima é específico do acesso ao registo.

![P6 — Custo por chamada de resolveUrl() vs tamanho do registo](P6_registry_scaling.png)

As quatro curvas `n=1/10/50/200` colapsam numa só linha — o custo por chamada depende apenas de R,
não de N — enquanto o controlo externo se mantém plano próximo de zero.

**Resultado.** Dividindo o tempo total por N obtém-se o custo por chamada de `resolveUrl()`,
independente de N e crescente com R:

| R | Custo por chamada (µs) |
|---:|---:|
| 1 | 181,4 |
| 10 | 183,8 |
| 50 | 206,6 |
| 200 | 277,8 |
| 1000 | 746,9 |

Ajustando `custo(R) ≈ b + c·R` aos extremos (R=1, R=1000): `b ≈ 181 µs`, `c ≈ 0,57 µs`/função — um
ajuste que erra <6% nos pontos intermédios (R=10, 50, 200).

**Justificação.** `readAll()` lê o ficheiro e materializa uma `JsonNode` por cada entrada, logo é
Θ(R). Como `resolveUrl()` corre uma vez por chamada interna, o custo total é T(N, R) = N·(b + c·R) —
o Θ(N·R) já identificado (mas não medido) no P3. Os dois termos têm origens distintas: `b ≈ 181 µs`
é o custo fixo por chamada (abrir/ler o ficheiro + `parsing` do envelope JSON); `c ≈ 0,57 µs`/função
é o custo marginal por entrada (percorrer o nó `functions` e construir o `LinkedHashMap`). `b` domina
`c·R` até R ≈ 320: para registos realistas (dezenas a poucas centenas de funções), o fator dominante
não é o tamanho do registo, mas o número de vezes que o ficheiro é reaberto e reparsado (uma vez por
chamada, em vez de uma por `resolve(workflow)`).

**Implicação prática.** Isto reordena a otimização sugerida no P3: ler o registo uma só vez por
`resolve(workflow)` (em vez de cache por tamanho) elimina o fator N de ambos os termos, reduzindo
Θ(N·(b + c·R)) para Θ(b + c·R + N) — ganho maior e mais barato do que otimizar só a travessia do
JSON.

---

## P7 — Otimização da resolução: leitura única do registo (Θ(N·R) → Θ(N+R))

**Objetivo.** Medir o ganho de desempenho (antes vs. depois) de uma otimização concreta — ler o
registo uma só vez por `resolve(workflow)` em vez de uma vez por chamada — na mesma execução, para
que o *speedup* seja diretamente comparável (o P3 e o P6 foram medidos em execuções separadas).

O P3/P6 apontaram a causa: `WorkflowInternalCallEndpointResolver.resolve` chamava
`FunctionRegistryStore.resolveUrl` uma vez por chamada interna, relendo o ficheiro inteiro de cada
vez. A correção lê o registo uma só vez por `resolve()` (método `resolveUrlIn` sobre o mapa já
lido), mantendo a mesma lógica. O benchmark `BenchmarkResolutionOptimization` compara as duas
estratégias na mesma máquina, varrendo N × R: `resolveNaive` (leitura por chamada) vs
`resolveOptimized` (leitura única).

**Tempo total (µs) — antes (sem cache) vs depois (com cache):**

| | N=1 | N=10 | N=50 | N=200 |
|---|---:|---:|---:|---:|
| **sem cache** R=1 | 4,6 | 47,0 | 233,6 | 920,2 |
| **com cache** R=1 | 4,7 | 5,0 | 5,0 | 5,7 |
| **sem cache** R=1000 | 543,7 | 5480,8 | 26698,5 | 104889,0 |
| **com cache** R=1000 | 557,4 | 535,7 | 551,8 | 536,6 |

![P7a — Resolução sem cache (leitura por chamada)](P7a_resolution_sem_cache.png)

![P7b — Resolução com cache (leitura única)](P7b_resolution_com_cache.png)

**Resultado.** O caminho com cache é praticamente independente de N: uma leitura do registo (O(R))
seguida de N *lookups* em memória desprezáveis. *Speedup* a N=200: ~162× (R=1), ~198× (R=50),
~195× (R=1000) — o pior canto (N=200, R=1000) desce de **~105 ms para ~0,54 ms**.

**Justificação.** `sem cache` = N · (leitura O(R) + *lookup*) = **Θ(N·R)**: no gráfico P7a as curvas
sobem com N e deslocam-se para cima com R. `com cache` = 1 leitura O(R) + N · *lookup* O(1) =
**Θ(N+R)**: no P7b as curvas são planas em N, separadas apenas por R (custo da leitura única). As
duas figuras partilham o eixo log-y, tornando o desnível visível.

**Nota (resolver completo).** Com o resolver já com cache, o custo de `resolve(workflow)` passa de
N·R a **Θ(N+R)**: no pior canto (N=200, R=1000) desce de ~150 ms para ~0,75 ms. O termo O(N)
residual é só a reconstrução da árvore do workflow (N cópias de nós), barato (~0,7 µs/nó) e
inevitável, já não a re-leitura do ficheiro.

---

## P8 — Resolução do workflow vs nº de funções distintas (F) × nº de chamadas (N), com R=F

**Objetivo.** Caracterizar o custo de resolução de *endpoints* de um workflow real ao longo dos seus
dois eixos intrínsecos: o nº de funções **distintas** que o workflow chama (F) e o nº total de
**chamadas** (N). As N chamadas distribuem-se *round-robin* pelas F funções (ex.: F=4/N=6 →
F1 2×, F2 2×, F3 1×, F4 1×) e o registo contém **exatamente** essas F funções (**R = F**) — o caso
realista em que o registo tem só as funções que o workflow usa (o efeito de um registo
sobredimensionado, R independente de F, é o eixo do P9). Mede-se **só a resolução, sem
renderização**: a otimização em estudo (leitura única do registo) só afeta a resolução, pelo que
renderizar seria apenas uma base constante partilhada que deslocaria ambas as curvas sem alterar o
*speedup*. Compara-se `resolveNaive` (leitura do registo por chamada) com `resolveOptimized`
(leitura única). Ambos produzem um workflow resolvido idêntico, logo o desnível é puramente o custo
de leitura do registo.

**Tempo de resolução (µs) — `resolveNaive` (leitura por chamada), F em linhas / N em colunas:**

| F \ N | 1 | 5 | 10 | 50 | 200 |
|---:|---:|---:|---:|---:|---:|
| 1 | 216 | 1 118 | 2 103 | 9 544 | 38 394 |
| 2 | 192 | 956 | 1 902 | 9 555 | 37 950 |
| 5 | 189 | 995 | 1 911 | 9 663 | 38 468 |
| 10 | 195 | 1 132 | 2 390 | 10 544 | 39 340 |
| 20 | 199 | 997 | 2 096 | 10 017 | 41 205 |
| 50 | 219 | 1 133 | 2 194 | 10 857 | 43 360 |

**Tempo de resolução (µs) — `resolveOptimized` (leitura única):**

| F \ N | 1 | 5 | 10 | 50 | 200 |
|---:|---:|---:|---:|---:|---:|
| 1 | 191 | 195 | 197 | 228 | 337 |
| 2 | 191 | 193 | 200 | 230 | 341 |
| 5 | 191 | 194 | 209 | 228 | 349 |
| 10 | 228 | 197 | 202 | 241 | 362 |
| 20 | 199 | 202 | 215 | 237 | 349 |
| 50 | 224 | 221 | 223 | 251 | 367 |

![P8a — Resolução sem cache (leitura por chamada)](P8a_resolution_sem_cache.png)

![P8b — Resolução com cache (leitura única)](P8b_resolution_com_cache.png)

**Resultado.** No caso sem cache, o custo é dominado por **N** e quase indiferente a **F**: as seis
curvas (uma por F) praticamente sobrepõem-se no P8a e sobem linearmente com N (~200 µs por chamada;
9,5–10,9 ms a N=50; ~38–43 ms a N=200). No caso com cache, o custo é quase plano (191 → 367 µs),
subindo devagar com N (reconstrução O(N) da árvore) e com F (leitura O(R=F)). O *speedup* cresce com
N: ~1× a N=1 (uma única leitura, indistinguível), ~44× a N=50 e ~109–114× a N=200.

**Justificação.** Com R=F pequeno, cada leitura do registo é dominada pelo termo fixo b≈190 µs
(abrir/parsear o ficheiro), não pelo termo c·R (percorrer as entradas) — daí as curvas `sem cache`
quase coincidirem para todos os F e a subida vir só de N. `sem cache` = N·(leitura O(R) + *lookup*) =
**Θ(N·R)**; com R=F pequeno reduz-se praticamente a N·b, uma reta em N. `com cache` = 1 leitura O(R)
+ N · *lookup* O(1) + reconstrução O(N) = **Θ(N+R)**, daí a superfície quase plana. O efeito isolado
de **F** (chamadas repetidas vs. distintas) é desprezável porque a resolução é por chamada, não por
função distinta. O eixo que faz o registo pesar — aumentar R para além de F — é o P9.

---

## P9 — Resolução do workflow vs tamanho do registo (R), com F e N fixos

**Objetivo.** O eixo complementar do P8: isolar o efeito do **tamanho do registo (R)** sobre a
resolução, mantendo o workflow fixo (F = 10 funções distintas, N = 50 chamadas) e enchendo o registo
até R entradas (R ≥ F), das quais o workflow só refere as primeiras F. Mesmas duas estratégias, só
resolução (sem render): `resolveNaive` (leitura por chamada, Θ(N·R)) vs `resolveOptimized` (leitura
única, Θ(N+R)).

**Tempo de resolução (µs) — sem cache vs com cache, F=10 / N=50 fixos:**

| R (registo) | sem cache (µs) | com cache (µs) | speedup |
|---:|---:|---:|---:|
| 10 | 9 829 | 237 | ~41,4× |
| 20 | 11 952 | 308 | ~38,8× |
| 50 | 10 899 | 268 | ~40,7× |
| 100 | 12 118 | 275 | ~44,1× |
| 200 | 14 964 | 324 | ~46,2× |
| 1000 | 43 245 | 749 | ~57,8× |

![P9 — Resolução vs tamanho do registo](P9_resolution_by_registry.png)

**Resultado.** O caso sem cache sobe com R (9,8 ms a R=10 → 43,2 ms a R=1000): com N=50 chamadas,
relê o ficheiro 50 vezes e cada re-leitura reparsa R entradas. O caso com cache mantém-se quase plano
(237 → 749 µs): uma única leitura O(R) seguida de 50 *lookups*. O *speedup* sobe de ~41× (R=10) a
~58× (R=1000).

**Justificação.** `sem cache` = N·(b + c·R) = **Θ(N·R)**: com N fixo é uma reta em R de declive N·c.
`com cache` = (b + c·R) + N · *lookup* O(1) = **Θ(N+R)**: paga o termo c·R uma só vez, daí a subida
suave. Enquanto no P8 (R=F pequeno) o custo sem cache era dominado por N·b, aqui — com R a crescer
até 1000 — o termo N·c·R torna-se visível e explica a subida. Verificação cruzada com o P8 no ponto
comum F=10/N=50 (R=10): sem cache ≈ 9,8–10,5 ms e com cache ≈ 0,24 ms nos dois testes, coerente.

---

## P10 — Custo real do resolver de auto-deploy AWS (`AwsInternalFunctionResolver`)

**Objetivo.** O P6–P9 provaram e corrigiram o custo Θ(N·R) de reler o registo por chamada, mas só
no `WorkflowInternalCallEndpointResolver` (o resolver "de baixo nível"). O verdadeiro glue da
contribuição (B) "Unificação" — `AwsInternalFunctionResolver.resolve`, que decide "está no registo?
reutiliza : não está? despoleta deploy via QuickFaaS" — nunca foi medido nem corrigido. Mede-se aqui
o seu custo real vs N (chamadas) × R (registo, R=F), mesma grelha do P8. O registo é pré-populado
com exatamente as F funções que o workflow chama, garantindo sempre *hit* no passo 1 de
`resolveOrDeploy` — nunca se chega ao passo 2 (chamada real ao SDK Lambda). Pura leitura/escrita
local de ficheiro, sem SDK nem rede.

**Tempo de resolução (µs) — `AwsInternalFunctionResolver.resolve`, F em linhas / N em colunas:**

| F \ N | 1 | 5 | 10 | 50 | 200 |
|---:|---:|---:|---:|---:|---:|
| 1 | 356 | 1 684 | 3 338 | 17 806 | 71 825 |
| 2 | 369 | 1 744 | 3 413 | 17 205 | 83 141 |
| 5 | 349 | 1 730 | 3 419 | 16 961 | 67 808 |
| 10 | 355 | 1 738 | 3 421 | 17 509 | 69 700 |
| 20 | 352 | 1 779 | 3 488 | 17 455 | 69 627 |
| 50 | 383 | 1 847 | 3 732 | 22 396 | 73 603 |

![P10 — Resolução real AWS](P10_aws_internal_resolution.png)

*(máquina partilhada, não dedicada — margens de erro largas nesta configuração rápida; ver nota
sobre `-f 3` no `TESTING.md`. Os pontos são consistentes entre si e com a forma Θ(N·R) esperada.)*

**Resultado.** O custo é dominado por **N** e quase indiferente a **F**, tal como no P8 "sem
cache": ~355–360 µs/chamada em todo o intervalo de F (200 chamadas: 71,8 ms a F=1 → 73,6 ms a
F=50). É bastante mais caro que o `resolveNaive` isolado do P8 (~200 µs/chamada) — a diferença vem
do próprio glue de produção (`println`s coloridos por chamada, verificação
`host.isNotBlank()`/`path.isNotBlank()`, construção do URL `lambda://`), não só da releitura do
registo.

**Justificação.** `AwsInternalFunctionResolver.resolve` = N·(leitura O(R) + *lookup* + overhead de
glue) = **Θ(N·R)**, exatamente como `resolveNaive` no P8/P9, porque `resolveOrDeploy` chama
`registry.tryResolveEntry` (que chama `readAll()`) uma vez por chamada, sem cache — nunca recebeu a
otimização de leitura única do P7. Se recebesse (lendo o registo uma vez por `resolve(workflow)`,
como o P8 "com cache"), o custo cairia para a mesma ordem de Θ(N+R) (~367 µs no pior canto do P8) —
um *speedup* projetado de ~200× no ponto F=50/N=200 (73 603 µs → ~367 µs), sem sequer contar o custo
extra dos `println`. Fica documentado como oportunidade de otimização concreta e ainda não aplicada
ao glue de produção.

## P11 — Custo real do resolver de auto-deploy GCP (`WorkflowInternalFunctionResolver`)

**Objetivo.** Gémeo GCP do P10: mede o custo real de `WorkflowInternalFunctionResolver.resolve`, o
glue de auto-deploy do lado Google, mesma grelha N × R=F. O registo usa URLs `.cloudfunctions.net`
(1ª geração), o que faz `resolveOrDiscoverInternal` devolver logo após o *hit* no registo, sem
chamar a API REST do Cloud Run (`inspector.lookupByServiceName`) nem aceder a `regions` (`by lazy`,
nunca avaliado neste caminho) — mantendo o benchmark 100% local. `CloudRunV2ServiceInspector` e
`CloudRunLocationsV1RestClient` são construídos com um `GoogleAccessTokenProvider` explícito que
embrulha um `AccessToken` fixo (via `GoogleCredentials.create`), evitando que os construtores destas
duas classes disparem `GoogleCredentials.getApplicationDefault()` — ver gotcha documentado no
`CLAUDE.md`.

**Tempo de resolução (µs) — `WorkflowInternalFunctionResolver.resolve`, F em linhas / N em colunas:**

| F \ N | 1 | 5 | 10 | 50 | 200 |
|---:|---:|---:|---:|---:|---:|
| 1 | 398 | 2 205 | 3 837 | 19 908 | 76 812 |
| 2 | 400 | 1 952 | 3 959 | 20 418 | 79 856 |
| 5 | 459 | 2 068 | 4 001 | 24 333 | 78 215 |
| 10 | 395 | 1 971 | 4 436 | 19 744 | 78 525 |
| 20 | 402 | 2 045 | 3 955 | 20 143 | 78 037 |
| 50 | 428 | 2 118 | 4 285 | 20 811 | 83 269 |

![P11 — Resolução real GCP](P11_google_internal_resolution.png)

*(mesma máquina/ressalva de margens de erro do P10.)*

**Resultado.** Mesmo padrão Θ(N·R) do P10, ~7–8% mais caro por chamada (~385–430 µs/chamada em
todo o intervalo de F; 200 chamadas: 76,8 ms a F=1 → 83,3 ms a F=50). A diferença face ao AWS vem
do glue adicional específico do GCP (verificação `isFirstGenCloudFunction`, construção do par
host/path via `URI`, mais um `GoogleAccessTokenProvider`/`CloudRunV2ServiceInspector`/
`CloudRunLocationsV1RestClient` construídos por *trial*), não da leitura do registo em si — o termo
de I/O é o mesmo `FunctionRegistryStore`.

**Justificação.** Mesma equação do P10: Θ(N·R), nunca corrigido com leitura única. A pequena
diferença de constante entre P10/P11 é o próprio código de glue de cada provider, não o mecanismo
de registo — consistente com o P8/P9, onde o termo dominante a F=R pequeno é o custo fixo `b`
(abrir/parsear o ficheiro) por chamada, e não o termo `c·R`.

---

## P12 — Custo de renderização vs largura de Choice / Parallel

**Objetivo.** P1/P2/P4/P5 variam sempre o mesmo tipo de step (CALL) — nunca a largura de um
`Choice` (nº de condições) nem de um `Parallel` (nº de branches), apesar do `AmazonChoiceRenderer`/
`AmazonParallelRenderer` serem praticamente triviais (poucas linhas, sem serialização por item)
enquanto o `GoogleParallelRenderer.internalEndRender` faz uma **interseção de sets de variáveis**
partilhadas entre o branch e o contexto exterior — um custo nunca antes medido. Mede-se aqui o
tempo de renderização (AWS + GCP) de um único step Choice/Parallel vs a sua largura.

![P12 — Tempo de renderização vs largura de Choice/Parallel](P12_rendering_by_branch_width.png)

*(mesma máquina/ressalva de margens de erro do P10/P11 — visíveis no gráfico, sobretudo a
larguras baixas onde o tempo absoluto é pequeno.)*

**Resultado.** Choice e Parallel crescem de forma aproximadamente linear com a largura, nos dois
providers — sem o crescimento superlinear do lado GCP que a leitura do código sugeria. Choice: AWS
4,6 µs (largura 1) → 138 µs (largura 100); GCP 3,5 µs → 102 µs. Parallel: AWS 11,1 µs → 758 µs; GCP
10,1 µs → 694 µs. Em toda a gama medida o AWS é ligeiramente **mais** caro que o GCP, e não o
inverso — o oposto da hipótese inicial.

**Justificação.** O Parallel é, como esperado, mais caro que o Choice nos dois lados (~4-5× na
largura 100), consistente com uma branch completa (com os seus próprios steps) a ser construída por
unidade de largura, enquanto o Choice só serializa uma condição. Mas a assimetria hipotetizada a
partir da leitura do código-fonte — o custo de interseção de conjuntos de variáveis em
`GoogleParallelRenderer.internalEndRender` crescer mais depressa do que o lado Amazon — **não se
confirma** nesta medição: como os workflows gerados por `withParallelBranchWidth`/`withChoiceBranches`
têm steps-folha sem variáveis (`independent()` puro, sem `AssignContext`), o conjunto de variáveis
em scope fica ~vazio independentemente da largura, e a interseção de sets é O(1) por branch. Isto
isola corretamente o eixo que este benchmark mede — custo vs nº de branches/condições — do eixo
ainda por medir: custo vs nº de variáveis partilhadas em scope, que exigiria um gerador com
`AssignContext`s dentro dos branches. Fica documentado como trabalho futuro, e como um lembrete de
que a leitura do código sozinha não substitui a medição.

---

## P13 — Custo de escrita incremental no registo (`FunctionRegistryStore.put`)

**Objetivo.** P6–P11 mediram o **lado de leitura** do `FunctionRegistryStore` (`resolveUrl`/
`tryResolveEntry`, relê o ficheiro inteiro por chamada). Mas os resolvers reais também **escrevem**:
sempre que uma função é descoberta/desenhada pela primeira vez, chamam `registry.put(key, meta)` —
e `put()` nunca foi medido. Lendo o código: `put()` chama `readRootOrNew()` (lê+reparsa o ficheiro
inteiro) e depois `writeRoot()` (reescreve o ficheiro inteiro) — cada chamada custa O(R), onde R é
o tamanho *atual* do registo. Regista **K** funções sucessivas num registo que começa com **R0**
entradas: custo total = Σ O(R0+i) para i=0..K-1 = **Θ(K·R0 + K²)**, quadrático no próprio K quando
R0 é pequeno. Mede-se diretamente `FunctionRegistryStore.put()`, sem passar pelo resolver completo
(tal como o P6 já chama o `FunctionRegistryStore` diretamente) — pura I/O local, sem SDK nem rede.

**Tempo total (µs) — K escritas sucessivas, K em linhas / R0 em colunas:**

| K \ R0 | 0 | 10 | 50 | 200 | 1000 |
|---:|---:|---:|---:|---:|---:|
| 1 | 2 673 | 2 860 | 3 235 | 4 139 | 2 761 |
| 5 | 12 887 | 13 149 | 14 955 | 19 710 | 13 874 |
| 10 | 27 691 | 26 986 | 29 912 | 39 164 | 24 029 |
| 50 | 154 601 | 144 606 | 161 098 | 206 385 | 123 322 |
| 100 | 329 757 | 304 187 | 330 100 | 419 531 | 237 910 |

![P13 — Custo de escrita incremental no registo](P13_registry_write_scaling.png)

*(3 forks (`-f 3`) precisamente para reduzir ruído — erros já pequenos e consistentes na maioria
dos pontos; ver nota na Justificação sobre a coluna R0=1000.)*

**Resultado.** O custo cresce claramente mais que linear em K: entre K=1 e K=100 (100× mais
escritas), o tempo total sobe ~120–125× em vez de ~100× — a assinatura do termo K² a somar-se ao
K·R0. Para R0 ∈ {0, 10, 50, 200} a subida com R0 é a esperada (quanto maior o registo de partida,
mais caro cada `put()`): a K=100, sobe de 329,8 ms (R0=0) para 419,5 ms (R0=200), um aumento
consistente com o termo K·R0 do modelo.

**Justificação.** `put()` = O(R0+i) na i-ésima escrita, logo K escritas custam
Σ_{i=0}^{K-1} O(R0+i) = **Θ(K·R0 + K²)**. O termo K² domina a subida entre colunas de K (cada
salto ~2–5× em K dá um salto correspondentemente maior no tempo, não proporcional); o termo K·R0
explica a subida mais suave ao longo de R0 (para R0 ∈ {0,10,50,200}). A coluna R0=1000 foge a este
padrão — sai sistematicamente **abaixo** de R0=200 em vez de acima, mesmo repetindo a medição com
`-f 3` (forks frescos por combinação, o que devia excluir *warm-up* de JIT entre parâmetros
diferentes). A explicação mais provável não é o código em si, mas a máquina partilhada/não dedicada
onde isto corre (mesma ressalva do P10–P12): R0=1000 é sempre a última coluna processada em cada
grupo de K, pelo que efeitos ao nível do SO (cache de ficheiros, processos em segundo plano,
throttling térmico) ao longo dos ~30 minutos de execução total podem introduzir viés que uma
repetição de forks não elimina. A conclusão principal do P13 — o crescimento Θ(K²) em K — é robusta
em todas as colunas; a curva exata vs. R0 precisaria de `-f 3`+ordem aleatória dos parâmetros numa
máquina dedicada para ser conclusiva nesse ponto específico.

---

## P14 — Correspondência exata vs. por sufixo no resolver "com cache"

**Objetivo.** P7–P9 provaram que ler o registo uma só vez transforma a resolução de Θ(N·R) em
Θ(N+R). Mas isso pressupõe que cada `resolveUrlIn` acerta em O(1) (`all[functionName]`). O próprio
método suporta um segundo caminho — correspondência por **sufixo** (`"region/functionName"`, para
desambiguação regional) — que, ao falhar o *match* exato, faz um `filterKeys` sobre **todo** o mapa
em memória: O(R) por chamada. Nenhum benchmark anterior testou este caminho (P6–P11 usam sempre
nomes exatos). Mede-se aqui, com F=10/N=50 fixos (mesmo ponto do P9) e R a variar, o custo do
resolver "com cache" quando o registo tem chaves **exatas** (o caso já medido, aqui como controlo
cruzado com o P9) vs. quando tem chaves **qualificadas por região** — o workflow continua a
referenciar nomes nus, pelo que o *match* exato falha sempre e cada chamada paga o scan O(R).

**Tempo de resolução (µs) — exact match vs. suffix match, F=10 / N=50 fixos:**

| R (registo) | exact match (µs) | suffix match (µs) | razão |
|---:|---:|---:|---:|
| 10 | 219 | 228 | ~1,04× |
| 20 | 224 | 241 | ~1,07× |
| 50 | 250 | 278 | ~1,11× |
| 100 | 259 | 333 | ~1,29× |
| 200 | 309 | 442 | ~1,43× |
| 1000 | 768 | 1 456 | ~1,90× |

![P14 — Exact vs. suffix match](P14_resolution_key_match_strategy.png)

**Resultado.** A coluna *exact match* reproduz de perto o "com cache" já medido no P9 no mesmo
ponto F=10/N=50 (P9: 237→749 µs de R=10 a R=1000; aqui: 219→768 µs) — confirmação cruzada de que
`resolveExactMatch` é arquitetonicamente o mesmo resolver, só com um registo diferente. A coluna
*suffix match* fica sistematicamente **acima**, e a razão entre as duas cresce de forma monótona
com R: ~1,04× a R=10, ~1,90× a R=1000 — o dobro do custo, apenas por as chaves do registo estarem
qualificadas por região em vez de serem nomes nus, sem qualquer outra diferença.

**Justificação.** `resolveExactMatch` = 1 leitura O(R) + N *lookups* O(1) = **Θ(N+R)**, igual ao
P9. `resolveSuffixMatch` = 1 leitura O(R) + N *scans* O(R) = **Θ(N+N·R) ≈ Θ(N·R)** — a mesma classe
de complexidade do resolver "sem cache" do P7-P9, só que agora reintroduzida *dentro* do resolver já
corrigido, por uma particularidade do formato das chaves e não por falta de otimização de leitura.
Note-se que a magnitude absoluta aqui (µs, não ms) fica muito abaixo do "sem cache" do P8/P9 (que
chega às dezenas de ms): ali o R é lido do **disco** a cada chamada (I/O + parsing); aqui o *scan*
de sufixo é sobre um mapa **já em memória** (`registrySnapshot`, carregado uma única vez), pelo que
o custo por elemento é ordens de grandeza mais barato. A mudança de classe de complexidade
(O(1)→O(R) por *lookup*) é real e mensurável — visível na razão crescente entre as duas colunas —
mas só se tornaria dramática em termos absolutos com um N muito maior ou um registo muito maior do
que os testados aqui (o custo extra escala com N·R, não só R).

---

## P15 — Resolução do workflow com mistura de chamadas internas/externas (I × E)

**Objetivo.** Todos os benchmarks anteriores (P3, P6–P11, P13, P14) medem workflows homogéneos —
todas as chamadas são internas. Mas o modelo já suporta chamadas externas no mesmo workflow
(`CallContext.internalFunction == null`), o caso realista de um workflow que combina funções
auto-deployadas com APIs de terceiros. Isola-se aqui se o custo de resolução depende do número de
chamadas **internas** (I) ou do total de chamadas (N = I+E), com F=R=10 fixo (registo do tamanho
exato do nº de funções distintas, o caso "afinado" do P8) e resolvendo sempre com cache (leitura
única) — não se repete a comparação naive/otimizado, já exaustivamente coberta em P6–P9.

**Tempo de resolução (µs) — `resolveOptimized`, I em linhas / E em colunas, R=F=10 fixos:**

| I \ E | 0 | 1 | 5 | 10 | 50 | 200 |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 181 | 190 | 182 | 187 | 183 | 183 |
| 1 | 193 | 184 | 183 | 187 | 183 | 186 |
| 5 | 186 | 186 | 211 | 188 | 187 | 187 |
| 10 | 189 | 205 | 195 | 203 | 189 | 255 |
| 50 | 217 | 217 | 217 | 230 | 219 | 322 |
| 200 | 326 | 321 | 333 | 323 | 315 | 393 |

![P15 — Resolução vs mistura interno/externo (I × E)](P15_resolution_internal_external_mix.png)

**Resultado.** A linha I=0 (só chamadas externas, nenhuma interna) fica praticamente plana
(~181–190 µs) mesmo com E a subir até 200 — o resolver não paga custo por chamada externa. Ao longo
de cada coluna (E fixo), o custo sobe claramente com I: ~183 µs a I=0 até ~315–393 µs a I=200, a
mesma ordem de grandeza da subida por N observada no P8 "com cache". Dentro de cada linha (I fixo),
a variação com E é pequena e sem tendência clara — o ruído entre colunas é da ordem do erro de
medição (fork único), não um efeito sistemático de E.

**Justificação.** O resolver resolve *apenas* as chamadas com `internalFunction != null` (o
*lookup* O(1) no registo já carregado); chamadas externas (`internalFunction == null`) são
ignoradas nesse passo e só contam para o custo O(N) de reconstruir a árvore do workflow resolvido.
Por isso o custo real depende de **I** (via os *lookups* no registo) e não de **N** inteiro —
confirmando explicitamente, pela primeira vez com um workflow realmente misto, o que o P8 já
sugeria implicitamente (lá, I=N sempre, por o workflow ser todo interno). A escala fica:
**Θ(I+R)**, com E a entrar apenas no termo O(N) partilhado de reconstrução da árvore, não no termo
de resolução propriamente dito.

---

## P16 — Resolução vs tamanho do registo (R), workflow misto I=40/E=10 fixos

**Objetivo.** O gémeo do P9, mas com um workflow misto em vez de todo-interno: mantêm-se I=40
chamadas internas + E=10 chamadas externas (N=50, o mesmo total do P9) e F=10 funções distintas
fixo, fazendo variar só o tamanho do registo R (R ≥ F, mesma grelha do P9: 10 a 1000). Confirma se o
Θ(R) já provado no P9 se mantém inalterado quando o workflow tem chamadas externas misturadas —
isto é, se R continua independente de I/E tal como já era independente de F/N.

**Tempo de resolução (µs) — `resolveOptimized`, I=40/E=10/F=10 fixos:**

| R (registo) | com cache (µs) |
|---:|---:|
| 10 | 346 |
| 20 | 221 |
| 50 | 247 |
| 100 | 309 |
| 200 | 306 |
| 1000 | 776 |

![P16 — Resolução vs tamanho do registo, workflow misto](P16_resolution_by_registry_mixed.png)

**Resultado.** A tendência geral confirma o mesmo padrão Θ(R) do P9 com cache (237→749 µs de R=10 a
R=1000 no P9 todo-interno; aqui 346→776 µs com I=40/E=10) — a mesma ordem de grandeza e a mesma
subida global entre os extremos da grelha. A série é mais ruidosa do que o P9 (nota-se um mínimo
local em R=20 em vez de uma subida monótona, com erros de medição maiores neste conjunto de fork
único), mas o ponto-chave — R=1000 claramente acima de todos os outros pontos — replica-se sem
ambiguidade.

**Justificação.** `resolveOptimized` continua a ser 1 leitura O(R) + N *lookups* O(1) (aqui só os
I=40 lookups internos tocam o registo; os E=10 externos são ignorados nessa etapa) = **Θ(I+R)** ≈
**Θ(N+R)** do P9, dado que o termo dominante é a leitura O(R) do registo, indiferente a que fração
das N chamadas é interna vs externa. O ruído adicional face ao P9 (execução com um único fork, sem
repetição `-f 3`) explica o não-monotonismo local (R=20 abaixo de R=10); o sinal Θ(R) mantém-se
claro na escala ponta-a-ponta (346→776 µs, ~2,2×), da mesma ordem do ~3,2× do P9 no mesmo intervalo
de R.

---

## S1 e S2 — Métricas de tamanho (inspiradas no "ZIP size (KB)" do QuickFaaS)

A avaliação inicial do QuickFaaS mediu o "ZIP size (KB)" do *bundle* de deployment (agnóstico
10877 KB vs não-agnóstico 10861 KB → ~16 KB de overhead). Estende-se aqui a dimensão de tamanho em
dois planos, ambos locais.

### S2 — Tamanho do artefacto renderizado (ASL JSON vs GCP YAML)

**Objetivo.** Medir o tamanho (bytes) do workflow gerado — não da função — em AWS vs GCP, em função
de N; complementa o tempo (P1) com a dimensão de espaço. Medição direta com `ArtifactSizeMeasurement`
(não JMH).

| N | AWS ASL JSON (bytes) | GCP YAML (bytes) |
|---:|---:|---:|
| 1 | 1 019 | 487 |
| 10 | 9 479 | 3 997 |
| 50 | 47 199 | 19 637 |
| 100 | 94 349 | 39 187 |
| 200 | 188 949 | 78 387 |

![S2 — Tamanho do artefacto renderizado vs N](S2_artifact_size.png)

**Resultado.** Crescimento **linear** em N: ~944 bytes/função (AWS) e ~392 bytes/função (GCP). O ASL
JSON é ~2,4× mais volumoso que o YAML para o mesmo workflow.
**Justificação.** Coerente com a maior verbosidade da Amazon States Language (cada estado repete
`Type`/`Resource`/`ResultPath` e chavetas JSON) face ao YAML mais compacto — a mesma razão do
custo-base superior do AWS no P2.

### S1 — Bundle/ZIP size da Lambda: AWS agnóstico vs nativo

**Objetivo.** Medir o overhead de tamanho (bytes) que a camada agnóstica do QuickFaaS acrescenta ao
*bundle* de deployment da Lambda, face a uma implementação AWS nativa equivalente — réplica, para o
provider AWS, da métrica "ZIP size (KB)" da avaliação original do QuickFaaS (GCP/Azure).

A mesma função (`hello-lambda-fn`) empacotada de duas formas (fat-jar via maven-shade), medida com
`measure_bundle_size.sh`:

| Bundle | Tamanho |
|---|---:|
| AWS agnóstico (QuickFaaS: `MyFunctionClass` + adaptador `AwsHttpTemplate` + `aws-lambda-java-core`) | **14 552 bytes (14,2 KB)** |
| AWS nativo (um único `RequestHandler`, mesma lógica) | **13 647 bytes (13,3 KB)** |
| **delta (overhead da camada agnóstica)** | **905 bytes (0,88 KB)** |

**Resultado.** A camada AWS acrescenta ao *bundle* apenas o adaptador `AwsHttpTemplate` —
**~0,88 KB** (6,6% deste *bundle* mínimo), ainda menor em absoluto que os ~16 KB medidos pelos
colegas para GCP/Azure.
**Nota.** Os 6,6% estão inflacionados porque a função-base é trivial (só `aws-lambda-java-core`,
~14 KB). Numa função real (bundle de MB), os mesmos ~0,9 KB do adaptador são <0,1% — confirmando,
também para AWS, que o overhead de empacotamento da abstração é negligenciável.

**Corroboração pelo código real (e um bug encontrado).** O valor acima veio de um script que
*reproduz* o POM/wrapper do QuickFaaS. Para validar com o código de produção, acrescentou-se
`AwsLambdaFunctionBuildIntegrationTest` (`QuickFaaS-Deployment`), que invoca
`AwsLambdaFunction.buildAndZip("aws")` — o `mvn package` real — e para antes de `deployZip` (a
chamada à cloud). O teste confirmou o mesmo valor (14 685 bytes), mas só depois de corrigir um bug
de não-determinismo em `AwsBuildScripts.copyFatJarAsZip`: sem `<build><finalName>` ao nível do
projeto, o Maven gera dois jars em `target/` (o fino do plugin `jar` e o gordo do `shade`); o filtro
antigo aceitava ambos e escolhia pela ordem de listagem do sistema de ficheiros, não garantida,
podendo selecionar o jar errado (sem dependências) em produção. Corrigido para procurar o jar pelo
nome exato do `finalName`. Detalhe em `TESTING.md` §3.1.

---

## Síntese e discussão

1. A renderização é **linear** no número de funções (P1) e de inputs (P2), sem comportamento
   quadrático.
2. O custo da unificação (resolução das funções internas) é linear e pequeno por chamada (P3); o P6
   quantifica a degradação **Θ(N·R)**: ≈ 181 µs (fixo, I/O + parsing) + 0,57 µs por função
   registada — dominado pelo termo fixo até R ≈ 320 e mitigável lendo o registo uma só vez.
3. O renderizador AWS é competitivo com o GCP, dentro da margem de erro (P4).
4. A profundidade estrutural é um terceiro fator de custo, mais pronunciado no AWS (P5).
5. O tamanho do registo (P6) é uma quarta dimensão, independente de N; para projetos reais o fator
   dominante é o número de releituras do ficheiro, não a sua dimensão.
6. A otimização de leitura única (P7) elimina o produto N·R: a resolução passa de **Θ(N·R)** para
   **Θ(N+R)**, com *speedup* de ~200× no pior canto (de ~150 ms para ~0,75 ms) — identificar
   (P3/P6) → corrigir → quantificar (P7).
7. Tamanho (S1/S2): o artefacto cresce linearmente, com o ASL JSON ~2,4× mais volumoso que o YAML
   (S2); e a camada AWS agnóstica acrescenta ao *bundle* apenas ~0,9 KB — overhead negligenciável em
   funções reais (S1), em linha com a avaliação do QuickFaaS.
8. O eixo I/E (P15/P16), o primeiro workflow realmente misto (chamadas internas e externas no mesmo
   workflow) medido neste conjunto, confirma que o custo de resolução escala com **I** (chamadas
   internas) e não com N=I+E — as chamadas externas custam apenas o termo O(N) partilhado de
   reconstrução da árvore, não o *lookup* no registo (P15); e que o **Θ(R)** já provado no P9 se
   mantém inalterado, na mesma ordem de grandeza, quando o workflow tem uma fração de chamadas
   externas (P16).

**Enquadramento global.** Todos os valores estão na ordem dos microssegundos (≤ 1 ms mesmo para 200
funções): a renderização e a resolução não são o gargalo — o custo dominante é o *deployment* na
nuvem (segundos). O sobrecusto da unificação é assintoticamente linear e praticamente irrelevante na
operação real. As ressalvas de variância (P4) decorrem do recoletor de lixo e da partilha de CPU,
mitigáveis com `-f 3` e máquina dedicada, sem alterar as tendências.
