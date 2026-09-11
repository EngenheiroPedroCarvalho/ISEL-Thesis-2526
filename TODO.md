# TODO: thesis improvements

Ideas from the logical-flow review of 2026-09-11. Chapter numbers are those of the PDF; see
`CLAUDE.md` for the file-to-chapter map. Line numbers drift as text is edited, so search for the
quoted phrases.

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

## 1. Blockers before submission

- [ ] Write the English and Portuguese abstracts (both are still template text). Write them last:
      problem → approach → evidence → implication, at most 300 words.
- [ ] Fix the P6 figure in Ch6: its caption was copied from the runtime-invocation figure, and it
      reuses the label `fig:aws-runtime-invocation`.
- [ ] Replace or remove the remaining template placeholders: acronyms, glossary, `appendix2`,
      `annex2`, and check statement/acknowledgments/dedicatory.
- [ ] Decide on the four commented-out Background figures in Ch2 (QuickFaaS deployment pipeline,
      OmniFlow components, abstract workflow model, OmniFlow sequence diagram). Their images exist
      nowhere under `IdeaProjects`: recover the originals from the QuickFaaS/OmniFlow reports, or
      delete the commented-out blocks.

## 2. Argument (highest impact)

- [ ] Reframe the problem premise (Ch1 §1.4–1.5) around three points: the endpoint is unknown
      before the first deployment, it differs per account/project/provider, and the manual steps
      are real (use Ch2's QuickFaaS procedure as evidence). Drop "redeploying changes the endpoint"
      as the main premise.
- [ ] Add research questions (RQ1: feasibility and correctness; RQ2: overhead) and answer them
      explicitly in Ch6 and Ch8.
- [ ] Make AWS support an explicit objective in Ch1 and give the reason for it (OmniFlow and
      QuickFaaS only overlapped on GCP).
- [ ] State that binding is static (Ch4): the rendered workflow embeds the endpoint, so an endpoint
      change after deployment needs a re-deploy. Qualify the walkthrough's claim that the registry
      "would have absorbed" the change.
- [ ] Narrow the "deploy or update" promises in Ch1 to "deploy if missing"; move updating to
      future work.
- [ ] Related Works: acknowledge deployment-time reference resolution in Terraform, AWS SAM
      (`DefinitionSubstitutions`) and the Serverless Framework step-functions plugin. Replace the
      "Workflow-first resolution" column with honest dimensions, and footnote that the portability
      marks come from OmniFlow and QuickFaaS.
- [ ] Remove `secretRef` from Ch1, or move it to future work (it doesn't exist in the code).
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
- [ ] "Safe by default" means secrets in Ch1 and non-mutating discovery in Ch4. Define it once, and
      note that AWS repairs the trust policy of a role you supply.
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

- [ ] Add a correctness section: a scenario matrix (first deploy, redeploy, registry lost, drift,
      deleted, ambiguous name, no descriptor, both forms set, forbidden region) × AWS/GCP, with
      expected and observed results.
- [ ] Count the provider API calls per cascade level (worked out from the code).
- [ ] Time a few end-to-end deployments (L1/L2/L3 × AWS/GCP; report median and range).
- [ ] Compare manual effort before and after (manual actions and hand-copied values).
- [ ] Re-run JMH with `-f 3`, report error margins, and remove the "not thesis-grade" caveat.
- [ ] P3: it runs the single-read resolver (`BenchmarkInternalCallResolution.kt:89`), not a
      re-read on every call. Fix the explanation.
- [ ] P6: add the per-N figure, or drop the "four N curves collapse" sentence. Derive R≈320 here,
      not in the Discussion.
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
- [ ] Cite or soften "most serverless platforms enforce a tight coupling" (Ch1 Motivation).
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
- [ ] `TESTING.md`: the P10 row still says the single-read fix was never applied.
- [ ] `GoogleCloudDeployer` bootstrap log prints the literal text `'registryPath'` (missing `$`)
      and says "Boothstrapping".
- [ ] Add a migration note: an old `function-registry.json` is no longer read.

## 7. Housekeeping

- [ ] Put this thesis folder under git (it has no version control).
