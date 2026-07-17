import type { SandboxResult } from './types';

/**
 * Default hard cap on the string handed back to the model (~6k tokens). A depth-1/2
 * `resolveRef` of even the largest schemas fits comfortably; a whole-`spec` dump does not.
 */
const DEFAULT_MAX_CHARS = 25_000;

export interface FormatSandboxResultOptions {
    /** Hard cap on the returned string (chars). Default {@link DEFAULT_MAX_CHARS}. */
    maxChars?: number;
    /** Tool-specific guidance appended inside the truncation notice. */
    truncationHint?: string;
}

/**
 * Render a {@link SandboxResult} into the single string a tool hands back to the model, and
 * hard-cap its length so one query can't flood the context window.
 *
 * There is NO truncation anywhere else on the result path — whatever the model's code returns is
 * stringified whole. This is the one place that bounds it. The cap is applied to the final
 * combined string in BOTH the success and error branches (an `HttpError` body embedded in an
 * error message can itself be huge). On truncation, a clear notice explains the cut and tells the
 * model how to narrow the query rather than silently dropping data.
 */
export function formatSandboxResult(
    result: SandboxResult,
    options?: FormatSandboxResultOptions
): string {
    const maxChars = options?.maxChars ?? DEFAULT_MAX_CHARS;

    let out: string;
    if (!result.success) {
        const errorMsg = result.error
            ? `${result.error.name}: ${result.error.message}`
            : 'Unknown error';
        const logs = result.logs.length > 0 ? `\nLogs:\n${result.logs.join('\n')}` : '';
        out = `Error: ${errorMsg}${logs}`;
    } else {
        const value =
            typeof result.value === 'string'
                ? result.value
                : JSON.stringify(result.value, null, 2);
        const logs = result.logs.length > 0 ? `\n\n--- Logs ---\n${result.logs.join('\n')}` : '';
        out = `${value}${logs}`;
    }

    if (out.length <= maxChars) return out;

    const hint = options?.truncationHint ? ` ${options.truncationHint}` : '';
    return (
        out.slice(0, maxChars) +
        `\n\n[output truncated at ${maxChars} of ${out.length} chars — refine the query: ` +
        `select specific paths/fields, use pick()/first(), or resolve one schema at a time.${hint}]`
    );
}
