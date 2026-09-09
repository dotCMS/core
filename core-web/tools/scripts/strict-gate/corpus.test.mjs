/**
 * T029 — integrity of the replay corpus.
 *
 * The corpus is the spike's evidence base, so its one methodological rule is that every case's
 * expectation is fixed BEFORE the gate runs against it. Without that, the sample gets fitted to
 * the result and the false-positive rate measures nothing. These tests enforce the rule in code
 * rather than trusting whoever edits corpus.mjs to remember it.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { CORPUS, adjudicate, summarize } from './corpus.mjs';

test('every case carries a pre-registered expectation and a stated reason', () => {
    assert.ok(CORPUS.length >= 5, 'the corpus needs enough cases to say anything');
    for (const sample of CORPUS) {
        assert.ok(Number.isInteger(sample.pr), 'each case identifies a pull request');
        assert.ok(['clean', 'debt'].includes(sample.expectation), `#${sample.pr}: bad expectation`);
        assert.match(sample.rationale ?? '', /\S/, `#${sample.pr}: the label must justify itself`);
        // The rationale must be structural — derivable before the gate runs — not a gate result.
        assert.doesNotMatch(
            sample.rationale,
            /gate (found|reported)|after running/i,
            `#${sample.pr}: the label was derived from a run, which defeats pre-registration`
        );
    }
});

test('the corpus holds both clean and debt cases', () => {
    const clean = CORPUS.filter((s) => s.expectation === 'clean');
    const debt = CORPUS.filter((s) => s.expectation === 'debt');
    assert.ok(clean.length >= 2, `expected clean cases, got ${clean.length}`);
    assert.ok(debt.length >= 3, `expected debt cases, got ${debt.length}`);
});

test('a clean case reporting findings is counted as a false positive', () => {
    const sample = { pr: 1, expectation: 'clean', rationale: 'all projects meet the bar', knownFindings: [] };
    const report = { findings: [{ file: 'a.ts', line: 1, code: 'TS2345' }] };

    const verdict = adjudicate(sample, report);
    assert.equal(verdict.matchedExpectation, false);
    assert.equal(verdict.falsePositives.length, 1);
});

test('a debt case is judged against its known findings, and extras are flagged for adjudication', () => {
    const sample = {
        pr: 2,
        expectation: 'debt',
        rationale: 'touches a non-strict lib',
        knownFindings: [{ file: 'a.ts', line: 10, code: 'TS4111' }]
    };
    const report = {
        findings: [
            { file: 'a.ts', line: 10, code: 'TS4111' },
            { file: 'b.ts', line: 3, code: 'TS7030' }
        ]
    };

    const verdict = adjudicate(sample, report);
    assert.equal(verdict.matchedExpectation, true);
    assert.equal(verdict.unexpected.length, 1, 'an unexpected finding needs a human judgement');
    assert.equal(verdict.missed.length, 0);
});

test('a debt case that reports nothing is a miss, not a pass', () => {
    const sample = {
        pr: 3,
        expectation: 'debt',
        rationale: 'known violations',
        knownFindings: [{ file: 'a.ts', line: 10, code: 'TS4111' }]
    };
    const verdict = adjudicate(sample, { findings: [] });

    assert.equal(verdict.matchedExpectation, false);
    assert.equal(verdict.missed.length, 1);
});

test('summarize reports the false-positive rate with its sample size, never a bare percentage', () => {
    const summary = summarize([
        { sample: { pr: 1, expectation: 'clean' }, verdict: { falsePositives: [], matchedExpectation: true } },
        { sample: { pr: 2, expectation: 'clean' }, verdict: { falsePositives: [{}], matchedExpectation: false } }
    ]);

    assert.equal(summary.cleanCases, 2);
    assert.equal(summary.cleanCasesWithFindings, 1);
    assert.equal(summary.falsePositiveRate, 0.5);
    // A rate without its denominator invites being quoted as if it were a statistical claim.
    assert.equal(summary.sampleSize, 2);
    assert.match(summary.caveat, /not a statistical claim/i);
});

/* ── Granularity-qualified known findings ───────────────────────────────────
 * The anchor case reports five violations under whole-file granularity and three under
 * line-level: two of the five sit on pre-existing lines the pull request did not write. A single
 * flat list of known findings therefore reports a MISMATCH under one granularity or the other,
 * no matter which numbers it holds. The expectation has to name the granularity it belongs to.
 */

test('known findings can be qualified per granularity', () => {
    const sample = {
        pr: 1,
        expectation: 'debt',
        rationale: 'non-strict project',
        knownFindings: {
            file: [
                { file: 'a.ts', line: 10, code: 'TS4111' },
                { file: 'a.ts', line: 99, code: 'TS4111' }
            ],
            line: [{ file: 'a.ts', line: 10, code: 'TS4111' }]
        }
    };
    const report = { findings: [{ file: 'a.ts', line: 10, code: 'TS4111' }] };

    assert.equal(adjudicate(sample, report, 'line').matchedExpectation, true);
    assert.equal(adjudicate(sample, report, 'file').missed.length, 1, 'whole-file expects both');
});

test('a flat known-findings array still works for any granularity', () => {
    const sample = {
        pr: 2,
        expectation: 'debt',
        rationale: 'x',
        knownFindings: [{ file: 'a.ts', line: 10, code: 'TS4111' }]
    };
    const report = { findings: [{ file: 'a.ts', line: 10, code: 'TS4111' }] };
    assert.equal(adjudicate(sample, report, 'line').matchedExpectation, true);
    assert.equal(adjudicate(sample, report).matchedExpectation, true);
});

test('the anchor case declares both granularities', () => {
    const anchor = CORPUS.find((s) => s.pr === 37264);
    assert.ok(anchor.knownFindings.file?.length === 5, 'five under whole-file');
    assert.ok(anchor.knownFindings.line?.length === 3, 'three under line-level');
});
