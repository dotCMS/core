import { parse as parseToml } from 'smol-toml';

import { confirmConnection } from './connect';
import { ENTRY_KEY } from './constants';
import { protectFromVersionControl, type GitignoreOutcome } from './gitignore';
import { installSkills } from './skills';
import { writeJsonTargetDetailed } from './targets/json-target';
import { getTarget, detectTargets, TARGET_IDS } from './targets/registry';
import { writeTomlTarget } from './targets/toml-target';

import { mintToken, verifyToken } from '../../shared/auth';
import { CAN_RESTRICT, hasEntry } from '../../shared/config-file';
import { ENV_KEYS, readEnv } from '../../shared/env';
import { ConflictingAuthError, UnknownTargetError } from '../../shared/errors';
import { checkReachable } from '../../shared/instance';
import { resolveRequiredInputs } from '../../shared/prompts';

import type { TargetId } from './targets/types';
import type { RunOptions, TargetOutcome, Token } from '../../shared/types';





export interface SetupResult {
    outcomes: TargetOutcome[];
    /** Present only for folder scope, which is the default and therefore the common case. */
    versionControl?: GitignoreOutcome;
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

    // 2. Resolve the required inputs — prompting only for what is missing, and only where
    //    there is a terminal to ask on (FR-003i, FR-003k).
    const inputs = await resolveRequiredInputs(
        { ...opts, authToken: auth.token, user: auth.user, password: auth.password },
        opts.promptPort
    );
    const url = inputs.url;
    const step = opts.onProgress ?? (() => undefined);

    step(`Checking ${url} is reachable`);
    await checkReachable(url);

    // 3. Authenticate.
    let token: Token;
    if (inputs.authToken) {
        token = { value: inputs.authToken, origin: 'supplied', verified: false };
    } else {
        step('Minting an access token');
        token = await mintToken({
            url,
            user: inputs.user as string,
            password: inputs.password as string
        });
    }

    // 4. Verify. Past this line, and only past it, may anything be written.
    step('Verifying the token');
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
    step(`Writing configuration for ${targets.length} editor${targets.length === 1 ? '' : 's'}`);
    const outcomes: TargetOutcome[] = [];
    const byId = new Map(targets.map((t) => [t.id as string, t]));
    const seen = new Set<string>();
    for (const target of targets) {
        const file = target.configPath(scope, opts.cwd);
        if (!file || seen.has(file)) continue;
        seen.add(file);
        try {
            // Ask BEFORE replacing (FR-017). --force and --yes skip the question, never the
            // token verification that already happened above.
            const isToml = target.format === 'toml';
            const existing = await hasEntry({
                file,
                containerKey: target.containerKey,
                entryKey: ENTRY_KEY,
                parse: isToml ? (raw) => parseToml(raw) as Record<string, unknown> : undefined
            });
            if (existing && !opts.force && !opts.yes && opts.confirmOverwrite) {
                const proceed = await opts.confirmOverwrite(file);
                if (!proceed) {
                    outcomes.push({
                        targetId: target.id, scope, path: file, result: 'skipped',
                        reason: 'left the existing entry in place',
                        permissionsApplied: false, skillsInstalled: 'no'
                    });
                    continue;
                }
            }

            // The registry's `format` selects the writer; the flow branches on nothing
            // target-specific (FR-013).
            const written = isToml
                ? {
                      path: await writeTomlTarget({ target, scope, url, token: token.value, cwd: opts.cwd }),
                      permissionsApplied: CAN_RESTRICT,
                      replacedExisting: existing
                  }
                : await writeJsonTargetDetailed({ target, scope, url, token: token.value, cwd: opts.cwd });

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

    // 6. Version-control safety. Folder scope is the default, so a token has just landed in a
    //    working directory on almost every run — naming the files is not optional (FR-023).
    //    `--yes` takes the SAFE answer here rather than skipping the step: this is the one
    //    confirmation where the conventional meaning of -y would be actively harmful.
    let versionControl: GitignoreOutcome | undefined;
    const written = outcomes.filter((o) => o.result !== 'failed' && o.path).map((o) => o.path as string);
    if (scope === 'folder' && written.length) {
        versionControl = await protectFromVersionControl({
            files: written,
            cwd: opts.cwd ?? process.cwd(),
            confirmExclude: opts.yes ? async () => true : opts.confirmExclude
        });
    }

    // 7. Skills — non-fatal by design (FR-026).
    if (!opts.skipSkills) {
        const ids = outcomes
            .filter((o) => o.result !== 'failed')
            .map((o) => byId.get(o.targetId))
            .filter((t): t is NonNullable<typeof t> => Boolean(t?.skillsAgentId))
            .map((t) => t.skillsAgentId as string);
        if (ids.length) {
            step('Installing the dotCMS skills');
            const skills = await installSkills({ agentIds: ids, global: scope === 'global' });
            for (const o of outcomes) {
                if (o.result === 'failed') continue;
                // FR-027: never claim skills landed where the editor is not confirmed to read
                // them. Driven by the registry rather than a hardcoded editor id.
                const target = byId.get(o.targetId);
                o.skillsInstalled = !skills.ok
                    ? 'no'
                    : target?.skillsLocationVerified
                      ? 'yes'
                      : 'unverified';
            }
        }
    }

    // 8. Prove the agent connects (FR-024a).
    let connection: SetupResult['connection'] = 'skipped';
    let connectionReason: string | undefined;
    if (!opts.skipVerify) {
        step('Starting the server to confirm it responds (this can take a minute on a cold npx cache)');
        const result = await confirmConnection({ url, token: token.value });
        connection = result.ok ? 'ok' : 'failed';
        if (!result.ok) connectionReason = `${result.cause}: ${result.detail}`;
    }

    const anyFailed = outcomes.some((o) => o.result === 'failed') || connection === 'failed';
    return { outcomes, versionControl, connection, connectionReason, exitCode: anyFailed ? 1 : 0 };
}
