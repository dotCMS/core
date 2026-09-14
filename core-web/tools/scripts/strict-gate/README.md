# strict-gate — spike harness (issue #37401)

> **Temporary by design — delete this when #37198 merges.**
> This gate exists only while `core-web/tsconfig.base.json` is non-strict. Once the
> workspace-wide strict migration lands, every file in this directory comes out.
> Procedure, inventory and preconditions:
> [`specs/37401-diff-scoped-strict-typecheck-gate/DECOMMISSION.md`](../../../../specs/37401-diff-scoped-strict-typecheck-gate/DECOMMISSION.md)

**This is spike output, not production tooling.** It exists to answer one question:

> Can a diff-scoped strict typecheck block new non-strict TypeScript from landing on `main`,
> without requiring the dependency libraries to be strict first?

It runs each project's existing configuration with strictness forced on, then discards every
diagnostic whose file is not part of the pull request's diff. If that works, `main` stops
accumulating strict debt today, independently of when the workspace-wide strict PR (#37198)
merges.

Spec, plan and decisions: `specs/37401-diff-scoped-strict-typecheck-gate/`.

## Running it

```bash
cd core-web
nvm use                       # Node pinned in .nvmrc
node tools/scripts/strict-gate/run.mjs --base origin/main --head HEAD
node --test --test-concurrency=1 'tools/scripts/strict-gate/*.test.mjs'
```

Full command contract: `specs/37401-diff-scoped-strict-typecheck-gate/contracts/cli.md`.

**Run the tests with `--test-concurrency=1`.** `node --test` runs files in parallel by default, and
each of these spins up TypeScript or Angular programs over the real workspace. Under that pressure
a heavy acceptance case can time out and report a failure that does not reproduce in isolation —
93/93 pass serially in ~120s. Left as a flag rather than papered over, because a suite that fails
intermittently is a suite people stop trusting.

## Guarantees

- Writes nothing into the working tree. Version-controlled files stay byte-identical, including
  after an interrupted run — strictness is forced in memory, never through a temporary config.
- Adds no dependency. TypeScript, the Angular compiler and Nx come from the workspace; tests use
  `node:test`, built into Node.
- Not an Nx project. Registering one would put the harness into the project graph it measures.
- Every child process is invoked with an argument array, never a shell string. Refs and paths
  come from pull-request metadata and are untrusted input.

## Result

**The mechanism works.** On a representative portlet, 217 of 219 diagnostics are discarded (99.1 %)
and only the 2 belonging to the project itself survive. Across a 5-pull-request corpus the gate
reported 11 findings, **all 11 real**, every one on a line its pull request wrote — 0 false
positives after excluding three module-resolution codes that are never strictness violations.

**Recommended invocation:**

```bash
node tools/scripts/strict-gate/run.mjs \
    --base origin/main --head HEAD \
    --flags strict --granularity line --scope core-web --format github
```

- `--flags strict` — the repo convention (`strict` + `noPropertyAccessFromIndexSignature`,
  `noImplicitOverride`, `noImplicitReturns`, `noFallthroughCasesInSwitch`), the same yardstick as
  `tsconfig.base.json` on the strict-mode branch. At line granularity it costs **one** extra
  finding over the narrow set across the whole corpus.
- `--granularity line` — whole-file makes an author inherit 83 % of what it reports from lines they
  did not write. New files are unaffected: every line of an added file is a changed line.

**The spike recommended shipping non-blocking first** — precision was better than the spec asked
for, but runtime was the open question (8.4–9.4 s average, 12 s at the tail, against a 10 s budget),
and the cost is entirely dependency-closure recompilation with untried optimisations. *(Superseded
by #37536: the gate ships blocking. The runtime question is unchanged and now matters more, not
less — see Status.)* Templates remain a **no-go** either way: 2.2× the compiler time on the largest
application.

Full measurements, adjudication of every finding, and the go/no-go:
`specs/37401-diff-scoped-strict-typecheck-gate/findings.md` and issue #37401.

## Where this runs

**Wired and live since #37536**: a `strict-gate` execution in `core-web/pom.xml`, inside the
`validate` profile beside `lint-test` and `format-test`.

**It blocks.** A new strict-mode violation on a line your pull request wrote fails the check, and
the change does not merge until it is fixed. You get the violation annotated inline on the diff,
plus a job summary — so the fix is usually a type annotation on the line the annotation points at.
Read the scope note in the output before you touch anything else: only the lines you changed are
checked, and diagnostics from dependencies and untouched code were discarded on purpose.

It runs in two places, doing different jobs:

| Event | Role |
|---|---|
| `pull_request` | Where you read it. Annotations render inline on the diff. |
| `merge_group` | Where it is enforced. `main` declares no required status checks, so a red check on a pull request does not by itself stop a merge; a job failing in the merge queue ejects the pull request, and that does. |

Trunk and nightly runs skip it — `HEAD` equals `origin/main` there, so the diff is empty.

Nothing runs it on your machine. To get the answer before pushing, run it yourself with the
command under "Running it" above.

There is **no escape hatch**. If you have a change that legitimately must add a violation, it
cannot merge until the violation is fixed or the gate is turned off repository-wide. That gap is
known and accepted; if you hit it, say so on #37536 rather than working around it quietly.

Two things worth knowing before you change it:

- **Two activation profiles, not one.** `strict-gate-pull-request` and `strict-gate-merge-queue`
  each flip `skip.strict.gate` on their own `GITHUB_EVENT_NAME`. Maven property activation has no
  OR, so both are needed — and dropping either one breaks a different half: without the first
  nobody sees the annotations, without the second nothing is enforced.
- **`successCodes` lists `0` and nothing else.** `1` (findings) and `2` (the harness could not
  run) both fail, and both must: a gate nobody has to obey is a report, and a gate that reports
  "clean" without having looked is worse than no gate, because it is indistinguishable from a
  clean pull request. Do not add codes here to get a build through.

**Run the tests with `--test-concurrency=1`.** `corpus.acceptance.test.mjs` additionally needs
network access to the GitHub API (it resolves the corpus pull requests through `gh`) and carries
machine-dependent timing assertions; it goes red offline or on a slow machine while every other
file stays green.

## Status

Live and blocking. The open question is cost, not correctness: the spike measured 8.4–9.4 s
average with a 12 s tail against a 10 s budget, on a synthetic corpus. That cost now sits on the
critical path of every frontend merge, so every run prints its own elapsed time in the job summary
— collect it on #37536, and if the real distribution is worse than the corpus suggested, the
conversation is about making the harness faster, not about switching the gate off.

Either way the end state is the same: **#37198 merging retires this gate.** Promotion only changes
how much there is to remove — see §3.3 of
[`DECOMMISSION.md`](../../../../specs/37401-diff-scoped-strict-typecheck-gate/DECOMMISSION.md).
Note the precondition in §2: #37198 makes the baseline strict but adds nothing that *runs* a
type-check, so removal should follow a replacement, not precede one.
