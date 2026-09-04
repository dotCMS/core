import { confirmConnection } from './connect';
import { installSkills } from './skills';
import { writeJsonTargetDetailed } from './targets/json-target';
import { getTarget, detectTargets, TARGET_IDS } from './targets/registry';

import { mintToken, verifyToken } from '../../shared/auth';
import { ENV_KEYS, readEnv } from '../../shared/env';
import { ConflictingAuthError, MissingInputError, UnknownTargetError } from '../../shared/errors';
import { checkReachable, resolveUrl } from '../../shared/instance';

import type { TargetId } from './targets/types';
import type { RunOptions, TargetOutcome, Token } from '../../shared/types';





export interface SetupResult {
    outcomes: TargetOutcome[];
    connection: 'ok' | 'failed' | 'skipped';
    connectionReason?: string;
    exitCode: 0 | 1 | 2;
}

/**
 * Exactly two authentication modes, and they are alternatives rather than a fallback chain
 * (FR-003a/b). Supplying both is a usage error rather than a silent preference: silent
 * precedence hides a mistake in exactly the scripted runs these options exist for.
 */
function resolveAuthMode(opts: Partial<RunOptions>): { token?: string; user?: string; password?: string } {
    const token = opts.authToken ?? readEnv(ENV_KEYS.authToken);
    const password = opts.password ?? readEnv(ENV_KEYS.password);
    const user = opts.user;

    if (opts.authToken && (opts.user || opts.password)) throw new ConflictingAuthError();
    if (token && (user || password)) throw new ConflictingAuthError();
    return { token, user, password };
}

function resolveTargets(opts: Partial<RunOptions>): TargetId[] {
    if (!opts.agents?.length) return [];
    for (const id of opts.agents) {
        if (!TARGET_IDS.includes(id as TargetId)) throw new UnknownTargetError(id, TARGET_IDS);
    }
    return opts.agents as TargetId[];
}

/**
 * The flow, in the order fixed by contracts/cli-interface.md.
 *
 * The ordering is the load-bearing part: NOTHING touches the filesystem until the token has
 * been verified (FR-008a). A failure before that point leaves no file, no directory and no
 * skills install, so a bad token cannot produce seven configurations that fail confusingly
 * later. `--yes` / `--force` govern confirmation prompts only and cannot disable it (FR-008c).
 */
export async function runSetup(opts: Partial<RunOptions>): Promise<SetupResult> {
    // 1. Usage errors first — before any network call or filesystem touch.
    const auth = resolveAuthMode(opts);
    const explicitTargets = resolveTargets(opts);
    const scope = opts.scope ?? 'folder';

    // 2. Instance.
    const url = await resolveUrl(opts);
    await checkReachable(url);

    // 3. Authenticate.
    let token: Token;
    if (auth.token) {
        token = { value: auth.token, origin: 'supplied', verified: false };
    } else if (auth.user && auth.password) {
        token = await mintToken({ url, user: auth.user, password: auth.password });
    } else {
        throw new MissingInputError(auth.user ? 'A password' : 'An authentication method');
    }

    // 4. Verify. Past this line, and only past it, may anything be written.
    token = await verifyToken(url, token);

    const targets = explicitTargets.length
        ? explicitTargets.map(getTarget)
        : await detectTargets();

    if (opts.skipMcp) {
        return { outcomes: [], connection: 'skipped', exitCode: 0 };
    }

    // 5. Write. One target's failure never stops the others and never rolls back what already
    //    succeeded (FR-020a, FR-020d) — a half-configured machine the developer can read beats
    //    an all-or-nothing unwind.
    const outcomes: TargetOutcome[] = [];
    const seen = new Set<string>();
    for (const target of targets) {
        const file = target.configPath(scope, opts.cwd);
        if (!file || seen.has(file)) continue;
        seen.add(file);
        try {
            const written = await writeJsonTargetDetailed({
                target, scope, url, token: token.value, cwd: opts.cwd
            });
            outcomes.push({
                targetId: target.id,
                scope,
                path: written.path,
                result: written.replacedExisting ? 'replaced' : 'written',
                reason: null,
                permissionsApplied: written.permissionsApplied,
                skillsInstalled: 'no'
            });
        } catch (error) {
            outcomes.push({
                targetId: target.id,
                scope,
                path: file,
                result: 'failed',
                reason: (error as Error).message,
                permissionsApplied: false,
                skillsInstalled: 'no'
            });
        }
    }

    // 6. Skills — non-fatal by design (FR-026).
    if (!opts.skipSkills) {
        const ids = outcomes
            .filter((o) => o.result !== 'failed')
            .map((o) => getTarget(o.targetId as TargetId))
            .filter((t) => t.skillsAgentId)
            .map((t) => t.skillsAgentId as string);
        if (ids.length) {
            const skills = await installSkills({ agentIds: ids, global: scope === 'global' });
            for (const o of outcomes) {
                if (o.result === 'failed') continue;
                // VS Code's skills location is the Copilot CLI directory and is unconfirmed for
                // the in-editor agent, so it is never reported as installed (FR-027).
                o.skillsInstalled = !skills.ok ? 'no' : o.targetId === 'vscode' ? 'unverified' : 'yes';
            }
        }
    }

    // 7. Prove the agent connects (FR-024a).
    let connection: SetupResult['connection'] = 'skipped';
    let connectionReason: string | undefined;
    if (!opts.skipVerify) {
        const result = await confirmConnection({ url, token: token.value });
        connection = result.ok ? 'ok' : 'failed';
        if (!result.ok) connectionReason = `${result.cause}: ${result.detail}`;
    }

    const anyFailed = outcomes.some((o) => o.result === 'failed') || connection === 'failed';
    return { outcomes, connection, connectionReason, exitCode: anyFailed ? 1 : 0 };
}
