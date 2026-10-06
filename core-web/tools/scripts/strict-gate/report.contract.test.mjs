/**
 * T015 — the report and exit-code contract.
 *
 * The follow-up task inherits these two interfaces, so they are pinned here rather than left to
 * whatever the implementation happens to emit. Contract: contracts/cli.md + report.schema.json.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { validateReport } from './lib/validate-report.mjs';
import { buildReport } from './lib/report.mjs';

const SHA_A = 'a'.repeat(40);
const SHA_B = 'b'.repeat(40);

const baseInput = {
    base: SHA_A,
    head: SHA_B,
    flagSet: 'strict',
    granularity: 'file',
    targets: [],
    unmapped: [],
    findings: [],
    discarded: {
        byOrigin: { dependency: 0, untouched: 0, infrastructure: 0 },
        byLayer: { source: 0, template: 0 }
    },
    durationMs: { total: 1, typescript: 1, templateAware: 0 }
};

const finding = {
    file: 'libs/mine/src/a.ts',
    line: 11,
    column: 3,
    code: 'TS2345',
    message: 'nope',
    origin: 'changed',
    layer: 'source'
};

test('an empty diff yields a valid report that passes', () => {
    const report = buildReport(baseInput);
    assert.deepEqual(validateReport(report), { valid: true, errors: [] });
    assert.equal(report.exitCode, 0);
    assert.deepEqual(report.findings, []);
});

test('exit code is non-zero if and only if there are findings', () => {
    const failing = buildReport({ ...baseInput, findings: [finding] });
    assert.notEqual(failing.exitCode, 0);
    assert.deepEqual(validateReport(failing), { valid: true, errors: [] });

    const passing = buildReport(baseInput);
    assert.equal(passing.exitCode, 0);
});

test('every surviving finding carries origin "changed"', () => {
    const report = buildReport({ ...baseInput, findings: [finding] });
    for (const f of report.findings) assert.equal(f.origin, 'changed');
});

test('deduplicates a diagnostic reported by two configurations', () => {
    // Real case: src/utils/index.ts in sdk-create-app reports TS7030 under both the lib and the
    // spec configuration. One defect, one finding.
    const report = buildReport({ ...baseInput, findings: [finding, { ...finding }] });
    assert.equal(report.findings.length, 1);
});

test('rejects a report whose exit code contradicts its findings', () => {
    const broken = { ...buildReport({ ...baseInput, findings: [finding] }), exitCode: 0 };
    const { valid, errors } = validateReport(broken);
    assert.equal(valid, false);
    assert.match(errors.join('\n'), /exitCode/);
});

test('records the toolchain the measurement was produced with', () => {
    const report = buildReport(baseInput);
    assert.ok(report.durationMs.total >= 0);
    assert.ok('typescript' in report.durationMs && 'templateAware' in report.durationMs);
});

/* ── T040 (US3) — the report must carry the settings that produced it ───────
 * Every number in findings.md is quoted alongside a flag set and a granularity. A report that
 * does not say which produced it cannot be compared with another one, and the decision matrix
 * SC-007 asks for is exactly a comparison across those two axes.
 */

test('the report echoes the flag set and granularity it ran under', () => {
    for (const flagSet of ['strict', 'null-checks', 'strict-max']) {
        for (const granularity of ['file', 'line']) {
            const report = buildReport({ ...baseInput, flagSet, granularity });
            assert.equal(report.flagSet, flagSet);
            assert.equal(report.granularity, granularity);
            assert.deepEqual(validateReport(report), { valid: true, errors: [] });
        }
    }
});

test('durationMs separates the two execution modes so their costs can be compared', () => {
    const report = buildReport({
        ...baseInput,
        durationMs: { total: 12000, typescript: 9000, templateAware: 3000 }
    });

    assert.equal(report.durationMs.typescript, 9000);
    assert.equal(report.durationMs.templateAware, 3000);
    assert.ok(report.durationMs.total >= report.durationMs.typescript);
});

test('the base and head recorded are the resolved SHAs, not the refs asked for', () => {
    // findings.md cites results by pull request; those must be traceable to exact commits, since
    // origin/main moves and a re-run months later has to reproduce the same numbers.
    const report = buildReport(baseInput);
    assert.match(report.base, /^[0-9a-f]{40}$/);
    assert.match(report.head, /^[0-9a-f]{40}$/);
});
