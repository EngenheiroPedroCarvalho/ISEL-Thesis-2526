# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `ea710f2` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-29

### Todo o documento: letra Verdana (LuaLaTeX) e margens, pelas normas do TFM do ISEL (n.º 10)

- A compilação passa de pdfLaTeX a LuaLaTeX (`Makefile`, alvos `pdf` e `vv`), e o texto sai em
  Verdana em vez de Latin Modern Sans: `Config/_packages.tex` só carrega `fontenc`/`lmodern` com
  pdfLaTeX. O PDF passa de 90 para 99 páginas.
- `template.tex`: o número do capítulo, o título e o filete ficam dentro da margem (antes o número
  ficava 1,5 cm dentro da margem esquerda); `\emergencystretch` de 1em para 3em;
  `\hyphenation{Omni-Flow Quick-FaaS Cloud-Formation}`; coluna de descrição do glossário com
  0.55\textwidth; listagens com `xleftmargin=24pt`, para os números de linha não ficarem na margem.
- Tabelas que a Verdana fazia passar a margem: Tabela 2.1 (`chapter2.tex`, `\footnotesize`,
  primeira coluna mais estreita e alinhada à esquerda); Tabelas 6.3 (T1, minipages 0.34/0.63), 6.4
  (células alinhadas à esquerda) e 6.6 (S1, primeira coluna com largura fixa) em `chapter7.tex`;
  Tabelas C.1, C.9 e C.10 em `appendix-benchmarks.tex`.
- Apêndice B (`appendix-listings.tex`): a Listagem B.6 fica numa minipage, para a legenda não ir
  sozinha para a página seguinte.
