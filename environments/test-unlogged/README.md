# Disposable UNLOGGED CI experiment (#38427)

Implements the experiment proposed in PR #38428. Production code/schema is untouched.
Spec-Kit/TDD approval gates were explicitly waived for this hackathon draft.

## Run

Normal runs remain logged. Add `-Ptest-unlogged` to an integration suite or Postman batch:

```sh
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false \
  -Dit.test=MainSuite3a -Dit.test.forkcount=1 \
  -Dmaven.build.cache.enabled=false -Ptest-unlogged -Dcontext.name=unlogged-experiment
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false \
  -Dpostman.collections=template -Dmaven.build.cache.enabled=false \
  -Ptest-unlogged -Dcontext.name=unlogged-experiment
```

Use a unique `context.name` locally: the normal Maven harness removes/recreates its named
containers and volumes. Never use the context of an existing development environment.
Do not supply an external database. Conversion refuses unmarked databases; the opt-in profile
mounts an init script that marks only newly initialized disposable Docker databases.
The SQL also checks the database name and application-schema readiness.

JUnit 4 prepares in `MainBaseSuite` after application initialization and before constructing
class runners. Jupiter uses an auto-detected extension enabled only by this profile: its root
store shares preparation across classes, including remembered failures, and is recreated for
each suite's Jupiter engine. Postman prepares once after the server issues a token and before
processing any collection. Conversion failure prevents execution. Tables created later by tests
remain logged; there is no per-test conversion and no conversion back.

Conversion is a single atomic DO statement, child before parent, preserving constraints and
rows. Self-references and already-unlogged tables are supported. Cross-table FK cycles,
partitioning/inheritance, materialized views and foreign tables fail instead of weakening
constraints or touching other schemas. Extension-owned tables are excluded. The lock timeout
is 10 seconds and the preparation deadline is 120 seconds. A failed conversion rolls back all
rewrites. PostgreSQL crash recovery can truncate UNLOGGED data: discard such a run/database.

## Draft CI and comparison

This draft enables `unlogged-experiment` **only in the PR caller**. The existing test-matrix
workflow pairs every integration suite and Postman batch as `[logged]` / `[unlogged]`, on the
same checkout and runner class with identical test selections and retry settings. Both disable
the Maven build cache. Other callers default off. This doubles those jobs while the experiment
is enabled; disable the caller input to stop the extra CI cost.

Each paired job uploads normal XML reports and an `unlogged-benchmark-*` JSON artifact and
writes a step summary with actual completion/failure/error/skip counts, retry events, summed
JUnit XML elapsed time, and elapsed time across the Maven composite action (including setup,
conversion and artifact handling, but excluding runner queue/checkout). Missing/zero/all-skipped
execution is rejected. Postman's `completed` is its existing **request execution count**;
JUnit XML describes its assertion counts. Neither is directly comparable to integration counts.
Conversion duration/count/database are in the job log under `UNLOGGED experiment`; the caller
also logs preparation duration. Use GitHub job start/end times for full job elapsed time.

Compare **within each suite/batch**, never pool counts/timings across suites. Require equal
nonzero counts, no new failures, and report retries; early failures are not a speedup. Run at
least three complete matched samples per mode before comparing medians and percentage change
`100 * (unlogged_median - logged_median) / logged_median`. Retain run links and individual times;
exclude skipped/cached/crashed samples with reasons. Queue time and the extra concurrent jobs
can add noise; report that rather than attributing all variance to PostgreSQL.

**No speedup is established by this implementation.** CI measurements remain pending.

Local validation: a fresh opt-in PostgreSQL 18 integration environment converted 169 initialized
application tables (`conversion_ms=200`, total preparation 224 ms). The focused
`UnloggedSuiteExtensionTest` executed 3 tests, zero failures/errors/skips; Maven removed its
containers and volumes. This is correctness evidence, **not** a logged/unlogged timing comparison.

## Focused guardrails

```sh
bash environments/test-unlogged/self-test.sh  # isolated PG18 container, removed on exit
node --test dotcms-postman/prepare-unlogged.test.js environments/test-unlogged/matrix.test.cjs
python3 -m unittest discover -s environments/test-unlogged -p 'test_*.py'
```

The SQL check preserves rows, FK enforcement, self-FKs and quoted names; checks repeat
conversion and other-schema/extension isolation; rejects unmarked and empty targets; and
verifies cycle/lock-timeout failures roll back earlier rewrites; rejects unsupported table kinds
and non-owner execution. These run once in the matrix-setup
CI job, outside both measured jobs so neither mode pays extra guardrail time. `UnloggedSuiteExtensionTest`, registered in `Junit5Suite1` for framework compatibility,
checks shared/independent suite roots, sticky setup failures and disabled preparation.
