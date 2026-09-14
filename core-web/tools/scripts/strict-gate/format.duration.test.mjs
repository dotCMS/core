/**
 * The job summary must state how long the run took (issue #37536, FR-025).
 *
 * Why this is worth pinning: shipping the gate non-blocking is justified entirely by the promise
 * to measure its real cost on real pull requests and revisit the blocking decision with data
 * (SC-005). The harness already measures every run — `durationMs.total` — and simply never
 * printed it, so the only way to honour that promise was to time runs by hand from CI logs. That
 * is the class of post-merge chore that does not get done, and the decision then gets retaken
 * with no more information than before.
 *
 * Both the clean and the findings summary are covered on purpose. A duration emitted only when
 * there are findings would sample the fast and slow cases unevenly, and it is the tail that the
 * blocking decision turns on.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { buildReport } from './lib/report.mjs';
import { formatMarkdown } from './lib/format.mjs';

const SHA_A = 'a'.repeat(40);
const SHA_B = 'b'.repeat(40);

const reportWith = ({ findings = [], durationMs, targets = [] }) =>
    buildReport({
        base: SHA_A,
        head: SHA_B,
        flagSet: 'strict',
        granularity: 'line',
        targets,
        unmapped: [],
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

test('a sub-second run is not reported as 0s', () => {
    // The no-op case — a pull request touching no frontend file — costs ~0.3s. Rounding that to
    // "0s" would make the cheapest and most common case invisible in the evidence.
    const report = reportWith({ durationMs: { total: 331, typescript: 0, templateAware: 0 } });

    const summary = formatMarkdown(report);

    assert.doesNotMatch(summary, /\b0\.0\s*s\b/, 'sub-second runs must not round to zero');
    assert.match(summary, /0\.3\s*s/);
});
