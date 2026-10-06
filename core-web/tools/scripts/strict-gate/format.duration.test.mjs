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
 *
 * **Assert on the extracted cost line, by equality — not with a regex over the whole summary.**
 * The first version of this suite used whole-document regexes and four separate mutations of the
 * implementation survived it, including one where the assertion was satisfied by the unmapped
 * note rather than the cost line, because both render a number followed by the word "file".
 * Equality on one extracted line is what the sibling suites do (`hunks.test.mjs`,
 * `report.contract.test.mjs`), and it is what catches those.
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

const unmappedEntry = {
    path: 'libs/orphan/src/b.ts',
    reason: 'no configuration includes this file'
};

/** The one line under test, isolated so an assertion cannot be satisfied by some other line. */
const costLineOf = (summary) => {
    const line = summary.split('\n').find((l) => l.startsWith('_') && l.endsWith('s._'));
    assert.ok(line, 'the summary must carry a cost line');
    return line;
};

test('the clean summary states the elapsed time and the diff size', () => {
    const report = reportWith({
        durationMs: { total: 7088, typescript: 6900, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/dot-utils.ts'])]
    });

    assert.equal(report.exitCode, 0, 'guard: this fixture must be the clean case');
    assert.equal(
        costLineOf(formatMarkdown(report)),
        '_1 changed file(s) across 1 project config(s), 7.1s._'
    );
});

test('the findings summary states them too', () => {
    const report = reportWith({
        findings: [finding],
        durationMs: { total: 12400, typescript: 12100, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/dot-utils.ts'])]
    });

    assert.equal(report.exitCode, 1, 'guard: this fixture must be the findings case');
    assert.equal(
        costLineOf(formatMarkdown(report)),
        '_1 changed file(s) across 1 project config(s), 12.4s._'
    );
});

test('files are counted across every target', () => {
    const report = reportWith({
        durationMs: { total: 9000, typescript: 8800, templateAware: 0 },
        targets: [
            target('utils', ['libs/utils/src/lib/a.ts', 'libs/utils/src/lib/b.ts']),
            target('ui', ['libs/ui/src/lib/c.ts'])
        ]
    });

    assert.equal(
        costLineOf(formatMarkdown(report)),
        '_3 changed file(s) across 2 project config(s), 9.0s._'
    );
});

test('a file claimed by two configs of one project counts once', () => {
    // config-select claims a source under EVERY eligible config, so a lib/spec pair both holding
    // the same file produces two target entries for one changed file. Summing `files.length`
    // reports a one-file diff as two, which silently inflates the evidence this line exists for.
    const file = 'libs/utils/src/lib/dot-utils.ts';
    const report = reportWith({
        durationMs: { total: 5000, typescript: 4800, templateAware: 0 },
        targets: [
            { ...target('utils', [file]), configPath: 'libs/utils/tsconfig.lib.json' },
            { ...target('utils', [file]), configPath: 'libs/utils/tsconfig.spec.json' }
        ]
    });

    assert.equal(
        costLineOf(formatMarkdown(report)),
        '_1 changed file(s) across 2 project config(s), 5.0s._',
        'one changed file, claimed twice, is still one file'
    );
});

test('a changed file no project claimed is counted, not dropped', () => {
    // An unmapped file was changed and NOT examined. Leaving it out understates the diff this
    // duration came from.
    const report = reportWith({
        durationMs: { total: 4000, typescript: 3900, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/a.ts'])],
        unmapped: [unmappedEntry]
    });

    assert.equal(
        costLineOf(formatMarkdown(report)),
        '_2 changed file(s) across 1 project config(s), 4.0s._',
        'one mapped + one unmapped = two changed files'
    );
});

test('unmapped files are named on a passing run', () => {
    // The gate blocks now. A changed TypeScript file that no project compiles must not read as a
    // clean pass with nothing said about it.
    const report = reportWith({
        durationMs: { total: 4000, typescript: 3900, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/a.ts'])],
        unmapped: [unmappedEntry]
    });

    const summary = formatMarkdown(report);

    assert.equal(report.exitCode, 0, 'guard: this is the passing case');
    assert.match(summary, /libs\/orphan\/src\/b\.ts/, 'the unexamined file must be named');
    assert.match(summary, /not examined/i, 'and it must say it was not checked');
});

test('unmapped files are named on a failing run too', () => {
    // The branch an author actually reads. Covered separately because the first version of this
    // suite exercised only the passing branch, which left the note removable from the findings
    // branch with every test still green.
    const report = reportWith({
        findings: [finding],
        durationMs: { total: 8000, typescript: 7900, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/dot-utils.ts'])],
        unmapped: [unmappedEntry]
    });

    const summary = formatMarkdown(report);

    assert.equal(report.exitCode, 1, 'guard: this is the findings case');
    assert.match(summary, /libs\/orphan\/src\/b\.ts/, 'the unexamined file must be named here too');
    assert.match(summary, /not examined/i);
});

test('the findings table is separated from what precedes it', () => {
    // A Markdown table renders as a table only when a blank line precedes it. Pinned because the
    // unmapped note is inserted immediately above it, and losing that separator degrades the
    // summary to a row of pipes without failing anything else.
    const report = reportWith({
        findings: [finding],
        durationMs: { total: 8000, typescript: 7900, templateAware: 0 },
        targets: [target('utils', ['libs/utils/src/lib/dot-utils.ts'])],
        unmapped: [unmappedEntry]
    });

    const lines = formatMarkdown(report).split('\n');
    const header = lines.findIndex((l) => l.startsWith('| File |'));
    assert.ok(header > 0, 'the findings table must be present');
    assert.equal(lines[header - 1], '', 'a blank line must precede the table');
});

test('a sub-second run keeps its precision instead of rounding to zero', () => {
    // The real boundary, not a value comfortably clear of it: `toFixed(1)` renders anything under
    // 50ms as "0.0s". The measured no-op path is ~175ms, within a factor of four of that cliff.
    const report = reportWith({ durationMs: { total: 40, typescript: 0, templateAware: 0 } });

    assert.equal(
        costLineOf(formatMarkdown(report)),
        '_0 changed file(s) across 0 project config(s), 40ms._',
        'the cheapest and most common outcome must not be invisible in the evidence'
    );
});

test('a run at or over a second renders in seconds', () => {
    const report = reportWith({ durationMs: { total: 1000, typescript: 900, templateAware: 0 } });

    assert.equal(
        costLineOf(formatMarkdown(report)),
        '_0 changed file(s) across 0 project config(s), 1.0s._'
    );
});

test('a malformed duration renders as unknown, not NaN', () => {
    // `formatMarkdown` is exported, and buildReport's default only fires on `undefined`, so a
    // partial object gets through. A plausible-looking wrong number is the failure mode this file
    // exists to avoid.
    const report = reportWith({ durationMs: {} });

    assert.equal(
        costLineOf(formatMarkdown(report)),
        '_0 changed file(s) across 0 project config(s), ?s._'
    );
});
