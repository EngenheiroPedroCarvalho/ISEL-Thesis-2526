# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `ca1b03f` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-14 — Revisão do orientador de 14 out (`template_14out_js.pdf`), pontos consensuais

- **Resumo e Abstract** (`abstract-pt.tex`, `abstract-en.tex`): o custo deixa de ser
  "desprezável"/"negligible" e passa a "mínimo e muito inferior ao da própria implantação na
  cloud" / "minimal and far below that of the cloud deployment itself".
- **Cap. 1** (`chapter1.tex`):
  - §1.1: acrescentado o *vendor lock-in* como desafio (cita QuickFaaS e OmniFlow) e o que
    *multi-cloud* significa nesta dissertação (a mesma definição implanta-se em mais de um
    fornecedor; cada implantação tem um só fornecedor).
  - §1.5: a contribuição 3 inclui a passagem do fornecedor GCP do QuickFaaS para funções de
    2.ª geração; retirado "with the supervisors of this dissertation" do parágrafo da publicação.
- **Cap. 3** (`chapter3.tex`):
  - Fig. 3.3 (`call-step-model`) à largura total do texto.
  - §3.2: reescritas as definições de função interna e de `functionRef`.
  - §3.3: o Nível 1 explica porque procura em todas as regiões uma função que desapareceu; o
    Nível 2 explica o que é uma função "encontrada" (implantada à mão ou noutra máquina).
  - §3.4: retirado o parágrafo "Why not two communicating services"; o parágrafo do subprocesso
    diz só "QuickFaaS" e explica que o ponto a integrar melhor é correr um JAR como processo à
    parte em vez de uma biblioteca.

## 2026-10-14 — Revisão do orientador de 14 out, decisões pendentes

- **"Resolution ladder" → "resolution sequence"** em todos os capítulos, apêndice A e glossário
  (`abstract-en.tex`, `chapter1/2/3/5/6/7/8.tex`, `appendix-cascade.tex`, `glossary.tex`); no
  Resumo, "escada" → "sequência". Títulos: §3.3 "The Resolution Sequence", §6.2 "Correctness of
  the Resolution Sequence". Os títulos dos seis diagramas por nível
  (`images/implementation/resolution-cascade-level*.png`) foram regenerados com o novo termo.
  Etiquetas e nomes de ficheiros (`sec:resolution-cascade`, `appendix-cascade.tex`) não mudam.
- **Caso de estudo com um só nome**, "payment authorization workflow": §1.5 (contribuição 4) e
  §7.1 deixam de dizer "card-payment fraud detection".
- **Fig. 3.2** (`images/architecture/architecture-after.png`) refeita: o QuickFaaS JAR passa para
  baixo, as caixas GCP e AWS ficam lado a lado e sai o título interno (repetia a legenda); a
  imagem fica mais estreita e, por isso, maior na página.
- **Capa** (`Config/_cover.tex`): data de entrega 16 October 2026.

## 2026-10-14 — Cap. 2, §2.1.1

- `chapter2.tex`: "This coupling has two axes" passa a "Function code depends on the provider in
  two ways"; as duas frases sobre a passagem do fluxo de controlo para um orquestrador ficam num
  parágrafo próprio.
