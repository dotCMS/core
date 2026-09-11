import { ENTRY_KEY, MCP_SERVER_PACKAGE, SERVER_ENV } from '../constants';

import type { AgentTarget } from './types';

export { ENTRY_KEY };

/**
 * The entry each editor expects. OpenCode differs structurally, not merely by key.
 *
 * Format-agnostic on purpose: it branches on the registry's `entryShape`, never on a target id,
 * and both writers serialize whatever it returns. Living in `json-target.ts` meant the TOML
 * writer imported from the JSON writer for something neither owns.
 */
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
