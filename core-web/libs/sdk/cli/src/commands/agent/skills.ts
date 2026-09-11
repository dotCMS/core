import * as childProcess from 'node:child_process';

import { SKILLS_SOURCE } from './constants';

import { envWithoutSecrets } from '../../shared/env';

export interface SkillsResult {
    ok: boolean;
    /** The exact command to re-run, printed when installation fails (FR-026). */
    command: string;
    reason?: string;
}

export function buildSkillsArgs(agentIds: string[], global: boolean): string[] {
    return [
        '-y',
        'skills',
        'add',
        SKILLS_SOURCE,
        ...agentIds.flatMap((id) => ['-a', id]),
        ...(global ? ['-g'] : []),
        '-y'
    ];
}

/**
 * One invocation covering every selected target.
 *
 * Failure is NON-FATAL (FR-026): the configuration work already done stands, and the developer
 * gets the exact command to finish the job. No secret is passed — the toolkit repository is
 * public, so nothing here needs credentials.
 */
export async function installSkills(args: {
    agentIds: string[];
    global: boolean;
}): Promise<SkillsResult> {
    const argv = buildSkillsArgs(args.agentIds, args.global);
    const command = `npx ${argv.join(' ')}`;
    try {
        const result = childProcess.spawnSync('npx', argv, {
            stdio: 'inherit',
            // The doc comment above says no secret is passed; `spawnSync` defaults to the whole
            // environment, so it was not true until this line.
            env: envWithoutSecrets(),
            // On Windows `npx` is `npx.cmd`, and since the CVE-2024-27980 fix Node refuses to
            // execute `.cmd`/`.bat` without a shell — so this spawn failed on every Windows run
            // and FR-025 never installed anything there.
            shell: process.platform === 'win32'
        });
        // `spawnSync` does NOT throw on a spawn failure: it returns `{ status: null, error }`.
        // The catch below never ran, and the developer got "exited with code null" instead of
        // `spawn npx ENOENT` — with no verbose mode, that message is all they get (FR-032a).
        if (result.error) return { ok: false, command, reason: result.error.message };
        if (result.status === 0) return { ok: true, command };
        return { ok: false, command, reason: `skills exited with code ${result.status}` };
    } catch (error) {
        return { ok: false, command, reason: (error as Error).message };
    }
}
