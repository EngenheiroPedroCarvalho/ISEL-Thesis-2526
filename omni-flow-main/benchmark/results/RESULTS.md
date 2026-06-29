# Resultados dos testes de desempenho (P1–P5)

Medições **locais** de **renderização** (DSL → Amazon States Language / GCP Workflows YAML) e
**resolução** de funções internas. Não há chamadas à cloud. Geradas com JMH a partir do módulo
`benchmark/`.

## Metodologia

- **Ferramenta:** JMH 1.37, modo `AverageTime`, unidade **µs/op**, `Blackhole` a consumir o
  resultado (evita *dead-code elimination*).
- **Configuração da execução:** `-f 1 -wi 3 -i 5 -w 1 -r 1` (1 *fork*; 3 iterações de *warmup* e
  5 de medição, de 1 s cada). Suficiente para médias estáveis dado que cada operação é de
  µs (milhares de invocações por iteração).
- **Dados brutos:** `jmh-results.csv`. **Gráficos:** `P1_*.png … P5_*.png`
  (regenerar com `python3 plot_benchmarks.py`).
- **Ressalva:** execução num ambiente partilhado; os valores absolutos servem para comparar
  *tendências* e *relações*, não como números definitivos de hardware. Para a versão final da
  dissertação, recomenda-se repetir com `-f 3` e máquina dedicada.

## P1 — Degradação com o número de funções

| N (funções) | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 1 | 5,7 | 6,0 |
| 10 | 40,5 | 45,1 |
| 50 | 190,3 | 263,7 |
| 100 | 428,7 | 445,3 |
| 200 | 810,9 | 901,2 |

**Conclusão:** o tempo de renderização cresce de forma **aproximadamente linear** com o número de
funções (≈4 µs por função adicional, em ambos os destinos). Não há sinais de comportamento
quadrático — a travessia depth-first e a construção do output escalam bem.

## P2 — Efeito do número de parâmetros por função

| Parâmetros | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 0 | 49,3 | 27,7 |
| 1 | 85,7 | 77,7 |
| 5 | 142,3 | 150,2 |
| 10 | 184,5 | 243,2 |
| 20 | 350,3 | 440,5 |

**Conclusão:** o número de parâmetros **afeta** o tempo de renderização, de forma também
**aproximadamente linear**. O GCP parte de um valor mais baixo (sem parâmetros) mas cresce mais
depressa, ultrapassando o AWS a partir de ~5 parâmetros.

## P3 — Custo da unificação: resolução de funções internas

| Nº de chamadas | Internas — resolução (µs) | Externas — sem resolução (µs) |
|---:|---:|---:|
| 1 | 7,5 | 0,02 |
| 10 | 74,6 | 0,14 |
| 50 | 368,0 | 0,65 |
| 100 | 747,2 | 1,10 |
| 200 | 1511,4 | 2,34 |

**Conclusão (a métrica central da contribuição):** resolver os endpoints das funções internas — o
passo que **liga as duas ferramentas** — custa **~7,5 µs por função interna** e escala
**linearmente**. As chamadas externas (que não disparam resolução) são praticamente gratuitas
(~0,01 µs). Ou seja, o *overhead* introduzido pela unificação é linear e, em termos absolutos,
**negligenciável** face à renderização e, sobretudo, ao deployment real (segundos). É o custo de
uma comodidade: deixar de ligar workflow e funções à mão.

## P4 — Renderizador AWS vs GCP

| N | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 1 | 5,8 | 5,8 |
| 50 | 221,4 | 233,0 |
| 100 | 423,0 | 469,5 |
| 200 | 817,4 | 912,3 |

**Conclusão:** o renderizador **AWS** (a minha contribuição) tem custo **equivalente** ao
renderizador GCP existente — mesma ordem de grandeza ao longo de todo o intervalo, com o GCP
ligeiramente mais caro à escala. A extensão AWS não introduz penalização de desempenho.

## P5 — Efeito da estrutura/aninhamento

| Profundidade | AWS (µs) | GCP (µs) |
|---:|---:|---:|
| 0 | 83,9 | 96,3 |
| 1 | 109,7 | 100,8 |
| 3 | 155,4 | 114,5 |
| 5 | 205,8 | 133,6 |

**Conclusão:** com o número total de passos fixo, **aumentar a profundidade de aninhamento**
(parallel/iteration encadeados) aumenta o tempo de renderização. O AWS é **mais sensível** ao
aninhamento (≈2,5× de 0→5) do que o GCP (≈1,4×), provavelmente por gerar mais estrutura na
Amazon States Language por nível. Confirma que não é só o **número** de funções a pesar, mas
também a **complexidade estrutural**.

## Síntese

1. Renderização **linear** no nº de funções (P1) e no nº de parâmetros (P2) — escala previsível.
2. O **custo da unificação** (resolução interna) é **linear e pequeno** (~7,5 µs/função) (P3).
3. O renderizador **AWS** é **competitivo** com o GCP (P4).
4. A **profundidade estrutural** importa, mais no AWS (P5).
