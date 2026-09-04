import * as childProcess from 'node:child_process';

import { installSkills } from './skills';

/** See registry.spec.ts — namespace objects are non-configurable under ts-jest. */
jest.mock('node:child_process', () => ({
    ...jest.requireActual('node:child_process'),
    spawnSync: jest.fn()
}));

const spawnSync = childProcess.spawnSync as unknown as jest.Mock;

describe('installSkills (FR-025, FR-026)', () => {
    afterEach(() => { jest.clearAllMocks(); });

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
