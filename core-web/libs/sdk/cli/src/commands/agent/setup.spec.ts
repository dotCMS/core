import * as fs from 'node:fs/promises';
import * as os from 'node:os';
import * as path from 'node:path';

import { runSetup } from './setup';
import * as skills from './skills';
import * as registry from './targets/registry';

import {
    appConfiguration,
    appConfigurationResponse
} from '../../shared/__fixtures__/appconfiguration';
import { TOOL_VERSION } from '../../shared/version';

import type { PromptPort } from '../../shared/prompts';

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
        if (u.includes('/appconfiguration')) return appConfigurationResponse();
        return new Response('', { status: 401 });
    });
}

describe('ordering guarantee (FR-008a) — the load-bearing test', () => {
    it('writes NOTHING when the token is rejected: no file, no directory, no skills install', async () => {
        mockRejectedToken();
        // The error must identify the token rejection. `rejects.toThrow()` alone is satisfied
        // by an unimplemented stub, so it could never go Red.
        const err = await runSetup({
            url: URL_,
            authToken: 'bad',
            agents: ['cursor', 'claude-code'],
            scope: 'folder',
            cwd: dir
        }).catch((e: Error) => e);
        expect((err as Error).message).toMatch(/token/i);
        expect((err as Error).message).toMatch(/reject|invalid|not authoriz/i);
        await expect(fs.readdir(dir)).resolves.toEqual([]);
    });

    it('cannot be bypassed by --yes or --force (FR-008c)', async () => {
        mockRejectedToken();
        const err = await runSetup({
            url: URL_,
            authToken: 'bad',
            agents: ['cursor'],
            scope: 'folder',
            yes: true,
            force: true,
            cwd: dir
        }).catch((e: Error) => e);
        expect((err as Error).message).toMatch(/token/i);
        await expect(fs.readdir(dir)).resolves.toEqual([]);
    });

    it('reports a rejected token differently from an unreachable instance (FR-008b)', async () => {
        mockRejectedToken();
        const rejected = await runSetup({
            url: URL_,
            authToken: 'bad',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir
        }).catch((e: Error) => e.message);

        jest.restoreAllMocks();
        jest.spyOn(globalThis, 'fetch').mockRejectedValue(
            Object.assign(new TypeError('fetch failed'), { cause: { code: 'ECONNREFUSED' } })
        );
        const unreachable = await runSetup({
            url: URL_,
            authToken: 'x',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir
        }).catch((e: Error) => e.message);

        expect(rejected).not.toEqual(unreachable);
    });
});

describe('auth mode exclusivity (FR-003b)', () => {
    it('rejects a token supplied together with a username/password as a usage error', async () => {
        await expect(
            runSetup({
                url: URL_,
                authToken: 'tok',
                user: 'a@b.com',
                password: 'pw',
                agents: ['cursor'],
                cwd: dir
            })
        ).rejects.toThrow(/authToken|mutually exclusive|alternative/i);
    });

    it('mints nothing and writes nothing in that case', async () => {
        const fetchMock = jest.spyOn(globalThis, 'fetch');
        const err = await runSetup({
            url: URL_,
            authToken: 'tok',
            user: 'a@b.com',
            password: 'pw',
            agents: ['cursor'],
            cwd: dir
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
/**
 * A healthy instance: a real appconfiguration body for the reachability probe, and a 200 for
 * everything else. Routing by URL matters — a single blanket response meant the probe was
 * answered with a token payload, which the dotCMS fingerprint now correctly rejects.
 */
function mockAcceptedToken() {
    jest.spyOn(globalThis, 'fetch').mockImplementation(async (input) =>
        String(input).includes('/appconfiguration')
            ? appConfigurationResponse()
            : new Response(JSON.stringify({ entity: { token: 'dot_ok' } }), { status: 200 })
    );
}

describe('overwrite confirmation (FR-017)', () => {
    it('asks before replacing an existing dotcms entry', async () => {
        mockAcceptedToken();
        const confirm = jest.fn().mockResolvedValue(true);
        const file = path.join(dir, '.cursor', 'mcp.json');
        await fs.mkdir(path.dirname(file), { recursive: true });
        await fs.writeFile(
            file,
            JSON.stringify({ mcpServers: { dotcms: { command: 'old' } } }),
            'utf8'
        );

        await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            confirmOverwrite: confirm
        });
        expect(confirm).toHaveBeenCalled();
    });

    it('does not ask when --force is supplied', async () => {
        mockAcceptedToken();
        const confirm = jest.fn().mockResolvedValue(true);
        const file = path.join(dir, '.cursor', 'mcp.json');
        await fs.mkdir(path.dirname(file), { recursive: true });
        await fs.writeFile(
            file,
            JSON.stringify({ mcpServers: { dotcms: { command: 'old' } } }),
            'utf8'
        );

        await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            force: true,
            skipSkills: true,
            skipVerify: true,
            confirmOverwrite: confirm
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
        await fs.writeFile(
            file,
            JSON.stringify({ mcpServers: { dotcms: { command: 'old' } } }),
            'utf8'
        );

        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            confirmOverwrite: async () => false
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
            url: URL_,
            authToken: 'good',
            agents: ['cursor', 'claude-code'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true
        });
        const claude = result.outcomes.find((o) => o.targetId === 'claude-code');
        expect(claude?.result).toBe('written');
        await expect(fs.stat(path.join(dir, '.mcp.json'))).resolves.toBeDefined();
    });

    it('reports the failing target with a reason', async () => {
        mockAcceptedToken();
        await makeCursorUnwritable();
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor', 'claude-code'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true
        });
        const cursor = result.outcomes.find((o) => o.targetId === 'cursor');
        expect(cursor?.result).toBe('failed');
        expect(cursor?.reason).toBeTruthy();
    });

    it('exits non-zero when any target failed, even though others succeeded', async () => {
        mockAcceptedToken();
        await makeCursorUnwritable();
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor', 'claude-code'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true
        });
        expect(result.exitCode).toBe(1);
    });

    it('does not roll back what already succeeded', async () => {
        mockAcceptedToken();
        await makeCursorUnwritable();
        await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['claude-code', 'cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true
        });
        // claude-code was written BEFORE cursor failed, and must survive it.
        await expect(fs.stat(path.join(dir, '.mcp.json'))).resolves.toBeDefined();
    });

    it('exits zero when every selected target succeeded', async () => {
        mockAcceptedToken();
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor', 'claude-code'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true
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
            url: URL_,
            authToken: 'good',
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true
        });

        expect(detect).toHaveBeenCalled();
        expect(result.outcomes.map((o) => o.targetId).sort()).toEqual(['claude-code', 'cursor']);
    });

    it('writes nothing and does not throw when no editor is detected', async () => {
        mockAcceptedToken();
        jest.spyOn(registry, 'detectTargets').mockResolvedValue([]);
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true
        });
        expect(result.outcomes).toEqual([]);
        expect(result.exitCode).toBe(0);
    });

    it('defaults to FOLDER scope, writing into the working directory rather than $HOME', async () => {
        mockAcceptedToken();
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            cwd: dir,
            skipSkills: true,
            skipVerify: true
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
            url: URL_,
            authToken: 'good',
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: { text: jest.fn(), password: jest.fn(), select: jest.fn(), multiSelect }
        });
        expect(multiSelect).toHaveBeenCalled();
    });

    it('offers every supported editor, with the detected ones pre-checked', async () => {
        mockAcceptedToken();
        jest.spyOn(registry, 'detectTargets').mockResolvedValue([registry.getTarget('cursor')]);
        const multiSelect = jest.fn().mockResolvedValue(['cursor']);

        await runSetup({
            url: URL_,
            authToken: 'good',
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
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
            url: URL_,
            authToken: 'good',
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: {
                text: jest.fn(),
                password: jest.fn(),
                select: jest.fn(),
                multiSelect: jest.fn().mockResolvedValue(['claude-code'])
            }
        });
        expect(result.outcomes.map((o) => o.targetId)).toEqual(['claude-code']);
    });

    it('does NOT ask when --agent was supplied', async () => {
        mockAcceptedToken();
        const multiSelect = jest.fn();
        await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: { text: jest.fn(), password: jest.fn(), select: jest.fn(), multiSelect }
        });
        expect(multiSelect).not.toHaveBeenCalled();
    });

    it('falls back to every detected editor when there is no prompt port', async () => {
        mockAcceptedToken();
        jest.spyOn(registry, 'detectTargets').mockResolvedValue([registry.getTarget('cursor')]);
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true
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
        jest.spyOn(skills, 'installSkills').mockResolvedValue({
            ok: true,
            command: 'npx skills add …'
        });

        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            scope: 'folder',
            cwd: dir,
            skipVerify: true
        });
        expect(result.outcomes[0].skillsInstalled).toBe('unverified');
    });

    it('reports "yes" for a target that is confirmed', async () => {
        mockAcceptedToken();
        jest.spyOn(skills, 'installSkills').mockResolvedValue({
            ok: true,
            command: 'npx skills add …'
        });
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['vscode'],
            scope: 'folder',
            cwd: dir,
            skipVerify: true
        });
        expect(result.outcomes[0].skillsInstalled).toBe('yes');
    });
});

describe('a rejected credential is re-asked, up to three times (FR-007)', () => {
    /** Reachable instance; the token is always refused. */
    function alwaysRejects() {
        jest.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
            const u = String(input);
            if (u.includes('/appconfiguration')) {
                return appConfigurationResponse();
            }
            return new Response('', { status: 401 });
        });
    }

    /** Refused twice, accepted on the third. */
    function rejectsTwice() {
        let verifyCalls = 0;
        jest.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
            const u = String(input);
            if (u.includes('/appconfiguration')) {
                return appConfigurationResponse();
            }
            verifyCalls += 1;
            return verifyCalls <= 2
                ? new Response('', { status: 401 })
                : appConfigurationResponse();
        });
    }

    const portThatRetypes = (token: string) => ({
        text: jest.fn().mockResolvedValue(URL_),
        password: jest.fn().mockResolvedValue(token),
        select: jest.fn().mockResolvedValue('token'),
        multiSelect: jest.fn().mockResolvedValue(['cursor'])
    });

    it('asks again instead of giving up after one bad token', async () => {
        rejectsTwice();
        const port = portThatRetypes('eventually-good');
        const onAuthRetry = jest.fn();

        const result = await runSetup({
            url: URL_,
            authToken: 'bad-first-try',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: port,
            onAuthRetry
        });

        expect(onAuthRetry).toHaveBeenCalledTimes(2);
        expect(result.outcomes[0].result).toBe('written');
    });

    it('gives up after exactly three attempts', async () => {
        alwaysRejects();
        const onAuthRetry = jest.fn();
        const err = await runSetup({
            url: URL_,
            authToken: 'bad',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: portThatRetypes('still-bad'),
            onAuthRetry
        }).catch((e: Error) => e);

        expect((err as Error).message).toMatch(/token/i);
        // Two notices for attempts 1 and 2; the third failure ends the run.
        expect(onAuthRetry).toHaveBeenCalledTimes(2);
    });

    it('writes nothing after exhausting the attempts', async () => {
        alwaysRejects();
        await runSetup({
            url: URL_,
            authToken: 'bad',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: portThatRetypes('still-bad')
        }).catch(() => undefined);
        await expect(fs.readdir(dir)).resolves.toEqual([]);
    });

    it('does NOT retry without a prompt port — a script cannot retype anything', async () => {
        alwaysRejects();
        const onAuthRetry = jest.fn();
        await runSetup({
            url: URL_,
            authToken: 'bad',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            onAuthRetry
        }).catch(() => undefined);
        expect(onAuthRetry).not.toHaveBeenCalled();
    });

    it('does NOT retry an unreachable instance — retyping a token cannot fix that', async () => {
        jest.spyOn(globalThis, 'fetch').mockRejectedValue(
            Object.assign(new TypeError('fetch failed'), { cause: { code: 'ENOTFOUND' } })
        );
        const onAuthRetry = jest.fn();
        await runSetup({
            url: URL_,
            authToken: 'bad',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: portThatRetypes('x'),
            onAuthRetry
        }).catch(() => undefined);
        expect(onAuthRetry).not.toHaveBeenCalled();
    });
});

describe('the instance is validated BEFORE credentials are asked for', () => {
    /** A username and password typed against a wrong address is wasted effort. */
    // Cast once: PromptPort.multiSelect is generic, which a per-property jest.fn() cannot
    // satisfy without more ceremony than the test is worth. What matters here is the ORDER of
    // the calls, not their types.
    const trackingPort = (calls: string[]) =>
        ({
            text: jest.fn(async () => {
                calls.push('ask:url');
                return URL_;
            }),
            password: jest.fn(async () => {
                calls.push('ask:password');
                return 'pw';
            }),
            select: jest.fn(async () => {
                calls.push('ask:mode');
                return 'signin';
            }),
            multiSelect: jest.fn(async () => {
                calls.push('ask:targets');
                return ['cursor'];
            })
        }) as unknown as PromptPort;

    it('never asks for a password when the address is not dotCMS', async () => {
        jest.spyOn(globalThis, 'fetch').mockResolvedValue(
            new Response('Not Found', { status: 404 })
        );
        const calls: string[] = [];
        const err = await runSetup({
            url: 'https://example.com',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: trackingPort(calls)
        }).catch((e: Error) => e);

        expect((err as Error).message).toMatch(/not a valid dotCMS instance/i);
        expect(calls).not.toContain('ask:password');
        expect(calls).not.toContain('ask:mode');
    });

    it('never asks for a password when the instance is unreachable', async () => {
        jest.spyOn(globalThis, 'fetch').mockRejectedValue(
            Object.assign(new TypeError('fetch failed'), { cause: { code: 'ENOTFOUND' } })
        );
        const calls: string[] = [];
        await runSetup({
            url: 'https://nope.invalid',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: trackingPort(calls)
        }).catch(() => undefined);

        expect(calls).not.toContain('ask:password');
    });

    it('asks for the address first, and only then for a credential', async () => {
        mockAcceptedToken();
        const calls: string[] = [];
        await runSetup({
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: trackingPort(calls)
        }).catch(() => undefined);

        expect(calls[0]).toBe('ask:url');
    });
});

describe('--skip-mcp is independent of --skip-skills', () => {
    it('still installs skills when only writing is skipped', async () => {
        mockAcceptedToken();
        const install = jest
            .spyOn(skills, 'installSkills')
            .mockResolvedValue({ ok: true, command: 'npx skills add …' });

        await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipMcp: true
        });
        expect(install).toHaveBeenCalled();
    });

    it('writes nothing, but SAYS the write was skipped', async () => {
        mockAcceptedToken();
        jest.spyOn(skills, 'installSkills').mockResolvedValue({ ok: true, command: 'x' });
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipMcp: true,
            skipSkills: true
        });
        await expect(fs.readdir(dir)).resolves.toEqual([]);
        expect(result.outcomes).toHaveLength(1);
        expect(result.outcomes[0].result).toBe('skipped');
        expect(result.outcomes[0].reason).toMatch(/skip-mcp/);
    });

    it('does not claim files contain a token when none were written', async () => {
        mockAcceptedToken();
        jest.spyOn(skills, 'installSkills').mockResolvedValue({ ok: true, command: 'x' });
        const confirmExclude = jest.fn().mockResolvedValue(true);
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipMcp: true,
            skipSkills: true,
            confirmExclude
        });
        expect(result.versionControl).toBeUndefined();
        expect(confirmExclude).not.toHaveBeenCalled();
    });

    it('skips the connection check too — there is no configuration to prove', async () => {
        mockAcceptedToken();
        jest.spyOn(skills, 'installSkills').mockResolvedValue({ ok: true, command: 'x' });
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipMcp: true
        });
        expect(result.connection).toBe('skipped');
    });
});

describe('a repeated --agent is counted once', () => {
    it('reports one editor, not two, and writes once', async () => {
        mockAcceptedToken();
        const steps: string[] = [];
        const result = await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor', 'cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            onProgress: (t) => steps.push(t)
        });
        expect(result.outcomes).toHaveLength(1);
        expect(steps.join(' ')).toContain('1 editor');
        expect(steps.join(' ')).not.toContain('2 editors');
    });
});

describe('a pre-existing directory keeps its own permissions', () => {
    it('does not widen a directory it did not create', async () => {
        mockAcceptedToken();
        const cursorDir = path.join(dir, '.cursor');
        await fs.mkdir(cursorDir, { recursive: true });
        await fs.chmod(cursorDir, 0o750);

        await runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true
        }).catch(() => undefined);

        const mode = (await fs.stat(cursorDir)).mode & 0o777;
        expect(mode).toBe(0o750);
    });
});

describe('FR-005a compatibility warning actually reaches the developer', () => {
    /**
     * The machinery existed and was unit-tested; nothing joined it up. These assert the WIRING
     * end to end — checkReachable's version reaching compatibilityWarning reaching the caller —
     * because testing compatibilityWarning() alone is exactly what let this ship dead twice.
     */
    /**
     * Both versions are DERIVED from TOOL_VERSION, never hardcoded. The release pipeline
     * rewrites `.version` to the dotCMS release tag at build time, so a literal like '1.0.0'
     * is "older" today (tool is 0.2.0) and "newer" after the first release — the test would
     * silently invert. Deriving keeps each case meaning what its name says.
     */
    const major = Number(TOOL_VERSION.split('.')[0]);
    const OLDER = `${Math.max(major - 1, 0)}.0.0`;
    const NEWER = `${major + 1}.0.0`;

    /** Serves an appconfiguration whose reported version is `version`, or none at all. */
    function instanceAt(version: string | null) {
        const body = appConfiguration(version ?? undefined);
        if (version === null) {
            delete (body.entity.config as Partial<typeof body.entity.config>).releaseInfo;
        }
        jest.spyOn(globalThis, 'fetch').mockImplementation(async (input) =>
            String(input).includes('/appconfiguration')
                ? new Response(JSON.stringify(body), { status: 200 })
                : new Response(JSON.stringify({ entity: {} }), { status: 200 })
        );
    }

    const run = (onWarning?: jest.Mock) =>
        runSetup({
            url: URL_,
            authToken: 'good',
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            onWarning
        });

    it('warns when the instance is OLDER than this tool', async () => {
        instanceAt(OLDER);
        const onWarning = jest.fn();
        const result = await run(onWarning);
        expect(result.warnings).toHaveLength(1);
        expect(result.warnings[0]).toContain(OLDER);
        expect(result.warnings[0]).toContain(TOOL_VERSION);
        expect(onWarning).toHaveBeenCalledWith(result.warnings[0]);
    });

    it('stays silent when the instance is NEWER', async () => {
        instanceAt(NEWER);
        const result = await run();
        expect(result.warnings).toEqual([]);
    });

    it('stays silent, and never throws, when the instance reports no version', async () => {
        instanceAt(null);
        const result = await run();
        expect(result.warnings).toEqual([]);
    });

    it('never blocks the run — the configuration is still written alongside a warning', async () => {
        instanceAt(OLDER);
        const result = await run();
        expect(result.outcomes[0].result).toBe('written');
        expect(result.exitCode).toBe(0);
    });
});

describe('an auth retry asks the human, it does not re-read the environment', () => {
    const OLD_ENV = process.env;
    beforeEach(() => {
        process.env = { ...OLD_ENV };
    });
    afterAll(() => {
        process.env = OLD_ENV;
    });

    /** Rejects the first two credentials, accepts the third. */
    function rejectsTwice() {
        let verifies = 0;
        jest.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
            if (String(input).includes('/appconfiguration')) return appConfigurationResponse();
            verifies += 1;
            return verifies <= 2
                ? new Response('', { status: 401 })
                : new Response(JSON.stringify({ entity: {} }), { status: 200 });
        });
    }

    it('prompts for a fresh credential when the rejected one came from the ENVIRONMENT', async () => {
        // The regression: resolveRequiredInputs consults the env first, so the same rejected
        // token was re-submitted until the attempts ran out, with no prompt ever shown.
        process.env['DOTCMS_AUTH_TOKEN'] = 'rejected-env-token';
        rejectsTwice();
        const password = jest.fn().mockResolvedValue('typed-by-hand');
        const port = {
            text: jest.fn(),
            password,
            select: jest.fn().mockResolvedValue('token'),
            multiSelect: jest.fn().mockResolvedValue(['cursor'])
        } as unknown as PromptPort;

        const result = await runSetup({
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: port,
            url: URL_
        });

        expect(password).toHaveBeenCalled();
        expect(result.outcomes[0].result).toBe('written');
    });

    it('does not re-submit the env value on the retry', async () => {
        process.env['DOTCMS_AUTH_TOKEN'] = 'rejected-env-token';
        rejectsTwice();
        const fetchSpy = globalThis.fetch as jest.Mock;
        const port = {
            text: jest.fn(),
            password: jest.fn().mockResolvedValue('typed-by-hand'),
            select: jest.fn().mockResolvedValue('token'),
            multiSelect: jest.fn().mockResolvedValue(['cursor'])
        } as unknown as PromptPort;

        await runSetup({
            url: URL_,
            agents: ['cursor'],
            scope: 'folder',
            cwd: dir,
            skipSkills: true,
            skipVerify: true,
            promptPort: port
        });

        const bearers = fetchSpy.mock.calls
            .map((c) => new Headers((c[1] as RequestInit)?.headers).get('authorization'))
            .filter(Boolean);
        // The rejected env token must appear at most once — the first attempt.
        expect(bearers.filter((b) => b === 'Bearer rejected-env-token').length).toBeLessThanOrEqual(
            1
        );
        expect(bearers).toContain('Bearer typed-by-hand');
    });
});
