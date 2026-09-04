import type { TargetOutcome } from './types';

export interface SummaryInput {
    outcomes: TargetOutcome[];
    connection: 'ok' | 'failed' | 'skipped';
    connectionReason?: string;
}

/** Render the end-of-run summary (FR-028, FR-024e). */
export function renderSummary(_input: SummaryInput): string {
    throw new Error('not implemented');
}
