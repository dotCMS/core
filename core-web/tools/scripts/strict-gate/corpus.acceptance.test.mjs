/**
 * T016 — SC-001, the primary acceptance case, replayed against real history.
 *
 * ── Correction to the issue's framing, verified before this test was written ──
 * Issue #37401 names "PR #37262" with "3 strict errors: src/index.ts (TS4111) and
 * src/utils/readiness.spec.ts x2 (TS2345)". Three things about that are wrong:
 *
 *   1. #37262 is an ISSUE, not a pull request. The pull request that merged the work is #37264,
 *      merge commit 788795e915.
 *   2. TS4111 is NOT a `--strict` error. `noPropertyAccessFromIndexSignature` is not among the
 *      flags `--strict` enables (verified against ts.optionDeclarations). It only appears under
 *      the repo's own strict convention, which the 22 opted-in projects all declare.
 *   3. The real count under that convention is FIVE, not three — and there are two TS4111, not one.
 *
 * Under bare `--strict` the case yields 2 findings; under the repo convention, 5. That gap is
 * itself a spike result: it lands the flag-set decision (FR-007) with evidence.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { workspaceRoot } from './lib/resolve-tools.mjs';
import { checkTypeScript } from './lib/check-ts.mjs';
import { CORPUS, TEMPLATE_CASES } from './corpus.mjs';
import { INFRASTRUCTURE_CODES } from './lib/filter.mjs';
import { resolveChangedFiles } from './lib/changed-files.mjs';
import { runGate } from './run.mjs';
import { resolveMergeRange } from './replay.mjs';

const repoRoot = path.resolve(workspaceRoot, '..');


/**
 * One gate run per (pull request, flag set, granularity), shared across every test in this file.
 * Without it the suite replays the whole corpus once per assertion — minutes of wall clock spent
 * recomputing identical results, which makes people stop running it.
 */
const runCache = new Map();
async function gateRunTemplates(pr, flagSet = 'strict', granularity = 'line') {
    return gateRun(pr, flagSet, granularity, true);
}

async function gateRun(pr, flagSet = 'strict', granularity = 'line', templates = false) {
    const key = `${pr}|${flagSet}|${granularity}|${templates}`;
    if (!runCache.has(key)) {
        runCache.set(
            key,
            (async () => {
                const { base, head } = await resolveMergeRange({ repoDir: repoRoot, pr });
                return runGate({ repoDir: repoRoot, base, head, flagSet, granularity, templates });
            })()
        );
    }
    return runCache.get(key);
}

/** PR #37264 — "fix(create-app): design contracts for local Docker start failure..." (#37262). */
const ACCEPTANCE_PR = 37264;

/** Verified with `tsc` against the merged tree before this test existed. */
const EXPECTED_UNDER_REPO_STRICT = [
    { file: 'core-web/libs/sdk/create-app/src/index.ts', line: 294, code: 'TS4111' },
    { file: 'core-web/libs/sdk/create-app/src/index.ts', line: 515, code: 'TS4111' },
    { file: 'core-web/libs/sdk/create-app/src/utils/index.ts', line: 41, code: 'TS7030' },
    { file: 'core-web/libs/sdk/create-app/src/utils/readiness.spec.ts', line: 263, code: 'TS2345' },
    { file: 'core-web/libs/sdk/create-app/src/utils/readiness.spec.ts', line: 271, code: 'TS2345' }
];

test('SC-001: the acceptance pull request is flagged, with every known violation reported', async () => {
    const { base, head } = await resolveMergeRange({ repoDir: repoRoot, pr: ACCEPTANCE_PR });

    const report = await runGate({
        repoDir: repoRoot,
        base,
        head,
        flagSet: 'strict',
        granularity: 'file'
    });

    assert.notEqual(report.exitCode, 0, 'a pull request that landed strict debt must fail the gate');

    const actual = report.findings.map((f) => `${f.file}:${f.line}:${f.code}`).sort();
    const expected = EXPECTED_UNDER_REPO_STRICT.map((f) => `${f.file}:${f.line}:${f.code}`).sort();
    assert.deepEqual(actual, expected);
});

test('SC-001: the spec-file violations are found, which a lib-first heuristic would miss', async () => {
    const report = await gateRun(ACCEPTANCE_PR, 'strict', 'file');

    const specFindings = report.findings.filter((f) => f.file.endsWith('readiness.spec.ts'));
    assert.equal(specFindings.length, 2, 'both TS2345 in the spec file must be reported');
});

test('the acceptance case leaks nothing: every finding is in a changed file', async () => {
    const report = await gateRun(ACCEPTANCE_PR, 'strict', 'file');

    for (const finding of report.findings) assert.equal(finding.origin, 'changed');
    assert.equal(report.unmapped.length, 0, 'every changed file must map to a project');
});

test('SC-004: a dependency-heavy project passes BECAUSE of the filter', async () => {
    // sdk-create-app is a leaf package — its program pulls in no workspace sources, so it discards
    // nothing and cannot evidence SC-004. The claim needs a project shaped like the one the issue
    // measured: dot-locales/portlet checks 8 files of its own and 387 from six dependency libs.
    const configPath = path.join(workspaceRoot, 'libs/portlets/dot-locales/portlet/tsconfig.lib.json');
    const { diagnostics } = await checkTypeScript({ configPath, flagSet: 'strict' });

    const own = diagnostics.filter((d) => d.file.includes('/libs/portlets/dot-locales/'));
    const fromDependencies = diagnostics.length - own.length;

    assert.ok(diagnostics.length > 0, 'the fixture premise: this program does report errors');
    assert.ok(
        fromDependencies > own.length * 10,
        `expected dependency errors to dominate; got ${fromDependencies} vs ${own.length} own`
    );
});

test('the narrow flag set under-reports this case — the flag-set decision, measured', async () => {
    const { base, head } = await resolveMergeRange({ repoDir: repoRoot, pr: ACCEPTANCE_PR });
    const narrow = await runGate({ repoDir: repoRoot, base, head, flagSet: 'null-checks', granularity: 'file' });

    assert.ok(
        narrow.findings.length < EXPECTED_UNDER_REPO_STRICT.length,
        'null-checks is expected to miss the index-signature and code-path violations'
    );
});

/* ── T030 (US2) — SC-002: the gate does not cry wolf ────────────────────────
 * Structural reality of this workspace, measured across 42 recent frontend pull requests:
 * only 2 touch exclusively projects that already meet the gate's bar, 1 touches only
 * strict-without-the-extras projects, and 39 (93%) touch at least one non-strict project.
 * The clean cases are therefore few by nature, not by cherry-picking — which is itself the
 * strongest argument for a diff-scoped filter, since waiting for opt-in covers 7% of pull requests.
 */

test('SC-002 as originally specified is REFUTED, and the refutation is the finding', async () => {
    // The spec asked for clean pull requests to produce zero findings. Both pre-registered clean
    // cases produce findings, and adjudication showed every one is REAL (findings.md §3).
    //
    // The pre-registration rule assumed "declares strict: true" implies "is strict-clean". It does
    // not: the typecheck target exists on 3 of 57 projects, so a project can carry the strictest
    // configuration in the workspace and accumulate errors indefinitely with nothing to notice.
    // This test pins the refutation so nobody later "fixes" it back into a false expectation.
    const clean = CORPUS.filter((s) => s.expectation === 'clean');
    let casesWithFindings = 0;

    for (const sample of clean) {
        const report = await gateRun(sample.pr, 'strict', 'line');
        if (report.findings.length > 0) casesWithFindings += 1;
    }

    assert.equal(
        casesWithFindings,
        clean.length,
        'if a structurally clean pull request ever DOES pass, revisit findings.md §4 — the ' +
            'workspace changed and the rule may now hold'
    );
});

test('SC-002 restated: the gate does not cry wolf — no finding is an infrastructure diagnostic', async () => {
    // The measurable precision guarantee, and the one that actually matters: a reported finding is
    // never a module-resolution or missing-file error dressed up as strict debt.
    for (const sample of CORPUS) {
        const report = await gateRun(sample.pr, 'strict', 'line');

        for (const finding of report.findings) {
            assert.ok(
                !INFRASTRUCTURE_CODES.has(finding.code),
                `#${sample.pr} reported ${finding.code} at ${finding.file}:${finding.line} — ` +
                    'that is broken tooling, not strict debt'
            );
        }
    }
});

test('every reported finding sits on a line its pull request wrote', async () => {
    // The precision property that replaces the refuted SC-002: under line granularity the gate
    // may only blame code the author actually touched. This is what keeps it from making whoever
    // edits a legacy file inherit that file's history.
    for (const sample of CORPUS) {
        const report = await gateRun(sample.pr, 'strict', 'line');
        const { files } = await resolveChangedFiles({
            repoDir: repoRoot,
            base: report.base,
            head: report.head
        });
        const spans = new Map(files.map((f) => [f.path, f.changedLines]));

        for (const finding of report.findings) {
            const ranges = spans.get(finding.file) ?? [];
            assert.ok(
                ranges.some(([a, b]) => finding.line >= a && finding.line <= b),
                `#${sample.pr}: ${finding.file}:${finding.line} is not on a changed line`
            );
        }
    }
});

test('SC-005: a pull request touching 1-3 projects completes within budget', async () => {
    const sample = CORPUS.find((s) => s.expectation === 'clean');
    const report = await gateRun(sample.pr, 'strict', 'line');

    assert.ok(
        report.durationMs.total <= 10_000,
        `budget is 10s (ADR-0013 protects frontend merge time); took ${Math.round(report.durationMs.total)}ms`
    );
});

/* ── T041 (US3) — runtime, measured against ADR-0013's cost model ───────────
 * SC-005 set a 10s budget to protect what ADR-0013 bought: frontend merge time cut from ~45min
 * to ~15min. Measured, the gate does NOT meet it universally — two of five corpus cases overrun.
 * These tests pin what was measured so a regression is visible, rather than asserting a budget
 * the implementation is known not to hold. The overruns are reported in findings.md §5, not
 * hidden behind a test that happens to pick a fast case.
 */

test('SC-005: small-program projects meet the 10s budget', async () => {
    // dot-auth: one project, modest dependency closure. This is the shape the budget was set for.
    const report = await gateRun(37405);
    assert.ok(
        report.durationMs.total <= 10_000,
        `expected <=10s for a single small project; took ${Math.round(report.durationMs.total)}ms`
    );
});

test('SC-005 is NOT met for projects with a large dependency closure — measured, not assumed', async () => {
    // libs/ui and libs/edit-content pull in thousands of dependency source files that are compiled
    // only to be discarded. Pinned so that if an optimisation later brings these under budget, this
    // test fails and findings.md §5 gets corrected instead of quietly going stale.
    for (const pr of [37415, 37372]) {
        const report = await gateRun(pr);
        assert.ok(
            report.durationMs.total > 10_000,
            `#${pr} now completes in ${Math.round(report.durationMs.total)}ms — under budget. ` +
                'Update findings.md §5: the runtime finding has changed.'
        );
    }
});

test('the cost is dominated by diagnostics computed only to be discarded', async () => {
    // The optimisation lead, evidenced: the slow cases are exactly the ones discarding thousands.
    const slow = await gateRun(37415);
    const fast = await gateRun(37405);
    const discarded = (r) => r.discarded.byOrigin.dependency + r.discarded.byOrigin.untouched;

    assert.ok(discarded(slow) > discarded(fast) * 2, 'slow runs discard far more than fast ones');
    assert.ok(slow.durationMs.total > fast.durationMs.total);
});

test('the narrow flag set is cheaper or comparable, and strictly less sensitive', async () => {
    const full = await gateRun(37415, 'strict');
    const narrow = await gateRun(37415, 'null-checks');

    assert.ok(narrow.findings.length <= full.findings.length, 'the narrow set cannot find more');
    const fullKeys = new Set(full.findings.map((f) => `${f.file}:${f.line}:${f.code}`));
    for (const f of narrow.findings) {
        assert.ok(fullKeys.has(`${f.file}:${f.line}:${f.code}`), `${f.code} appeared only under null-checks`);
    }
});

/* ── T054 (US4) — SC-011 / SC-012 / SC-013 ──────────────────────────────────
 * The template arm against real history: an application that switched template strictness OFF,
 * and a pull request that changed one of its templates.
 */

test('SC-011: template strictness is in force on an application that disables it', async () => {
    const sample = TEMPLATE_CASES[0];
    const report = await gateRunTemplates(sample.pr);

    const templateTargets = report.targets.filter((t) => t.mode === 'template-aware');
    assert.ok(templateTargets.length > 0, 'at least one project must have run template-aware');
});

test('SC-012: the application’s pre-existing template debt is discarded, and counted', async () => {
    const sample = TEMPLATE_CASES[0];
    const report = await gateRunTemplates(sample.pr);

    // dotcms-ui carries TODO(#35930) precisely because it has accumulated template errors. If the
    // gate reported them all, it would be unusable; if it counted none, the filter did nothing.
    assert.ok(
        report.discarded.byLayer.template > 0,
        'the application’s existing template debt must be discarded, not reported'
    );
    for (const finding of report.findings) {
        assert.equal(finding.origin, 'changed');
    }
});

test('SC-013: the template arm’s cost is measured separately from the TypeScript arm’s', async () => {
    const sample = TEMPLATE_CASES[0];
    const withTemplates = await gateRunTemplates(sample.pr);
    const withoutTemplates = await gateRun(sample.pr);

    assert.ok(withTemplates.durationMs.templateAware > 0, 'the template mode must report its own cost');
    // No budget is asserted: the measurement IS the deliverable. Inventing a threshold here would
    // prejudge the go/no-go this case exists to inform.
    assert.ok(withoutTemplates.durationMs.templateAware === 0);
});

test('a template-only pull request is not treated as "nothing changed"', async () => {
    const sample = TEMPLATE_CASES[0];
    const report = await gateRunTemplates(sample.pr);
    assert.ok(report.targets.length > 0, 'a template-only diff must still resolve a project to check');
});
