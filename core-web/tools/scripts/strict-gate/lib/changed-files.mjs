/**
 * Resolves what a pull request actually changed. The unit the whole gate is scoped by.
 */
import path from 'node:path';
import { git } from './exec.mjs';
import { parseHunks } from './hunks.mjs';

const SOURCE_EXT = new Set(['.ts', '.tsx', '.mts', '.cts']);
const TEMPLATE_EXT = new Set(['.html']);

/** @returns {'source'|'template'|null} null means no compiler reads this file. */
export function classify(filePath) {
    const ext = path.extname(filePath);
    if (SOURCE_EXT.has(ext)) return 'source';
    if (TEMPLATE_EXT.has(ext)) return 'template';
    return null;
}

async function resolves(repoDir, ref) {
    const { exitCode } = await git(['-C', repoDir, 'rev-parse', '--verify', '--quiet', `${ref}^{commit}`], {
        allowFailure: true
    });
    return exitCode === 0;
}

/**
 * Makes the base ref usable, fetching it when the checkout is shallow.
 *
 * The failure being guarded is subtle and expensive: if the base is missing and we let git diff
 * against nothing, the gate reports "no changes" and passes every pull request in CI. Silence
 * here is worse than an error, so an unresolvable base throws.
 */
export async function ensureBaseRef({ repoDir, base }) {
    if (await resolves(repoDir, base)) return;

    const attempts = [
        ['-C', repoDir, 'fetch', '--no-tags', '--depth=50', 'origin', base],
        ['-C', repoDir, 'fetch', '--no-tags', 'origin', base],
        ['-C', repoDir, 'fetch', '--no-tags', '--unshallow', 'origin']
    ];
    for (const args of attempts) {
        await git(args, { allowFailure: true });
        if (await resolves(repoDir, base)) return;
    }

    throw new Error(
        `base ref '${base}' cannot be resolved even after fetching. Refusing to report an empty ` +
            `diff, which would pass the gate for every pull request.`
    );
}

/**
 * Added/modified line spans, 1-based inclusive, from a zero-context diff.
 *
 * `base` must already be the merge base — `resolveChangedFiles` resolves it before calling here.
 * Passing a branch name would compare two trees and attribute the base's changes to this diff.
 */
export async function changedLinesFor({ repoDir, base, head, file }) {
    const { stdout } = await git([
        '-C', repoDir, 'diff', '--unified=0', '--no-color', `${base}..${head}`, '--', file
    ]);
    return parseHunks(stdout);
}

/**
 * @param {{ repoDir: string, base: string, head?: string }} options
 * @returns {Promise<{ files: object[], base: string, head: string,
 *                     baseResolution: 'merge-base'|'base-tip' }>}
 */
export async function resolveChangedFiles({ repoDir, base, head = 'HEAD' }) {
    await ensureBaseRef({ repoDir, base });

    const sha = async (ref) => (await git(['-C', repoDir, 'rev-parse', `${ref}^{commit}`])).stdout.trim();
    const headSha = await sha(head);

    // A pull request's diff is `base...head` — everything since the two diverged — not `base..head`,
    // which compares two trees. The difference is invisible while a branch is fresh and wrong once
    // it is stale: a tree comparison reports every file the BASE modified as changed, so the gate
    // blames the author for violations someone else merged. Resolving the merge base up front means
    // the report also CITES the point of divergence, which is what makes a re-run reproducible.
    const mergeBase = await git(['-C', repoDir, 'merge-base', base, headSha], { allowFailure: true });
    const resolvedMergeBase = mergeBase.exitCode === 0 ? mergeBase.stdout.trim() : '';

    // Falling back to the tip of base reinstates the very two-tree comparison the note above warns
    // about, so it says so out loud. Silent degradation here is what produced the run that reported
    // 50 findings, essentially none of them the branch's own (§7).
    if (!resolvedMergeBase) {
        process.stderr.write(
            `strict-gate: warning — no merge base between '${base}' and head; comparing against the ` +
                `tip of '${base}' instead. Findings may include changes the base introduced.\n`
        );
    }
    const baseSha = resolvedMergeBase || (await sha(base));
    const baseResolution = resolvedMergeBase ? 'merge-base' : 'base-tip';

    // -M so a rename is reported at its new path; ACMR so deletions never appear — there is
    // nothing to typecheck in a file that no longer exists at head.
    const { stdout } = await git([
        '-C', repoDir, 'diff', '--name-status', '-M', '--diff-filter=ACMR', `${baseSha}..${headSha}`
    ]);

    const files = [];
    for (const line of stdout.split('\n').filter(Boolean)) {
        const parts = line.split('\t');
        const status = parts[0][0];
        const filePath = parts[parts.length - 1]; // rename rows carry old\tnew
        const kind = classify(filePath);
        if (!kind) continue;
        files.push({
            path: filePath,
            status,
            kind,
            changedLines: await changedLinesFor({ repoDir, base: baseSha, head: headSha, file: filePath })
        });
    }

    return { files, base: baseSha, head: headSha, baseResolution };
}
