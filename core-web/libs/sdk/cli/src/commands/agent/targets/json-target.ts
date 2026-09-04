import type { AgentTarget } from './types';
import type { Scope } from '../../../shared/types';

export interface WriteArgs {
    target: AgentTarget;
    scope: Scope;
    url: string;
    token: string;
}

/** Write (or replace) the single `dotcms` entry, merging into whatever is already there. */
export async function writeJsonTarget(_args: WriteArgs): Promise<string> {
    throw new Error('not implemented');
}
