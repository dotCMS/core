import { hasJsonEntry, writeJsonTarget } from './json-target';
import { hasTomlEntry, writeTomlTarget } from './toml-target';

import type { AgentTarget, WriteArgs } from './types';
import type { WriteResult } from '../../../shared/config-file';

/**
 * Everything a format needs the flow to be able to do.
 *
 * `format` was the one editor-specific field that had leaked into the setup flow: it selected a
 * parser, selected a writer, and — because the writers returned different types — forced the
 * flow to hand-assemble half a result. That is the branch FR-013 exists to forbid, and it also
 * meant an eighth editor in a third format would be a third arm in `setup.ts` rather than one
 * object literal in the registry.
 */
export interface TargetWriter {
    hasEntry(file: string, target: AgentTarget): Promise<boolean>;
    write(args: WriteArgs): Promise<WriteResult>;
}

export const WRITERS: Record<AgentTarget['format'], TargetWriter> = {
    json: { hasEntry: hasJsonEntry, write: writeJsonTarget },
    toml: { hasEntry: hasTomlEntry, write: writeTomlTarget }
};
