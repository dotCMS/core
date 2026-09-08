/**
 * T038 — parsing changed line ranges from a zero-context diff.
 *
 * This is what makes line-level granularity possible, and getting it wrong is silent in both
 * directions: too-wide ranges make the gate blame untouched code, too-narrow ones make it miss
 * real violations. Neither shows up as an error, only as a wrong number in the write-up.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { parseHunks } from './lib/hunks.mjs';

test('a single-line change yields a one-line span', () => {
    assert.deepEqual(parseHunks('@@ -2 +2 @@\n-old\n+new\n'), [[2, 2]]);
});

test('an explicit count yields an inclusive span', () => {
    assert.deepEqual(parseHunks('@@ -10,0 +10,3 @@\n+a\n+b\n+c\n'), [[10, 12]]);
});

test('several hunks yield several spans, in order', () => {
    const diff = '@@ -1 +1 @@\n+a\n@@ -20,0 +21,2 @@\n+b\n+c\n@@ -50,2 +53 @@\n+d\n';
    assert.deepEqual(parseHunks(diff), [[1, 1], [21, 22], [53, 53]]);
});

test('a pure deletion hunk contributes no span', () => {
    // `+50,0` means nothing was added at that point — there is no line to blame.
    assert.deepEqual(parseHunks('@@ -50,3 +50,0 @@\n-a\n-b\n-c\n'), []);
});

test('an empty diff yields no spans', () => {
    assert.deepEqual(parseHunks(''), []);
    assert.deepEqual(parseHunks('\n'), []);
});

test('a rename with no content change yields no spans', () => {
    const diff = 'diff --git a/old.ts b/new.ts\nsimilarity index 100%\nrename from old.ts\nrename to new.ts\n';
    assert.deepEqual(parseHunks(diff), [], 'nothing was written, so nothing is attributable');
});

test('hunk headers appearing inside content are not mistaken for real hunks', () => {
    // A test fixture or a markdown file can legitimately contain a line starting with "@@".
    // Only headers at the start of a line in the diff stream count, and they must match the shape.
    const diff = '@@ -1 +1 @@\n+const marker = "@@ -99,0 +99,5 @@";\n';
    assert.deepEqual(parseHunks(diff), [[1, 1]]);
});
