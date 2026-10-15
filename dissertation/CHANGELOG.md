# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `3c68bb2` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-10-15 — Conformidade com as normas do TFM (`template.tex`)

- Parágrafos indentados 16 pt (`16bp`), incluindo o primeiro depois de cada título
  (`indentfirst`) (n.º 15).
- Subsecções (x.y.z) a 11 pt em vez de 10,5 pt (n.º 13).
- Texto todo a preto: `linkscolor=black` (citações, remissões e URLs) e listagens sem cores de
  sintaxe (n.º 10).
- Retirada a página de copyright entre a declaração de integridade e o Resumo
  (`\printcopyrightpage` vazio), para seguir a ordem dos n.º 4 a 7. O PDF passa de 100 para
  99 páginas.

## 2026-10-15 — Resumo abaixo das 300 palavras (n.º 6)

- `abstract-pt.tex`: o Resumo passa de cerca de 321 para 295 palavras, sem mudar o conteúdo.
  Ficou mais conciso o 1.º parágrafo (o *endpoint* e a cópia manual) e foram reformulados o suporte
  para o AWS Lambda, o caso de estudo ("foi executado no AWS e no Google Cloud"), as medições e a
  frase final ("que demora minutos").

## 2026-10-15 — Subcapa: designações do júri (n.º 3, Anexo II EN)

- `template.tex`: "Examination Committee" passa a "Jury" e "Raporteurs" (com erro ortográfico)
  passa a "Members of the Committee", como no Anexo II em inglês. "Chairperson" fica igual. O
  comentário dos tamanhos dos títulos foi corrigido para "subsections 11 pt".

## 2026-10-15 — Capa e subcapa: tamanhos de letra (n.º 2-3, Anexos I e II)

- `template.tex`: os itens da capa e da subcapa foram redefinidos com os tamanhos dos anexos,
  sem mexer em `defaults.clo`. Ficam: título a 17 pt; nome e grau a 12 pt (o nome na subcapa
  estava a 14 pt e o grau a 13 pt); texto do grau, orientadores, júri e data a 9 pt (estavam
  entre 11 e 13 pt). As posições e os espaçamentos não mudam.
- Só o título e a data ficam a bold, como nos anexos. O nome, "SUPERVISORS", "JURY",
  "Chairperson" e "Members of the Committee" passam a peso normal.

## 2026-10-15 — Dedicatória numerada (n.º 9)

- `template.tex`: `\dedicatory` foi redefinido sem `\thispagestyle{empty}`, pelo que a dedicatória
  passa a ter o número "i" no rodapé, como as outras páginas iniciais. Só a capa e a subcapa ficam
  sem número.

## 2026-10-15 — Espaçamento de 1,5 linhas como no Word (n.º 10)

- `template.tex`: `\setSpacing{1.474}` dá 20 pt entre linhas no texto a Verdana 11, o mesmo que
  "1,5 linhas" no Word (antes eram 16,4 pt). A bibliografia, os apêndices e os anexos mantêm o
  espaçamento simples. No índice, a caixa dos números de página foi alargada
  (`\setpnumwidth`, `\setrmarg`) para caber "xviii".
- O PDF passa de 99 para 108 páginas: o corpo do texto vai de 49 para 55 páginas e os
  agradecimentos ocupam agora duas páginas. O Resumo e o Abstract continuam a caber numa página,
  com as palavras-chave.
