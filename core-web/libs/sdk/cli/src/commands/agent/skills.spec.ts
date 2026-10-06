import * as childProcess from 'node:child_process';

import { installSkills } from './skills';

import type { Mock } from 'vitest';

/** See registry.spec.ts — namespace objects are non-configurable under ts-vi. */
vi.mock('node:child_process', async (importOriginal) => ({
    ...(await importOriginal<typeof childProcess>()),
    spawnSync: vi.fn()
}));

const spawnSync = childProcess.spawnSync as unknown as Mock;

describe('installSkills (FR-025, FR-026)', () => {
    afterEach(() => {
        vi.clearAllMocks();
    });

    it('makes ONE invocation covering all selected targets', async () => {
        spawnSync.mockReturnValue({ status: 0 });
        await installSkills({ agentIds: ['cursor', 'claude-code', 'codex'], global: false });
        expect(spawnSync).toHaveBeenCalledTimes(1);
    });

    it('targets the public agent-toolkit repository', async () => {
        spawnSync.mockReturnValue({ status: 0 });
        await installSkills({ agentIds: ['cursor'], global: false });
        expect(JSON.stringify(spawnSync.mock.calls[0])).toContain('dotCMS/agent-toolkit');
    });

    it('is non-fatal on failure and returns the exact command to re-run', async () => {
        spawnSync.mockReturnValue({ status: 1 });
        const result = await installSkills({ agentIds: ['cursor'], global: false });
        expect(result.ok).toBe(false);
        expect(result.command).toContain('skills add');
        expect(result.command).toContain('cursor');
    });

    it('passes no secret to the sub-process', async () => {
        spawnSync.mockReturnValue({ status: 0 });
        await installSkills({ agentIds: ['cursor'], global: true });
        expect(JSON.stringify(spawnSync.mock.calls[0])).not.toMatch(/AUTH_TOKEN|dot_/);
    });
});

describe('Windows needs a shell to run npx (FR-024a, FR-025)', () => {
    /**
     * `npx` on Windows is `npx.cmd`, and since the CVE-2024-27980 fix Node refuses to execute
     * `.cmd`/`.bat` without a shell. Windows is in scope (see `CAN_RESTRICT`, the `%APPDATA%`
     * path in the registry, research R5), so without this the connection check failed on EVERY
     * Windows run — after the configuration had been written correctly.
     *
     * Asserted by faking the platform, because CI here is not Windows and an untested claim
     * about another OS is worth very little.
     */
    const real = process.platform;
    const asPlatform = (value: string) =>
        Object.defineProperty(process, 'platform', { value, configurable: true });
    afterEach(() => asPlatform(real));

    it('passes shell: true on win32', () => {
        asPlatform('win32');
        spawnSync.mockReturnValue({ status: 0 });
        installSkills({ agentIds: ['cursor'], global: false });
        expect((spawnSync.mock.calls.at(-1) as unknown[])[2]).toMatchObject({ shell: true });
    });

    it('does not on posix', () => {
        asPlatform('darwin');
        spawnSync.mockReturnValue({ status: 0 });
        installSkills({ agentIds: ['cursor'], global: false });
        expect((spawnSync.mock.calls.at(-1) as unknown[])[2]).toMatchObject({ shell: false });
    });
});

describe('the child does not get our secrets (FR-022)', () => {
    /**
     * The doc comment on `installSkills` said "No secret is passed" — but `spawnSync` defaults
     * to `env: process.env`, so `npx skills add …` and every `postinstall` npm runs underneath
     * it received DOTCMS_AUTH_TOKEN and DOTCMS_PASSWORD whenever the developer supplied them by
     * environment, which our own `--password` help text recommends.
     */
    const OLD = process.env;
    beforeEach(() => {
        process.env = { ...OLD, DOTCMS_AUTH_TOKEN: 'dot_secret', DOTCMS_PASSWORD: 'hunter2' };
    });
    afterAll(() => {
        process.env = OLD;
    });

    it('strips DOTCMS_AUTH_TOKEN and DOTCMS_PASSWORD from the environment it passes', () => {
        spawnSync.mockReturnValue({ status: 0 });
        installSkills({ agentIds: ['cursor'], global: false });
        const env = (
            spawnSync.mock.calls.at(-1) as [string, string[], { env: NodeJS.ProcessEnv }]
        )[2].env;
        expect(env['DOTCMS_AUTH_TOKEN']).toBeUndefined();
        expect(env['DOTCMS_PASSWORD']).toBeUndefined();
        // Still a usable environment — PATH has to survive or npx cannot run.
        expect(env['PATH']).toBe(process.env['PATH']);
    });
});
