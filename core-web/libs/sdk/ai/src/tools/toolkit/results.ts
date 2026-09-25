import { isToolFailure, type ToolFailure } from './tool-runtime';

/**
 * What the two code tools (`search`, `execute`) resolve to: the capped text of the model's
 * code's result. An object rather than a bare string because some frameworks (Google ADK)
 * require tool results to be objects.
 */
export interface CodeToolResult {
    result: string;
}

/** A JSON value: the shape the Vercel AI SDK requires of a `json` model output. */
export type JSONValue =
    | null
    | string
    | number
    | boolean
    | { [key: string]: JSONValue | undefined }
    | JSONValue[];

/** A tool's model-facing output, in the shape the Vercel AI SDK's `toModelOutput` returns. */
export type ToolModelOutput = { type: 'text'; value: string } | { type: 'json'; value: JSONValue };

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

    return { type: 'json', value: toJSONValue(output) };
}

/**
 * A value as the JSON it serializes to — what every transport sends and the model receives.
 * A `Date` becomes its ISO string and an `undefined` field drops out, here rather than out of
 * sight in the transport. Results are small plain-data manifests, so the round trip is cheap.
 */
function toJSONValue(value: unknown): JSONValue {
    const text = JSON.stringify(value);

    return text === undefined ? null : (JSON.parse(text) as JSONValue);
}

/** A model output as the text a text-only transport (an MCP `content` block) carries. */
export function modelOutputText(output: ToolModelOutput): string {
    return output.type === 'text' ? output.value : JSON.stringify(output.value, null, 2);
}
