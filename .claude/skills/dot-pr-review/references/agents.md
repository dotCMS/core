# Stage 4: Partition & Launch Agents

Before writing any prompt, also read `sidecar-and-budget.md` (each reviewer's sidecar and turn budget) and `agent-prompts.md` (the templates). Local mode launches the same agents; only the diff source differs (`local-mode.md`).

## Standards bundles (once, before launching)

Each reviewer gets its standards, its diff slice, the existing-feedback digest and the ACs as **files in `<RUN_DIR>`**, which it reads once at the start of its run. Copy each doc into its bundle with `cat`, from the core checkout (one Bash call per bundle); a doc retyped into a prompt costs output tokens and drifts from the source, and a reviewer left to find its own docs spends its turns exploring.

```
<RUN_DIR>/bundles/frontend-standards.md     = docs/frontend/ANGULAR_STANDARDS.md + TYPESCRIPT_STANDARDS.md + STYLING_STANDARDS.md   # code bucket non-empty
<RUN_DIR>/bundles/testing-rules.md          = docs/frontend/TESTING_REVIEW_RULES.md + <SKILL_DIR>/references/right-test.md       # test bucket non-empty
<RUN_DIR>/bundles/java-code-standards.md    = docs/backend/JAVA_STANDARDS.md + CONFIGURATION_PATTERNS.md + VIRTUAL_THREADS.md + docs/core/PROGRESSIVE_ENHANCEMENT.md   # java or jsp bucket non-empty
<RUN_DIR>/bundles/java-api-standards.md     = docs/backend/REST_API_PATTERNS.md + dotCMS/src/main/java/com/dotcms/rest/README.md + dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md   # only if RUN_JAVA_API
<RUN_DIR>/bundles/java-persistence-standards.md = docs/backend/DATABASE_PATTERNS.md                                                # only if RUN_JAVA_PERSISTENCE
<RUN_DIR>/bundles/java-security-standards.md    = docs/backend/SECURITY_BACKEND.md + docs/core/SECURITY_PRINCIPLES.md                 # only if RUN_JAVA_SECURITY
<RUN_DIR>/bundles/java-test-standards.md    = docs/testing/BACKEND_UNIT_TESTS.md + docs/testing/INTEGRATION_TESTS.md + <SKILL_DIR>/references/right-test.md   # java_test bucket non-empty
<RUN_DIR>/bundles/ci-standards.md           = docs/core/CICD_PIPELINE.md + .github/workflows/README.md, both at BASE_SHA            # only if RUN_CI
<RUN_DIR>/bundles/maven-standards.md        = docs/backend/MAVEN_BUILD_SYSTEM.md at BASE_SHA                                       # only if RUN_MAVEN
<RUN_DIR>/bundles/effective-pom.diff        = "Effective POM" below                                                             # only if RUN_MAVEN
```

For example: `cat docs/backend/DATABASE_PATTERNS.md > <RUN_DIR>/bundles/java-persistence-standards.md`. Build a bundle only when its agent launches: the context a reviewer spends on docs it does not need is taken from the code.

The ci and maven bundles are read at the base, one doc per call, `git -C <CORE_REPO> show <BASE_SHA>:<path> >> <bundle>` (a plain redirect; `--output=` writes an empty file), because PRs of that kind often edit those very docs. The PR is judged by the rules it started from, and its edit of the doc goes to the docs reviewer. A doc missing at the base is left out of the bundle.

Two more bundles come from Stage 2: `existing-feedback.md` (PR mode) and `acceptance-criteria.md` (when an issue was resolved). Stage 6 extracts the cited rule for each validator from these same bundles with `grep -n`.

### Effective POM (only if `RUN_MAVEN`)

The maven reviewer judges inherited configuration by what Maven resolves (the script's `--help` says what it skips and prints):

```bash
bash <SKILL_DIR>/scripts/effective-pom.sh --root <CORE_REPO> --head <HEAD_DIR> --base-sha <BASE_SHA> --run-dir <RUN_DIR> --modules <modules> --changed <RUN_DIR>/changed-files.txt --out <RUN_DIR>/bundles/effective-pom.diff
```

- **Modules**: the directory of every changed `pom.xml` (`.` for the root one), plus `dotCMS` and `dotcms-integration` when `parent/pom.xml` or `bom/application/pom.xml` changed, since most inherited settings reach the build there. A PR with no changed `pom.xml` (only `mvnw` or `.mvn/`) has no modules: leave `--modules` out, and the script reports `NOT_COMPUTED`.
- **A PR from a fork** (`gh pr view <N> --repo dotCMS/core --json isCrossRepository` is true): add `--skip "PR from a fork"`.
- **`NOT_COMPUTED`** is a result, not a failure: launch the maven reviewer as usual, and Stage 7 names the reason on its Effective POM line.

In local mode `HEAD_DIR` is the working tree. Stage 10 removes the base worktree, `git -C <CORE_REPO> worktree remove --force <RUN_DIR>/base`, also when the run stops early.

## Diff slices

Write one path list per reviewer instance, `<RUN_DIR>/slices/<agent>-<i>.txt`, one repo-relative path per line, from the roster below. Then cut every slice in one call and let the script prove nothing was dropped:

```bash
bash <SKILL_DIR>/scripts/diff-slices.sh --out <RUN_DIR>/slices --expect <RUN_DIR>/changed-files.txt --diff <RUN_DIR>/pr.diff <RUN_DIR>/slices/*.txt
```

For a diff too large for `gh pr diff`, replace `--diff <RUN_DIR>/pr.diff` with `--git <BASE_SHA> --head <HEAD_SHA> --root <CORE_REPO>`; in local mode use `--git <BASE_SHA>` with no `--head`. The historical reviewer's list holds every changed path, so an `UNSLICED` line means a list is wrong.

**Any `FAILED` stops the launch**: a dropped range removes a file from every reviewer while the review still looks complete. Fix the lists, or rebuild the diff with `--git`, and run it again.

## Roster: who launches, with what, how many

Launch each reviewer by its bare name (`dotcms-pr-code-reviewer`): the copy in this repo's `.claude/agents/`, never a plugin-namespaced one. "Churn" is `additions + deletions` from the Stage 2 file list: balancing by churn instead of file count keeps an 800-line rewrite from landing on an instance that also carries its even share of files and runs out of turns.

| Agent | Launch when | Input | Instances |
|---|---|---|---|
| `dotcms-pr-code-reviewer` | `code` bucket non-empty and `STACK` is `frontend` or `mixed` | its share of the `code` bucket | by `code` count: 1-15 → 1, 16-30 → 2, 31-45 → 3, 46+ → 4 (cap). Keep a component's `.component.{ts,html,scss}` together, then balance by churn |
| `dotcms-pr-test-reviewer` | `test` bucket non-empty and `STACK` is `frontend` or `mixed` | its share of the `test` bucket | by `test` count: 1-4 → 1, 5-8 → 2, 9+ → 3 (cap); each `.spec.ts` stands alone |
| `dotcms-pr-java-code-reviewer` | `java` or `jsp` bucket non-empty and `STACK` is `backend` or `mixed` | its share of `java` + `jsp` | by `java` + `jsp` count: 1-12 → 1, 13-24 → 2, 25-36 → 3, 37+ → 4 (cap). Balance by churn; a class stays with the package siblings of the same change (a service and the interface it implements) |
| `dotcms-pr-java-test-reviewer` | `java_test` non-empty and `STACK` is `backend` or `mixed` | its share of `java_test`, plus the production classes they cover | 1-6 → 1, 7+ → 2 (cap). A test class stays with its production class when both changed: adequacy cannot be judged without the code under test |
| `dotcms-pr-java-api-reviewer` | `RUN_JAVA_API` | the resource classes, their Form/View objects and exception mappers | 1; 2 when endpoints span more than 6 resource classes |
| `dotcms-pr-java-persistence-reviewer` | `RUN_JAVA_PERSISTENCE` | the query-bearing classes, the `sql` bucket and the upgrade tasks | 1; 2 when persistence changes span more than 8 factories/DAOs |
| `dotcms-pr-java-security-reviewer` | `RUN_JAVA_SECURITY` | the **entire** `java` bucket, every changed `*.properties` and the `jsp` bucket, whatever triggered it | exactly 1, never split: authorization bugs live in the seam between two files |
| `dotcms-pr-historical-reviewer` | `RUN_HISTORICAL` (every run past the gate, any `STACK`) | the full file list | 1 |
| `dotcms-pr-silent-failure-hunter` | `RUN_SILENT_FAILURE` | the `.ts` files of the `code` bucket (no `.spec.ts`, `.html`, `.scss`), plus the `java` bucket on `backend`/`mixed` (`classify.md`) | 1 |
| `dotcms-pr-comment-analyzer` | `RUN_COMMENT_ANALYZER` | every file whose diff adds or modifies comment lines | 1 |
| `dotcms-pr-logic-reviewer` | `RUN_LOGIC_REVIEWER` | its logic-bearing files (stores, components, services, utils, mappers, guards, resolvers, interceptors, pipes), their diff as entry points, the ACs; it reads the **full files** | 1-8 files → 1, 9+ → 2 (cap). Split so each instance traces a complete flow: a `.store.ts` with the components that consume it, a util with its primary caller |
| `dotcms-pr-ac-validator` | `RUN_AC_VALIDATOR` | the full file list, the full diff, the ACs; it reads the full files | exactly 1. Returns an AC coverage table and an out-of-scope list, kept out of the finding stream. It measures completeness; the logic reviewer finds defects |
| `dotcms-pr-test-case-designer` | `RUN_TEST_DESIGNER`, whatever the test buckets hold | the ACs, the changed production files, the `test` and `java_test` buckets (may be empty), the full diff | exactly 1. Returns a test plan, kept out of the finding stream. It finds the tests that are missing; the test reviewers judge the ones that exist |
| `dotcms-pr-ci-reviewer` | `RUN_CI` | its `ci` files, `CI_GATE_EVIDENCE`, the full file list | 1 up to 15 `ci` files, 2 above (cap). Keep each workflow with the reusable workflows, composite actions, scripts and tests it calls: a removed input breaks in the caller |
| `dotcms-pr-maven-reviewer` | `RUN_MAVEN` | the `maven` bucket, the effective-POM diff, the full file list (a workflow passing `-P<profile>` is checked against the POMs) | exactly 1: a parent property and the module that overrides it are read together |
| `dotcms-pr-docs-reviewer` | `RUN_DOCS` | its `docs` files, the full file list | 1 up to 12 `docs` files, 2 above (cap), balanced by churn. Keep the agent-instruction files (`CLAUDE.md`, `AGENTS.md`, `.github/copilot-instructions.md`, `.github/instructions/**`) in one instance, which checks they state each rule the same way |

## Launch rules

- Launch every active instance in **one message** with one Agent call each; on `mixed` both families go in that same batch.
- **At most 20 subagents run at once**; calls past 20 are rejected, not queued. Above 20 instances, launch first java-security, ci, logic, ac-validator, java-code and code reviewers, then the rest as slots free up. A rejected call is sent again when a slot frees; it stays on the roster.
- Write the roster, `<RUN_DIR>/roster.txt`: one sidecar file name per launched instance, added as each launch happens. Stage 4b and `merge-sidecars.sh` check every name on it.
- The `sql` bucket goes only to java-persistence and the `config` bucket only to java-security.
- The silent-failure hunter, comment analyzer and logic reviewer receive only files outside the `ci`, `maven` and `docs` buckets (`classify.md`, "One reviewer per line").
- Under `FORCE` with an empty `reviewable` set, launch only the historical reviewer, plus the ac-validator and the test-case designer when their flags are set.
