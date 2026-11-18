import { buildEntry } from './entry';

import { hasEntry, type WriteResult, writeMerged } from '../../../shared/config-file';
import { NoConfigPathError } from '../../../shared/errors';
import { ENTRY_KEY } from '../constants';

import type { AgentTarget, WriteArgs } from './types';

export { buildEntry };

/** Is our entry already in this file? Read-only: FR-017's confirmation must happen before
 *  anything is modified, not as a rollback afterwards. */
export function hasJsonEntry(file: string, target: AgentTarget): Promise<boolean> {
    return hasEntry({ file, containerKey: target.containerKey, entryKey: ENTRY_KEY });
}

export async function writeJsonTarget(args: WriteArgs): Promise<WriteResult> {
    const file = args.target.configPath(args.scope, args.cwd);
    if (!file) throw new NoConfigPathError(args.target.displayName, args.scope);
    return writeMerged({
        file,
        containerKey: args.target.containerKey,
        entryKey: ENTRY_KEY,
        entry: buildEntry(args.target, args.url, args.token)
    });
}
