# Contract: the CI invocation

**Feature**: [../spec.md](../spec.md) | **Plan**: [../plan.md](../plan.md) | **Date**: 2026-09-14

The harness's own command contract is unchanged and lives at
`specs/37401-diff-scoped-strict-typecheck-gate/contracts/cli.md`. This file specifies only the one
place this feature invokes it from, and the exact guarantees it must uphold.

**Scope note.** An earlier draft of this feature also added a local `pre-push` git hook that
refused pushes on findings. It was cut: this change is continuous-integration verification only.
The reasoning that produced it is preserved in the spec's clarifications C-001 and C-002 for
whoever picks up a local hook later — in particular the finding that the harness compares two
*committed* points, so a commit-time hook would examine the previous commit and report "clean" on
the very violation being committed.

---

## The `strict-gate` execution — `core-web/pom.xml`

### Shape

An `<execution>` with id `strict-gate`, goal `exec`, phase `generate-resources`, declared **inside
the existing `validate` profile** so it does not exist unless `-Pvalidate` is active (R-005).

The exec plugin's `<executable>` for this module is `${node.install.dir}/pnpm`, so the argument list
begins with `exec` — the same shape `lint-test` and `format-test` already use.

```
exec node tools/scripts/strict-gate/run.mjs
     --base=${git.origin.branch}
     --flags=strict
     --granularity=line
     --scope=core-web
     --format=github
```

`parseArgs` in `run.mjs` splits on `=`, so the inline `--flag=value` form works as-is. Unknown
options are rejected by name — do not invent flags.

### Required configuration, and why each element is load-bearing

| Element | Value | Consequence if wrong |
|---|---|---|
| `<skip>` | `${skip.strict.gate}` | Governs FR-006. Defaults `true`; **two** profiles flip it to `false`, on `env.GITHUB_EVENT_NAME` = `pull_request` and = `merge_group`. Maven activation has no OR, hence two. Lose the first and nobody sees the annotations; lose the second and nothing is enforced, because `main` declares no required status checks. |
| `<successCodes>` | `0` — **and nothing else** | The entire Run Outcome model. `1` (findings) and `2` (could not run) both fail. Adding `1` turns the gate back into a report; adding `2` makes a broken harness look like a clean pull request. |
| `<timeout>` | `180000` | FR-013. Verified (R-002) to kill with exit `143`, which is outside `successCodes`, so the build fails and the log names the timeout. |
| `<useMavenLogger>` | `false` | **Pin explicitly, with a comment, even though it is the default.** Verified (R-001): the default streams stdout raw, which is why `::error` lines reach column 0 and GitHub renders them. Flipping it to `true` prefixes every line `[INFO] `, silently killing every annotation while the build stays green and the job summary still looks fine. |

### Guarantees this invocation must uphold

- **Findings fail the check** (FR-010) — exit `1` is not a success code, so a violation on a line the pull request wrote cannot merge.
- **A harness that could not run also fails it** (FR-011) — exit `2` and `143` likewise. Separate requirement, same consequence; keep them separate in the reasoning even though the configuration cannot distinguish them today.
- **Nothing runs on trunk or nightly** (FR-006) — the diff is empty there.
- **No retry or classification logic of our own** (FR-012) — whatever the harness reports reaches
  the build unaltered. Its own three-step base-ref recovery is the only recovery.
- **No workflow file is touched** (FR-005). `cicd_comp_test-phase.yml` already fetches `origin/main`
  for `-Pvalidate` jobs and `.github/filters.yaml` already gates the job on `core-web/**`.

### Verification

Before the change, both come back empty. After it, both must match:

```bash
git grep -n "strict-gate" -- core-web/pom.xml
```

A green build is **not** evidence the gate ran — see [../research.md](../research.md) R-003. Confirm
the gate's own output is present.

## What this invocation may not do

- Add a dependency, to Maven or to npm.
- Change what the harness *decides* — what it examines, what counts as a violation, how the
  comparison is made (FR-017). The elapsed-time line (FR-018) is output, not a decision, and is the
  only permitted edit.
- Register the harness as an Nx project. That would place it inside the graph it measures.
- Add any local git hook. Commit-time and push-time checks are both out of scope for this change;
  `core-web/lint-staged.config.mjs` and `core-web/.husky/` are untouched.
