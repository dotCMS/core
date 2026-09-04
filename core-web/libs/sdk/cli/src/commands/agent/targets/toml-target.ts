import type { AgentTarget } from './types';
import type { Scope } from '../../../shared/types';

export interface TomlWriteArgs {
    target: AgentTarget;
    scope: Scope;
    url: string;
    token: string;
    cwd?: string;
}

/**
 * Codex is the only non-JSON target. The file belongs to the developer and will carry unrelated
 * tables and comments, so it must round-trip rather than be regenerated.
 */
export async function writeTomlTarget(_args: TomlWriteArgs): Promise<string> {
    throw new Error('not implemented');
}
