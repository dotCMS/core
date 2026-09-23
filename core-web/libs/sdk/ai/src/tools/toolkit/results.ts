import { isToolFailure, type ToolFailure } from './tool-runtime';

/**
 * What the two code tools (`search`, `execute`) resolve to: the capped text of the model's
 * code's result. An object rather than a bare string because some frameworks (Google ADK)
 * require tool results to be objects.
 */
export interface CodeToolResult {
    result: string;
}

/** A tool's model-facing output, in the shape the Vercel AI SDK's `toModelOutput` returns. */
export type ToolModelOutput = { type: 'text'; value: string } | { type: 'json'; value: unknown };

/**
 * How a tool shows one of its results to the model — the one place that is decided.
 *
 * `toText` is the TOOL's declaration that it has a text form, typed against its own result:
 * nothing is inferred from the value's shape, so a result can never be mistaken for text
 * because it happens to look like one. A tool without it is shown as structured JSON, and so
 * is a failure from any tool.
 */
export function renderToolOutput<TResult>(
    toText: ((result: TResult) => string) | undefined,
    output: TResult | ToolFailure
): ToolModelOutput {
    if (toText && !isToolFailure(output)) {
        return { type: 'text', value: toText(output as TResult) };
    }

    return { type: 'json', value: output };
}

/** A model output as the text a text-only transport (an MCP `content` block) carries. */
export function modelOutputText(output: ToolModelOutput): string {
    return output.type === 'text' ? output.value : JSON.stringify(output.value, null, 2);
}
