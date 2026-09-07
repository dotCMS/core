# strict-gate — spike harness (issue #37401)

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

**Recommendation: ship non-blocking first.** Precision is better than the spec asked for; runtime
is the open issue (8.4–9.4 s average, 12 s at the tail, against a 10 s budget). The cost is entirely
dependency-closure recompilation and has untried optimisations. Templates are a **no-go for
blocking** for now — 2.2× the compiler time on the largest application.

Full measurements, adjudication of every finding, and the go/no-go:
`specs/37401-diff-scoped-strict-typecheck-gate/findings.md` and issue #37401.

## Status

Pending the follow-up task's decision to **promote** this into the real gate (durable script +
CI hook in `core-web/pom.xml` + local hook in `lint-staged.config.mjs`) or **delete** it.
