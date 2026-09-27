# Alterações aos capítulos da dissertação

Só alterações ainda não enviadas (push). Base: `0e7a35f` (último commit em
`origin/claude/progress-report-compliance-e4damf`). Depois de um push, o registo recomeça vazio.

(`changelog.txt`, nesta pasta, é o histórico do template `iselthesis` e não tem nada a ver com
este ficheiro.)

## 2026-09-27

### Revisão de estilo: repetições, adjetivos e termos pouco usados (todos os capítulos)

**Repetições cortadas** (fica uma explicação, e os outros sítios remetem para ela):
- Cap. 1 (`chapter1.tex`): o ARN deixa de ser explicado duas vezes em bullets seguidos de §1.2.
- Cap. 3 (`chapter3.tex`): a introdução e o roteiro do capítulo ficam mais curtos; os campos do
  descritor deixam de ser descritos outra vez em §3.3 (remissão para §2.1.3.4); a exclusão mútua
  entre `internalFunction` e `host`/`path` passa a ser dita uma só vez; tirado o parêntese sobre a
  região no ARN.
- Cap. 4 (`chapter5.tex`): o bootstrap já não repete a promoção das chaves para `region/name`;
  o bullet de drift remete para o Nível 1 em vez de o reescrever; §4.1.5 (Cloud Run Functions)
  deixa de repetir que cada função implementada é um serviço Cloud Run; a co-localização
  Lambda/bucket remete para §3.1.1; a subsecção Evolution remete para o snapshot de §4.1.4; o
  motivo de o `accessToken` ficar vazio na AWS passa para §4.2.2 (vinha de §3.3).
- Cap. 5 (`chapter6.tex`): tirados os nomes dos ficheiros do registo (já em §4.1.1).
- Cap. 7 (`chapter8.tex`): as funções de 2.ª geração já não são mencionadas no objetivo 2 nem no
  fim do limite "Cloud Run services only on GCP" (ficam no Resumo e no limite).

**Frases com muitos adjetivos ou ênfase simplificadas:** abertura do cap. 1 ("fundamentally
reshaped the landscape…", "transformative paradigm", frase sobre transformação digital retirada);
cap. 2 (FaaS, workflows geridos, View do MVC, estratégia ZIP, FaaSFlow, Temporal, FaaSr); cap. 3
("Crucially", "Importantly", "precisely", "heart of the integration", "three desirable
properties", "inherently… ephemeral"); cap. 4 ("clean extension point", "deliberately narrow",
"single, symmetric abstraction", "A deliberate design choice"); cap. 5 (introdução); cap. 6
(primeira conclusão da RQ2; retirado "identify → fix → quantify loop").

**Termos pouco usados substituídos:** "embryonic form", "paper over"/"rescued", "owed",
"memoise", "seam", "fires", "cold" (registo), "worst corner", "rules", "provider-shaped",
"surfaces", "keying error handling off…"; "drifted" (cap. 1) passa a ser explicado; o fragmento
`alt` da Fig. 4.2 é explicado.

**Terminologia e ortografia:** a camada de empacotamento passa a chamar-se sempre
"provider-agnostic layer" (caps. 1 e 6, incluindo a tabela de S1); o cap. 3 passa a dizer
"internal function/call" (definido em "Function Ownership Determines Resolution") em vez de
"resolved this way"; "Cloud Functions" → "Cloud Run functions" no cap. 5; `organized` →
`organised`, `behavior` → `behaviour`, `artefacts` → `artifacts`.

