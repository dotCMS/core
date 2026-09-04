import * as fs from 'node:fs/promises';
import * as os from 'node:os';
import * as path from 'node:path';

import { runSetup } from './setup';

const URL_ = 'https://demo.dotcms.com';

/**
 * A real temp directory rather than a mocked filesystem: "nothing was written" is the
 * assertion this whole suite turns on, and only a real empty directory proves it. Mocking
 * `fs` would only prove that one API was not called, which a write through any other path
 * would slip past.
 *
 * The directory is passed in as `cwd` rather than set with `process.chdir()`. chdir mutates
 * global process state, leaks between cases, and is left dangling if a case throws before
 * cleanup.
 */
let dir: string;
beforeEach(async () => {
    dir = await fs.mkdtemp(path.join(os.tmpdir(), 'dotcms-setup-'));
});
afterEach(async () => {
    await fs.rm(dir, { recursive: true, force: true });
    jest.restoreAllMocks();
});

/** Everything reachable; the token is refused. */
function mockRejectedToken() {
    jest.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
        const u = String(input);
        if (u.includes('/appconfiguration')) return new Response(JSON.stringify({ entity: {} }), { status: 200 });
        return new Response('', { status: 401 });
    });
}

describe('ordering guarantee (FR-008a) — the load-bearing test', () => {
    it('writes NOTHING when the token is rejected: no file, no directory, no skills install', async () => {
        mockRejectedToken();
        // The error must identify the token rejection. `rejects.toThrow()` alone is satisfied
        // by an unimplemented stub, so it could never go Red.
        const err = await runSetup({
            url: URL_, authToken: 'bad', agents: ['cursor', 'claude-code'], scope: 'folder', cwd: dir
        }).catch((e: Error) => e);
        expect((err as Error).message).toMatch(/token/i);
        expect((err as Error).message).toMatch(/reject|invalid|not authoriz/i);
        await expect(fs.readdir(dir)).resolves.toEqual([]);
    });

    it('cannot be bypassed by --yes or --force (FR-008c)', async () => {
        mockRejectedToken();
        const err = await runSetup({
            url: URL_, authToken: 'bad', agents: ['cursor'], scope: 'folder', yes: true, force: true, cwd: dir
        }).catch((e: Error) => e);
        expect((err as Error).message).toMatch(/token/i);
        await expect(fs.readdir(dir)).resolves.toEqual([]);
    });

    it('reports a rejected token differently from an unreachable instance (FR-008b)', async () => {
        mockRejectedToken();
        const rejected = await runSetup({ url: URL_, authToken: 'bad', agents: ['cursor'], scope: 'folder', cwd: dir })
            .catch((e: Error) => e.message);

        jest.restoreAllMocks();
        jest.spyOn(globalThis, 'fetch').mockRejectedValue(
            Object.assign(new TypeError('fetch failed'), { cause: { code: 'ECONNREFUSED' } })
        );
        const unreachable = await runSetup({ url: URL_, authToken: 'x', agents: ['cursor'], scope: 'folder', cwd: dir })
            .catch((e: Error) => e.message);

        expect(rejected).not.toEqual(unreachable);
    });
});

describe('auth mode exclusivity (FR-003b)', () => {
    it('rejects a token supplied together with a username/password as a usage error', async () => {
        await expect(
            runSetup({ url: URL_, authToken: 'tok', user: 'a@b.com', password: 'pw', agents: ['cursor'], cwd: dir })
        ).rejects.toThrow(/authToken|mutually exclusive|alternative/i);
    });

    it('mints nothing and writes nothing in that case', async () => {
        const fetchMock = jest.spyOn(globalThis, 'fetch');
        const err = await runSetup({
            url: URL_, authToken: 'tok', user: 'a@b.com', password: 'pw', agents: ['cursor'], cwd: dir
        }).catch((e: Error) => e);
        // Naming the conflict is the point — a generic throw would pass without it.
        expect((err as Error).message).toMatch(/authToken/);
        expect((err as Error).message).toMatch(/user|password/);
        expect(fetchMock).not.toHaveBeenCalled();
        await expect(fs.readdir(dir)).resolves.toEqual([]);
    });
});
