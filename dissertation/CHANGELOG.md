# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `8126f3c` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-15 — Validação da bibliografia contra as fontes

- `chapter1.tex` (Secção 1.1): os desafios em aberto (armazenamento de estado, *cold starts*,
  desempenho imprevisível) passam a ser atribuídos a Jonas et al., que os enumera na sua Secção 3;
  o *survey* de Shafiei et al. não os trata como tópicos.
- `chapter2.tex` (Secção 2.1.1): a frase sobre o fornecedor que gere recursos, aplica *patches* ao
  sistema operativo e escala passa a citar também Jonas et al. (o *survey* não fala de *patches*).
  "cannot directly call another" passa a "direct communication between functions is hard to do
  efficiently", porque o *survey* diz que as funções se chamam umas às outras, com dificuldade.
- `chapter2.tex` (Secção 2.1.2): a frase sobre controlo, dados e erros do *workflow* cita também
  a página de conceitos do Step Functions (erros, *Wait*, *Choice*, *Parallel*), que o SeBS-Flow
  não cobre.
- `chapter2.tex` (Secção 2.1.4): os quatro componentes do OmniFlow passam a citar Costa (Secção
  3.1), em vez de Silva, que descreve três.
- `chapter2.tex` (Secção 2.2.1): o Cloud Framework e o `@pulumi/cloud` da Pulumi passam a ser
  descritos como uma *preview* cuja última versão é de 2023.
- `bibliography.bib`: o terceiro autor do SEAPORT é Ayhan Kaplan (não "Avshalom"); o SeBS-Flow
  passa a citar a versão publicada no EuroSys '25 (DOI `10.1145/3689031.3717465`), mantendo a
  chave `sebsflow_arxiv`; o URL do Cloud Framework, que dava 404, passa a uma cópia do Wayback
  Machine de 14 de maio de 2023.
- `chapter2.tex` (Secção 2.2.1, CODE): o máximo de 9,23× menos linhas de código é face ao
  Terraform; face ao Serverless Framework o artigo dá 8,44×. O trabalho futuro do CODE passa a ser
  descrito como o artigo o enuncia (um *middleware* para workflows AFCL), em contraste com os
  *workflows* nativos do fornecedor desta dissertação.
- `chapter2.tex` (Tabela 2.1): o FaaSFlow passa de "P" a "--" em *Workflow portab.*; o artigo
  trata só da eficiência de execução e não fala de portabilidade.

