# TODO: thesis and code improvements

Ideas from the logical-flow review of 2026-09-11. Chapter numbers are those of the PDF; see
`dissertation/CLAUDE.md` for the file-to-chapter map. Thesis paths (`Chapters/…`, `Config/…`,
`images/…`) are relative to `dissertation/`; code paths are relative to the repo root. Line numbers
drift as text is edited, so search for the quoted phrases.

## Done

- [x] One registry file per provider in the code (`function-registry.gcp.json` / `.aws.json`).
- [x] Discovery errors (inaccessible regions) no longer fall through to a QuickFaaS deployment
      (code and tests).
- [x] The GCP first-generation Cloud Functions gap is documented as a limitation (Ch2, Ch4, Ch5,
      Ch7, Ch8).
- [x] The cascade-diagram appendix is ported to `Chapters/appendix-cascade.tex` (Appendix A). The
      template R appendix (`appendix1.tex`) is no longer built, and all references resolve.
- [x] Edits that existed only in `ISEL-Thesis-2526/thesis/` are merged: Ch1's 8-chapter structure
      list, Ch5's corrected "propagation" sentence, Ch7's punctuation and listing fixes, and Ch8's
      closing sentence.
- [x] The P6 figure in Ch6 has its own caption and label (`fig:eval-p6`), and the text references it.
- [x] The lorem-ipsum Appendix B and Annex I are no longer built (commented out in
      `Config/_files.tex`; the files stay on disk).
- [x] `TESTING.md`'s P10/P11 rows describe the single-read resolvers; the `GoogleCloudDeployer`
      bootstrap log prints the path and says "Bootstrapping … Cloud Run APIs".
- [x] `acronyms.tex` defines the thesis's acronyms, and all of them are printed (`\glsaddall`),
      because the text writes them as plain text. `glossary.tex` keeps its template entries,
      which nothing uses, so no glossary is printed.
- [x] The dedicatory is built (`dedicatory_.tex` renamed to `dedicatory.tex`). Its text is still
      the template's "To my family and friends ⋯"; personalise it if you like.
- [x] The four commented-out Background figures in Ch2 (their images were never recovered) and
      the commented-out references to them are deleted.
- [x] Ch1 §1.4–1.5 argue from three points: the endpoint exists only after deployment, it depends
      on the account/project/provider, and the manual steps are real (citing Ch2's QuickFaaS
      procedure, now labelled `subsec:quickfaas-example`). Ch1 no longer mentions `secretRef`,
      promises "deploy or update" (only "deploy if missing"), or claims "most serverless platforms
      enforce a tight coupling".
- [x] Research questions: Ch1 states RQ1 (feasibility and correctness) and RQ2 (overhead); Ch6
      answers them (new correctness section, "Answer to RQ1/RQ2" in the Discussion, a threat to
      validity), and Ch8's Objectives Revisited restates the answers.
- [x] AWS support is Ch1's Objective 3, with its reason: OmniFlow targets AWS and GCP, QuickFaaS
      targeted GCP and Azure, so they overlapped only on GCP. Ch8 lists it among four objectives.
- [x] Ch4 states that binding is static (new "Static binding" paragraph in the resolution-cascade
      section), and the walkthrough no longer claims the registry "would have absorbed" an
      endpoint change: on AWS the next deployment picks it up; on GCP first-gen it doesn't.
- [x] Ch4's "Stale binding" edge case covers both providers, says it aborts even with a
      descriptor, and notes that stale GCP first-gen entries go undetected.
- [x] Ch4 contradictions: Level 1 explains what the registry buys (validation is one lookup in a
      known region, not a scan); the region-scope paragraph says the search starts in the
      workflow's region and co-location is expected, not required; the architecture text, both
      registry listings (now per provider, with `cloudfunctions.net` URLs for QuickFaaS-deployed
      GCP functions and full Cloud Run resource names) and `architecture-after.puml`/`.png` show
      one registry per provider. The PNG was rendered with the PlantUML 1.2024.7 jar and
      `-Playout=smetana` (Graphviz isn't installed).
- [x] The last three §3 contradictions: Ch5 step 2 says a stale entry is rediscovered or aborts
      (even with a descriptor) and why; Ch4's "safe by default" covers validation and discovery
      and says what a deployment changes (permissions, including AWS trust-policy repair); Ch4
      explains the `functionRef` vs `function.name` mismatch (deploys, then times out).
- [x] Conclusions: each objective has a verdict with evidence (1 and 3 met, 2 met on AWS and
      partially on GCP, 4 partially met); "the design is sound", "already-sound design" and
      "validated concept" are gone; the Critical Assessment adds static binding and the missing
      update path, and Future Work adds the update path.
- [x] Case Study: the requirements are "one workflow, several accounts", "an exit strategy" (DORA
      Arts. 28(8) and 29, new bib entry `dora2022`) and auditability; the "redeploy a week later"
      example is replaced by promotion to a production account (with its own registry file); the
      Discussion says in-place model updates keep the endpoint and need no registry. Ch4 (intro,
      architecture), Ch5 (motivation, registry evolution) and Ch8 Applicability no longer argue
      from "redeploying changes the endpoint".
- [x] Related Works: the IaC section acknowledges deployment-time binding in AWS SAM
      (`DefinitionSubstitutions`), the Serverless Framework Step Functions plugin (`Fn::GetAtt`),
      Terraform (interpolated references) and Pulumi (outputs as inputs), each cited to its docs;
      the comparison table replaces "Workflow-first resolution" with "Deploy-time binding" (Y for
      the IaC tools), splits the IaC row, and footnotes that this work's portability marks come
      from OmniFlow and QuickFaaS.

## 1. Blockers before submission

- [x] Abstracts written (2026-09-12), both following problem → approach → evidence → implication:
      `Chapters/abstract-en.tex` (296 words) and `Chapters/abstract-pt.tex` (300 words, the limit),
      each with keywords. They deliberately cite no absolute benchmark figure — only the shape
      ("milliseconds", "linear in the number of internal calls, not in the registry size") — so the
      `-f 3` re-run cannot invalidate them. Re-read them once the final numbers are in.
- [x] Acknowledgments written (2026-09-13) in `Chapters/acknowledgments.tex`, in English to
      match the body: supervisors by name (from `Config/_cover.tex`), Santander Portugal and
      its Enterprise Architecture team, parents and family, and Mariana. Institutional first,
      then personal, as the template suggests.

## 2. Argument (highest impact)


## 3. Contradictions to fix


## 4. Evaluation (Ch6)

- [x] Correctness (RQ1): Ch6 "Correctness of the Resolution Cascade" (`tab:eval-correctness`)
      reports the resolver unit tests; "absent, no descriptor" and "both `internalFunction` and
      host/path set" are covered on both providers (45 resolver tests, 22 AWS + 23 GCP).
      Evidence against the real providers: **decided not to do (2026-09-13)** — it needs AWS and
      GCP accounts, and this machine has neither (`aws` and `gcloud` are not installed, there is
      no `~/.aws` and no application-default credentials). Two things to know before reopening
      this: the two `@Ignore`d tests in `WorkflowTest` would **not** close the gap as written —
      they are hardcoded to the original author's GCP project (`workflow-test-380423`) and AWS
      account (`610299836666`), and their workflow has no internal function at all (both `call`s
      set a literal API Gateway `host`), so they never enter the cascade. Closing the gap means
      new tests using `internalFunction` against an account of your own. Ch7 states the limit in
      `sec:eval-threats` ("the resolver tests replace the provider with test doubles ... not that
      the real AWS and GCP APIs report what the doubles assume").
- [x] Count the provider API calls per cascade level: Ch7 §"Provider API Calls per Resolution"
      (`sec:eval-api-calls`, `tab:eval-api-calls`) counts them from the code. Key point: Level 1
      validates **per call**, not per distinct function, so a deployment of I internal calls costs
      I requests when the registry is warm and up to 1 + I·G (G = candidate regions) when it is
      cold with bare references.
- [x] Time a few end-to-end deployments (L1/L2/L3 × AWS/GCP; report median and range):
      **decided not to do (2026-09-13)**, same reason — no cloud credentials here, and measuring
      it would create billable Lambdas, Step Functions, Cloud Functions and Cloud Workflows.
      `BenchmarkAmazonDeployment`/`BenchmarkGoogleDeployment` do not help: they time
      `createStateMachine`/`deploy` for a workflow with no internal calls, so they never touch the
      cascade. Ch7 already scopes the evaluation as local-only in `sec:eval-threats` ("it
      quantifies the resolution and packaging overhead, not the end-to-end cloud deployment
      latency").
- [x] Compare manual effort before and after: Ch7 §"Manual Effort Before and After"
      (`sec:eval-manual-effort`, `tab:eval-manual-effort`) — 9 steps / 4 hand-carried values
      (separate tools, GCP) vs 4 steps / 0, derived from Ch2's `subsec:quickfaas-example` and Ch4's
      `sec:developer-workflow`. Two qualifications stated: configuration is relocated, not removed,
      and the saving is per function and per endpoint change.
- [x] **The P-tables mix several measurement runs** — resolvido 2026-09-12: toda a suite correu numa
      só sessão com `-f 3` (3h10, 21:26), o CSV está em `benchmark/results/jmh-results-f3.csv`, os
      `jmh-results-p*.csv` foram regerados a partir dele e as 15 tabelas do Cap. 7 vieram desse
      único run. O parágrafo de proveniência saiu dos "Threats to Validity".
      Contexto original:
- [x] **(original) The P-tables mixed several measurement runs** (found 2026-09-12 by diffing each table
      against its CSV). `tab:eval-p7`, `tab:eval-p9` and `tab:eval-p13` do NOT match
      `jmh-results-p7/p9/p13.csv`; P3, P6, P8, P10, P11 and P17 do. The proof it matters: P17 =
      P13 + a failed lookup, so P17 must cost more, yet the current CSVs give P17 < P13 (3309 vs
      4682 µs at K=1/R0=0). Likewise a registry read costs ~5–7 µs in the P3/P7 tables and ~180 µs
      in the P6/P8/P9 runs. So: re-run the WHOLE suite in ONE session with `-f 3`, regenerate every
      table from it, then delete the provenance paragraph now in "Threats to Validity".
- [x] Re-run JMH with `-f 3` — feito 2026-09-12. Margens de erro: intervalo de confiança a 99,9%
      abaixo de 1% do valor em metade das medições e abaixo de 6% em nove em cada dez; os poucos
      pontos ruidosos (P3 N=10, P17 K=10/R0=10, P18 depth=2) estão identificados no texto. O caveat
      "not thesis-grade" saiu; a Metodologia agora descreve a máquina (portátil Apple Silicon, ocioso
      mas não dedicado) e diz que o fator de hardware dominante é a velocidade do disco.
      Receita usada, confirmada na prática (manter para futuras repetições). **A corrida demorou
      3h10m** (18:16→21:26), não as ~2h11m que o JMH estimou no início, por isso conta com mais de
      três horas de máquina ocupada:

      1. Build: `JAVA_HOME=/opt/homebrew/opt/openjdk@26/libexec/openjdk.jdk/Contents/Home \
         ./mvnw -o -q -pl benchmark -am clean package -DskipTests`
      2. Run from a scratch directory with `-f 3 -wi 3 -i 5 -w 1 -r 1 -rf csv -rff <out>.csv`,
         passing an **include** regex that names exactly the 14 classes behind P3 and P6–P19:
         InternalCallResolution, RegistryScaling, ResolutionOptimization,
         ResolveWorkflowByFunctionsAndCalls, ResolveWorkflowByRegistrySize(Mixed),
         AwsInternalFunctionResolution, GoogleInternalFunctionResolution, RegistryWriteScaling,
         ResolutionKeyMatchStrategy, ResolveWorkflowByInternalExternalMix, RegistryMissAndDeploy,
         InternalResolutionByNesting, InternalResolutionByBranchWidth.
      3. **Never run it without that include list.** `BenchmarkAmazonDeployment` calls
         `createStateMachine` and `BenchmarkGoogleDeployment` calls `deploy` — they create real
         resources on AWS/GCP.
      4. **Filter stdout**: `| grep --line-buffered -E '^#|^Iteration|^Result|^Benchmark'`. Já não é
         crítico desde 2026-09-12 (os resolvers deixaram de imprimir por chamada), mas antes disso um
         log não filtrado crescia ~1 GB/min (chegou a 2,1 GB num minuto antes de ser morto), por isso
         mantém o filtro.
      5. JMH writes the CSV only when the whole run ends, so an interrupted run leaves nothing.
         Write it to a scratch path and only copy into `benchmark/results/` once it is complete,
         so the current CSVs survive a failed attempt.
- [x] Os `println` **são** materiais, e agora está medido: em P10/P11 dominam o custo por chamada
      (~2,3 µs por linha impressa contra 0,22 µs de resolução). A prova está na diferença entre
      providers — o resolver AWS imprime 2 linhas por chamada e o GCP 3, e a diferença medida
      (2,4 µs/chamada) é exatamente uma linha. Está dito no parágrafo P10/P11 e nos "Threats to
      Validity". Nota: P3 e P8–P19 usam o `OptimizedEndpointResolver` do módulo de benchmarks, que
      **não** imprime, por isso só P10/P11 foram afetados.
- [x] **Resolvido 2026-09-12:** as mensagens por chamada dos dois resolvers de produção passaram de
      `println`/`logger.info` para `logger.debug` (que ao nível INFO por omissão nem avalia o
      lambda); as mensagens raras — descoberta por regiões, deployment QuickFaaS, drift, erros —
      continuam a ser impressas. P10/P11 foram re-medidos com `-f 3` (24 min) e caíram de 1006→29,1 µs
      (AWS) e 1344→81,3 µs (GCP) em F=10/N=200. Os 45 testes dos resolvers continuam a passar.
      **Consequência a lembrar:** P10/P11 passam a vir de uma segunda sessão de medição; está dito na
      Metodologia e nos "Threats to Validity", com a prova de que as duas sessões são compatíveis (a
      coluna N=1 do P10/P11 reproduz o custo de leitura do P6).
- [x] **Diferença AWS/GCP explicada:** depois de calados os `println`, o GCP continua ~4× mais caro
      por chamada (0,35 vs 0,07–0,14 µs) porque `splitUrl` faz `URI(url)` a cada chamada, onde o AWS
      só concatena `lambda://` ao ARN. O resolver de referência (P3/P8), que também faz o parsing,
      fica no meio, a 0,22 µs.
- [x] P3: explained as the single-read resolver with R=1 (per-call cost = snapshot lookup + URL
      split + node rebuild), no longer as a re-read per call.
- [x] P6: R≈320 is derived in the P6 paragraph (0.56·R = 180), and the Discussion only cites it.
- [x] P10/P11: local fake inspector (AWS) / 1st-gen short-circuit (GCP) stated; growth in N
      explained as per-call validation; "comparable to P8" dropped.
- [x] P13/P17: cost per write is roughly fixed (~2.7→3.3 ms from K=1 to K=100), so the total is
      near-linear in K, not compounding; the miss adds 22–27% (46–99% in the noisy R0=1000
      column); "monotonic" is gone.
- [x] P6 figure: `plot_benchmarks.py` está todo em inglês, as 18 figuras foram regeradas a partir
      do run `-f 3` e `P6_registry_scaling.png` foi copiada para `dissertation/images/` (a única
      figura de benchmark que a tese inclui). PNG *file names* ficaram como estavam
      (`P7a_resolution_sem_cache.png`, …), porque a tese os inclui por nome.
- [x] Explain the missing P1/P2/P4/P5/P12: Ch7's new "Experiment identifiers" paragraph in the
      Methodology says they measure OmniFlow's renderers alone (code this work did not change) and
      that identifiers are kept, not renumbered, so they match the benchmark suite.
- [x] Label experiments consistently: the two headings that lacked IDs now carry them ("Cost of
      the unification (P3)", "Effect of registry size (P6)"), so every measured paragraph in Ch7 is
      identified.
- [x] Soften "overhead practically irrelevant": Ch7's Global framing now says the local overhead is
      "small beside the deployment it precedes" and that the figures are a **lower bound**, because
      per-hit live validation and the region scan are network round-trips replaced by local doubles;
      it points at `sec:eval-api-calls` for how many, and at the threats section for how long.

## 5. Structure and style

- [x] Ch1: §1.1 (Historical Context) and §1.2 (Modern Cloud Architectures) condensed into a single
      opening paragraph, keeping Parkhill/Vaquero/Armbrust/Buyya and dropping the IaaS/PaaS/SaaS
      bullets and Kubernetes. The chapter now reaches the problem in §1.2 instead of §1.4.
      `awsWhitepaper2024` and `kubernetesDesign` are no longer cited by Ch1.
- [x] Ch2: intro rewritten (it claimed this chapter reviews related work, which is Ch3); sections
      reordered to FaaS → Workflows and Orchestration → QuickFaaS → OmniFlow, with the two
      overlapping workflow sections merged into one under `sec:workflows`.
- [x] Ch1 "Structure of the Work" updated for the new chapter order (6 Case Study, 7 Evaluation).
      Worth remembering: the build cannot catch this kind of drift, since it is prose, not
      `\ref`s — any further chapter reordering has to be checked there by hand.
- [x] Ch5 (`chapter5.tex`): "Concept and Conventions" and "Deployment-Time Endpoint Resolution"
      point at `sec:dsl-extensions` and `sec:resolution-cascade` instead of restating them, and the
      two duplicated listings are gone (the registry example is Ch4's `lst:registry-structure`).
      Five changelog phrases removed. Deliberately kept: three "no longer exists" (they describe
      state, not history), one "used to validate" (it means "employed to"), and the "now" inside
      "Evolution of the Registry Design", the one subsection where history belongs.
- [x] Ch5: `subsec:validation-error-semantics` no longer restates Ch4. The "Unresolvable function"
      and "Ambiguous references" paragraphs are replaced by a reference to `sec:walkthrough`,
      keeping only the one consequence that belongs to the implementation (ambiguity is never
      rescued by a descriptor). "Stale registry entries" and "Insufficient permissions" stay.
- [x] Split `chapter4.tex` into `chapter5.tex` (Implementation), `chapter6.tex` (Case Study),
      `chapter7.tex` (Evaluation) and `chapter8.tex` (Conclusions); `Config/_files.tex` lists them
      in that order, so file numbers now match chapter numbers from 5 on. Watch out: the original
      `chapter4.tex` had **no trailing newline**, so its last line ("take the prototype to
      production.") was line 1814 and had to be restored by hand after the `sed` split.
- [x] Case Study moved before the Evaluation; its intro now points forward to
      `cha:evaluation`, and the two "gaps already identified" phrases (pointing at the
      Conclusions, which come later) are forward references.
- [x] `Chapters/chapter4.tex` deleted, after confirming content parity with the four new files
      (1332 non-blank lines on each side) and that the Ch7 evaluation edits survived the split.

## 6. Code ideas (ISEL-Thesis-2526)

- [ ] A Cloud Functions v1 client on GCP for validation, discovery and bootstrap (closes the
      documented limitation).
- [ ] Fail fast when a descriptor's `function.name` differs from the `functionRef` (for example in
      `QuickFaasDescriptorLoader.validate`); today the deployment runs and then times out waiting
      for a function that was deployed under another name.
- [ ] An update path: store a source hash in each registry entry, and redeploy when the
      descriptor's function file changes.
- [ ] Registry hardening: cross-process locking, integrity checks, a pluggable backend, an
      attributable write history.
- [ ] Invoke QuickFaaS in-process instead of as a subprocess.
- [ ] Least-privilege IAM: pre-provisioned roles and a dry-run mode.
- [ ] Test seams for the classes that call cloud APIs.
- [ ] Add a migration note: an old `function-registry.json` is no longer read.

## 6b. Benchmarks — o que ficou por fazer

- [x] `benchmark/results/RESULTS.md` reescrito (2026-09-12) a partir de `jmh-results-f3.csv` e dos
      CSVs re-medidos do P10/P11: todas as tabelas geradas por script (sem transcrição manual), todos
      os "Resumo" refeitos, e a Síntese final. As secções de notação e de caracterização de cada
      teste mantiveram-se, por não dependerem dos números. Correções de fundo, além dos valores: o
      P3 deixou de dizer que cada chamada relê o registo (usa o resolver de leitura única, e a
      variante externa paga a mesma leitura); o P13 deixou de prever Θ(K²); o P6 passou a R≈60; e o
      P10/P11 ganharam a nota sobre a consola e a explicação do `URI` por chamada no GCP.
- [ ] `jmh-results.csv` (P1/P2/P4/P5 + P3 antigo) e `jmh-results-p12.csv` ficaram do run de agosto:
      são benchmarks de renderização, fora do âmbito da tese. As figuras P1/P2/P5 continuam com
      rótulos em português por não terem sido regeradas (não são incluídas na tese).

## 7. Housekeeping

- [x] Put the thesis under git, then add it to the ISEL-Thesis-2526 repository as `dissertation/`
      (commit `5cb6328`), so it's on GitHub with the code.
- [ ] Once you're happy working here, delete the old `iselthesis-master` folder.
