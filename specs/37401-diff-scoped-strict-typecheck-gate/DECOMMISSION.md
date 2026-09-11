# Decommissioning the strict-gate

**This gate is scaffolding with an expiry date.** It exists to stop new non-strict TypeScript
landing on `main` *while* the workspace-wide strict migration waits for QA. When that migration
merges, the reason for the scaffolding is gone and every artifact listed here comes out.

This file is the removal procedure. It is written to be executable by someone — or something —
with no memory of why the gate was built.

**Issue**: dotCMS/core#37401 · **Trigger**: dotCMS/core#37198 · **Follow-up**: dotCMS/core#37448

---

## 1. The trigger

Remove the gate when **PR #37198 (`35932-enable-strict-mode-v3`, epic #35932) is merged to `main`**
and `core-web/tsconfig.base.json` on `main` carries `"strict": true`.

Confirm both, do not assume either:

```bash
gh pr view 37198 --repo dotCMS/core --json state,mergedAt --jq '{state, mergedAt}'
git fetch origin main
git show origin/main:core-web/tsconfig.base.json | grep -A1 '"strict"'
```

`state: MERGED` **and** `"strict": true` in the baseline. If the PR merged but the baseline is
still `false`, the migration was split or reverted — stop and find out which before deleting
anything.

## 2. Precondition — read this before deleting

Removing the gate on the trigger alone reopens a hole the spike discovered by accident.
`findings.md` §4 is the relevant finding, and it is counter-intuitive:

> **Declaring `strict: true` does not mean anything compiles it.**

**"But the build type-checks it" is the obvious objection, and it is half true.** `nx build` runs a
real `ngc`/`tsc`, so what it compiles is genuinely checked. It just does not compile most of this
workspace, and it never compiles the half where the spike found its violations:

| Fact | Value |
|---|---|
| Nx projects in `core-web` | 57 |
| …with a `build` target | **18** — the other 39 have nothing that compiles them |
| …with a `typecheck` target | **5**, all inferred by `@nx/vite/plugin`, **0** declared in a `project.json` |
| Does the build see `.spec.ts`? | **No** — `tsconfig.lib.json` carries `exclude: ["src/**/*.spec.ts", …]` |
| Where the spike's findings lived | **8 of 11 in `.spec.ts`** (§3) — 73 %, in files the build excludes by design |
| `tsconfig.spec.json` files that set `"strict": false` themselves | **5 of 51** — a strict baseline does not reach them |
| Files #37198 changes | 77 `.ts`, 13 `.html`, 8 `.json`, 1 `.prettierignore`, 1 `.md` |
| Does #37198 touch `nx.json`, any `project.json`, `pom.xml` or a workflow? | **No** |
| …how many `tsconfig.spec.json` does it fix? | **1** (`apps/dotcms-block-editor`), leaving the other 4 opted out |

So #37198 makes the configuration strict and fixes the existing violations, but adds no mechanism
that *runs* a type-check over what the build skips. Lint does not type-check. The gap that outlives
the merge is **the 39 projects with no build plus every `.spec.ts` in the workspace** — which is
where 73 % of what this gate caught was living.

> Two figures here supersede earlier ones. `findings.md` §4 says "3 of 57" projects have a
> `typecheck` target; re-running the command below returns 5. §4 also frames the gap as "nothing
> type-checks 54 of 57 projects", which overstates it — the build does cover 18. Re-measure rather
> than trusting any of these numbers; they move.

**Not verified, and it changes the size of the gap:** whether `ts-jest` reports type errors during
`nx test` or only transpiles. With `jest-preset-angular` 17 it should type-check, which would cover
the specs of the 46 projects whose `tsconfig.spec.json` does not opt out — but this was never
confirmed. Settle it by putting a deliberate type error in a spec and running that project's tests.

**Before deleting, verify something else type-checks the workspace:**

```bash
cd core-web
# Coverage today. A replacement should close the gap between these two.
NX_NO_CLOUD=true pnpm nx show projects --with-target typecheck
NX_NO_CLOUD=true pnpm nx show projects --with-target build
NX_NO_CLOUD=true pnpm nx show projects | tr ',' '\n' | wc -l

# Does anything run tsc in the build pipeline?
grep -rn "typecheck\|tsc --noEmit" pom.xml ../.github/workflows/ | grep -v node_modules

# Do the spec configs still opt out of strict?
grep -rl '"strict"[[:space:]]*:[[:space:]]*false' apps libs --include='tsconfig.spec.json'
```

A replacement only closes the gap if it **includes the specs** and **overrides the spec configs
that set `strict: false`**. A `typecheck` target that runs the build configuration reproduces the
exact blind spot this gate was built to cover.

If nothing covers it, the honest sequence is **replace, then delete** — not delete and hope.
Deleting first is still a valid choice, but make it knowingly and say so in the removal PR.

**Status at the time of writing (2026-09-08):** adding a `typecheck` to #37198 itself is the
intended replacement, being handled on that pull request. If it landed, this precondition is
already satisfied — confirm with the commands above rather than assuming, then delete freely.

## 3. What comes out

Everything below was created for this gate and has no other consumer. Verified: nothing outside
these two directories references `strict-gate` — not `core-web/pom.xml`, not `nx.json`, not
`lint-staged.config.mjs`, not any workflow, not `docs/`, not `.cursor/rules/`.

### 3.1 The harness — 31 tracked files

```
core-web/tools/scripts/strict-gate/
├── run.mjs  replay.mjs  corpus.mjs  writeup.check.mjs  README.md
├── lib/            13 modules
├── fixtures/        3 builders
└── *.test.mjs       9 suites
```

### 3.2 The spec directory — 5 tracked files

```
specs/37401-diff-scoped-strict-typecheck-gate/
├── spec.md  findings.md  data-model.md  DECOMMISSION.md   (this file)
└── contracts/cli.md  contracts/report.schema.json
```

Plus these, present locally but **gitignored** (`specs/*/plan.md` etc.) — they disappear with the
directory and are in no commit:

```
plan.md  research.md  tasks.md  quickstart.md  checklists/
```

### 3.3 Only if the follow-up (#37448) promoted the gate

The spike wires the harness into **nothing**. If #37448 shipped the production gate first, these
exist and must come out too — check each before assuming it does not:

| Location | What to remove |
|---|---|
| `core-web/pom.xml` | the `<execution>` with `<id>strict-gate</id>`, next to `lint-test` / `format-test` in the `generate-resources` phase |
| `core-web/lint-staged.config.mjs` | any `strict-gate` entry in the `**/*.{ts,js,mjs,...}` task list |
| `.github/workflows/` | only if a step was added; §12 of `findings.md` records that none was needed |
| wherever the durable script landed | the promoted copy, if it moved out of `tools/scripts/` |

## 4. What to keep

**Archive `findings.md` before deleting it.** It is the only record of measurements that cost more
than the spike's timebox (§11) and that justify decisions outliving the gate: the 0-of-11
false-positive rate, the 83 % whole-file inheritance cost, the 2.2× template-checking cost, and
the `typecheck`-coverage finding in §2 above. Losing it means re-running the spike to answer the
same questions.

```bash
gh issue comment 37401 --repo dotCMS/core \
    --body-file specs/37401-diff-scoped-strict-typecheck-gate/findings.md
```

The issue outlives the directory. Do this **before** step 5, not after.

## 5. The removal

```bash
git switch -c "removal/37401-retire-strict-gate" origin/main
git rm -r core-web/tools/scripts/strict-gate
git rm -r specs/37401-diff-scoped-strict-typecheck-gate
```

Commit message — say what made it removable, so the history explains itself:

```
chore(37401): retire the diff-scoped strict typecheck gate

#37198 merged and core-web/tsconfig.base.json is now strict: true, so the
diff-scoped gate has no remaining job: new code is held to the workspace
baseline like every other line.

Removes the spike harness (core-web/tools/scripts/strict-gate/) and its
spec directory. findings.md is archived on #37401 — it holds the measured
false-positive rate, the granularity cost, and the typecheck-coverage
finding, none of which are reproduced by anything left in the repo.

Closes #37401
```

## 6. Verify nothing is left

Every command must come back empty. Run them from the repo root.

```bash
# 1. No file references the gate. The only expected hit is unrelated:
#    dotcms-postman/.../historical-event.json matches on the id 3740143, not on #37401.
grep -rIl "strict-gate\|37401" . \
    --exclude-dir=node_modules --exclude-dir=.git --exclude-dir=dist --exclude-dir=.nx

# 2. Both directories are gone, from the worktree and from the index.
ls core-web/tools/scripts/strict-gate specs/37401-diff-scoped-strict-typecheck-gate 2>&1
git ls-files core-web/tools/scripts/strict-gate specs/37401-diff-scoped-strict-typecheck-gate

# 3. The build is unaffected — it never referenced the gate, and this proves it still does not.
cd core-web && NX_NO_CLOUD=true pnpm nx format:check --all
```

Then let CI confirm it: the removal touches no build input, so a green `PR Test / Frontend Unit
Tests` and `PR Build / Initial Artifact Build` is the whole verification story.

## 7. Issues to settle

| Issue | Action |
|---|---|
| **#37401** | Close. Archive `findings.md` on it first (§4). |
| **#37448** | Close as obsolete **if** the gate was never promoted. If it was, the follow-up's own work is what §3.3 removes — close it with a note pointing at the removal PR. |
| **#37086** | Independent of the gate (`libs/sdk/angular`, the intermediate tier). Leave open. |
| **#35930** | `TODO(#35930)`, the four apps with `strictTemplates: false`. **Not addressed by #37198** — the template arm was a no-go (§7). Leave open. |

---

## Why the gate does not simply become permanent

Worth recording, because it is the obvious counter-argument and it was considered.

`findings.md` §4 makes a real case that the gate's value outlives the migration: it is the only
thing checking the 39 projects with no build, and every `.spec.ts`, before and after #37198. But a
*diff-scoped* gate is the wrong shape for that job. Its entire design — discarding 99.1 % of
diagnostics, forgiving untouched lines, forcing flags the config does not declare — exists to be
useful while the baseline is **non-strict**. Once the baseline is strict, the right tool is an
ordinary workspace-wide `typecheck` target that compiles each project under its own configuration,
with no diff filter and nothing forced in memory.

Keeping the diff-scoped gate to fill that gap means maintaining a filter that no longer filters
anything meaningful. Replace it (§2), do not repurpose it.
