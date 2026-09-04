import type { RunOptions, TargetOutcome } from '../../shared/types';

export interface SetupResult {
    outcomes: TargetOutcome[];
    exitCode: 0 | 1 | 2;
}

/** The flow, in the order fixed by contracts/cli-interface.md. */
export async function runSetup(_opts: Partial<RunOptions>): Promise<SetupResult> {
    throw new Error('not implemented');
}
