import * as childProcess from 'node:child_process';

import { installSkills } from './skills';

describe('installSkills (FR-025, FR-026)', () => {
    afterEach(() => { jest.restoreAllMocks(); });

    it('makes ONE invocation covering all selected targets', async () => {
        const spawn = jest.spyOn(childProcess, 'spawnSync').mockReturnValue({ status: 0 } as never);
        await installSkills({ agentIds: ['cursor', 'claude-code', 'codex'], global: false });
        expect(spawn).toHaveBeenCalledTimes(1);
    });

    it('targets the public agent-toolkit repository', async () => {
        const spawn = jest.spyOn(childProcess, 'spawnSync').mockReturnValue({ status: 0 } as never);
        await installSkills({ agentIds: ['cursor'], global: false });
        expect(JSON.stringify(spawn.mock.calls[0])).toContain('dotCMS/agent-toolkit');
    });

    it('is non-fatal on failure and returns the exact command to re-run', async () => {
        jest.spyOn(childProcess, 'spawnSync').mockReturnValue({ status: 1 } as never);
        const result = await installSkills({ agentIds: ['cursor'], global: false });
        expect(result.ok).toBe(false);
        expect(result.command).toContain('skills add');
        expect(result.command).toContain('cursor');
    });

    it('passes no secret to the sub-process', async () => {
        const spawn = jest.spyOn(childProcess, 'spawnSync').mockReturnValue({ status: 0 } as never);
        await installSkills({ agentIds: ['cursor'], global: true });
        expect(JSON.stringify(spawn.mock.calls[0])).not.toMatch(/AUTH_TOKEN|dot_/);
    });
});
