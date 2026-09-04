import { parse as parseToml } from 'smol-toml';

import { confirmConnection } from './connect';
import { ENTRY_KEY } from './constants';
import { protectFromVersionControl, type GitignoreOutcome } from './gitignore';
import { installSkills } from './skills';
import { writeJsonTargetDetailed } from './targets/json-target';
import { getTarget, detectTargets, TARGETS, TARGET_IDS } from './targets/registry';
import { writeTomlTarget } from './targets/toml-target';

import { mintToken, verifyToken } from '../../shared/auth';
import { CAN_RESTRICT, hasEntry } from '../../shared/config-file';
import { ENV_KEYS, readEnv } from '../../shared/env';
import {
    ConflictingAuthError,
    CredentialsRejectedError,
    TokenRejectedError,
    UnknownTargetError
} from '../../shared/errors';
import { checkReachable } from '../../shared/instance';
import { resolveInstanceUrl, resolveRequiredInputs } from '../../shared/prompts';

import type { TargetId } from './targets/types';
import type { RunOptions, TargetOutcome, Token } from '../../shared/types';





/** Three, matching FR-007. A fourth prompt after three refusals is nagging, not helping. */
const MAX_AUTH_ATTEMPTS = 3;

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

    if (token && (user || password)) {
        // Name the source the developer actually used, flag or environment variable.
        const tokenSource = opts.authToken ? '--authToken' : ENV_KEYS.authToken;
        const credentialSource = opts.user
            ? '--user'
            : opts.password
              ? '--password'
              : ENV_KEYS.password;
        throw new ConflictingAuthError(tokenSource, credentialSource);
    }
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

    const step = opts.onProgress ?? (() => undefined);

    // 2. The address first, and CHECKED first. Only once the instance is confirmed to be a
    //    real dotCMS is anyone asked for a credential: a password typed against a wrong
    //    address is wasted effort, and the failure would land after the work rather than
    //    before it.
    const url = await resolveInstanceUrl(opts, opts.promptPort);
    step(`Checking ${url}`);
    await checkReachable(url);

    // 3. Now the credential — prompting only for what is missing, and only where there is a
    //    terminal to ask on (FR-003i, FR-003k).
    let inputs = await resolveRequiredInputs(
        { ...opts, url, authToken: auth.token, user: auth.user, password: auth.password },
        opts.promptPort
    );

    // 4. Authenticate and verify, retrying a REJECTION up to three times (FR-007).
    //
    // Both halves are inside the loop on purpose. A supplied token that the instance refuses is
    // the same user error as a mistyped password — asking again is obviously right, and failing
    // outright after one bad paste is not. A rejection is retried; anything else (unreachable
    // instance, TLS, a 500) is not, because re-typing a credential cannot fix it.
    let token: Token;
    for (let attempt = 1; ; attempt++) {
        try {
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

            // Past this line, and only past it, may anything be written.
            step('Verifying the token');
            token = await verifyToken(url, token);
            break;
        } catch (error) {
            const rejected =
                error instanceof TokenRejectedError || error instanceof CredentialsRejectedError;
            if (!rejected || !opts.promptPort || attempt >= MAX_AUTH_ATTEMPTS) throw error;

            opts.onAuthRetry?.((error as Error).message, attempt, MAX_AUTH_ATTEMPTS);
            // Ask again from scratch: the url is settled, the credential is what was wrong.
            inputs = await resolveRequiredInputs({ url, cwd: opts.cwd }, opts.promptPort);
        }
    }

    // Targets: explicit --agent wins. Otherwise ASK when there is someone to ask (FR-010),
    // and fall back to every detected editor when there is not (FR-003j). Defaulting silently
    // in an interactive run would write to editors the developer never chose.
    let targets;
    if (explicitTargets.length) {
        targets = explicitTargets.map(getTarget);
    } else {
        // Use the objects detection returned, never re-derive them from the registry by id:
        // that silently replaces whatever the caller resolved, which is a real bug and not
        // only a testing inconvenience.
        const detected = await detectTargets();
        const detectedById = new Map(detected.map((t) => [t.id as string, t]));
        if (opts.promptPort) {
            const chosen = await opts.promptPort.multiSelect(
                'Which editors should we configure?',
                TARGETS.map((t) => ({
                    name: t.displayName,
                    value: t.id,
                    checked: detectedById.has(t.id)
                }))
            );
            targets = chosen.map((id) => detectedById.get(id) ?? getTarget(id as TargetId));
        } else {
            targets = detected;
        }
    }

    // 5. Write — unless asked not to. `--skip-mcp` skips WRITING, nothing else: the flags are
    //    documented as independent, and returning here also skipped the skills install and the
    //    summary, so `--skip-mcp` alone did nothing at all and said nothing about it.
    //    The connection check is skipped implicitly, since there is no configuration to prove
    //    (FR-024b). One target's failure never stops the others and never rolls back what already
    //    succeeded (FR-020a, FR-020d) — a half-configured machine the developer can read beats
    //    an all-or-nothing unwind.
    // 7. Deduplicate BEFORE counting: `--agent cursor --agent cursor` announced two editors
    //    and wrote one, and two targets can legitimately resolve to the same file.
    const byId = new Map(targets.map((t) => [t.id as string, t]));
    const plan: { target: (typeof targets)[number]; file: string }[] = [];
    const seen = new Set<string>();
    for (const target of targets) {
        const file = target.configPath(scope, opts.cwd);
        if (!file || seen.has(file)) continue;
        seen.add(file);
        plan.push({ target, file });
    }

    const outcomes: TargetOutcome[] = [];
    if (opts.skipMcp) {
        // Say so. Returning an empty summary made `--skip-mcp` look like a no-op run.
        for (const { target, file } of plan) {
            outcomes.push({
                targetId: target.id,
                scope,
                path: file,
                result: 'skipped',
                reason: 'configuration writing skipped (--skip-mcp)',
                permissionsApplied: false,
                skillsInstalled: 'no'
            });
        }
    }
    if (!opts.skipMcp && plan.length) {
        step(`Writing configuration for ${plan.length} editor${plan.length === 1 ? '' : 's'}`);
    }
    for (const { target, file } of opts.skipMcp ? [] : plan) {
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
    // Only files actually written. `skipped` outcomes carry a path so the summary can name
    // them, and including those made --skip-mcp announce "these files now contain an access
    // token" about files that were never created.
    const written = outcomes
        .filter((o) => (o.result === 'written' || o.result === 'replaced') && o.path)
        .map((o) => o.path as string);
    if (scope === 'folder' && written.length) {
        versionControl = await protectFromVersionControl({
            files: written,
            cwd: opts.cwd ?? process.cwd(),
            confirmExclude: opts.yes ? async () => true : opts.confirmExclude
        });
    }

    // 7. Skills — non-fatal by design (FR-026).
    if (!opts.skipSkills) {
        // Driven by the SELECTED targets, not by successful writes. Deriving it from writes
        // meant `--skip-mcp` silently installed nothing, even though the two flags are
        // independent. A target whose write failed is still excluded — its editor is not
        // configured, so skills for it would be half a job.
        const eligible = opts.skipMcp
            ? plan.map((p) => p.target)
            : outcomes
                  .filter((o) => o.result !== 'failed')
                  .map((o) => byId.get(o.targetId))
                  .filter((t): t is NonNullable<typeof t> => Boolean(t));
        const ids = eligible
            .filter((t) => Boolean(t.skillsAgentId))
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
    if (!opts.skipVerify && !opts.skipMcp) {
        step('Starting the server to confirm it responds (this can take a minute on a cold npx cache)');
        const result = await confirmConnection({ url, token: token.value });
        connection = result.ok ? 'ok' : 'failed';
        if (!result.ok) connectionReason = `${result.cause}: ${result.detail}`;
    }

    const anyFailed = outcomes.some((o) => o.result === 'failed') || connection === 'failed';
    return { outcomes, versionControl, connection, connectionReason, exitCode: anyFailed ? 1 : 0 };
}
