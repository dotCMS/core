# Stage 3: Classify Files + Detect Triggers

Classification happens inline (no agent): four passes, then two checks for PR shapes that need more than the diff. In local mode every pass runs on the Local Stage 2 file list and diff (`local-mode.md`) wherever this file says `gh pr view --json files` or `gh pr diff`.

## Pass 0 — Drop Deleted Files

Discard every file whose `changeType` is `DELETED` (the large-PR and local lists are mapped to that value) before any bucketing: a deleted file has nothing to review, and a reviewer given one burns a turn or invents a finding on a path that no longer exists. A deleted file leaves **both** sides of the gate arithmetic, so a cleanup PR that removes 20 files and changes 3 is still reviewed on its 3.

Keep the deleted paths in a short list for the historical reviewer: a removed `.spec.ts` beside a modified source file means coverage was dropped, which is a finding.

`RENAMED` files are reviewed under their new path.

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

**Last, READMEs next to code.** A `README.md` left in `out-of-scope` moves to **docs** when the PR also changes a file of the `code`, `test`, `java`, `java_test`, `jsp` or `sql` bucket in the README's directory or below it. Other Markdown (changelogs, a README with no changed code under it) stays out of scope.

The path rules run first so a workflow's script or test (`*.ts`, `*.cjs`, `*.sh`, `*.py` under `.github/`) goes to the reviewer that knows the workflow.

`sql` and `config` go only to their specialist, never to the java-code reviewer, which has nothing to say about them and would only grow its instance count.

A JSP is production code and an entry point. The `jsp` bucket goes to the security reviewer (output encoding by context, and completeness: grep the sibling JSP and JS files for the same values; `UtilMethods.webifyString` replaces only the double quote, so it is not HTML encoding) and to the java-code reviewer (display regressions). The security reviewer keeps at least 24 turns when JSPs are in its input.

A `*.java` file lands in exactly one of `java` / `java_test`. The **specialist** Java reviewers (api, persistence, security) own no bucket: each receives the subset of the `java` bucket its Pass 3 triggers select, and one file can go to several of them (a REST resource that runs a query and checks a permission deserves all three lenses); Stage 5 dedupes the overlap.

## Pass 2 — Stack Gate

```
frontend_files = code_bucket.length + test_bucket.length
backend_files  = java_bucket.length + java_test_bucket.length + jsp_bucket.length + sql_bucket.length + config_bucket.length
infra_files    = ci_bucket.length + maven_bucket.length + docs_bucket.length
reviewable     = frontend_files + backend_files + infra_files

STACK = "frontend" if frontend_files > 0 and backend_files == 0
      = "backend"  if backend_files  > 0 and frontend_files == 0
      = "mixed"    if both > 0
      = "none"     if both == 0 (only ci, maven or docs files)

If reviewable == 0 → SKIP: "PR #<N> has no files any reviewer owns. Skipping review." and stop
If reviewable >= 1 → PROCEED with STACK
```

`STACK` names the code families only. On `none` no frontend or Java agent launches; the `ci`, `maven` and `docs` reviewers launch from their own flags (Pass 3) on any `STACK`.

**The gate is a floor, not a ratio**: one reviewable file costs one reviewer instance, and the noise bucket already keeps generated output out. When `reviewable` is under 25% of the non-noise changed files, proceed and say so in one line before Stage 4:

```
Reviewing <reviewable> of <non_noise_total> changed files — the rest are out of scope and carry no reviewer.
```

When `reviewable` exceeds 120 files, add to that line that the instance caps (4 code, 4 java-code) leave each reviewer 30+ files, so the review is real but shallower per file. Proceed without asking.

**`force`**: a `reviewable == 0` SKIP becomes a warning, and the run proceeds with the `out-of-scope` files routed to the historical reviewer and the ac-validator only.

On `mixed`, both agent families run in the same parallel batch: a PR that changes an endpoint and its consumer has its defects where the two halves meet.

Record `STACK`; Stage 4 keys the frontend and Java launches off it, and the ci, maven and docs launches off their flags.

## Pass 3 — Trigger Detection (grep the diff content)

Scan the raw diff (stdout of `gh pr diff <N>`) with literal matching on **added or modified lines only** (lines starting with `+` but not `+++`):

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

**Logic-reviewer trigger** — activate when the PR adds or modifies **non-trivial logic in any TypeScript file** (not `*.spec.ts`), that is, when any of these hold:

- *Reactive markers* in `*.ts` additions: `signalStore(` / `signalStoreFeature(` / `withState(` / `withMethods(` / `withComputed(` / `patchState(` / `ComponentStore` / `tapResponse(` / `rxMethod(`, or a changed `*.store.ts`.
- *Functional-unit files* changed: `*.service.ts`, `*.guard.ts`, `*.resolver.ts`, `*.interceptor.ts`, `*.pipe.ts`, or files named/located as utilities (`*.util.ts`, `*.utils.ts`, `*.helper.ts`, `*.mapper.ts`, `**/util*/**`, `**/helpers/**`).
- *Non-trivial logic* added/modified in any other `*.ts` or `*.component.ts`: a function/method with branching (`if`/`switch`/ternary), a loop, a `map`/`filter`/`reduce`, an `effect()`/`computed()`, or a state machine — i.e. more than a one-line getter, constant, DTO/interface, or barrel re-export.

Changes confined to interfaces and types, constants, barrel files, or markup and styling carry no logic and do not activate it.

**Java specialist triggers** — evaluated when the `java`, `jsp`, `sql`, or `config` buckets are non-empty. Grep the added/modified lines of `*.java` files; a non-empty `sql` bucket activates persistence on its own, and a non-empty `config` or `jsp` bucket activates security on its own, with no grep needed:

| Flag | Activate when the `java` bucket contains | Reviewer |
|---|---|---|
| `RUN_JAVA_API` | `@Path(`, `@GET`/`@POST`/`@PUT`/`@DELETE`, `WebResource.InitBuilder`, `webResource.init(`, `@Produces`, `@Consumes`, `@Operation`/`@ApiResponse`/`@Schema(`, a changed `openapi.yaml`, a class under `**/rest/**`, or a Form/View/record used as a request or response body | `dotcms-pr-java-api-reviewer` |
| `RUN_JAVA_PERSISTENCE` | `DotConnect`, `setSQL(`, `addParam(`, `LocalTransaction`, `executeUpdate(`, `loadResults(`, a changed `*.sql`, a class under `**/business/**` or `**/factories/**`, or an upgrade task | `dotcms-pr-java-persistence-reviewer` |
| `RUN_JAVA_SECURITY` | `WebResource`, `InitDataObject`, `checkPermission`, `PermissionAPI`, `getAnonymousUser`, `respectFrontendRoles`, `UserAPI`, `Config.get*Property`, `DwrUtil`, `SecurityLogger`, `DotSecurityException`, a changed DWR class (`*Ajax.java` or a class under `**/ajax/**`), a changed Struts action (`*Action.java` extending `DotPortletAction`), a changed `*.properties`, **or any new endpoint at all** (`RUN_JAVA_API` true) | `dotcms-pr-java-security-reviewer` |

`RUN_JAVA_SECURITY` fires on every new endpoint, DWR class and Struts action even with no auth keyword in the diff: a missing permission check has no keyword to grep for.

**Stack-aware input of the agnostic agents** (extend their input; launch no duplicate):

- **Silent-failure-hunter**: TypeScript only. Java error handling (an empty or swallowing `catch`, `e.printStackTrace()`, a `catch` that only logs at DEBUG, a swallowed `InterruptedException`) belongs to the java-code reviewer, which receives the `java` bucket anyway.
- **Comment-analyzer**: on `backend`/`mixed`, also trigger on `+ *//`, `+ */\*`, and Javadoc tags (`@param`, `@return`, `@throws`, `@deprecated`) in `*.java` additions.
- **Logic-reviewer**: on `backend`/`mixed`, treat a Java class with branching, loops, stream pipelines, or state transitions as a logic-bearing unit — services, managers, factories, and helpers under `com.dotcms.**`. It traces input → transformation → output the same way it does for a TypeScript utility.
- **AC-validator** and **historical-reviewer** receive the full changed-file list on any `STACK`.

**Historical-reviewer**: always active on `PROCEED`; no content trigger.

**CI, Maven and docs reviewers**: no grep. `RUN_CI`, `RUN_MAVEN` and `RUN_DOCS` are true when the `ci`, `maven` or `docs` bucket has a file, whatever `STACK` is.

**One reviewer per line.** The silent-failure, comment-analyzer and logic triggers read only files outside the `ci`, `maven` and `docs` buckets, and those agents receive only such files. The ci reviewer owns the comments and inline scripts of workflows and actions (YAML `#` comments, `run:` bash, `github-script` JS), the maven reviewer the comments of POMs, and the docs reviewer the Markdown.

Record the trigger results as flags: `RUN_SILENT_FAILURE`, `RUN_COMMENT_ANALYZER`, `RUN_HISTORICAL`, `RUN_LOGIC_REVIEWER`, `RUN_JAVA_API`, `RUN_JAVA_PERSISTENCE`, `RUN_JAVA_SECURITY`, `RUN_CI`, `RUN_MAVEN`, `RUN_DOCS`.

## PR shapes that need more than the diff

Check these after Pass 3. Each sets a flag Stage 4 and the report read.

### CI changes and CI gates (`CI_GATE`)

A CI change is proven by the run that executes it, not by its diff. Set `CI_GATE = true` when the `ci` bucket has a file, or a `pom.xml` adds or changes a `<profile>`. Then:

1. **Triggers.** For every changed workflow, list the events in its `on:`. For a changed reusable workflow (`workflow_call`) or composite action, list the workflows that call it (`grep -rln` under `<HEAD_DIR>/.github/workflows`) and their events.
2. **What this PR's checks ran** (PR mode only):

   ```bash
   gh pr checks <N> --repo dotCMS/core --json name,state,link,workflow,event
   ```

   For a gate the PR adds (a workflow job, a Maven profile, a check script), read that job's log too: `gh run view --repo dotCMS/core --job <JOB_ID> --log`, where the job id is the last number in the check's `link` (the jobs logs API returns an empty body for these runs).
3. **Record `CI_GATE_EVIDENCE`**, one line per changed workflow: `ran in this PR's checks: <check> <state>`, or `first runs on <event>` when no check of the PR ran it. Merge-queue, release, scheduled, `workflow_run`, `workflow_dispatch` and push-to-main workflows never run on a PR, so green checks prove nothing about them. For a gate, add whether it really ran (not a silent skip, a cached step or zero tests) and which path it exercised.

Stage 7 reports `CI_GATE_EVIDENCE`, and a changed workflow that first runs after merge raises the risk level by one. The ci reviewer receives it too. In local mode there is no run: record the triggers and "not exercised yet; check the PR's run".

### Vendored imports (`VENDORED`)

A PR that imports reviewed upstream source (its title or body says so, or it adds a copied third-party tree with its license) is reviewed against the upstream, not line by line as new code. Set `VENDORED = true` and:

1. Clone the upstream at the imported commit into `<RUN_DIR>/upstream` and `cmp` every imported file against it. Identical files are not line-reviewed.
2. Line-review only the adapted files (they differ from upstream), the upstream delta between the version core ships today and the imported one (`git -C <RUN_DIR>/upstream diff <shipped ref> <imported commit>`), and the hand-written glue.
3. The import's workflow and `pom.xml` changes go to the ci and maven reviewers through their buckets, with the vendored line of their templates (`agent-prompts.md`).
4. A finding on an unchanged vendored line is marked `PRE-EXISTING UPSTREAM`, never `ADDED-BY-THIS-PR`.
5. Grep the upstream branch name and its commit messages for a `private-issues` reference. If one is there, tell the user privately; it must not be quoted on the PR.
