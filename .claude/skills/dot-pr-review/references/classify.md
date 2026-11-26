# Stage 3: Classify Files + Detect Triggers

Classification happens inline (no agent). Four passes, then two checks for PR shapes that need a different review. In local mode every pass runs on the Local Stage 2 file list and diff (`local-mode.md`) wherever this file says `gh pr view --json files` or `gh pr diff`.

## Pass 0 — Drop Deleted Files

From the Stage 2 `gh pr view --json files` output, discard every file whose `changeType` is `DELETED` (the value `gh pr view --json files` gives; the large-PR and local lists are mapped to it) before any bucketing. A deleted file has no content to review, so assigning one to a reviewer burns an agent turn and produces either nothing or a hallucinated finding against a path that no longer exists.

**A deleted file leaves both sides of the gate arithmetic** — it is neither reviewable nor part of the total. Counting deletions only in the denominator is how a cleanup PR that removes 20 files and changes 3 gets skipped for "not enough reviewable files", which is exactly backwards: the 3 changed files are the risky part.

Keep the deleted paths in a short list — they are still evidence for the historical reviewer (a removed `.spec.ts` alongside a modified source file means coverage was dropped, which IS a finding).

`RENAMED` files are reviewed normally: the new path carries the content.

## Pass 1 — Path and Extension Classification

**Path rules first**, in this order. The first match wins, and a file a path rule claims never reaches the extension rules:

| Pattern | Bucket | Reviewer |
|---|---|---|
| Lockfiles, `**/*.snap`, `**/dist/**`, generated sources | **noise** | None — and excluded from the gate denominator |
| `CLAUDE.md` or `AGENTS.md` in any directory, `.github/copilot-instructions.md`, `.github/instructions/**`, `.github/prompts/**`, `docs/**/*.md` | **docs** | `dotcms-pr-docs-reviewer` |
| `.github/workflows/**` and `.github/actions/**` (workflows, composite actions, and their scripts, tests and READMEs), `.github/scripts/**`, `.github/*.yml`, `.github/*.yaml` | **ci** | `dotcms-pr-ci-reviewer` |
| `**/pom.xml`, `.mvn/**`, `mvnw`, `mvnw.cmd` | **maven** | `dotcms-pr-maven-reviewer` |

**Then by extension**:

| Pattern | Bucket | Reviewer |
|---|---|---|
| `*.component.ts`, `*.component.html`, `*.component.scss`, `*.directive.ts`, `*.pipe.ts`, `*.service.ts` | **code** | `dotcms-pr-code-reviewer` |
| `*.ts` (excluding `*.spec.ts` and Angular-specific above) | **code** | `dotcms-pr-code-reviewer` |
| `*.scss`, `*.css` (standalone, not `.component.scss`) | **code** | `dotcms-pr-code-reviewer` |
| `*.html` (standalone, not `.component.html`) | **code** | `dotcms-pr-code-reviewer` |
| `*.spec.ts` | **test** | `dotcms-pr-test-reviewer` |
| `*.java` under `dotcms-integration/**`, or named `*Test.java` / `*IT.java` | **java_test** | `dotcms-pr-java-test-reviewer` |
| Any file under `test-karate/**` or `dotcms-postman/**` (Karate features, Postman collections) | **java_test** | `dotcms-pr-java-test-reviewer` |
| `*.java` (all other production Java) | **java** | `dotcms-pr-java-code-reviewer` + specialists per Pass 3 |
| `*.jsp` | **jsp** | `dotcms-pr-java-security-reviewer` + `dotcms-pr-java-code-reviewer` |
| `*.sql`, `db/**` | **sql** | `dotcms-pr-java-persistence-reviewer` only |
| `*.properties` under `dotCMS/**` | **config** | `dotcms-pr-java-security-reviewer` only |
| Everything else (`*.json`, `*.xml` other than POMs, other `*.md`, `*.yml` outside `.github/`, `*.vtl`, Dockerfiles, issue and PR templates, etc.) | **out-of-scope** | None |

**Last, READMEs next to code.** A `README.md` left in `out-of-scope` moves to **docs** when the PR also changes a file of the `code`, `test`, `java`, `java_test`, `jsp` or `sql` bucket in the README's directory or below it: a README beside changed code is where it drifts. Other Markdown (changelogs, a README with no changed code under it) stays out of scope.

Workflows, POMs and docs break the build as surely as code does: a module `<argLine>` that replaces the parent's breaks every integration shard, and a release workflow first runs after merge. The path rules run first so a workflow's script or test (`*.ts`, `*.cjs`, `*.sh`, `*.py` under `.github/`) goes to the reviewer that knows the workflow.

`sql` and `config` are deliberately **not** part of the `java` bucket. Handing a `.properties` file to the general Java reviewer produces nothing — it has no Java to review — while inflating that agent's file count and pushing it toward an extra instance. Each goes only to the specialist that has something to say about it.

A JSP is production code: it renders what the user sees and is an entry point. It used to fall into `out-of-scope`, so a JSP-only PR was skipped (`reviewable == 0`) and a JSP plus a test reviewed only the test. The `jsp` bucket goes to the security reviewer (output encoding by context, and completeness: grep the sibling JSP and JS files for the same values; `UtilMethods.webifyString` replaces only the double quote, so it is not HTML encoding) and to the java-code reviewer (display regressions). The security reviewer keeps at least 24 turns when JSPs are in its input.

A `*.java` file lands in exactly one of `java` / `java_test`. The **specialist** Java reviewers (api, persistence, security) do not own separate buckets — they receive a *subset* of the `java` bucket selected by their Pass 3 triggers, and a single file can go to several of them. That is intentional: a REST resource that runs a query and checks a permission deserves all three lenses, and Stage 5 dedupes the overlap.

## Pass 2 — Stack Gate

Decide which stacks this PR touches, and route accordingly.

```
frontend_files = code_bucket.length + test_bucket.length
backend_files  = java_bucket.length + java_test_bucket.length + jsp_bucket.length + sql_bucket.length + config_bucket.length
infra_files    = ci_bucket.length + maven_bucket.length + docs_bucket.length
reviewable     = frontend_files + backend_files + infra_files

STACK = "frontend" if frontend_files > 0 and backend_files == 0
      = "backend"  if backend_files  > 0 and frontend_files == 0
      = "mixed"    if both > 0
      = "none"     if both == 0 (only ci, maven or docs files)

If reviewable == 0 → SKIP (report to user and stop)
If reviewable >= 1 → PROCEED with STACK
```

`STACK` names the code families only. On `none` no frontend or Java agent launches; the `ci`, `maven` and `docs` reviewers launch from their own flags (Pass 3) on any `STACK`.

**The gate is a floor, not a ratio.** It used to be `reviewable > 50% of total_files`, and that ratio was wrong in both directions:

- A backend PR is routinely a handful of `.java` files alongside several `pom.xml`, a changelog, and a couple of YAML workflows. Three Java files out of eight is 37% — skipped, despite three files of real production Java.
- A PR that adds 40 generated snapshots and changes one service was *passed* at 97% noise, and the one file that mattered got the same treatment as the forty that did not.

What the old ratio was actually protecting against — burning agents on a PR that is mostly not code — is now handled better by three things that did not exist when it was written: the `noise` bucket leaves generated output out entirely, both stacks are covered so "not frontend" no longer means "not reviewable", and the per-agent turn budgets cap the cost of a thin review. A PR with one reviewable file costs one reviewer instance. That is cheap and correct.

**Report the shape instead of hiding it.** When `reviewable` is under 25% of the non-noise changed files, still proceed, but say so in one line before Stage 4:

```
Reviewing <reviewable> of <non_noise_total> changed files — the rest are out of scope and carry no reviewer.
```

The user sees what is covered and decides whether that is enough. That is strictly better than a silent skip, which is what the ratio produced: the review the user asked for simply did not happen, for a reason they never saw.

**`force`**: if the skill was invoked with the `force` token, a `reviewable == 0` SKIP becomes a warning and the pipeline proceeds with whatever `out-of-scope` files exist, routed to the historical reviewer and the ac-validator only. Use it for a PR that is entirely out-of-scope files (JSON, non-POM XML, templates) but still worth a second pair of eyes.

On `mixed`, run **both** agent families in the same parallel batch. Do not make the user choose, and do not review one stack and mention the other — a PR that changes an endpoint and its consumer is exactly where the interesting defects are, and both halves must be read to find them.

**Cost note, not a gate**: when `reviewable` exceeds 120 files the instance caps (4 for code, 4 for java-code) mean each reviewer carries 30+ files and will use its whole budget. Say so in the same line above — the review is real but shallower per file. Do not stop to ask; the caps exist precisely so a large PR still completes.

Record `STACK`; Stage 4 keys the frontend and Java launches off it, and the ci, maven and docs launches off their flags.

## Pass 3 — Trigger Detection (grep the diff content)

Scan the raw diff content (stdout of `gh pr diff <N>`) for patterns that activate specialized agents. Use literal string matching on **added or modified lines only** (lines starting with `+` but not `+++`):

**Silent-failure-hunter trigger** — activate if any of these patterns appear in `*.ts` additions:
- `try {` followed by `catch`
- `.catch(`
- `.subscribe(` (with zero or one argument)
- `catchError(`
- `catch (` with `{}` or `{ return null` or `{ return;`
- `?? null` or `?? undefined` or `|| null` on a call result

**Comment-analyzer trigger** — activate if any of these patterns appear as additions:
- Lines matching `^\+\s*//` (line comments)
- Lines matching `^\+\s*/\*` or `^\+\s*\*` (block comments / JSDoc continuation)
- Lines matching `^\+.*@param\s` or `^\+.*@returns?\s` (JSDoc tags)
- Lines matching `^\+.*<!--` (HTML comments)

**Logic-reviewer trigger** — activate if the PR adds or modifies **non-trivial logic in any TypeScript file** (not `*.spec.ts`). Concretely, activate if any of these hold:

- *Reactive markers* in `*.ts` additions: `signalStore(` / `signalStoreFeature(` / `withState(` / `withMethods(` / `withComputed(` / `patchState(` / `ComponentStore` / `tapResponse(` / `rxMethod(`, or a changed `*.store.ts`.
- *Functional-unit files* changed: `*.service.ts`, `*.guard.ts`, `*.resolver.ts`, `*.interceptor.ts`, `*.pipe.ts`, or files named/located as utilities (`*.util.ts`, `*.utils.ts`, `*.helper.ts`, `*.mapper.ts`, `**/util*/**`, `**/helpers/**`).
- *Non-trivial logic* added/modified in any other `*.ts` or `*.component.ts`: a function/method with branching (`if`/`switch`/ternary), a loop, a `map`/`filter`/`reduce`, an `effect()`/`computed()`, or a state machine — i.e. more than a one-line getter, constant, DTO/interface, or barrel re-export.

Do NOT activate for changes confined to interfaces/types, constants, barrel files, or pure markup/styling — those carry no logic to verify.

**Java specialist triggers** — evaluated when the `java`, `jsp`, `sql`, or `config` buckets are non-empty. Grep the added/modified lines of `*.java` files; a non-empty `sql` bucket activates persistence on its own, and a non-empty `config` or `jsp` bucket activates security on its own, with no grep needed:

| Flag | Activate when the `java` bucket contains | Reviewer |
|---|---|---|
| `RUN_JAVA_API` | `@Path(`, `@GET`/`@POST`/`@PUT`/`@DELETE`, `WebResource.InitBuilder`, `webResource.init(`, `@Produces`, `@Consumes`, `@Operation`/`@ApiResponse`/`@Schema(`, a changed `openapi.yaml`, a class under `**/rest/**`, or a Form/View/record used as a request or response body | `dotcms-pr-java-api-reviewer` |
| `RUN_JAVA_PERSISTENCE` | `DotConnect`, `setSQL(`, `addParam(`, `LocalTransaction`, `executeUpdate(`, `loadResults(`, a changed `*.sql`, a class under `**/business/**` or `**/factories/**`, or an upgrade task | `dotcms-pr-java-persistence-reviewer` |
| `RUN_JAVA_SECURITY` | `WebResource`, `InitDataObject`, `checkPermission`, `PermissionAPI`, `getAnonymousUser`, `respectFrontendRoles`, `UserAPI`, `Config.get*Property`, `DwrUtil`, `SecurityLogger`, `DotSecurityException`, a changed DWR class (`*Ajax.java` or a class under `**/ajax/**`), a changed Struts action (`*Action.java` extending `DotPortletAction`), a changed `*.properties`, **or any new endpoint at all** (`RUN_JAVA_API` true) | `dotcms-pr-java-security-reviewer` |

`RUN_JAVA_SECURITY` deliberately fires on every new endpoint even with no auth keyword in the diff — a missing permission check has no keyword to grep for. Absence of evidence is the finding. DWR classes and Struts actions are entry points too, so changing one fires it the same way.

**Stack-aware behaviour of the agnostic agents.** These already run on either stack; extend their input rather than duplicating them:

- **Silent-failure-hunter**: on `backend`/`mixed`, also trigger on Java additions containing `catch (`, `catch (Exception`, an empty `{}` catch body, `e.printStackTrace()`, a `catch` that only logs at DEBUG, or a swallowed `InterruptedException`. Pass it the `java` bucket alongside the `.ts` files.
- **Comment-analyzer**: on `backend`/`mixed`, also trigger on `+ *//`, `+ */\*`, and Javadoc tags (`@param`, `@return`, `@throws`, `@deprecated`) in `*.java` additions.
- **Logic-reviewer**: on `backend`/`mixed`, treat a Java class with branching, loops, stream pipelines, or state transitions as a logic-bearing unit — services, managers, factories, and helpers under `com.dotcms.**`. It traces input → transformation → output the same way it does for a TypeScript utility.
- **AC-validator** and **historical-reviewer** are fully stack-agnostic already; they receive the full changed-file list regardless of `STACK`.

**Historical-reviewer**: always activate when `PROCEED` from Pass 2. No content trigger needed.

**CI, Maven and docs reviewers**: no grep. `RUN_CI`, `RUN_MAVEN` and `RUN_DOCS` are true when the `ci`, `maven` or `docs` bucket has a file, whatever `STACK` is.

**One reviewer per line.** The silent-failure, comment-analyzer and logic triggers read only files outside the `ci`, `maven` and `docs` buckets, and those agents receive only such files. The ci reviewer owns the comments and inline scripts of workflows and actions (YAML `#` comments, `run:` bash, `github-script` JS), the maven reviewer the comments of POMs, and the docs reviewer the Markdown.

Record the trigger results as flags: `RUN_SILENT_FAILURE`, `RUN_COMMENT_ANALYZER`, `RUN_HISTORICAL`, `RUN_LOGIC_REVIEWER`, `RUN_JAVA_API`, `RUN_JAVA_PERSISTENCE`, `RUN_JAVA_SECURITY`, `RUN_CI`, `RUN_MAVEN`, `RUN_DOCS`.


## PR shapes that need more than the diff

Check these after Pass 3. Each sets a flag Stage 4 and the report read.

### CI changes and CI gates (`CI_GATE`)

A CI change is proven by the run that executes it, not by its diff: the diff shows the workflow as written, the run shows whether it works. Set `CI_GATE = true` when the `ci` bucket has a file, or a `pom.xml` adds or changes a `<profile>`. Then:

1. **Triggers.** For every changed workflow, list the events in its `on:`. For a changed reusable workflow (`workflow_call`) or composite action, list the workflows that call it (`grep -rln` under `<HEAD_DIR>/.github/workflows`) and their events.
2. **What this PR's checks ran** (PR mode only):

   ```bash
   gh pr checks <N> --repo dotCMS/core --json name,state,link,workflow,event
   ```

   For a gate the PR adds (a workflow job, a Maven profile, a check script), read that job's log too: `gh run view --repo dotCMS/core --job <JOB_ID> --log`, where the job id is the last number in the check's `link` (the jobs logs API returns an empty body for these runs).
3. **Record `CI_GATE_EVIDENCE`**, one line per changed workflow: `ran in this PR's checks: <check> <state>`, or `first runs on <event>` when no check of the PR ran it. Merge-queue, release, scheduled, `workflow_run`, `workflow_dispatch` and push-to-main workflows never run on a PR, so green checks prove nothing about them. For a gate, add whether it really ran (not a silent skip, a cached step or zero tests) and which path it exercised.

Stage 7 reports `CI_GATE_EVIDENCE`, and a changed workflow that first runs after merge raises the risk level by one. The ci reviewer receives it too. In local mode there is no run: record the triggers and "not exercised yet; check the PR's run".

### Vendored imports (`VENDORED`)

A PR that imports reviewed upstream source (its title or body says so, or it adds a copied third-party tree with its license) is not line-reviewed as if it were new code. Set `VENDORED = true` and:

1. Clone the upstream at the imported commit into `<RUN_DIR>/upstream` and `cmp` every imported file against it. Identical files are not line-reviewed.
2. Line-review only the adapted files (they differ from upstream), the upstream delta between the version core ships today and the imported one (`git -C <RUN_DIR>/upstream diff <shipped ref> <imported commit>`), and the hand-written glue.
3. The import's workflow and `pom.xml` changes go to the ci and maven reviewers through their buckets, with the vendored line of their templates (`agent-prompts.md`).
4. A finding on an unchanged vendored line is marked `PRE-EXISTING UPSTREAM`, never `ADDED-BY-THIS-PR`.
5. Grep the upstream branch name and its commit messages for a `private-issues` reference. If one is there, tell the user privately; it must not be quoted on the PR.
