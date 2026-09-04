import type { AgentTarget, TargetId } from './types';

/** All seven supported editors (SC-006). */
export const TARGETS: readonly AgentTarget[] = [];

export function getTarget(_id: TargetId): AgentTarget {
    throw new Error('not implemented');
}

export async function detectTargets(): Promise<AgentTarget[]> {
    throw new Error('not implemented');
}
