import type { RunOptions } from './types';

export interface InstanceInfo {
    /** Normalized base URL, no trailing slash. */
    url: string;
    /** Instance version from /api/v1/appconfiguration, or null when absent/unreadable. */
    version: string | null;
}

/** Resolve the instance address: option -> environment -> prompt (FR-004). */
export async function resolveUrl(_opts: Partial<RunOptions>): Promise<string> {
    throw new Error('not implemented');
}

/** Confirm the instance is reachable before asking for credentials (FR-005). */
export async function checkReachable(_url: string): Promise<InstanceInfo> {
    throw new Error('not implemented');
}

/**
 * Warn when the instance is older than this tool (FR-005a, ADR-0019).
 * MUST fail open: returns null rather than throwing when the version is absent or unreadable.
 */
export function compatibilityWarning(
    _instanceVersion: string | null,
    _toolVersion: string
): string | null {
    throw new Error('not implemented');
}
