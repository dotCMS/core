import { type Result } from '@dotcms/http';

/**
 * Turns a dependency-install `Result` into an explicit report.
 *
 * This exists because of contract X7. `Err(val)` is `{ ok: false, val }` — a truthy object — and
 * the caller tested `if (!result)`, which is never true. The failure branch was therefore
 * unreachable and a failed `npm install` was reported to the user as success. Branching on a
 * string-discriminated report makes that mistake impossible to repeat by accident.
 */
export type InstallReport = { kind: 'installed' } | { kind: 'failed'; reason: string };

function describe(value: unknown): string {
    if (value instanceof Error) {
        return value.message;
    }

    return typeof value === 'string' ? value : JSON.stringify(value);
}

export function reportInstallResult(result: Result<unknown, unknown>): InstallReport {
    // `result.ok`, never `!result` — see the spec's "Err() is truthy" case.
    if (result.ok) {
        return { kind: 'installed' };
    }

    return { kind: 'failed', reason: describe(result.val) };
}
