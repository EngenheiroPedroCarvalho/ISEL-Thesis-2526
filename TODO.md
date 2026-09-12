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

- [ ] Write the English and Portuguese abstracts (both are still template text). Write them last:
      problem → approach → evidence → implication, at most 300 words.
- [ ] Write the acknowledgments (`acknowledgments.tex` is still template text).

## 2. Argument (highest impact)


## 3. Contradictions to fix


## 4. Evaluation (Ch6)

- [ ] Correctness (RQ1): Ch6 "Correctness of the Resolution Cascade" (`tab:eval-correctness`)
      reports the resolver unit tests; "absent, no descriptor" and "both `internalFunction` and
      host/path set" are now covered on both providers (45 resolver tests, 22 AWS + 23 GCP). Still
      open: evidence against the real providers (the two `@Ignore`d full-deployment tests in
      `WorkflowTest`).
- [ ] Count the provider API calls per cascade level (worked out from the code).
- [ ] Time a few end-to-end deployments (L1/L2/L3 × AWS/GCP; report median and range).
- [ ] Compare manual effort before and after (manual actions and hand-copied values).
- [ ] **The P-tables mix several measurement runs** (found 2026-09-12 by diffing each table
      against its CSV). `tab:eval-p7`, `tab:eval-p9` and `tab:eval-p13` do NOT match
      `jmh-results-p7/p9/p13.csv`; P3, P6, P8, P10, P11 and P17 do. The proof it matters: P17 =
      P13 + a failed lookup, so P17 must cost more, yet the current CSVs give P17 < P13 (3309 vs
      4682 µs at K=1/R0=0). Likewise a registry read costs ~5–7 µs in the P3/P7 tables and ~180 µs
      in the P6/P8/P9 runs. So: re-run the WHOLE suite in ONE session with `-f 3`, regenerate every
      table from it, then delete the provenance paragraph now in "Threats to Validity".
- [ ] Re-run JMH with `-f 3`, report error margins, and remove the "not thesis-grade" caveat.
      (Same run as the item above.) Recipe, worked out and started on 2026-09-12, then stopped
      because the machine was needed — **the run takes ~2h11m, so start it when the machine can be
      left alone**:

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
      4. **Filter stdout**: `| grep --line-buffered -E '^#|^Iteration|^Result|^Benchmark'`. The
         resolvers `println` on every resolved call, so an unfiltered log grows ~1 GB/min (it hit
         2.1 GB in one minute before being killed).
      5. JMH writes the CSV only when the whole run ends, so an interrupted run leaves nothing.
         Write it to a scratch path and only copy into `benchmark/results/` once it is complete,
         so the current CSVs survive a failed attempt.
- [ ] Those `println` calls sit **inside the measured method**, so P3 and P8–P19 measure resolution
      plus console I/O. Check whether it is material once the `-f 3` numbers exist; if it is, say so
      in "Threats to Validity". Silencing them would mean changing production code — ask first.
- [x] P3: explained as the single-read resolver with R=1 (per-call cost = snapshot lookup + URL
      split + node rebuild), no longer as a re-read per call.
- [x] P6: R≈320 is derived in the P6 paragraph (0.56·R = 180), and the Discussion only cites it.
- [x] P10/P11: local fake inspector (AWS) / 1st-gen short-circuit (GCP) stated; growth in N
      explained as per-call validation; "comparable to P8" dropped.
- [x] P13/P17: cost per write is roughly fixed (~2.7→3.3 ms from K=1 to K=100), so the total is
      near-linear in K, not compounding; the miss adds 22–27% (46–99% in the noisy R0=1000
      column); "monotonic" is gone.
- [ ] P6 figure: `fig:eval-p6`'s axis labels are in Portuguese; regenerate in English with
      `plot_benchmarks.py` (the whole script's titles/labels are Portuguese).
- [ ] Explain the missing P1/P2/P4/P5/P12 (rendering benchmarks, see the code's `TESTING.md`), or
      renumber the experiments.
- [ ] Label experiments consistently. `ISEL-Thesis-2526/thesis/` had IDs in the P3 and P6 headings
      and captions (and "P13, P17" in the Methodology); the workspace dropped those but still uses
      P7–P19 elsewhere. Use IDs everywhere or nowhere.
- [ ] Soften "overhead practically irrelevant": the local benchmarks exclude live validation and
      region scans.

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
- [ ] Ch5: `subsec:validation-error-semantics` still restates Ch4's Level 3 in its first two
      paragraphs ("Unresolvable function", "Ambiguous references"); trim those to references. The
      rest of the subsection is genuinely implementation-level and should stay.
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

## 7. Housekeeping

- [x] Put the thesis under git, then add it to the ISEL-Thesis-2526 repository as `dissertation/`
      (commit `5cb6328`), so it's on GitHub with the code.
- [ ] Once you're happy working here, delete the old `iselthesis-master` folder.
