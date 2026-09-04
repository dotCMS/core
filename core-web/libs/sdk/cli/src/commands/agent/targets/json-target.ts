import { type WriteResult, writeMerged } from '../../../shared/config-file';
import { ENTRY_KEY, MCP_SERVER_PACKAGE, SERVER_ENV } from '../constants';

import type { AgentTarget } from './types';
import type { Scope } from '../../../shared/types';

export interface WriteArgs {
    target: AgentTarget;
    scope: Scope;
    url: string;
    token: string;
    /** Base directory for folder scope. Defaults to `process.cwd()`. */
    cwd?: string;
}

/** The entry each editor expects. OpenCode differs structurally, not merely by key. */
export function buildEntry(target: AgentTarget, url: string, token: string): unknown {
    const env = { [SERVER_ENV.url]: url, [SERVER_ENV.token]: token };
    if (target.entryShape === 'opencode-local') {
        return {
            type: 'local',
            command: ['npx', '-y', MCP_SERVER_PACKAGE],
            enabled: true,
            environment: env
        };
    }
    return { type: 'stdio', command: 'npx', args: ['-y', MCP_SERVER_PACKAGE], env };
}

export async function writeJsonTarget(args: WriteArgs): Promise<string> {
    const result = await writeJsonTargetDetailed(args);
    return result.path;
}

export async function writeJsonTargetDetailed(args: WriteArgs): Promise<WriteResult> {
    const file = args.target.configPath(args.scope, args.cwd);
    if (!file) throw new Error(`${args.target.displayName} has no configuration file at ${args.scope} scope.`);
    return writeMerged({
        file,
        containerKey: args.target.containerKey,
        entryKey: ENTRY_KEY,
        entry: buildEntry(args.target, args.url, args.token)
    });
}
