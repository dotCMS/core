/**
 * Fixture git repositories, built in a temp dir and torn down afterwards.
 *
 * The unit tests must never touch the real repository: the harness's whole contract is that it
 * writes nothing, and a test that mutates the working tree could not tell a real violation of
 * that contract from its own mess.
 */
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { git } from '../lib/exec.mjs';

const AUTHOR = [
    '-c', 'user.name=strict-gate fixture',
    '-c', 'user.email=fixture@example.invalid',
    '-c', 'commit.gpgsign=false'
];

/**
 * @typedef {Object} FixtureRepo
 * @property {string} dir            Absolute path to the repository.
 * @property {(tree: Record<string,string|null>, message: string) => Promise<string>} commit
 *           Writes a file tree and commits it. A `null` value deletes the file. Returns the SHA.
 * @property {(from: string, to: string, message: string) => Promise<string>} rename
 * @property {(depth?: number) => Promise<FixtureRepo>} shallowClone
 * @property {() => Promise<void>} cleanup
 */

/**
 * @param {Record<string, string|null>} [initialTree]
 * @returns {Promise<FixtureRepo>}
 */
export async function makeRepo(initialTree) {
    const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'strict-gate-repo-'));
    const created = [dir];

    await git(['init', '--initial-branch=main', dir]);

    async function writeTree(tree) {
        for (const [relative, contents] of Object.entries(tree)) {
            const target = path.join(dir, relative);
            if (contents === null) {
                await fs.rm(target, { force: true });
                continue;
            }
            await fs.mkdir(path.dirname(target), { recursive: true });
            await fs.writeFile(target, contents, 'utf8');
        }
    }

    async function commit(tree, message) {
        await writeTree(tree);
        await git(['-C', dir, 'add', '--all']);
        await git(['-C', dir, ...AUTHOR, 'commit', '--allow-empty', '-m', message]);
        const { stdout } = await git(['-C', dir, 'rev-parse', 'HEAD']);
        return stdout.trim();
    }

    async function rename(from, to, message) {
        await fs.mkdir(path.dirname(path.join(dir, to)), { recursive: true });
        await git(['-C', dir, 'mv', from, to]);
        await git(['-C', dir, ...AUTHOR, 'commit', '-m', message]);
        const { stdout } = await git(['-C', dir, 'rev-parse', 'HEAD']);
        return stdout.trim();
    }

    async function shallowClone(depth = 1) {
        const cloneDir = await fs.mkdtemp(path.join(os.tmpdir(), 'strict-gate-shallow-'));
        created.push(cloneDir);
        // file:// forces a real fetch protocol; a plain path clone would hardlink the full history
        // and the shallow-checkout test would silently exercise nothing.
        await git(['clone', '--depth', String(depth), `file://${dir}`, cloneDir]);
        return {
            dir: cloneDir,
            commit: () => {
                throw new Error('shallow clone fixtures are read-only');
            },
            rename: () => {
                throw new Error('shallow clone fixtures are read-only');
            },
            shallowClone: () => {
                throw new Error('cannot re-clone a shallow fixture');
            },
            revParse: async (ref = 'HEAD') => {
                const { stdout } = await git(['-C', cloneDir, 'rev-parse', `${ref}^{commit}`]);
                return stdout.trim();
            },
            cleanup: async () => fs.rm(cloneDir, { recursive: true, force: true })
        };
    }

    /** Resolves a ref to a SHA now. Tests must capture the base BEFORE committing: passing the
     * literal 'HEAD' makes git resolve it at diff time, so base === head and the diff is empty. */
    async function revParse(ref = 'HEAD') {
        const { stdout } = await git(['-C', dir, 'rev-parse', `${ref}^{commit}`]);
        return stdout.trim();
    }

    async function cleanup() {
        await Promise.all(created.map((d) => fs.rm(d, { recursive: true, force: true })));
    }

    if (initialTree) await commit(initialTree, 'initial');

    return { dir, commit, rename, shallowClone, revParse, cleanup };
}
