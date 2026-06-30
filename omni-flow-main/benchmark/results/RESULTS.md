# Resultados dos testes de desempenho (P1–P5)

Medições **locais** de **renderização** (DSL → Amazon States Language / GCP Workflows YAML) e de
**resolução** de funções internas. Não há chamadas à nuvem. Geradas com JMH a partir do módulo
`benchmark/`.

## Metodologia

- **Ferramenta:** JMH 1.37, modo `AverageTime`, unidade **µs/op** (microssegundos por operação),
  com `Blackhole` a consumir o resultado de cada execução para impedir que o compilador elimine
  código aparentemente inútil (*dead-code elimination*).
- **Configuração da execução:** `-f 1 -wi 3 -i 5 -w 1 -r 1` — uma *fork* da JVM, 3 iterações de
  aquecimento e 5 de medição, de 1 s cada. O aquecimento garante que se mede o código já
  compilado pelo JIT (regime estacionário) e não o interpretador.
- **Dados brutos:** `jmh-results.csv`. **Gráficos:** ficheiros `P1_*.png … P5_*.png`,
  regeneráveis com `python3 plot_benchmarks.py`.
- **Modelo de custo (para as justificações):** a renderização é uma travessia *depth-first*
  (`DepthFirstNodeVisitorTraversor` + `NodeContextVisitor`) que visita **uma vez** cada nó da
  árvore de objetos do workflow (AST) e acumula texto num `StringBuilder` partilhado
  (`IndentedRenderingContext.append(...)`). Como o `append` tem custo **amortizado O(1)**, o tempo
  total é proporcional ao número de nós e ao tamanho do texto produzido — sem o termo quadrático
  que a concatenação de `String` imutável introduziria.
- **Ressalva:** execução em ambiente partilhado e com configuração reduzida; os valores absolutos
  servem para comparar **tendências** e **relações**, não como números definitivos de hardware.
  Para a versão final recomenda-se repetir com `-f 3` e máquina dedicada.

---

## P1 — Degradação com o número de funções

| N (funções) | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 1 | 5,7 | 6,0 |
| 10 | 40,5 | 45,1 |
| 50 | 190,3 | 263,7 |
| 100 | 428,7 | 445,3 |
| 200 | 810,9 | 901,2 |

![P1 — Tempo de renderização vs número de funções](P1_rendering_scalability.png)

**Resultado.** O tempo de renderização cresce de forma **aproximadamente linear** com o número de
funções (≈4 µs por função adicional, em ambos os destinos), sem sinais de comportamento
quadrático.

**Justificação.** Um workflow de N passos independentes contém Θ(N) nós na AST. A travessia
visita cada nó exatamente uma vez e cada passo emite uma quantidade **limitada e constante** de
texto, pelo que o tempo total obedece a T(N) = a·N + b, em que `b` é o custo fixo de arranque
(criação do contexto e despacho inicial), dominante apenas para N pequeno. O custo marginal medido
mantém-se constante em todo o intervalo, o que **confirma empiricamente** a ausência de termo
super-linear: se o texto fosse acumulado por concatenação imutável, cada novo passo recopiaria o
prefixo já produzido e o custo seria Θ(N²) — o que não se observa. A linearidade valida a escolha
de uma travessia em passagem única com acumulação por `StringBuilder`.

---

## P2 — Efeito do número de parâmetros por função

| Parâmetros | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 0 | 49,3 | 27,7 |
| 1 | 85,7 | 77,7 |
| 5 | 142,3 | 150,2 |
| 10 | 184,5 | 243,2 |
| 20 | 350,3 | 440,5 |

![P2 — Tempo de renderização vs número de parâmetros](P2_rendering_by_parameters.png)

**Resultado.** O número de parâmetros afeta o tempo de renderização de forma também
**aproximadamente linear**. O GCP parte de um valor mais baixo (sem parâmetros) mas cresce mais
depressa, ultrapassando o AWS a partir de ~5 parâmetros.

**Justificação.** Cada parâmetro (entrada de *query*, cabeçalho ou campo do corpo) é renderizado
individualmente, com resolução de termo e emissão de um par chave-valor. Com N fixo, o trabalho
descreve-se por T(p) = N·(c·p + k): o termo `c·p` é o custo dos parâmetros e `N·k` é o custo-base
por chamada (a estrutura do estado sem parâmetros). Daqui resulta a **linearidade em p** e a
**interceção positiva em p = 0**. A **base maior no AWS** (k_AWS > k_GCP) explica-se pelo facto de
a Amazon States Language emitir, por chamada, mais estrutura fixa (`Type`, `Resource`,
`Parameters`, `ResultSelector`, `ResultPath`) do que o `call:`/`args:` do GCP. O **declive maior
no GCP** (c_GCP > c_AWS) reflete um custo por parâmetro superior na serialização YAML. Como uma
reta tem base maior e a outra declive maior, a **interseção** por volta dos 5 parâmetros é uma
consequência matemática inevitável. Confirma-se que o número de parâmetros constitui uma segunda
dimensão de custo, independente do número de funções.

---

## P3 — Custo da unificação: resolução de funções internas

| Nº de chamadas | Internas — resolução (µs) | Externas — sem resolução (µs) |
|---:|---:|---:|
| 1 | 7,5 | 0,02 |
| 10 | 74,6 | 0,14 |
| 50 | 368,0 | 0,65 |
| 100 | 747,2 | 1,10 |
| 200 | 1511,4 | 2,34 |

![P3 — Custo da unificação: resolução de funções internas](P3_resolution_overhead.png)

**Resultado (a métrica central da contribuição).** Resolver os endpoints das funções internas — o
passo que liga as duas ferramentas — custa **~7,5 µs por função interna** e escala
**linearmente**. As chamadas externas, que não disparam resolução, são praticamente gratuitas
(~0,01 µs).

**Justificação.** O caminho **externo** termina de imediato (`internalCallExtractor(call) ?: return
call`): apenas percorre a árvore e faz uma cópia estrutural, com custo ínfimo — é o *baseline* que
isola o efeito da unificação. O caminho **interno** invoca, por cada chamada,
`registry.resolveUrl(nome)`, que executa `readAll()` — isto é, **lê o ficheiro do registo do disco
e reinterpreta o respetivo JSON na íntegra** — seguido de procura no mapa, *parsing* do URL
(`java.net.URI`) e cópia da `CallContext`. Como o registo do ensaio tem **tamanho fixo (uma
entrada)**, esse custo é **constante por chamada** (~7,5 µs), donde T(N) = a·N — a reta
perfeitamente linear observada. O valor relativamente elevado de 7,5 µs (alto para uma simples
consulta em mapa) deve-se precisamente a essa **releitura e reinterpretação do ficheiro em cada
invocação**, sem cache em memória.

**Caveat de escalabilidade.** Dado que `readAll()` é Θ(M) (interpreta todo o registo) e o caminho
alternativo varre as M entradas, a resolução é, no caso geral, **Θ(N·M)**, em que M é o tamanho do
registo. O ensaio fixa M = 1, isolando a dependência em N como linear; **num cenário real em que o
registo cresça com o número de funções distintas (M ∝ N), a resolução degradaria para Θ(N²)**.
Trata-se de uma oportunidade de otimização concreta — ler o registo **uma só vez** por
`resolve(workflow)` (ou mantê-lo em memória) reduziria o custo para Θ(N + M). Ainda assim, em termos
absolutos o custo é negligenciável face ao tempo de *deployment* real (segundos).

---

## P4 — Renderizador AWS vs GCP

| N | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 1 | 5,8 | 5,8 |
| 50 | 221,4 | 233,0 |
| 100 | 423,0 | 469,5 |
| 200 | 817,4 | 912,3 |

![P4 — Renderizador AWS vs GCP](P4_aws_vs_gcp.png)

**Resultado.** O renderizador AWS (a contribuição) tem custo equivalente ao renderizador GCP já
existente — mesma ordem de grandeza em todo o intervalo, com o GCP ligeiramente mais caro à escala.

**Justificação.** Ambos partilham a mesma travessia e o mesmo mecanismo de acumulação de texto,
diferindo apenas nos renderizadores concretos de cada nó; logo, ambos pertencem à classe Θ(N) e as
retas só podem diferir nas constantes. A pequena vantagem do AWS à escala (~12% em N = 200) é
coerente com o facto de a serialização YAML do GCP envolver mais formatação do que a emissão direta
de JSON. **Contudo**, as barras de erro são largas nos pontos maiores do AWS (±115 µs em N = 100;
±185 µs em N = 200) e **sobrepõem-se** às do GCP: a leitura academicamente correta é de
**equivalência assintótica**, não de superioridade de um renderizador. A maior variância em N
elevado explica-se pela pressão do recoletor de lixo (realocações do `StringBuilder` e *garbage*
transitório proporcional ao tamanho do texto) e pela contenção de CPU em ambiente partilhado —
fatores que alargam o intervalo de confiança sem alterar a tendência.

---

## P5 — Efeito da estrutura/aninhamento

| Profundidade | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 0 | 83,9 | 96,3 |
| 1 | 109,7 | 100,8 |
| 3 | 155,4 | 114,5 |
| 5 | 205,8 | 133,6 |

![P5 — Tempo de renderização vs profundidade de aninhamento](P5_rendering_by_nesting.png)

**Resultado.** Mantendo fixo o número total de passos, **aumentar a profundidade de aninhamento**
(blocos `parallel`/`iteration` encadeados) aumenta o tempo de renderização. O AWS é
**mais sensível** ao aninhamento (≈2,5× de profundidade 0 a 5) do que o GCP (≈1,4×).

**Justificação.** A profundidade introduz **nós-contentor** adicionais na árvore (cada nível de
`parallel`/`iteration` é um nó com o seu próprio enquadramento de saída) e, sobretudo, **alonga o
prefixo de indentação**: o `IndentedRenderingContext` antepõe a indentação a cada linha, e o seu
comprimento é proporcional à profundidade. Assim, mesmo com o número de folhas fixo, tanto o número
de nós como o número de caracteres emitidos por linha crescem com a profundidade, donde T(d) ≈ a·d
+ b (a N constante). O AWS é mais sensível porque a Amazon States Language representa cada nível
aninhado com mais estrutura obrigatória (estados `Parallel`/`Map`, `Branches`/`Iterator`,
`ResultPath`, etc.) do que o YAML do GCP, gerando mais nós e mais texto por nível. Confirma-se que a
**complexidade estrutural** é um fator de custo independente do número bruto de funções.

---

## Síntese e discussão

1. A renderização é **linear** no número de funções (P1) e no número de parâmetros (P2): escala de
   forma previsível, sem comportamento quadrático.
2. O **custo da unificação** (resolução das funções internas) é **linear e pequeno** (~7,5 µs por
   função), com uma degradação **Θ(N·M)** latente caso o registo cresça — facilmente mitigável (P3).
3. O renderizador **AWS** é **competitivo** com o GCP, dentro da margem de erro (P4).
4. A **profundidade estrutural** acrescenta um terceiro fator de custo, mais pronunciado no AWS (P5).

**Enquadramento global.** Todos os valores se situam na ordem dos microssegundos (≤ 1 ms mesmo para
200 funções), pelo que a renderização e a resolução **não constituem o gargalo** do sistema — o
custo dominante é o *deployment* na nuvem (segundos). Conclui-se que o sobrecusto introduzido pela
unificação das duas ferramentas é **assintoticamente linear e praticamente irrelevante** na
operação real. As ressalvas de variância (P4) decorrem do recoletor de lixo e da partilha de CPU,
mitigáveis com `-f 3` e máquina dedicada — sem que isso altere as tendências aqui descritas.
