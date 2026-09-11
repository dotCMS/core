/**
 * T014 — the diff-scoped filter. This is the spike's actual hypothesis in code form:
 * the dependency's errors do not need to be fixed, they need to stop counting.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { filterDiagnostics } from './lib/filter.mjs';

const changed = [
    { path: 'libs/mine/src/a.ts', status: 'M', kind: 'source', changedLines: [[10, 12]] },
    { path: 'libs/mine/src/b.ts', status: 'A', kind: 'source', changedLines: [[1, 5]] }
];

const diag = (file, line, code, layer = 'source') => ({
    file,
    line,
    column: 1,
    code,
    message: `${code} at ${file}:${line}`,
    layer
});

test('keeps diagnostics in changed files and marks them as changed', () => {
    const { findings } = filterDiagnostics({
        diagnostics: [diag('libs/mine/src/a.ts', 11, 'TS2345')],
        changedFiles: changed,
        granularity: 'file'
    });

    assert.equal(findings.length, 1);
    assert.equal(findings[0].origin, 'changed');
});

test('discards diagnostics from another project and counts them as dependency-origin', () => {
    const { findings, discarded } = filterDiagnostics({
        diagnostics: [
            diag('libs/dep/src/x.ts', 3, 'TS7006'),
            diag('libs/dep/src/y.ts', 9, 'TS18047'),
            diag('libs/mine/src/a.ts', 11, 'TS2345')
        ],
        changedFiles: changed,
        granularity: 'file'
    });

    assert.equal(findings.length, 1, 'only the changed file survives');
    assert.equal(discarded.byOrigin.dependency, 2);
});

test('discards diagnostics in untouched files of the same project', () => {
    const { findings, discarded } = filterDiagnostics({
        diagnostics: [diag('libs/mine/src/untouched.ts', 4, 'TS7006')],
        changedFiles: changed,
        granularity: 'file'
    });

    assert.equal(findings.length, 0);
    assert.equal(discarded.byOrigin.dependency + discarded.byOrigin.untouched, 1);
});

test('reports discarded counts on a PASSING run, not only on a failing one', () => {
    // A pass with no evidence is indistinguishable from the harness having checked nothing.
    // This count is what shows the filter produced the pass (SC-004).
    const { findings, discarded } = filterDiagnostics({
        diagnostics: [diag('libs/dep/src/x.ts', 3, 'TS7006')],
        changedFiles: changed,
        granularity: 'file'
    });

    assert.equal(findings.length, 0);
    assert.ok(discarded.byOrigin.dependency > 0, 'a passing run must still show what it discarded');
});

test('whole-file granularity keeps a diagnostic outside the changed lines', () => {
    const { findings } = filterDiagnostics({
        diagnostics: [diag('libs/mine/src/a.ts', 99, 'TS2345')],
        changedFiles: changed,
        granularity: 'file'
    });

    assert.equal(findings.length, 1, 'whole-file inherits the file’s existing debt by design');
});

test('line granularity discards a diagnostic outside the changed lines', () => {
    const { findings, discarded } = filterDiagnostics({
        diagnostics: [diag('libs/mine/src/a.ts', 99, 'TS2345')],
        changedFiles: changed,
        granularity: 'line'
    });

    assert.equal(findings.length, 0);
    assert.equal(discarded.byOrigin.untouched, 1);
});

test('line granularity keeps a diagnostic on a changed line', () => {
    const { findings } = filterDiagnostics({
        diagnostics: [diag('libs/mine/src/a.ts', 11, 'TS2345')],
        changedFiles: changed,
        granularity: 'line'
    });

    assert.equal(findings.length, 1);
});

test('counts discarded diagnostics separately by layer', () => {
    const { discarded } = filterDiagnostics({
        diagnostics: [
            diag('libs/dep/src/x.ts', 3, 'TS7006', 'source'),
            diag('libs/dep/src/x.component.html', 2, 'NG8002', 'template')
        ],
        changedFiles: changed,
        granularity: 'file'
    });

    assert.equal(discarded.byLayer.source, 1);
    assert.equal(discarded.byLayer.template, 1);
});

/* ── T028 (US2) ─────────────────────────────────────────────────────────────
 * A pass with no evidence is indistinguishable from the harness having checked nothing. These
 * assert the evidence is present and correctly attributed even when the gate is green.
 */

test('a passing run reports discarded counts in BOTH dimensions', () => {
    const { findings, discarded } = filterDiagnostics({
        diagnostics: [
            diag('libs/dep/src/x.ts', 3, 'TS7006', 'source'),
            diag('libs/dep/src/x.component.html', 2, 'NG8002', 'template'),
            diag('libs/mine/src/untouched.ts', 4, 'TS7030', 'source')
        ],
        changedFiles: changed,
        granularity: 'file'
    });

    assert.equal(findings.length, 0, 'this run must pass');
    assert.equal(discarded.byOrigin.dependency + discarded.byOrigin.untouched, 3);
    assert.equal(discarded.byLayer.source, 2);
    assert.equal(discarded.byLayer.template, 1);
});

test('every diagnostic is accounted for: findings + discarded equals the input', () => {
    const diagnostics = [
        diag('libs/mine/src/a.ts', 11, 'TS2345'),
        diag('libs/mine/src/a.ts', 99, 'TS2345'),
        diag('libs/dep/src/x.ts', 3, 'TS7006'),
        diag('libs/mine/src/untouched.ts', 4, 'TS7030')
    ];
    for (const granularity of ['file', 'line']) {
        const { findings, discarded } = filterDiagnostics({ diagnostics, changedFiles: changed, granularity });
        const total = findings.length + discarded.byOrigin.dependency + discarded.byOrigin.untouched;
        assert.equal(total, diagnostics.length, `${granularity}: a diagnostic was silently lost`);
    }
});

test('projectRoots, when supplied, classify untouched vs dependency exactly', () => {
    const { discarded } = filterDiagnostics({
        diagnostics: [
            diag('libs/mine/deep/nested/other.ts', 4, 'TS7030'),
            diag('libs/dep/src/x.ts', 3, 'TS7006')
        ],
        changedFiles: changed,
        granularity: 'file',
        projectRoots: ['libs/mine']
    });

    assert.equal(discarded.byOrigin.untouched, 1, 'same project, file the diff did not touch');
    assert.equal(discarded.byOrigin.dependency, 1, 'another project entirely');
});

/* ── Infrastructure diagnostics ─────────────────────────────────────────────
 * Adjudication of the corpus turned up one false positive: TS2307 "Cannot find module
 * '@openng/spectator/jest'" on a pre-registered clean pull request. It appears with plain `tsc`
 * too, with no flags forced — it is a module-resolution problem, not a strictness violation, and
 * it never will be one. A strictness gate that reports it is crying wolf.
 *
 * These are DISCARDED, not silently dropped: the count is reported like every other, because a
 * gate that hides what it ignored cannot be audited.
 */

test('module-resolution diagnostics are discarded as infrastructure, not reported', () => {
    const { findings, discarded } = filterDiagnostics({
        diagnostics: [
            diag('libs/mine/src/a.ts', 11, 'TS2307'),
            diag('libs/mine/src/a.ts', 11, 'TS2688'),
            diag('libs/mine/src/a.ts', 11, 'TS6053'),
            diag('libs/mine/src/a.ts', 11, 'TS2345')
        ],
        changedFiles: changed,
        granularity: 'file'
    });

    assert.deepEqual(findings.map((f) => f.code), ['TS2345'], 'only the strictness violation survives');
    assert.equal(discarded.byOrigin.infrastructure, 3);
});

test('an infrastructure diagnostic is discarded even on a changed line', () => {
    const { findings } = filterDiagnostics({
        diagnostics: [diag('libs/mine/src/b.ts', 3, 'TS2307')],
        changedFiles: changed,
        granularity: 'line'
    });
    assert.equal(findings.length, 0, 'a missing module is never this gate’s business');
});

test('infrastructure diagnostics are still counted in the layer totals', () => {
    const { discarded } = filterDiagnostics({
        diagnostics: [diag('libs/mine/src/a.ts', 11, 'TS2307')],
        changedFiles: changed,
        granularity: 'file'
    });
    assert.equal(discarded.byLayer.source, 1, 'discarded, but never invisible');
});

/* ── T039 (US3) — line granularity boundaries ───────────────────────────────
 * The adoption argument rests entirely on this: touching one line of a legacy file must not make
 * the author inherit the file's history. An off-by-one at either end of a span breaks that
 * promise quietly — no error, just a wrong number in the write-up.
 */

test('line granularity includes both endpoints of a span', () => {
    const files = [{ path: 'a.ts', status: 'M', kind: 'source', changedLines: [[10, 12]] }];
    const kept = (line) =>
        filterDiagnostics({
            diagnostics: [diag('a.ts', line, 'TS2345')],
            changedFiles: files,
            granularity: 'line'
        }).findings.length;

    assert.equal(kept(9), 0, 'one line before the span');
    assert.equal(kept(10), 1, 'first line of the span');
    assert.equal(kept(12), 1, 'last line of the span');
    assert.equal(kept(13), 0, 'one line after the span');
});

test('a file with no changed lines contributes nothing under line granularity', () => {
    // A pure rename: the file is in the diff, but the author wrote none of it.
    const files = [{ path: 'a.ts', status: 'R', kind: 'source', changedLines: [] }];
    const { findings, discarded } = filterDiagnostics({
        diagnostics: [diag('a.ts', 1, 'TS2345'), diag('a.ts', 500, 'TS7006')],
        changedFiles: files,
        granularity: 'line'
    });

    assert.equal(findings.length, 0, 'renaming a file must not make you own its debt');
    assert.equal(discarded.byOrigin.untouched, 2);
});

test('whole-file granularity is a strict superset of line granularity', () => {
    const files = [{ path: 'a.ts', status: 'M', kind: 'source', changedLines: [[10, 12]] }];
    const diagnostics = [diag('a.ts', 5, 'TS7006'), diag('a.ts', 11, 'TS2345'), diag('a.ts', 90, 'TS2531')];

    const byFile = filterDiagnostics({ diagnostics, changedFiles: files, granularity: 'file' }).findings;
    const byLine = filterDiagnostics({ diagnostics, changedFiles: files, granularity: 'line' }).findings;

    assert.equal(byFile.length, 3);
    assert.equal(byLine.length, 1);
    const fileKeys = new Set(byFile.map((f) => `${f.file}:${f.line}`));
    for (const f of byLine) assert.ok(fileKeys.has(`${f.file}:${f.line}`));
});

/**
 * Regression: an unrecognised granularity used to fall through the `=== 'line'` test and behave as
 * whole-file — reporting pre-existing debt on untouched lines while the report still echoed the
 * name it was given. A plural typo was enough. It must fail by name instead.
 */
test('an unknown granularity is rejected rather than treated as whole-file', () => {
    for (const granularity of ['lines', 'Line', 'per-line', '']) {
        assert.throws(
            () =>
                filterDiagnostics({
                    diagnostics: [diag('libs/mine/src/a.ts', 99, 'TS2345')],
                    changedFiles: changed,
                    granularity
                }),
            /unknown granularity/,
            `granularity '${granularity}' should be rejected`
        );
    }
});

test('the two supported granularities are still accepted', () => {
    for (const granularity of ['file', 'line']) {
        assert.doesNotThrow(() =>
            filterDiagnostics({ diagnostics: [], changedFiles: changed, granularity })
        );
    }
});
