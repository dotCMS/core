import type { ToolModelOutput } from './results';
import type { ToolFailure } from './tool-runtime';
import type { RuntimeAllow } from '../../runtime';
import type { z } from 'zod';

/**
 * Behavior hints a host forwards to its framework — they map one-to-one onto MCP's
 * `ToolAnnotations`. Hints, not guarantees: `execute` is marked destructive because the code a
 * model writes for it CAN delete content, not because every call does.
 */
export interface DotCMSToolAnnotations {
    readOnlyHint: boolean;
    destructiveHint: boolean;
    idempotentHint: boolean;
    openWorldHint: boolean;
}

// Tool options describe the TOOL. Who is calling which instance lives in the connection
// (`dotcmsConnection`), and which endpoints a fixed-purpose tool calls is the tool's own
// business — `page_create` knows it needs folders, content types and the workflow fire
// endpoint, so a consumer never has to know those paths and cannot break the tool by leaving
// one out.

/** Options for `executeTool`. */
export interface ExecuteToolOptions {
    /**
     * Allow-list or policy bounding what the model's code can reach — the same one
     * `createRuntime` takes. The one tool that has it: here the MODEL chooses the endpoints,
     * so this is the consumer governing the model, not configuring the tool.
     */
    allow?: RuntimeAllow;
    /** Sandbox wall-clock timeout (ms) for the model's code. Default 45000. */
    timeout?: number;
    /** Pass host stack traces through to the code's error results. Default false. */
    includeStacks?: boolean;
}

/** Options for the tools that call the API directly: the page and asset tools. */
export interface RequestToolOptions {
    /** Deadline (ms) for each request. Default 30000. */
    requestTimeout?: number;
}

/** Options for `uploadAssetsTool` and `downloadAssetsTool`, which touch the local disk. */
export interface AssetToolOptions extends RequestToolOptions {
    /**
     * The local directory the model's `src` / `dest` must stay inside — symlinks resolved.
     * Required, because the paths come from the model: without a boundary a hosted server's
     * `upload_assets` reads any directory the process can, and `download_assets` writes into
     * any directory it can. `'/'` is the explicit "whole disk" choice, for a local agent acting
     * as its own user.
     */
    root: string;
}

/**
 * One dotCMS tool, shaped the way the Vercel AI SDK and the MCP TypeScript SDK both expect:
 * pass it straight into AI SDK's `tools`, or into MCP's `registerTool` config. Every result is
 * an object, as Google ADK requires.
 *
 * `execute` never throws. It validates `input` (it comes from the model, so this is the trust
 * boundary), runs the tool, and resolves to the tool's result — or to a {@link ToolFailure}
 * carrying a stable `code` and a `retryable` flag, because a model cannot `instanceof` its
 * way through a failure.
 *
 * `input` is typed from the tool's own schema, so code that calls a tool directly gets
 * completion and a compile error for a wrong field. That is a convenience for the caller, not
 * the check: `execute` still validates at run time, because what reaches it from a model was
 * never type-checked. A host that holds several tools in one list and calls whichever the
 * model picked types that list as {@link AnyDotCMSTool}.
 */
export interface DotCMSTool<
    TInput extends z.ZodObject = z.ZodObject,
    TResult = unknown
> extends DotCMSToolBase<TResult> {
    readonly inputSchema: TInput;
    execute(input: z.input<TInput>): Promise<TResult | ToolFailure>;
}

/**
 * Any dotCMS tool, whatever its schema — the element type of a registry: `AnyDotCMSTool[]`, or
 * a map from tool name to tool. `execute` takes `unknown`, because a registry dispatches on the
 * name the model chose and hands over the model's arguments as they came; `execute` validates
 * them. Every {@link DotCMSTool} is assignable to it.
 */
export interface AnyDotCMSTool extends DotCMSToolBase<unknown> {
    readonly inputSchema: z.ZodObject;
    execute(input: unknown): Promise<unknown>;
}

/** What every tool has, whatever its input: identity, annotations and rendering. */
interface DotCMSToolBase<TResult> {
    /**
     * The name the tool's description expects to be called by. The descriptions refer to their
     * siblings by these names ("use the `search` tool first"), so register each tool under it.
     */
    readonly name: string;
    readonly title: string;
    /** The model-facing description — tuned prompt text. */
    readonly description: string;
    readonly annotations: DotCMSToolAnnotations;
    /**
     * What the model sees of a result, for frameworks that ask (the AI SDK picks this up on its
     * own): text for the tools that declare a text form (`search`, `execute`), structured JSON
     * for the rest and for any failure.
     */
    toModelOutput(options: { output: TResult | ToolFailure }): ToolModelOutput;
    /**
     * The same rendering, as the text a text-only transport such as an MCP `content` block
     * carries: a text tool's output as-is, everything else pretty-printed JSON.
     */
    toText(output: TResult | ToolFailure): string;
}
