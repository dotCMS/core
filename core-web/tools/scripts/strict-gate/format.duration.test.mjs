/**
 * The job summary must state how long the run took, over how much diff (issue #37536, FR-018).
 *
 * Why this is worth pinning: the gate blocks, so its cost is on the critical path of every
 * frontend merge. The harness already measures every run — `durationMs.total` — and simply never
 * printed it, so the only way to know the real distribution was to time runs by hand from CI
 * logs, which is the class of chore that does not get done.
 *
 * Both the clean and the findings summary are covered on purpose: a duration emitted only when
 * there are findings would sample the fast and slow cases unevenly, and it is the tail that
 * matters.
 *
 * The count is of DISTINCT changed paths. Two of the cases below exist because the obvious
 * implementation — summing `targets[].files.length` — is wrong in two ways at once: it
 * double-counts a file claimed by two configs of one project, and it drops unmapped files
 * entirely.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { buildReport } from './lib/report.mjs';
import { formatMarkdown } from './lib/format.mjs';

const SHA_A = 'a'.repeat(40);
const SHA_B = 'b'.repeat(40);

const reportWith = ({ findings = [], durationMs, targets = [], unmapped = [] }) =>
    buildReport({
        base: SHA_A,
        head: SHA_B,
        flagSet: 'strict',
        granularity: 'line',
        targets,
        unmapped,
        findings,
        discarded: {
            byOrigin: { dependency: 0, untouched: 0, infrastructure: 0 },
            byLayer: { source: 0, template: 0 }
        },
        durationMs
    });

const target = (project, files) => ({
    project,
    root: `libs/${project}`,
    configPath: `libs/${project}/tsconfig.lib.json`,
    mode: 'typescript',
    files
});

const finding = {
    file: 'libs/utils/src/lib/dot-utils.ts',
    line: 270,
    column: 35,
    code: 'TS7006',
    message: "Parameter 'value' implicitly has an 'any' type.",
    origin: 'changed',
    layer: 'source'
};

test('the clean summary states the elapsed time', () => {
    const report = reportWith({
        durationMs: { total: 7088, typescript: 6900, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/dot-utils.ts'])]
    });

    const summary = formatMarkdown(report);

    assert.equal(report.exitCode, 0, 'guard: this fixture must be the clean case');
    assert.match(
        summary,
        /7\.1\s*s/,
        'a passing run must still report its duration — the tail is what the blocking decision turns on'
    );
});

test('the findings summary states the elapsed time', () => {
    const report = reportWith({
        findings: [finding],
        durationMs: { total: 12400, typescript: 12100, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/dot-utils.ts'])]
    });

    const summary = formatMarkdown(report);

    assert.equal(report.exitCode, 1, 'guard: this fixture must be the findings case');
    assert.match(summary, /12\.4\s*s/, 'the findings summary must report its duration too');
});

test('the duration is paired with the size of the diff that produced it', () => {
    const report = reportWith({
        durationMs: { total: 9000, typescript: 8800, templateAware: 0 },
        targets: [
            target('utils', ['libs/utils/src/lib/a.ts', 'libs/utils/src/lib/b.ts']),
            target('ui', ['libs/ui/src/lib/c.ts'])
        ]
    });

    const summary = formatMarkdown(report);

    // 3 files across 2 project configs. A duration without the diff size it came from is not
    // comparable across pull requests, which makes it useless for the measurement SC-005 wants.
    assert.match(summary, /\b3\b[^|\n]*file/i, 'the summary must state how many files were checked');
});

test('a file claimed by two configs of one project counts once', () => {
    // config-select claims a source under EVERY eligible config, so a lib/spec pair both holding
    // the same file produces two target entries for one changed file. Summing `files.length`
    // reports a one-file diff as two, which silently inflates the SC-005 evidence the cost line
    // exists to provide.
    const file = 'libs/utils/src/lib/dot-utils.ts';
    const report = reportWith({
        durationMs: { total: 5000, typescript: 4800, templateAware: 0 },
        targets: [
            { ...target('utils', [file]), configPath: 'libs/utils/tsconfig.lib.json' },
            { ...target('utils', [file]), configPath: 'libs/utils/tsconfig.spec.json' }
        ]
    });

    const summary = formatMarkdown(report);

    assert.match(summary, /\b1\b[^|\n]*file/i, 'one changed file, claimed twice, is still one file');
    assert.doesNotMatch(summary, /\b2\s*file/i);
});

test('a changed file no project claimed is counted, not dropped', () => {
    // An unmapped file was changed and NOT examined. Leaving it out of the count understates the
    // diff and, worse, hides that something went unchecked behind a passing run.
    const report = reportWith({
        durationMs: { total: 4000, typescript: 3900, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/a.ts'])],
        unmapped: [{ path: 'libs/orphan/src/b.ts', reason: 'no configuration includes this file' }]
    });

    const summary = formatMarkdown(report);

    assert.match(summary, /\b2\b[^|\n]*file/i, 'one mapped + one unmapped = two changed files');
});

test('unmapped files are named in the summary, not silently passed', () => {
    // The gate blocks now. A changed TypeScript file that no project compiles must not read as a
    // clean pass with nothing said about it.
    const report = reportWith({
        durationMs: { total: 4000, typescript: 3900, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/a.ts'])],
        unmapped: [{ path: 'libs/orphan/src/b.ts', reason: 'no configuration includes this file' }]
    });

    const summary = formatMarkdown(report);

    assert.equal(report.exitCode, 0, 'guard: this is the passing case');
    assert.match(summary, /libs\/orphan\/src\/b\.ts/, 'the unexamined file must be named');
    assert.match(summary, /unexamined|not examined|no project/i, 'and it must say it was not checked');
});

test('a sub-second run is not reported as 0s', () => {
    // The no-op case — a pull request touching no frontend file — costs ~0.3s. Rounding that to
    // "0s" would make the cheapest and most common case invisible in the evidence.
    const report = reportWith({ durationMs: { total: 331, typescript: 0, templateAware: 0 } });

    const summary = formatMarkdown(report);

    assert.doesNotMatch(summary, /\b0\.0\s*s\b/, 'sub-second runs must not round to zero');
    assert.match(summary, /0\.3\s*s/);
});
