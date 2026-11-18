/**
 * Parses added/modified line ranges out of a zero-context diff.
 *
 * Kept separate from the git plumbing because it is the one piece of pure logic in the
 * changed-file path, and getting it wrong is silent in both directions: too-wide ranges make the
 * gate blame untouched code, too-narrow ones make it miss real violations.
 */

// Anchored to a line start and matched against the full header shape, so a "@@" that appears
// inside file content — a fixture string, a markdown table — is never mistaken for a hunk.
const HUNK = /^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@/gm;

/**
 * @param {string} diff  Output of `git diff --unified=0`.
 * @returns {[number, number][]} 1-based inclusive spans of lines present at head.
 */
export function parseHunks(diff) {
    const spans = [];
    for (const match of diff.matchAll(HUNK)) {
        const start = Number(match[1]);
        const count = match[2] === undefined ? 1 : Number(match[2]);
        // `+N,0` is a pure deletion: nothing was written there, so nothing is attributable.
        if (count > 0) spans.push([start, start + count - 1]);
    }
    return spans;
}
