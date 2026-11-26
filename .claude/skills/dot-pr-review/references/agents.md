# Stage 4: Partition & Launch Agents

Launch agents in **a single parallel batch**. Mix and match based on the flags from Stage 3. Before launching, read `sidecar-and-budget.md` (the block every prompt carries) and `agent-prompts.md` (the templates). Local mode launches the same agents; only the diff source differs (`local-mode.md`).

## Preload Shared Context as bundles (once, before launching)

Each reviewer gets its standards, its diff slice, the existing-feedback digest and the ACs as **files in `<RUN_DIR>`**, read once at the start of its run. The orchestrator never retypes a standards doc or a diff into a prompt: retyping costs output tokens and drifts from the source, while a file is copied by the shell and read by the agent in one call. Agents given everything inline or nothing at all spent their turns exploring and ended with no report.

Copy the standards with `cat` (one Bash call per bundle), from the core checkout, never from memory:

```
<RUN_DIR>/bundles/frontend-standards.md     = docs/frontend/ANGULAR_STANDARDS.md + TYPESCRIPT_STANDARDS.md + STYLING_STANDARDS.md   # code bucket non-empty
<RUN_DIR>/bundles/testing-rules.md          = docs/frontend/TESTING_REVIEW_RULES.md                                                # test bucket non-empty
<RUN_DIR>/bundles/java-code-standards.md    = docs/backend/JAVA_STANDARDS.md + CONFIGURATION_PATTERNS.md + VIRTUAL_THREADS.md + docs/core/PROGRESSIVE_ENHANCEMENT.md   # java or jsp bucket non-empty
<RUN_DIR>/bundles/java-api-standards.md     = docs/backend/REST_API_PATTERNS.md + dotCMS/src/main/java/com/dotcms/rest/README.md + dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md   # only if RUN_JAVA_API
<RUN_DIR>/bundles/java-persistence-standards.md = docs/backend/DATABASE_PATTERNS.md                                                # only if RUN_JAVA_PERSISTENCE
<RUN_DIR>/bundles/java-security-standards.md    = docs/backend/SECURITY_BACKEND.md + docs/core/SECURITY_PRINCIPLES.md                 # only if RUN_JAVA_SECURITY
<RUN_DIR>/bundles/java-test-standards.md    = docs/testing/BACKEND_UNIT_TESTS.md + docs/testing/INTEGRATION_TESTS.md                 # java_test bucket non-empty
<RUN_DIR>/bundles/ci-standards.md           = docs/core/CICD_PIPELINE.md + .github/workflows/README.md, both at BASE_SHA            # only if RUN_CI
<RUN_DIR>/bundles/maven-standards.md        = docs/backend/MAVEN_BUILD_SYSTEM.md at BASE_SHA                                       # only if RUN_MAVEN
<RUN_DIR>/bundles/effective-pom.diff        = "Effective POM" below                                                             # only if RUN_MAVEN
```

For example: `cat docs/backend/DATABASE_PATTERNS.md > <RUN_DIR>/bundles/java-persistence-standards.md`. Build a bundle **only** when its agent is launching. Loading all twelve docs on a PR that changes one Java utility wastes context the reviewers need for the code itself.

The ci and maven bundles are read at the base, one doc per call, `git -C <CORE_REPO> show <BASE_SHA>:<path> >> <bundle>` (a plain redirect; `--output=` writes an empty file), because PRs of that kind often edit those very docs (#38238 changes `MAVEN_BUILD_SYSTEM.md` and a POM together). The PR is judged by the rules it started from, and its edit of the doc goes to the docs reviewer. A doc missing at the base is left out of the bundle.

### Effective POM (only if `RUN_MAVEN`)

The maven reviewer judges inherited configuration by what Maven resolves, so before launching it, build its effective-POM bundle (the script's `--help` says what it skips and prints):

```bash
bash <SKILL_DIR>/scripts/effective-pom.sh --root <CORE_REPO> --head <HEAD_DIR> --base-sha <BASE_SHA> --run-dir <RUN_DIR> --modules <modules> --changed <RUN_DIR>/changed-files.txt --out <RUN_DIR>/bundles/effective-pom.diff
```

- **Modules**: the directory of every changed `pom.xml` (`.` for the root one), plus `dotCMS` and `dotcms-integration` when `parent/pom.xml` or `bom/application/pom.xml` changed, since most inherited settings reach the build there. A PR with no changed `pom.xml` (only `mvnw` or `.mvn/`) has no modules: leave `--modules` out, and the script reports `NOT_COMPUTED`.
- **A PR from a fork** (`gh pr view <N> --repo dotCMS/core --json isCrossRepository` is true): add `--skip "PR from a fork"`.
- **`NOT_COMPUTED`** is a result, not a failure: launch the maven reviewer as usual, and Stage 7 names the reason on its Effective POM line.

In local mode `HEAD_DIR` is the working tree. Stage 10 removes the base worktree, `git -C <CORE_REPO> worktree remove --force <RUN_DIR>/base`, also when the run stops early.

Two more bundles come from Stage 2: `existing-feedback.md` (PR mode) and `acceptance-criteria.md` (when an issue was resolved). Stage 6 extracts the cited rule for each validator from these same bundles with `grep -n`.

## Diff slices

Write one path list per reviewer instance, `<RUN_DIR>/slices/<agent>-<i>.txt`, one repo-relative path per line, from the partitioning below. Then cut every slice in one call and let the script prove nothing was dropped:

```bash
bash <SKILL_DIR>/scripts/diff-slices.sh --out <RUN_DIR>/slices --expect <RUN_DIR>/changed-files.txt --diff <RUN_DIR>/pr.diff <RUN_DIR>/slices/*.txt
```

For a diff too large for `gh pr diff`, replace `--diff <RUN_DIR>/pr.diff` with `--git <BASE_SHA> --head <HEAD_SHA> --root <CORE_REPO>`; in local mode use `--git <BASE_SHA>` with no `--head`. `--expect` is `changed-files.txt` from Stage 2 (every changed path the diff holds). The historical reviewer's list holds all of them, so an `UNSLICED` line means a list is wrong.

The script fixes `diff --git` headers glued onto the previous file's last line (a file with no trailing newline in `gh pr diff` output), then asserts that the full diff holds one block per expected path and that every slice holds one block per path in its list, summed over all slices. **Any `FAILED` stops the launch**: a dropped line range silently removes a file from every reviewer, and the review would look complete. Fix the lists (or rebuild the diff with `--git`) and run it again.

## Partitioning Formulas

**Code reviewer** (based on count of **code** bucket):
```
1-15 files   → 1 instance
16-30 files  → 2 instances
31-45 files  → 3 instances
46+ files    → 4 instances (cap)
```
When partitioning: keep `.component.{ts,html,scss}` of the same component in the same instance, then balance instances by **churn** (`additions + deletions`, from the Stage 2 `files` JSON) rather than by file count. Ten one-line changes and one 800-line rewrite are not the same workload, and an instance that receives the rewrite plus its even share of files runs out of turns before it finishes reviewing.

**Test reviewer** (based on count of **test** bucket):
```
1-4 files    → 1 instance
5-8 files    → 2 instances
9+ files     → 3 instances (cap)
```
No grouping rules — each `.spec.ts` is independent.

**Java code reviewer** (based on count of the **java** bucket plus the **jsp** bucket):
```
1-12 files   → 1 instance
13-24 files  → 2 instances
25-36 files  → 3 instances
37+ files    → 4 instances (cap)
```
Balance by **churn**, not file count, exactly as for the frontend reviewer. Keep a class together with its package-level siblings when they are part of one change (a service and the interface it implements belong in the same instance).

**Java test reviewer** (based on count of the **java_test** bucket):
```
1-6 files    → 1 instance
7+ files     → 2 instances (cap)
```
Keep a test class in the same instance as its production class when both changed — the reviewer cannot judge whether a test is adequate without the code under test.

**Java API reviewer**: 1 instance when `RUN_JAVA_API`; 2 if endpoints span more than 6 resource classes. Receives the resource classes, their Form/View objects, and their exception mappers.

**Java persistence reviewer**: 1 instance when `RUN_JAVA_PERSISTENCE`; 2 if persistence changes span more than 8 factories/DAOs. Receives the query-bearing classes plus any changed `*.sql` and upgrade tasks.

**Java security reviewer**: always exactly **1 instance** when `RUN_JAVA_SECURITY` — never split. Authorization bugs live in the seam between two files, and two instances each holding half the seam find nothing. It receives the **entire** `java` bucket plus any changed `*.properties` and `*.jsp`, regardless of which files triggered it.

**Historical reviewer**: always exactly 1 instance. Receives the full file list.

**Silent-failure-hunter**: always exactly 1 instance. Receives only `.ts` files from the code bucket (not `.spec.ts`, not `.html`, not `.scss`).

**Comment-analyzer**: always exactly 1 instance. Receives all files (any extension) whose diff contains added/modified comment lines.

**Logic-reviewer** (based on count of triggering **logic-bearing** files — stores, components, services, utils, mappers, guards, resolvers, interceptors, pipes):
```
1-8 files    → 1 instance
9+ files     → 2 instances (cap)
```
When splitting across 2 instances, keep a store's `.store.ts` together with the component(s) that consume it, and keep a util together with its primary caller, so each instance can trace a complete flow. Each instance receives its files, their diff (as entry points), and the `ACCEPTANCE_CRITERIA` block. Unlike the other agents it is expected to `Read` the **full files**, not just the diff.

**AC-validator** (`dotcms-pr-ac-validator`): always exactly **1 instance**, launched only when `RUN_AC_VALIDATOR` is true. Receives the full changed-file list, the full diff, and the `ACCEPTANCE_CRITERIA` block. It reads the **full files** and returns a per-AC coverage table + an out-of-scope list — NOT line-level findings. Its output is handled separately from the finding stream (see Stages 5-8); it is complementary to the logic-reviewer, not a replacement (logic-reviewer finds correctness defects; ac-validator measures completeness).

**Test-case designer** (`dotcms-pr-test-case-designer`): always exactly **1 instance**, launched only when `RUN_TEST_DESIGNER` is true, on either stack. Receives the `ACCEPTANCE_CRITERIA` block, the changed production files, the changed test files (the `test` and `java_test` buckets, which may be empty), and the full diff. It defines equivalence partitions and boundary values per AC, derives the test cases (TC-###), and maps each one to an existing test: COVERED / PARTIAL / NOT COVERED / UNSPECIFIED. It returns a test plan, NOT test code and NOT line-level findings, so like the ac-validator its output stays out of the finding stream. It complements the test reviewers: they judge the tests that exist, it finds the tests that don't.

**CI reviewer** (`dotcms-pr-ci-reviewer`, when `RUN_CI`): 1 instance for up to 15 files of the `ci` bucket, 2 above that (cap). When splitting, keep each workflow with the reusable workflows, composite actions, scripts and tests it calls: a removed input or a renamed output breaks in the caller, and an instance that holds only one side finds nothing. Receives its `ci` files, `CI_GATE_EVIDENCE` and the full changed-file list.

**Maven reviewer** (`dotcms-pr-maven-reviewer`, when `RUN_MAVEN`): always exactly **1 instance**. Inheritance is the seam: a parent property and the module that overrides it must be read together. Receives the `maven` bucket, the effective-POM diff and the full changed-file list (a workflow passing `-P<profile>` is checked against the POMs).

**Docs reviewer** (`dotcms-pr-docs-reviewer`, when `RUN_DOCS`): 1 instance for up to 12 files of the `docs` bucket, 2 above that (cap), balanced by churn. Keep the agent-instruction files (`CLAUDE.md`, `AGENTS.md`, `.github/copilot-instructions.md`, `.github/instructions/**`) in one instance, since it checks they state each rule the same way. Receives its `docs` files and the full changed-file list.

## Launch Rules

- Launch **every active agent in parallel** (single message with multiple Agent tool calls)
- **At most 20 subagents run at once.** Calls past 20 are rejected, not queued. If more than 20 instances are active, launch the 20 that matter most first (java-security, ci, logic, ac-validator, java-code and code reviewers, then the rest) and send the others as slots free up; record each launch in the roster as it happens
- Write the roster, `<RUN_DIR>/roster.txt`: one sidecar file name per launched instance. Stage 4b and `merge-sidecars.sh` check every name on it
- Skip code-reviewer if `code_bucket` is empty
- Skip test-reviewer if `test_bucket` is empty
- Skip silent-failure-hunter if `RUN_SILENT_FAILURE` is false
- Skip comment-analyzer if `RUN_COMMENT_ANALYZER` is false
- Skip logic-reviewer if `RUN_LOGIC_REVIEWER` is false
- Skip ac-validator if `RUN_AC_VALIDATOR` is false (i.e. no linked issue / no ACs)
- Skip test-case-designer if `RUN_TEST_DESIGNER` is false. It does **not** depend on the test buckets: launch it even when the PR has no test files
- Skip **all** frontend agents (code-reviewer, test-reviewer) when `STACK == "backend"` or `"none"`
- Skip **all** Java agents when `STACK == "frontend"` or `"none"`
- Launch the ci, maven and docs reviewers on their flags (`RUN_CI`, `RUN_MAVEN`, `RUN_DOCS`), whatever `STACK` is; skip each one whose flag is false
- On `STACK == "mixed"`, launch both families in the **same** parallel batch — not in two waves
- Skip java-code-reviewer if the `java` and `jsp` buckets are both empty; java-test-reviewer if `java_test` is empty
- Skip each Java specialist if its `RUN_JAVA_*` flag is false
- The `sql` bucket goes **only** to java-persistence; the `config` bucket goes **only** to java-security. Neither is ever passed to java-code-reviewer
- Under `FORCE` with an empty `reviewable` set, launch only historical-reviewer and (if `RUN_AC_VALIDATOR`) ac-validator, plus (if `RUN_TEST_DESIGNER`) test-case-designer
- Historical reviewer runs if `RUN_HISTORICAL` is true (always, past the stack gate — on any `STACK`)
- The silent-failure hunter, comment-analyzer and logic reviewer receive only files outside the `ci`, `maven` and `docs` buckets (`classify.md`, "One reviewer per line")
