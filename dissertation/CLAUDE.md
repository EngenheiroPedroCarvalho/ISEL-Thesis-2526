# CLAUDE.md

Guidance for AI agents working on this MSc dissertation. Open work (thesis and code) is tracked in
`../TODO.md`. Code-level guidance, including the resolution behaviour the thesis must match, is in
`../CLAUDE.md`, which Claude Code also loads when you work here.

## What this is

ISEL MSc dissertation (English, `docdegree=msc`, `lang=en`) on integrating **OmniFlow** (Kotlin
DSL that renders and deploys workflows to AWS Step Functions and GCP Workflows) with **QuickFaaS**
(portable deployment of serverless functions). The contribution has two parts:

1. **Unification:** a Function Registry and a three-level *resolution cascade* (registry →
   provider discovery → QuickFaaS deployment), triggered by
   `internalFunction(name, deploymentDescriptorPath)` on the OmniFlow `call` step.
2. **AWS support:** an AWS Lambda provider added to QuickFaaS.

The document uses the `iselthesis` LaTeX class (template v4.x).

This folder is `dissertation/` inside the ISEL-Thesis-2526 git repository (GitHub remote `origin`,
branch `claude/progress-report-compliance-e4damf`). Commit only when the user asks, and treat a
commit as published: commits in this repo have been pushed to GitHub automatically (probably by
IntelliJ). Prefer small `Edit`s over rewriting whole files, and never delete files without asking.
Build output (`template.pdf`, `pdfa.xmpi`), the template `.zip` and `outputs/` are git-ignored.

## Where things are

| File | PDF chapter(s) | Label(s) |
|---|---|---|
| `Chapters/chapter1.tex` | 1 Introduction | `cha:introduction` |
| `Chapters/chapter2.tex` | 2 Background, 3 Related Works | `cha:background`, `ch:related-works` |
| `Chapters/chapter3.tex` | 4 Proposed Solution | `ch:proposed_solution` |
| `Chapters/chapter4.tex` | 5 Implementation, 6 Evaluation, 7 Case Study, 8 Conclusions | `cha:impl`, `cha:evaluation`, `cha:case-study`, `cha:conclusions` |
| `Chapters/appendix-cascade.tex` | Appendix A: Resolution Cascade Sequence Diagrams | `app:cascade-sequences` |

- File numbers don't match chapter numbers. `chapter4.tex` is about 1,600 lines, with long runs of
  blank lines between chapters, so `grep -n '\\chapter{'` first and read with `offset`/`limit`.
- `Config/_files.tex` decides which files are included (`\addfile`, `\appendixfile`, `\annexfile`,
  …). Files with a trailing underscore (`annex1_.tex`, `ganttdiagram_.tex`) don't
  match the names there and are silently skipped. `appendix1.tex` (the template's R example) stays
  on disk but is no longer built.
- Several files still contain **template placeholder text**, not thesis content: `abstract-en.tex`,
  `abstract-pt.tex`, `acknowledgments.tex` and `glossary.tex` (which prints nothing, because the
  text never uses `\gls`). `acronyms.tex` holds the real acronyms and prints them all with
  `\glsaddall`; add an entry there when the text introduces a new acronym. `appendix2.tex`,
  `annex1_.tex` and `annex2.tex`
  are lorem ipsum and are commented out in `Config/_files.tex`. Check a file's content before
  relying on it.
- Figures: `images/<topic>/*.png`. Their PlantUML sources are in `../diagrams/`.
- Bibliography: `Bibliography/bibliography.bib` (biblatex with the BibTeX backend).
- Template internals (`iselthesis.cls`, `ISELthesis-files/`, `Logo/`, `Config/_*.tex` apart from
  `_files.tex`): don't edit.

## Build and check

- `make pdf` runs latexmk/pdflatex (with `-shell-escape`, batch mode) and produces `template.pdf`
  (about 80 pages). The tools are in `/Library/TeX/texbin`.
- After adding or renaming an included file, force a rebuild with `make pdf FLAGS=-g`: latexmk
  only tracks files it has already read, so a plain `make pdf` reports success without picking up
  the new file.
- Avoid `make clean`: it deletes every `template.*` file except the `.tex` and `.pdf`, including
  the tracked, hand-filled PDF/A metadata `template.xmpdata`. If it runs, restore that file with
  `git checkout -- template.xmpdata`.
- After adding a bibliography entry, check its number: biblatex runs with `defernumbers=true`, so
  numbers from earlier runs are kept in `template.aux` and a new entry is printed as `[0]`, with no
  warning in the log. Check with `grep -c abx@aux@number template.aux` (it should equal the number
  of cited entries). To fix, delete `template.aux`, `template.bbl` and `template.fdb_latexmk` (all
  untracked) and run `pdflatex -shell-escape`, `bibtex`, then `pdflatex` twice; deleting only the
  `.aux` leaves latexmk's state inconsistent and `make pdf` fails with a BibTeX error.
- After editing, check the log:
  `grep -nE "undefined|multiply defined|^!" template.log`. A clean build reports none (a
  `T1/lmss/c/n` font-shape warning is harmless).

## Related code (the source of truth for technical claims)

The code is in the same repository, one level up (`..` is
`/Users/pedrocarvalho/IdeaProjects/ISEL-Thesis-2526`). `../CLAUDE.md` describes its packages, how
the resolution cascade behaves (the facts the thesis text must stay consistent with), and how to
run its tests, including the `JAVA_HOME` workaround for this Mac's terminal.

- `../omni-flow-main/` is OmniFlow (Maven modules `deployment/` and `benchmark/`; tests are
  documented in `../omni-flow-main/TESTING.md`).
- `../omni-flow-main/quickfaas-essentials/QuickFaaS-Deployment/` is the QuickFaaS deployer (Gradle,
  Kotlin 1.6.20).
- `../thesis/` is an **older split copy** of these chapters (Aug 2026), kept at the user's request.
  The canonical text is this folder's `Chapters/`. Don't edit the old copy. Everything in it was
  merged here on 2026-09-11 (its appendix is now `Chapters/appendix-cascade.tex`), so there's
  nothing left to take from it.
- `/Users/pedrocarvalho/IdeaProjects/iselthesis-master` is this folder's previous location, kept
  until the user deletes it. Don't edit it.

Key classes, under `../omni-flow-main/deployment/src/main/kotlin/costaber/com/github/omniflow/`:

| Concern | Path |
|---|---|
| GCP cascade | `internalfunction/WorkflowInternalFunctionResolver.kt` |
| AWS cascade | `internalfunction/quickfaas/AwsInternalFunctionResolver.kt` |
| Level 3 deployers | `internalfunction/quickfaas/` (`QuickFaasDeployer`, `AwsLambdaDeployer`) |
| Registry | `registry/FunctionRegistryStore.kt`, `registry/FunctionRegistryBootstrapper.kt` |
| Entry points, default registry paths | `cloud/provider/{google,amazon}/deployer/*CloudDeployer.kt` |
| Benchmarks (P-numbers) | `../omni-flow-main/benchmark/.../metrics/Benchmark*.kt`; the mapping is in `../omni-flow-main/TESTING.md` |

## Writing conventions

- English, mostly British spelling (*realise*, *organise*, *behaviour*). Match the surrounding text.
- Cross-references use `Chapter~\ref{}`, `Section~\ref{}` and `\S\ref{}`. Label prefixes are
  `sec:`, `subsec:`, `fig:`, `tab:`, `lst:`, `app:`. Chapter labels mix `cha:` and `ch:`: reuse the
  existing labels and never rename them.
- Put class and method names in `\texttt{}`, and break long ones with `\allowbreak`
  (`Workflow\allowbreak Internal\allowbreak Function\allowbreak Resolver`).
- Listings use `lstlisting` with `language=Kotlin` or `language=json`. Tables are plain `tabular`
  with `\hline`, with thousands written as `1\,234`.
- Newer prose is hard-wrapped at about 100 columns.
- Terminology: an *internal call* uses `internalFunction`; an *external call* has a literal
  `host`/`path`. Also `functionRef`, *Function Registry*, and *resolution cascade* with Level 1/2/3.
- Technical claims must match the code. When discussing them with the user, cite `file:line`.
- When evaluation numbers change, update the table and the prose together; the Discussion and
  Conclusions restate numbers.
- Describe the final state of the design, not its history ("now", "used to"). History belongs only
  in the "Evolution of the Registry Design" subsection.
