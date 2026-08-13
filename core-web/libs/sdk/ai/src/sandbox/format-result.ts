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
        // Logs are built BEFORE the value is stringified. Serialization can fail (see
        // `stringifyValue`), and when it did, the throw escaped this function with the logs
        // still unattached — so every `console.log` the model had written to debug its code
        // was discarded at the exact moment it became most useful.
        const logs = result.logs.length > 0 ? `\n\n--- Logs ---\n${result.logs.join('\n')}` : '';
        const value =
            typeof result.value === 'string' ? result.value : stringifyValue(result.value);
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

/**
 * `JSON.stringify` for a value that arrived over `postMessage`.
 *
 * The two serializers do NOT agree on what is representable. `postMessage` uses structured
 * clone, which handles circular references and `BigInt`; JSON handles neither. So a worker
 * could report `success: true`, the value could transfer intact, and stringifying it here
 * would then throw `TypeError: Converting circular structure to JSON` — out of a function
 * whose job is to REPORT the result. It presented as "the tool breaks only when my code
 * succeeds", which is about the worst shape a failure can take.
 *
 * Circular references are not exotic here: building a tree from a flat folder or page list
 * with parent back-pointers is the ordinary way to do it. They are replaced with a marker
 * rather than dropped, so the model can see the shape it produced and why it could not be
 * returned whole.
 */
function stringifyValue(value: unknown): string {
    try {
        return JSON.stringify(value, circularSafeReplacer(), 2) ?? String(value);
    } catch (error) {
        const reason = error instanceof Error ? error.message : String(error);

        return (
            `[value could not be serialized: ${reason}]\n` +
            `The worker returned this value successfully, but it cannot be represented as JSON. ` +
            `Return a plain-data projection instead — e.g. pick(items, ['id','title']) — rather ` +
            `than the object graph itself.`
        );
    }
}

/** A replacer that survives cycles and BigInt, the two structured-clone/JSON mismatches. */
function circularSafeReplacer(): (key: string, value: unknown) => unknown {
    const seen = new WeakSet<object>();

    return function replacer(this: unknown, _key: string, value: unknown) {
        if (typeof value === 'bigint') {
            // BigInt has no JSON representation at all — stringify throws on it outright.
            return `${value.toString()}n`;
        }
        if (typeof value === 'object' && value !== null) {
            if (seen.has(value)) {
                return '[Circular]';
            }
            seen.add(value);
        }

        return value;
    };
}
