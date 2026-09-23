/**
 * What the two code tools (`search`, `execute`) resolve to: the capped text of the model's
 * code's result. An object rather than a bare string because some frameworks (Google ADK)
 * require tool results to be objects.
 */
export interface CodeToolResult {
    result: string;
}

/** Whether a tool result is a {@link CodeToolResult}: exactly one key, `result`, a string. */
export function isCodeToolResult(value: unknown): value is CodeToolResult {
    if (typeof value !== 'object' || value === null) {
        return false;
    }
    const keys = Object.keys(value);

    return (
        keys.length === 1 &&
        keys[0] === 'result' &&
        typeof (value as { result?: unknown }).result === 'string'
    );
}

/**
 * Any tool result as the text a text-only transport (an MCP `content` block) hands the model:
 * a code tool's text as-is, everything else — a manifest, a `ToolFailure` — as pretty JSON.
 *
 * The one place that rendering is decided, so an MCP host and `toModelOutput` show the model
 * the same thing.
 */
export function toolResultText(result: unknown): string {
    if (isCodeToolResult(result)) {
        return result.result;
    }
    if (typeof result === 'string') {
        return result;
    }

    return JSON.stringify(result, null, 2);
}

/** A tool's model-facing output, in the shape the Vercel AI SDK's `toModelOutput` returns. */
export type ToolModelOutput = { type: 'text'; value: string } | { type: 'json'; value: unknown };

/**
 * What a model sees of a tool result in frameworks that ask (the AI SDK's `toModelOutput`):
 * a code tool's text as text — not a JSON string escaped inside an object — and everything else
 * as structured JSON.
 */
export function toModelOutput({ output }: { output: unknown }): ToolModelOutput {
    return isCodeToolResult(output)
        ? { type: 'text', value: output.result }
        : { type: 'json', value: output };
}
