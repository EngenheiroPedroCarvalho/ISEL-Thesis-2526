# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `27ccff7` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-29

### Avaliação: notas da revisão do Prof. José Simão (cap. 6 e 7, Apêndice C)

- **Cap. 6 (`chapter7.tex`), abertura e resposta à RQ2:** a RQ2 passa a ser o trabalho *local*
  que a resolução acrescenta e os pedidos que envia ao fornecedor.
- **Cap. 6, secção 6.1:** explica que a contagem de pedidos ao fornecedor é analítica (derivada
  do código, porque os benchmarks correm sem rede).
- **Cap. 6, secção 6.2:** diz explicitamente que os testes verificam a decisão do resolver perante
  uma resposta simulada do fornecedor; explica porque é que o registo é um ficheiro real num
  diretório temporário (I/O local, dentro do âmbito dos testes). Novo parágrafo sobre um registo
  alterado fora do OmniFlow: JSON inválido aborta; sem `functions` é lido como vazio; `url`
  alterado é corrigido como *drift*; `serviceName` alterado para outra função existente é aceite.
- **Cap. 6, secção 6.3:** explica `Blackhole`, *forks* da JVM e iterações de aquecimento; a
  máquina passa a ser identificada (Apple M5, 10 núcleos, 24 GB).
- **Cap. 6, secção 6.4:** o parágrafo T2–T5 foi reduzido a um resumo; a tabela T3
  (`tab:eval-t3`) passou para o Apêndice C.
- **Apêndice C (`appendix-benchmarks.tex`):** secção "Resolution Against the Reference Resolver"
  passa a "(T2--T5)", com a tabela T3 e o custo por leitura de T2 no texto; corrigido o
  `\texttt` que aparecia como "exttt".
- **Cap. 7 (`chapter8.tex`), limitações:** corrigido "a tampered `url` would be trusted": um `url`
  adulterado é corrigido pela validação; o que seria aceite é um `serviceName` adulterado.

### Avaliação: T1 medido de novo (cap. 6 e Apêndice C)

- **Cap. 6 (`chapter7.tex`), secção 6.4:** a tabela T1, a Figura T1
  (`images/benchmarks/T1_resolution_overhead.png`) e o texto usam a nova execução do T1 (`-f 3`,
  2026-09-29): leitura do registo ≈ 10,7 µs (era 9,8), 0,23 µs por chamada interna (era 0,22),
  0,005 µs por externa (era 0,006), 200 chamadas em 56,4 µs (era 54,9). O ponto N=10 passou de
  16,9 ± 16,8 µs a 13,3 ± 0,05 µs; saiu a frase da legenda sobre a barra de erro. Mesmos números na
  linha T1 da Tabela 6.2 e na resposta à RQ2.
- **Cap. 6, Metodologia e Ameaças à validade:** o T1 passa a contar, com o T6 e o T7, entre as
  experiências medidas depois da execução completa; os valores de execuções diferentes concordam a
  cerca de 10%.
- **Apêndice C:** o T1 N=10 saiu da lista de pontos ruidosos (ficam dois).
