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

## 1. Blockers before submission

- [ ] Write the English and Portuguese abstracts (both are still template text). Write them last:
      problem → approach → evidence → implication, at most 300 words.
- [ ] Write the acknowledgments (`acknowledgments.tex` is still template text).

## 2. Argument (highest impact)

- [ ] Ch1 now argues from "the endpoint exists only after deployment and depends on the target",
      but later chapters still argue from redeployment: Ch5's opening ("This makes workflows
      brittle: if a function is redeployed…") and registry-design section ("any redeployment that
      changes the endpoint forces an edit"), and the Case Study's "redeployed far more often"
      requirement. Align them when working on those chapters.
- [ ] State that binding is static (Ch4): the rendered workflow embeds the endpoint, so an endpoint
      change after deployment needs a re-deploy. Qualify the walkthrough's claim that the registry
      "would have absorbed" the change.
- [ ] Add updating an existing function to Ch8 Future Work (Ch1 now promises only "deploy if
      missing"); the code idea is the source-hash update path in §6.
- [ ] Related Works: acknowledge deployment-time reference resolution in Terraform, AWS SAM
      (`DefinitionSubstitutions`) and the Serverless Framework step-functions plugin. Replace the
      "Workflow-first resolution" column with honest dimensions, and footnote that the portability
      marks come from OmniFlow and QuickFaaS.
- [ ] Case Study: replace the "redeploy a week later" example (the ARN doesn't change, so it shows
      nothing). Reframe "multi-cloud posture" as exit/portability, and cite DORA
      (Regulation (EU) 2022/2554).
- [ ] Conclusions: mark each objective as met or partially met, with evidence. Drop "validated
      concept" and "the design is sound". Add static binding and the missing update path to the
      Critical Assessment.

## 3. Contradictions to fix

- [ ] Ch4 Level 1 says both "without contacting the provider" and "validated against the live
      resource". Explain what the registry buys given validation: it remembers the region and gives
      validation a target.
- [ ] Ch5 "Deployment-Time Endpoint Resolution", step 2, says a stale entry "falls through to the
      next step", but it aborts. Also explain why a deleted function isn't redeployed even with a
      descriptor (the deletion may have been deliberate).
- [ ] "Safe by default" now appears only in Ch4, meaning non-mutating discovery (Ch1's use, about
      secrets, is gone). Note there that AWS repairs the trust policy of a role you supply.
- [ ] Ch4 region-scope paragraph: "no region required, all regions searched" contradicts "the
      prototype assumes the same region".
- [ ] Ch4 edge-case list: "Stale binding (GCP)" now applies to AWS as well.
- [ ] Say what happens when `functionRef` differs from the descriptor's `function.name`.
- [ ] Text still describing one shared registry: the Ch4 architecture section ("shared
      `function-registry.json`"), and the registry listings that mix GCP and AWS entries in one file
      (Ch4 `lst:registry-structure`, Ch5 `lst:function-registry`). Also update
      `diagrams/architecture-after.puml` in the code repo and regenerate
      `images/architecture/architecture-after.png`.
- [ ] The registry listings show `a.run.app` URLs for functions deployed by QuickFaaS. On GCP these
      are `cloudfunctions.net` URLs.

## 4. Evaluation (Ch6)

- [ ] Correctness (RQ1): Ch6 "Correctness of the Resolution Cascade" (`tab:eval-correctness`)
      reports the resolver unit tests. Still open: tests for "absent, no descriptor" and "both
      `internalFunction` and host/path set" (both resolvers throw, untested), and evidence
      against the real providers (the two `@Ignore`d full-deployment tests in `WorkflowTest`).
- [ ] Count the provider API calls per cascade level (worked out from the code).
- [ ] Time a few end-to-end deployments (L1/L2/L3 × AWS/GCP; report median and range).
- [ ] Compare manual effort before and after (manual actions and hand-copied values).
- [ ] Re-run JMH with `-f 3`, report error margins, and remove the "not thesis-grade" caveat.
- [ ] P3: it runs the single-read resolver (`BenchmarkInternalCallResolution.kt:89`), not a
      re-read on every call. Fix the explanation.
- [ ] P6: derive R≈320 here, not in the Discussion. (The per-N figure is `fig:eval-p6`; its axis
      labels are in Portuguese, so regenerate it in English with `plot_benchmarks.py`.)
- [ ] P10/P11: say they use a fake inspector. Explain the growth in N as per-call validation, and
      drop "comparable to P8".
- [ ] P13/P17: recompute the percentages (the tables give 22–99%), drop "monotonic", and explain
      the roughly fixed cost per write.
- [ ] Explain the missing P1/P2/P4/P5/P12 (rendering benchmarks, see the code's `TESTING.md`), or
      renumber the experiments.
- [ ] Label experiments consistently. `ISEL-Thesis-2526/thesis/` had IDs in the P3 and P6 headings
      and captions (and "P13, P17" in the Methodology); the workspace dropped those but still uses
      P7–P19 elsewhere. Use IDs everywhere or nowhere.
- [ ] Soften "overhead practically irrelevant": the local benchmarks exclude live validation and
      region scans.

## 5. Structure and style

- [ ] Ch1 order: context → tools → manual process → consequences → research questions and
      objectives → contributions → structure. Trim §1.1–1.2, or connect them to the problem.
- [ ] Ch2: fix the intro; order the sections FaaS → workflows (merge the two workflow sections) →
      QuickFaaS → OmniFlow.
- [ ] Ch5: replace its restatements of Ch4 (conventions, cascade, error semantics) with references,
      and remove changelog phrasing ("now", "used to", "has since been").
- [ ] Move the Case Study before the Evaluation, and turn its "already identified" references into
      forward references.
- [ ] Split `chapter4.tex` into `chapter5.tex`–`chapter8.tex` and update `Config/_files.tex`.

## 6. Code ideas (ISEL-Thesis-2526)

- [ ] A Cloud Functions v1 client on GCP for validation, discovery and bootstrap (closes the
      documented limitation).
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
