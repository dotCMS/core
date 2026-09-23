import type { ToolFailure } from './tool-runtime';
import type { RequestCallEvent, RuntimeAllow } from '../../runtime';
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

/**
 * Options every tool factory accepts: where the instance is, and hooks to observe it.
 *
 * Deliberately nothing about WHICH endpoints a tool calls. A fixed-purpose tool owns that —
 * `page_create` knows it needs folders, content types and the workflow fire endpoint — so a
 * consumer never has to know those paths, and cannot break the tool by leaving one out.
 */
export interface DotCMSToolOptions {
    /** dotCMS instance URL. Defaults to `DOTCMS_URL` from the environment, read on each call. */
    url?: string;
    /**
     * dotCMS API token. Defaults to `AUTH_TOKEN` from the environment, read on each call.
     * Injected host-side — it never reaches the model or the sandbox.
     */
    token?: string;
    /** Observability hook fired around each request (token and sensitive bodies are never passed). */
    onCall?: (event: RequestCallEvent) => void;
    /** Called when loading instance context (sites, content types, …) fails. */
    onContextError?: (label: string, error: unknown) => void;
}

/** Options for `executeTool`. */
export interface ExecuteToolOptions extends DotCMSToolOptions {
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
export interface RequestToolOptions extends DotCMSToolOptions {
    /** Deadline (ms) for each request. Default 30000. */
    requestTimeout?: number;
}

/**
 * One dotCMS tool, shaped the way the Vercel AI SDK and the MCP TypeScript SDK both expect:
 * pass it straight into AI SDK's `tools`, or into MCP's `registerTool` config.
 *
 * `execute` never throws. It validates `input` (it comes from the model, so this is the trust
 * boundary), runs the tool, and resolves to the tool's result — or to a {@link ToolFailure}
 * carrying a stable `code` and a `retryable` flag, because a model cannot `instanceof` its
 * way through a failure.
 *
 * `input` is `unknown` rather than the schema's type for the same reason: it is the model's,
 * and checking it is `execute`'s job. It is also what lets a host hold several tools in one
 * `DotCMSTool[]` and call whichever the model picked.
 */
export interface DotCMSTool<TInput extends z.ZodObject = z.ZodObject, TResult = unknown> {
    /**
     * The name the tool's description expects to be called by. The descriptions refer to their
     * siblings by these names ("use the `search` tool first"), so register each tool under it.
     */
    readonly name: string;
    readonly title: string;
    /** The model-facing description — tuned prompt text. */
    readonly description: string;
    readonly inputSchema: TInput;
    readonly annotations: DotCMSToolAnnotations;
    execute(input: unknown): Promise<TResult | ToolFailure>;
}
