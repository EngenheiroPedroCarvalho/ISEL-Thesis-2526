# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `b20507a` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-29

### Apêndice A (`appendix-cascade.tex`): diagramas seguidos e maiores

- Os diagramas deixam de ser floats (`figure`) e passam a vir seguidos (`minipage` com
  `\captionof`), cada um no maior tamanho que a página permite; acabam as páginas com um só
  diagrama centrado.
- Níveis 2 e 3: os painéis (a) GCP e (b) AWS passam de lado a lado para um por cima do outro, à
  largura do texto.
- Nível 1: os dois painéis são altos demais para a mesma página; (a) GCP fica na Figura A.3 e
  (b) AWS continua-a na página seguinte (`\ContinuedFloat`, sem nova entrada na lista de figuras).
- A introdução do apêndice diz agora "panel (a)"/"panel (b)" em vez de "side by side".
- **Figura A.1 (modelo de classes):** o diagrama passa a ler-se de cima para baixo (pontos de
  entrada → resolvers → lookups e registry → deployers), com o GCP à esquerda e a AWS à direita;
  é mais estreito, por isso sai maior na página (`diagrams/omniflow-integration-classes.puml`).
- **Figura A.2 (sequência de deployment na AWS):** dividida em dois painéis. (a) é a visão geral,
  com 7 participantes em vez de 10 e o Nível 3 recolhido num bloco `ref`; (b), na página seguinte,
  expande o Nível 3 (nova fonte `diagrams/aws-deploy-sequence-level3-full.puml`). A nota do Nível 1
  aponta para a Figura A.3; a legenda foi reescrita em conformidade.
