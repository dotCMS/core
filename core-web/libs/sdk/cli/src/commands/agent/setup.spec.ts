import * as fs from 'node:fs/promises';
import * as os from 'node:os';
import * as path from 'node:path';

import { runSetup } from './setup';
import * as skills from './skills';
import * as registry from './targets/registry';

/**
 * setup.spec drives the WHOLE flow, including the skills install and the connection check —
 * both of which spawn processes. Without this, `installSkills` really ran `npx skills add` and
 * wrote skill trees into the repository while the suite stayed green.
 */
jest.mock('node:child_process', () => ({
    ...jest.requireActual('node:child_process'),
    spawn: jest.fn(() => {
        throw new Error('spawn is not available in unit tests — mock it explicitly');
    }),
    spawnSync: jest.fn(() => ({ status: 0 }))
}));

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

/** A reachable instance that accepts the token — so the run gets past verification and
 *  actually reaches the write step. */
function mockAcceptedToken() {
    jest.spyOn(globalThis, 'fetch').mockResolvedValue(
        new Response(JSON.stringify({ entity: { token: 'dot_ok' } }), { status: 200 })
    );
}

describe('overwrite confirmation (FR-017)', () => {
    it('asks before replacing an existing dotcms entry', async () => {
        mockAcceptedToken();
        const confirm = jest.fn().mockResolvedValue(true);
        const file = path.join(dir, '.cursor', 'mcp.json');
        await fs.mkdir(path.dirname(file), { recursive: true });
        await fs.writeFile(file, JSON.stringify({ mcpServers: { dotcms: { command: 'old' } } }), 'utf8');

        await runSetup({
            url: URL_, authToken: 'good', agents: ['cursor'], scope: 'folder', cwd: dir,
            skipSkills: true, skipVerify: true, confirmOverwrite: confirm
        });
        expect(confirm).toHaveBeenCalled();
    });

    it('does not ask when --force is supplied', async () => {
        mockAcceptedToken();
        const confirm = jest.fn().mockResolvedValue(true);
        const file = path.join(dir, '.cursor', 'mcp.json');
        await fs.mkdir(path.dirname(file), { recursive: true });
        await fs.writeFile(file, JSON.stringify({ mcpServers: { dotcms: { command: 'old' } } }), 'utf8');

        await runSetup({
            url: URL_, authToken: 'good', agents: ['cursor'], scope: 'folder', cwd: dir, force: true,
            skipSkills: true, skipVerify: true, confirmOverwrite: confirm
        });
        expect(confirm).not.toHaveBeenCalled();
        // The positive half: `not.toHaveBeenCalled()` alone is satisfied while the feature does
        // not exist at all, so assert the entry was actually replaced.
        const doc = JSON.parse(await fs.readFile(file, 'utf8'));
        expect(doc.mcpServers.dotcms.args).toEqual(['-y', '@dotcms/mcp-server@latest']);
    });

    it('leaves the existing entry alone when the developer declines', async () => {
        mockAcceptedToken();
        const file = path.join(dir, '.cursor', 'mcp.json');
        await fs.mkdir(path.dirname(file), { recursive: true });
        await fs.writeFile(file, JSON.stringify({ mcpServers: { dotcms: { command: 'old' } } }), 'utf8');

        const result = await runSetup({
            url: URL_, authToken: 'good', agents: ['cursor'], scope: 'folder', cwd: dir,
            skipSkills: true, skipVerify: true, confirmOverwrite: async () => false
        });
        const doc = JSON.parse(await fs.readFile(file, 'utf8'));
        expect(doc.mcpServers.dotcms.command).toBe('old');
        expect(result.outcomes[0].result).toBe('skipped');
    });
});

describe('partial failure (FR-020a-d, SC-006a)', () => {
    /** Make one target unwritable by putting a read-only DIRECTORY where its file must go. */
    async function makeCursorUnwritable() {
        const file = path.join(dir, '.cursor', 'mcp.json');
        await fs.mkdir(file, { recursive: true }); // a directory where a file belongs
    }

    it('still configures the other targets when one fails', async () => {
        mockAcceptedToken();
        await makeCursorUnwritable();
        const result = await runSetup({
            url: URL_, authToken: 'good', agents: ['cursor', 'claude-code'], scope: 'folder',
            cwd: dir, skipSkills: true, skipVerify: true
        });
        const claude = result.outcomes.find((o) => o.targetId === 'claude-code');
        expect(claude?.result).toBe('written');
        await expect(fs.stat(path.join(dir, '.mcp.json'))).resolves.toBeDefined();
    });

    it('reports the failing target with a reason', async () => {
        mockAcceptedToken();
        await makeCursorUnwritable();
        const result = await runSetup({
            url: URL_, authToken: 'good', agents: ['cursor', 'claude-code'], scope: 'folder',
            cwd: dir, skipSkills: true, skipVerify: true
        });
        const cursor = result.outcomes.find((o) => o.targetId === 'cursor');
        expect(cursor?.result).toBe('failed');
        expect(cursor?.reason).toBeTruthy();
    });

    it('exits non-zero when any target failed, even though others succeeded', async () => {
        mockAcceptedToken();
        await makeCursorUnwritable();
        const result = await runSetup({
            url: URL_, authToken: 'good', agents: ['cursor', 'claude-code'], scope: 'folder',
            cwd: dir, skipSkills: true, skipVerify: true
        });
        expect(result.exitCode).toBe(1);
    });

    it('does not roll back what already succeeded', async () => {
        mockAcceptedToken();
        await makeCursorUnwritable();
        await runSetup({
            url: URL_, authToken: 'good', agents: ['claude-code', 'cursor'], scope: 'folder',
            cwd: dir, skipSkills: true, skipVerify: true
        });
        // claude-code was written BEFORE cursor failed, and must survive it.
        await expect(fs.stat(path.join(dir, '.mcp.json'))).resolves.toBeDefined();
    });

    it('exits zero when every selected target succeeded', async () => {
        mockAcceptedToken();
        const result = await runSetup({
            url: URL_, authToken: 'good', agents: ['cursor', 'claude-code'], scope: 'folder',
            cwd: dir, skipSkills: true, skipVerify: true
        });
        expect(result.exitCode).toBe(0);
    });
});

describe('defaults never block a run (FR-003j, FR-010, FR-011)', () => {
    it('configures every DETECTED editor when no --agent is given', async () => {
        mockAcceptedToken();
        // Two detected, one not — the run must use exactly the detected set.
        const detect = jest
            .spyOn(registry, 'detectTargets')
            .mockResolvedValue([registry.getTarget('cursor'), registry.getTarget('claude-code')]);

        const result = await runSetup({
            url: URL_, authToken: 'good', scope: 'folder', cwd: dir,
            skipSkills: true, skipVerify: true
        });

        expect(detect).toHaveBeenCalled();
        expect(result.outcomes.map((o) => o.targetId).sort()).toEqual(['claude-code', 'cursor']);
    });

    it('writes nothing and does not throw when no editor is detected', async () => {
        mockAcceptedToken();
        jest.spyOn(registry, 'detectTargets').mockResolvedValue([]);
        const result = await runSetup({
            url: URL_, authToken: 'good', scope: 'folder', cwd: dir,
            skipSkills: true, skipVerify: true
        });
        expect(result.outcomes).toEqual([]);
        expect(result.exitCode).toBe(0);
    });

    it('defaults to FOLDER scope, writing into the working directory rather than $HOME', async () => {
        mockAcceptedToken();
        const result = await runSetup({
            url: URL_, authToken: 'good', agents: ['cursor'], cwd: dir,
            skipSkills: true, skipVerify: true
        });
        expect(result.outcomes[0].scope).toBe('folder');
        expect(result.outcomes[0].path).toContain(dir);
    });
});

describe('target selection (FR-010)', () => {
    /** A silent default in an interactive run would configure editors nobody chose. */
    it('ASKS which editors when none were named and there is a prompt port', async () => {
        mockAcceptedToken();
        jest.spyOn(registry, 'detectTargets').mockResolvedValue([registry.getTarget('cursor')]);
        const multiSelect = jest.fn().mockResolvedValue(['cursor']);

        await runSetup({
            url: URL_, authToken: 'good', scope: 'folder', cwd: dir,
            skipSkills: true, skipVerify: true,
            promptPort: { text: jest.fn(), password: jest.fn(), select: jest.fn(), multiSelect }
        });
        expect(multiSelect).toHaveBeenCalled();
    });

    it('offers every supported editor, with the detected ones pre-checked', async () => {
        mockAcceptedToken();
        jest.spyOn(registry, 'detectTargets').mockResolvedValue([registry.getTarget('cursor')]);
        const multiSelect = jest.fn().mockResolvedValue(['cursor']);

        await runSetup({
            url: URL_, authToken: 'good', scope: 'folder', cwd: dir,
            skipSkills: true, skipVerify: true,
            promptPort: { text: jest.fn(), password: jest.fn(), select: jest.fn(), multiSelect }
        });
        const choices = multiSelect.mock.calls[0][1] as { value: string; checked: boolean }[];
        expect(choices).toHaveLength(7);
        expect(choices.find((c) => c.value === 'cursor')?.checked).toBe(true);
        expect(choices.find((c) => c.value === 'codex')?.checked).toBe(false);
    });

    it('honours a deselection rather than configuring it anyway', async () => {
        mockAcceptedToken();
        jest.spyOn(registry, 'detectTargets').mockResolvedValue([
            registry.getTarget('cursor'),
            registry.getTarget('claude-code')
        ]);
        const result = await runSetup({
            url: URL_, authToken: 'good', scope: 'folder', cwd: dir,
            skipSkills: true, skipVerify: true,
            promptPort: {
                text: jest.fn(), password: jest.fn(), select: jest.fn(),
                multiSelect: jest.fn().mockResolvedValue(['claude-code'])
            }
        });
        expect(result.outcomes.map((o) => o.targetId)).toEqual(['claude-code']);
    });

    it('does NOT ask when --agent was supplied', async () => {
        mockAcceptedToken();
        const multiSelect = jest.fn();
        await runSetup({
            url: URL_, authToken: 'good', agents: ['cursor'], scope: 'folder', cwd: dir,
            skipSkills: true, skipVerify: true,
            promptPort: { text: jest.fn(), password: jest.fn(), select: jest.fn(), multiSelect }
        });
        expect(multiSelect).not.toHaveBeenCalled();
    });

    it('falls back to every detected editor when there is no prompt port', async () => {
        mockAcceptedToken();
        jest.spyOn(registry, 'detectTargets').mockResolvedValue([registry.getTarget('cursor')]);
        const result = await runSetup({
            url: URL_, authToken: 'good', scope: 'folder', cwd: dir,
            skipSkills: true, skipVerify: true
        });
        expect(result.outcomes.map((o) => o.targetId)).toEqual(['cursor']);
    });
});

describe('skills reporting honesty (FR-027)', () => {
    it('reports "unverified" for a target whose skills location is not confirmed', async () => {
        mockAcceptedToken();
        // Simulate a future editor added on documentation alone.
        jest.spyOn(registry, 'detectTargets').mockResolvedValue([
            { ...registry.getTarget('cursor'), skillsLocationVerified: false }
        ]);
        jest.spyOn(skills, 'installSkills').mockResolvedValue({ ok: true, command: 'npx skills add …' });

        const result = await runSetup({
            url: URL_, authToken: 'good', scope: 'folder', cwd: dir, skipVerify: true
        });
        expect(result.outcomes[0].skillsInstalled).toBe('unverified');
    });

    it('reports "yes" for a target that is confirmed', async () => {
        mockAcceptedToken();
        jest.spyOn(skills, 'installSkills').mockResolvedValue({ ok: true, command: 'npx skills add …' });
        const result = await runSetup({
            url: URL_, authToken: 'good', agents: ['vscode'], scope: 'folder', cwd: dir, skipVerify: true
        });
        expect(result.outcomes[0].skillsInstalled).toBe('yes');
    });
});
