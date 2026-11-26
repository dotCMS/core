---
name: dot-pr-review
description: >
  Autonomous multi-agent PR review for the dotCMS/core repo, covering the Angular frontend, the Java
  backend, and the CI workflows, Maven build and docs around them. Classifies changed files, detects
  which stack the PR touches, and partitions them across specialized reviewers — frontend
  (Angular/TS, styling, Spectator tests), backend (Java standards, REST/JAX-RS contracts, persistence
  and SQL, backend security, JSPs, JUnit/integration tests), CI (GitHub Actions workflows and
  composite actions), Maven (POMs, checked against the effective POM), docs and agent-instruction
  files, and stack-agnostic ones (historical regressions, silent failures, comment rot, behavioral
  logic vs acceptance criteria, per-AC coverage of the linked issue including out-of-scope work, and
  an equivalence-partition test design that surfaces the cases no test covers). Every
  reviewer writes findings to a crash-safe sidecar; anchors and duplicates are verified against the
  live PR before anything is posted. Use when the user asks to review a PR, run a code review, check
  PR quality, or mentions "/dot-pr-review" with a PR number or URL (e.g. "/dot-pr-review 34829", "review
  PR 34829", "review https://github.com/dotCMS/core/pull/34829"). A local mode runs the same pipeline
  on the local branch before any PR exists, with no GitHub writes, and writes the report to disk:
  use it when the user asks to "review my local changes", "review the branch before the PR", "review
  this before I open the PR", or runs "/dot-pr-review local" (optional base=, issue=, out=). Only for
  dotCMS/core: PRs against it, or a checkout or worktree of it.
allowed-tools: Bash(gh --version:*), Bash(gh pr view:*), Bash(gh pr diff:*), Bash(gh pr list:*), Bash(gh pr checks:*), Bash(gh run view:*), Bash(gh issue view:*), Bash(gh search issues:*), Bash(gh api repos/dotCMS/core/pulls/*), Bash(gh api repos/dotCMS/core/issues/*), Bash(gh api --paginate repos/dotCMS/core/*), Bash(gh api user:*), Bash(pwd), Bash(ls:*), Bash(cat:*), Bash(wc:*), Bash(grep:*), Bash(sed -n:*), Bash(cmp:*), Bash(jq:*), Bash(mktemp:*), Bash(mkdir -p:*), Bash(bash:*), Bash(git -C:*), Bash(git fetch:*), Bash(git clone:*), Bash(git diff:*), Bash(git show:*), Bash(git merge-base:*), Bash(git cat-file:*), Bash(git rev-parse:*), Bash(git symbolic-ref:*), Bash(git branch --show-current), Bash(git log:*), Bash(git ls-files:*), Bash(git status:*), Write(local/code-review/**), Write(local/agents/**), Write(local/security/**)
owner: "@oidacra"
status: experimental
---

# Autonomous PR Review System

Multi-agent code reviewer for both stacks of `dotCMS/core` — the Angular frontend and the Java backend — and for the CI workflows, Maven POMs and docs that ship with them. It classifies PR files, detects which stack they belong to, partitions the work, and launches specialized agents in parallel. Findings are written to crash-safe sidecars, verified against the live PR head, deduped against comments that already exist, and then presented as one table for a single posting decision.

In **local mode** (`/dot-pr-review local`, `references/local-mode.md`) the same pipeline reviews the local branch before any PR exists: same reviewers, same validation, no GitHub writes, and a report on disk instead of a posting decision. Our own changes are reviewed this way before they go up for human review.

> **Implicit dependency**: this plugin expects to run against the `dotCMS/core` repo. The reviewer agents read standards from the current working directory — `docs/frontend/*` (Angular, TypeScript, styling, testing rules) for the frontend agents and `docs/backend/*`, `docs/core/SECURITY_PRINCIPLES.md`, `dotCMS/src/main/java/com/dotcms/rest/{README,CLAUDE}.md` plus `docs/testing/*` (Java standards, REST API patterns and the REST/OpenAPI guide, database patterns, backend security, unit/integration testing) for the Java agents, `docs/core/CICD_PIPELINE.md` and `.github/workflows/README.md` for the CI reviewer, and `docs/backend/MAVEN_BUILD_SYSTEM.md` for the Maven reviewer, whose effective-POM diff also needs a JDK and a `~/.m2` holding core's build plus the `maven-help-plugin` version core's parent pins (without them it falls back to reading the POMs). Installing the plugin outside of `dotCMS/core` is unsupported.
>
> **Path convention**: the orchestrator and all agents MUST use paths relative to the current working directory for anything in the repo (e.g. `docs/frontend/ANGULAR_STANDARDS.md`, `core-web/libs/...`). Do not construct absolute paths such as `/Users/<name>/Work/core/...` inside agent prompts or Bash commands — the skill is always invoked from the repo root. The only absolute paths are the run directory in the session scratchpad (`RUN_DIR`, and `HEAD_DIR` inside it) and the skill's own directory (`SKILL_DIR`).

## How this skill is laid out

This file is the orchestrator: arguments, rules, the pipeline, and when to read what. The detail of each stage lives in `references/` and is read **at that stage, not before**; the mechanical steps are scripts in `scripts/`. `SKILL_DIR` is this skill's base directory (given when it loads); run every script by full path, `bash <SKILL_DIR>/scripts/<name>.sh`, and read every reference as `<SKILL_DIR>/references/<name>.md`. Each script prints its usage with `--help`.

## Bash Command Rules (MUST follow)

These rules apply to EVERY Bash command the orchestrator issues in this skill. Violating them triggers manual permission prompts and breaks the pipeline.

- **Never prepend `cd <absolute path> &&` to any command.** The skill is invoked at the repo root; commands must run in-place. A compound command that starts with `cd /Users/.../core && ...` will be blocked by Cursor as "compound command contains cd with output redirection — manual approval required".
- **Never pipe `gh` output through `head`, `tail`, or redirect stderr to `/dev/null`.** `gh` already returns a bounded JSON payload and meaningful errors on stderr; swallowing them hides auth/rate-limit issues. Let the full stdout reach you (saving stdout to a file in `RUN_DIR` is fine). The one exception is the version preflight's `gh --version | awk ...`, which transforms a single line into a verdict token rather than truncating anything — it is quoted verbatim below and must be run exactly as written.
- **Always target the dotCMS/core repo explicitly**: pass `--repo dotCMS/core` (or `-R dotCMS/core`) to every `gh pr ...` and `gh api` invocation. This makes the commands work regardless of the caller's current working directory and removes any reason to `cd`.
- **One command per Bash call.** Do not chain with `&&` or `;` unless both sides are trivial read-only commands already covered by `allowed-tools`.
- **Never combine `--comments` with `--json`** on `gh pr view` / `gh issue view`. Since gh 2.99 that combination is rejected outright (`specify only one of --comments or --json`). Fetch comments as a JSON field (`--json comments`) when you need to parse them, and use `--comments` only for the human-readable rendering.
- **Scripts run as files.** Use the scripts in `scripts/`; never write a heredoc script or an inline `python`/`node` one-off, which hooks can reject mid-run. Never `rm -rf` an absolute path (hooks block it): a new run gets a new directory instead.

### Preflight — toolchain + cwd sanity check (first action, before Stage 1)

Before running Stage 1, verify the toolchain and the current working directory. Local mode runs this preflight unchanged; a `dotCMS/core` worktree passes it like a clone. Run these commands (separately, no `cd`):

```bash
pwd
ls docs/frontend
```

**Then gate on the `gh` version.** Stages 2-3 rely on `gh pr view --json files` returning
`changeType`, on `gh pr diff --exclude`, and on the Issues-2.0 JSON fields (`issueType`,
`parent`), so an older `gh` fails mid-pipeline after the eligibility check has already run:

```bash
gh --version | awk 'NR==1{v=$3} END{if(v==""){print "GH_MISSING"; exit} split(v,a,"."); print (a[1]>2 || (a[1]==2 && a[2]>=99)) ? "GH_OK " v : "GH_TOO_OLD " v}'
```

Read the single token it prints — do **not** compare version numbers yourself (`2.100` sorts
below `2.99` lexically, and that mistake is silent):

| Output | Action |
|---|---|
| `GH_OK <version>` | proceed |
| `GH_TOO_OLD <version>` | **abort**: "This skill requires GitHub CLI 2.99.0 or newer (found `<version>`). Upgrade with `brew upgrade gh` or `mise up gh@latest`, then retry." |
| `GH_MISSING` | **abort**: "GitHub CLI (`gh`) is not installed or not on PATH. Install it from https://cli.github.com and run `gh auth login`." |

**If any later `gh` call fails with `unknown flag:` or `Unknown JSON field:`**, the `gh` actually
resolved on PATH is older than the preflight reported (a shim, or a second install earlier in
PATH). Abort and report the failing command verbatim. Do **not** retry without the flag, do
**not** drop the JSON field, and do **not** substitute an older equivalent — the flags in this
skill were chosen because the older path produced worse results, so silently degrading yields a
review or report the user has no reason to distrust and every reason to.

If `docs/frontend/` does not exist (or `ls` errors), abort with a single message to the user:

> This skill must be run from a clone of `dotCMS/core`. Current working directory is `<pwd output>`. Please `cd` into your `dotCMS/core` checkout and retry.

Do NOT attempt to recover by `cd`-ing somewhere on the user's behalf.

Store the verified working directory as `CORE_REPO` — Stage 7b re-slices the diff from this clone with `git -C <CORE_REPO>`, and it must be the same checkout the reviewers read files from.

## Usage

```bash
/dot-pr-review <PR_NUMBER>
/dot-pr-review <PR_URL>
/dot-pr-review <PR_NUMBER> auto     # non-interactive: post every surviving finding without prompting
/dot-pr-review <PR_URL> auto
/dot-pr-review local [base=<ref>] [issue=<ref>] [out=<dir>]   # local mode: the local branch before any PR; report on disk, nothing posted
```

### Arguments

| Token | Meaning |
|---|---|
| `<PR_NUMBER>` | Numeric PR id (e.g. `34829`). Resolved against `dotCMS/core`. |
| `<PR_URL>` | Full GitHub PR URL — the PR number is extracted from it. |
| `auto` | Optional token. Enables **auto-post mode**: Stage 8 skips the approval prompt and posts every finding that survived Stage 6 and Stage 7b in a single parallel batch. Use when you trust the filter and want a hands-off run. |
| `force` | Optional token. Overrides a Stage 1 `SKIP` (merged, draft, bot author, already reviewed) and a Stage 3 `reviewable == 0` skip. Use it to re-review a PR you have already reviewed, or one made entirely of files no reviewer owns. |
| `dry-run` | Optional token. Runs the **entire** pipeline — agents, consolidation, confidence validation, and the full Stage 7b anchor/duplicate verification — then prints the summary table and posts **nothing**. Never prompts. Use to test the pipeline or to preview what a review would say. |
| `local` | First token. Selects **local mode**: reviews the local branch, no PR involved. See `references/local-mode.md`. |
| `base=<ref>` | Local mode only. The base of the reviewed diff, `git diff <base>` (working tree against the base, so uncommitted changes are included). Default: `git merge-base HEAD origin/<default branch>`, after a fetch. For a re-review of fix commits, pass the reviewed sha. |
| `issue=<ref>` | Local mode only. The issue whose ACs the change is checked against: `38250` (a `dotCMS/core` issue), `owner/repo#N`, or an issue link. `dotCMS/private-issues#N` is private mode. Without it, the ac-validator and the test-case designer do not run. |
| `out=<dir>` | Local mode only. Where the report and the test plan are written. Default `local/code-review/`, or `local/security/<N>/` when `issue` is a private issue. |

Parse the arguments once at the start of the skill and store `AUTO_POST = true` if the `auto` token is present, `DRY_RUN = true` if `dry-run` is present.

`FORCE = true` if the `force` token is present.

`DRY_RUN` wins over `auto` if both are given — a dry run never posts, ever. `DRY_RUN` and `FORCE` both **bypass the Stage 1 eligibility check** (a merged or draft PR is a legitimate dry-run target, and `force` exists precisely to review something the gate would reject).

`LOCAL = true` if the first token is `local`; then parse the `key=value` tokens. In local mode `auto`, `force` and `dry-run` do not apply: print `Ignoring <token>: local mode never posts.` for each one given, and leave `AUTO_POST`, `DRY_RUN` and `FORCE` false. **Read `references/local-mode.md` now**: it replaces Stage 2 and says what changes at every other stage.

## Run directory (before Stage 1)

Create a fresh, empty directory for this run in the session scratchpad: `<scratchpad>/review<N>`, or `<scratchpad>/review-local-<BRANCH_SLUG>` in local mode (right after Local Stage 2 step 1); add `-2`, `-3` when the name exists. With no scratchpad in the session, use `mktemp -d`. Store it as `RUN_DIR`, and create its `sidecars/`, `slices/`, `bundles/` and `merged/` folders with one `mkdir -p`. `SIDECAR_DIR = <RUN_DIR>/sidecars`.

Everything the run writes for itself (diffs, slices, bundles, sidecars, merged findings, the PR head checkout) goes there, so two reviews never share a file and nothing has to be wiped. What must outlive the session (reports, test plans) goes to `local/`.

## Review Pipeline

| # | Stage | What it does | Done when | Read |
|---|---|---|---|---|
| 1 | Eligibility | Haiku checks state, draft, bot author, a review of yours at this head | `ELIGIBLE`, or a `SKIP` that `force` / `dry-run` overrides | `pr-data.md` |
| 2 | Fetch | metadata, base and head, diff, file list; PR head checked out; existing-feedback digest; linked issue and ACs; the brief | brief printed, `HEAD_DIR` ready | `pr-data.md`, `issue-and-ac.md` |
| 3 | Classify | buckets (code, CI, Maven, docs), stack gate, specialist triggers, CI exercise and vendored-import checks | `STACK` and every `RUN_*` flag recorded | `classify.md` |
| 4 | Launch | standards bundles, diff slices, roster, every active reviewer in parallel | `diff-slices.sh` prints `OK`; every active agent launched | `agents.md`, `sidecar-and-budget.md`, `agent-prompts.md` |
| 4b | Recovery | sidecar check, one resume per silent agent, report-only findings saved | every instance `OK` or `SIDECAR_ONLY`; `COVERAGE_GAPS` recorded | `recovery.md` |
| 5 | Consolidate | head check, merge, anchors re-derived, origin, dedupe and fold, AC reconciliation, test plan file | one anchored, deduped list | `validation.md` |
| 6 | Validate | Haiku per finding, Sonnet for logic, MISSING and contested ACs, at most 20 at once | every finding scored and filtered | `validation.md` |
| 7 | Report | the structured report, SHA-anchored | report printed | `report-template.md` |
| 7b | Verify | head and comments refetched, anchors re-checked, duplicates dropped, overlaps become replies | gate line printed | `anchors.md` |
| 8 | Post | AC walk-through as actions, one table, one posting decision, the verdict asked apart | posted, or declined | `posting.md` |
| 9 | Simplifier | optional proposals, never edits | answered or skipped | `posting.md` |
| 10 | Summary | summary, PR head checkout removed | summary printed | `posting.md` |

Stages 1-6 are automatic; 7 is the report; 7b is the gate nothing reaches GitHub without; 8-10 are interactive. In local mode, `local-mode.md` replaces Stage 2, 7b and 8, and skips Stages 1 and 9.

## Stages

**Stage 1 — Eligibility.** Read `references/pr-data.md` now. Run its Haiku check; on `SKIP`, print the reason and stop, unless `FORCE` or `DRY_RUN` is set (then warn and go on). Local mode: skipped.

**Stage 2 — Fetch.** Follow the rest of `pr-data.md`: the fetches (diff saved to `<RUN_DIR>/pr.diff`, paths to `<RUN_DIR>/changed-files.txt`), the large-PR path, the PR head checked out as `HEAD_DIR`, and the existing-feedback digest. Then read `references/issue-and-ac.md`: resolve the linked issue and its ACs (never abort for a missing one), set `PRIVATE_LINKED`, `SELF_TRIAGE`, `RUN_AC_VALIDATOR` and `RUN_TEST_DESIGNER`, and print the 200-300 word brief before Stage 3. Local mode: "Local Stage 2" in `local-mode.md`.

**Stage 3 — Classify.** Read `references/classify.md` now. Drop deleted files, bucket the rest (path rules first: agent-instruction files and `docs/` to docs, `.github/` workflows and actions to ci, POMs and `.mvn/` to maven; JSPs are backend production code), apply the stack gate (stop on `reviewable == 0` unless `FORCE`), grep the added lines for triggers, record how each changed workflow is exercised, and check for a vendored import.

**Stage 4 — Launch.** Read `references/agents.md`, `references/sidecar-and-budget.md` and `references/agent-prompts.md` now. Build only the bundles whose agents launch (with `RUN_MAVEN`, the effective-POM diff from `effective-pom.sh`), write the slice lists, run `diff-slices.sh` and launch nothing until it prints `OK`. Write the roster, then launch every active agent in one message (at most 20 at a time), each prompt being the Inputs block, its template, and the sidecar block with its own sidecar and budget. When the batch returns, run `merge-sidecars.sh`.

**Stage 4b — Recovery.** Read `references/recovery.md` now. Classify every instance on the roster, resume each `PARTIAL` or `LOST` one once with the exact message given there, save report-only findings as sidecar rows, and record `COVERAGE_GAPS`. Never ask the user to nudge an agent.

**Stages 5 and 6 — Consolidate and validate.** Read `references/validation.md` now. Check the head first, merge, re-derive every anchor with `verify-anchors.sh`, record each finding's origin, dedupe and fold; keep AC coverage and test design out of the finding stream and reconcile the AC table with the other reviewers; write the test plan. Then validate (Haiku tool-free for most domains, Sonnet for logic, CI, Maven and docs findings, MISSING and contested ACs; at most 20 at once) and keep scores of 80 or more, or everything under `SELF_TRIAGE`.

**Stage 7 — Report.** Read `references/report-template.md` now and print the report: the brief as its summary, AC coverage, test design, findings by severity with their origin, the recommendation and its gates.

**Stage 7b — Verify before posting.** Read `references/anchors.md` now. It runs unconditionally, in interactive and auto mode: refetch the head and every comment, re-slice from local git if the head moved, re-check every anchor, drop duplicates, turn overlaps into replies, and print the gate line. Local mode: its own 7b in `local-mode.md`.

**Stages 8 to 10 — Post, simplify, summarize.** Read `references/posting.md` now. Walk the AC gaps as drafted actions, print one table, ask once (a "post" already said is the answer), post in one parallel batch, then ask the verdict separately. Private linked issue: default to posting nothing and offer the neutral drafts. Then the opt-in simplifier and the summary, and remove `HEAD_DIR`. Local mode: write the report, post nothing, ask nothing (`local-mode.md`).

## Error Handling

| Failure | Response |
|---|---|
| PR fetch fails | `gh pr list --repo dotCMS/core --limit 100` to help locate; report "Unable to fetch PR #<N>." |
| No reviewable files (`reviewable == 0`) | Stop at Stage 3 with: "PR #<N> has no files any reviewer owns. Skipping review." (`force` reviews it anyway) |
| Haiku eligibility returns SKIP | Stop at Stage 1 with the skip reason |
| Agent returns empty findings | Fine — means no issues found in that domain |
| No linked issue / no ACs found | `RUN_AC_VALIDATOR = false`; skip the ac-validator and print "AC coverage not evaluated" in Stage 7. Never abort. |
| test-case-designer returns no report and an empty sidecar | Treat like any `LOST` agent in Stage 4b; if still empty, print "Test design unavailable" in Stage 7 and do not apply its gate |
| ac-validator flags MISSING but Sonnet re-check says FALSE_MISSING | Downgrade to COVERED (pre-existing/overlooked); do not let it drive Request Changes |
| Haiku validator times out on a finding | Treat as score 50 (filtered out under the ≥80 rule) |
| Comment posting fails (line not in diff) | Show the error, offer retry with nearest changed line, or skip |
| `HEAD_SHA` retrieval fails | Fall back to `main` in links, warn the user that links may go stale if the PR is rebased |
| `diff-slices.sh` prints `FAILED` | Launch nothing. Fix the slice lists, or rebuild the diff from local git (`--git`), and run it again: a dropped file is a file no reviewer sees |
| `gh pr diff` fails with HTTP 406, or `changedFiles` > 100 | Large-PR path in `pr-data.md`: paginated file list, diff from local git |
| The PR head cannot be checked out (`git worktree add` fails) | Abort and say why; reviewers reading `main` instead of the PR review the wrong code |
| An Agent call is rejected (more than 20 running) | Send it again when a slot frees; never drop it from the roster |
| `jq` is missing | `merge-sidecars.sh` and `verify-anchors.sh` stop with an error: install `jq` and rerun the stage |
| Local mode: the base does not resolve | Abort: "Cannot resolve the base `<ref>`." Never fall back to another base silently |
| Local mode: no changes against the base | Stop: "Nothing to review: no changes between <BASE_SHA> and the working tree." |
| Local mode: `issue=` cannot be read | Say so in one line and continue with `NO_LINKED_ISSUE`; the report says AC coverage was not evaluated |

## Tips

- Run again after a PR is updated to see if issues were addressed — the `HEAD_SHA` will differ, so links will point to the new snapshot
- For urgent reviews: "This is blocking deployment, prioritize critical issues only" — the pipeline will still run all agents but you can skip non-critical comments in Stage 8
- For large PRs (50+ files), the system automatically partitions work across up to 4 code-reviewer instances
- The Haiku validator is parallel — a 30-finding review validates in ~the time of a single Haiku call
- Review your own branch before you open the PR: `/dot-pr-review local issue=<ref>`. After fixing what it found, commit and run it again with `base=<the HEAD the report names>` to review only the fix.
