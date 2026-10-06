/**
 * T009, T010 — changed-file resolution.
 *
 * Runs entirely against fixture repositories in temp dirs. The harness's contract is that it
 * writes nothing; a test that mutated the real tree could not tell a genuine breach of that
 * contract from its own residue.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { makeRepo } from './fixtures/make-repo.mjs';
import { resolveChangedFiles } from './lib/changed-files.mjs';
import { git } from './lib/exec.mjs';

test('includes added, copied, modified and renamed files; excludes deleted ones', async (t) => {
    const repo = await makeRepo({
        'src/keep.ts': 'export const keep = 1;\n',
        'src/gone.ts': 'export const gone = 1;\n',
        'src/move-me.ts': 'export const moved = 1;\n'
    });
    t.after(() => repo.cleanup());

    const base = await repo.revParse();
    await repo.commit(
        {
            'src/added.ts': 'export const added = 1;\n',
            'src/keep.ts': 'export const keep = 2;\n',
            'src/gone.ts': null
        },
        'add, modify, delete'
    );
    await repo.rename('src/move-me.ts', 'src/moved.ts', 'rename');

    const { files } = await resolveChangedFiles({ repoDir: repo.dir, base, head: 'HEAD' });
    const paths = files.map((f) => f.path).sort();

    assert.deepEqual(paths, ['src/added.ts', 'src/keep.ts', 'src/moved.ts']);
    assert.ok(!paths.includes('src/gone.ts'), 'a deleted file has nothing to check');
    assert.ok(!paths.includes('src/move-me.ts'), 'a rename is reported at its new path only');
});

test('classifies each changed file as source or template', async (t) => {
    const repo = await makeRepo({ 'src/a.ts': 'export const a = 1;\n' });
    t.after(() => repo.cleanup());

    const base = await repo.revParse();
    await repo.commit(
        {
            'src/b.ts': 'export const b = 1;\n',
            'src/b.component.html': '<span>hi</span>\n',
            'README.md': '# not code\n'
        },
        'mixed'
    );

    const { files } = await resolveChangedFiles({ repoDir: repo.dir, base, head: 'HEAD' });
    const byPath = Object.fromEntries(files.map((f) => [f.path, f.kind]));

    assert.equal(byPath['src/b.ts'], 'source');
    assert.equal(byPath['src/b.component.html'], 'template');
    assert.equal(byPath['README.md'], undefined, 'files no compiler reads are not changed files');
});

test('records changed line ranges as 1-based inclusive spans', async (t) => {
    const repo = await makeRepo({ 'src/a.ts': 'const a = 1;\nconst b = 2;\nconst c = 3;\n' });
    t.after(() => repo.cleanup());

    const base = await repo.revParse();
    await repo.commit({ 'src/a.ts': 'const a = 1;\nconst b = 99;\nconst c = 3;\n' }, 'edit line 2');

    const { files } = await resolveChangedFiles({ repoDir: repo.dir, base, head: 'HEAD' });
    assert.deepEqual(files[0].changedLines, [[2, 2]]);
});

test('fetches the base ref on a shallow clone instead of reporting no changes', async (t) => {
    const repo = await makeRepo({ 'src/a.ts': 'export const a = 1;\n' });
    t.after(() => repo.cleanup());

    const base = (await repo.commit({ 'src/a.ts': 'export const a = 2;\n' }, 'base point')).slice(0, 40);
    await repo.commit({ 'src/new.ts': 'export const n = 1;\n' }, 'after base');

    const shallow = await repo.shallowClone(1);
    t.after(() => shallow.cleanup());

    // The whole failure mode being guarded: a missing base ref must NOT look like an empty diff.
    // Reporting "nothing changed" here would make the gate pass every pull request in CI.
    const { files } = await resolveChangedFiles({ repoDir: shallow.dir, base, head: 'HEAD' });
    assert.ok(files.length > 0, 'must fetch the base ref, not silently report an empty diff');
});

test('throws rather than passing when the base ref cannot be resolved at all', async (t) => {
    const repo = await makeRepo({ 'src/a.ts': 'export const a = 1;\n' });
    t.after(() => repo.cleanup());

    await assert.rejects(
        () => resolveChangedFiles({ repoDir: repo.dir, base: 'refs/heads/does-not-exist', head: 'HEAD' }),
        /base ref/i,
        'an unresolvable base is a harness failure (exit 2), never a clean run'
    );
});

/* ── T053 (US4) — template-only changes ─────────────────────────────────────
 * A pull request that edits only a .html file must still be checked. Treating "no TypeScript
 * changed" as "nothing to do" would let every template regression through, and the four
 * applications where template strictness is switched off are exactly where that matters.
 */

test('a diff containing only template files still yields changed files', async (t) => {
    const repo = await makeRepo({ 'src/a.component.html': '<span>one</span>\n' });
    t.after(() => repo.cleanup());

    const base = await repo.revParse();
    await repo.commit({ 'src/a.component.html': '<span>two</span>\n' }, 'template only');

    const { files } = await resolveChangedFiles({ repoDir: repo.dir, base, head: 'HEAD' });

    assert.equal(files.length, 1);
    assert.equal(files[0].kind, 'template');
    assert.deepEqual(files[0].changedLines, [[1, 1]]);
});

test('a new template file has every line attributable to its author', async (t) => {
    const repo = await makeRepo({ 'src/a.ts': 'export const a = 1;\n' });
    t.after(() => repo.cleanup());

    const base = await repo.revParse();
    await repo.commit({ 'src/new.component.html': '<a></a>\n<b></b>\n<c></c>\n' }, 'added template');

    const { files } = await resolveChangedFiles({ repoDir: repo.dir, base, head: 'HEAD' });
    const added = files.find((f) => f.path === 'src/new.component.html');

    assert.equal(added.status, 'A');
    assert.deepEqual(added.changedLines, [[1, 3]], 'nothing in a new file is inherited debt');
});

/* ── Merge-base semantics ───────────────────────────────────────────────────
 * A pull request's diff is `base...head` (three dots) — everything since the two diverged — not
 * `base..head`, which compares the two trees. The difference is invisible while a branch is fresh
 * and catastrophic once it is stale: a tree comparison reports every file the BASE moved on as
 * changed, so the gate blames the author for violations someone else merged into main.
 *
 * Measured on this very branch before the fix: 50 findings, essentially none of them its own.
 */

test('only the branch’s own changes are reported when the base has moved on', async (t) => {
    const repo = await makeRepo({ 'src/shared.ts': 'export const shared = 1;\n' });
    t.after(() => repo.cleanup());

    const divergedAt = await repo.revParse();

    // The branch writes one file.
    await repo.commit({ 'src/mine.ts': 'export const mine = 1;\n' }, 'branch work');
    const branchHead = await repo.revParse();

    // Meanwhile the base moves on: a NEW file (which a tree diff hides as a deletion, filtered by
    // ACMR) and — the case that actually bites — a MODIFIED shared file, which a tree diff reports
    // as changed and blames on this branch.
    await git(['-C', repo.dir, 'checkout', '-q', '-b', 'base-line', divergedAt]);
    await repo.commit(
        {
            'src/theirs.ts': 'export const theirs = 1;\n',
            'src/shared.ts': 'export const shared = 999;\n'
        },
        'someone else'
    );
    const baseHead = await repo.revParse();

    const { files } = await resolveChangedFiles({ repoDir: repo.dir, base: baseHead, head: branchHead });
    const paths = files.map((f) => f.path).sort();

    assert.deepEqual(
        paths,
        ['src/mine.ts'],
        'src/shared.ts was modified by the BASE; a tree diff blames this branch for it'
    );
});

test('the reported base is the merge base, so the numbers are reproducible', async (t) => {
    const repo = await makeRepo({ 'src/a.ts': 'export const a = 1;\n' });
    t.after(() => repo.cleanup());

    const divergedAt = await repo.revParse();
    await repo.commit({ 'src/mine.ts': 'export const mine = 1;\n' }, 'branch work');
    const branchHead = await repo.revParse();

    await git(['-C', repo.dir, 'checkout', '-q', '-b', 'other', divergedAt]);
    await repo.commit({ 'src/theirs.ts': 'export const theirs = 1;\n' }, 'someone else');
    const baseHead = await repo.revParse();

    const { base } = await resolveChangedFiles({ repoDir: repo.dir, base: baseHead, head: branchHead });
    assert.equal(base, divergedAt, 'the report must cite the point of divergence, not the base tip');
});
