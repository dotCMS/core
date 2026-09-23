import { z } from 'zod';

import { modelChosenPolicy, toolPolicy, type Endpoint } from './endpoints';
import { createToolRuntime, toToolFailure } from './tool-runtime';

import { ValidationError, type DotCMSRuntime } from '../../runtime';

import type {
    DotCMSTool,
    DotCMSToolAnnotations,
    DotCMSToolOptions,
    ExecuteToolOptions,
    RequestToolOptions
} from './types';

/** The union of every factory's options — what `createTool` may find on any of them. */
type AnyToolOptions = DotCMSToolOptions &
    Partial<Pick<ExecuteToolOptions, 'allow' | 'includeStacks'>> &
    Pick<RequestToolOptions, 'requestTimeout'>;

/**
 * The environment variables a tool falls back to when its factory was given no `url` /
 * `token`. The same names the dotCMS MCP server has always read, so an existing server config
 * keeps working unchanged.
 */
export const DOTCMS_URL_ENV = 'DOTCMS_URL';
export const AUTH_TOKEN_ENV = 'AUTH_TOKEN';

/** What a tool's handler receives for one call. */
export interface ToolContext<TOptions extends DotCMSToolOptions> {
    /** The options the tool's factory was called with. */
    options: TOptions;
    /**
     * Build this call's runtime. Throws `ConfigurationError` when no URL or token can be found,
     * which `execute` turns into a `CONFIGURATION` failure.
     */
    runtime(runtimeOptions?: { timeout?: number; includeSpec?: boolean }): DotCMSRuntime;
}

/**
 * The static half of a tool — everything except the options a consumer passes its factory.
 * `handler` receives input that already passed `inputSchema`, and anything it throws becomes a
 * `ToolFailure`.
 */
export interface ToolDefinition<
    TInput extends z.ZodObject,
    TResult,
    TOptions extends DotCMSToolOptions
> {
    name: string;
    title: string;
    description: string;
    inputSchema: TInput;
    annotations: DotCMSToolAnnotations;
    /**
     * Every endpoint the tool calls — enforced, not documented: any other request fails with
     * a `POLICY` failure before it reaches the wire. `'model-chosen'` is for `execute` alone,
     * where the model picks the endpoints and the consumer's `allow` bounds them instead.
     */
    endpoints: readonly Endpoint[] | 'model-chosen';
    /** Appended to the rejection message when a request falls outside `endpoints`. */
    endpointHint?: string;
    handler(input: z.output<TInput>, ctx: ToolContext<TOptions>): Promise<TResult>;
}

/** Identity helper that infers the generics, so each handler sees its own input type. */
export function defineTool<
    TInput extends z.ZodObject,
    TResult,
    TOptions extends DotCMSToolOptions = DotCMSToolOptions
>(
    definition: ToolDefinition<TInput, TResult, TOptions>
): ToolDefinition<TInput, TResult, TOptions> {
    return definition;
}

/** An environment variable, or undefined where there is no `process` (edge, browser). */
function fromEnv(name: string): string | undefined {
    return typeof process === 'undefined' ? undefined : process.env?.[name];
}

/**
 * Turn a definition plus a consumer's options into the tool a framework registers.
 *
 * Cheap and side-effect free: nothing is read, validated or fetched here. The environment
 * fallback and the configuration check both happen inside `execute`, so a host started
 * without credentials still boots and lists its tools, and each call reports the
 * `CONFIGURATION` problem instead of the host crashing at startup.
 */
export function createTool<TInput extends z.ZodObject, TResult, TOptions extends AnyToolOptions>(
    definition: ToolDefinition<TInput, TResult, TOptions>,
    options: TOptions
): DotCMSTool<TInput, TResult> {
    // The tool owns its endpoints; only `execute`'s are the model's to choose and the
    // consumer's to bound.
    const policy =
        definition.endpoints === 'model-chosen'
            ? modelChosenPolicy(options.allow)
            : toolPolicy(definition.name, definition.endpoints, definition.endpointHint);

    return {
        name: definition.name,
        title: definition.title,
        description: definition.description,
        inputSchema: definition.inputSchema,
        annotations: definition.annotations,
        async execute(input) {
            // The trust boundary, same as `defineAdapter`'s `input`: AI SDK and MCP validate
            // before calling, but a hand-rolled agent loop may not, and a handler must never
            // see input its schema did not accept. Re-parsing already-valid input is a no-op.
            const parsed = definition.inputSchema.safeParse(input);
            if (!parsed.success) {
                return toToolFailure(
                    definition.name,
                    new ValidationError(
                        `Invalid arguments for "${definition.name}":\n${z.prettifyError(parsed.error)}`,
                        parsed.error.issues
                    )
                );
            }

            const ctx: ToolContext<TOptions> = {
                options,
                runtime: (runtimeOptions) =>
                    createToolRuntime(
                        {
                            url: options.url ?? fromEnv(DOTCMS_URL_ENV) ?? '',
                            token: options.token ?? fromEnv(AUTH_TOKEN_ENV) ?? '',
                            allow: policy,
                            onCall: options.onCall,
                            onContextError: options.onContextError,
                            includeStacks: options.includeStacks
                        },
                        { ...runtimeOptions, requestTimeout: options.requestTimeout }
                    )
            };

            // A throw that escapes here reaches an MCP client as a PROTOCOL error, or an agent
            // loop as an exception, rather than as a result the model can read and act on.
            try {
                return await definition.handler(parsed.data, ctx);
            } catch (error) {
                return toToolFailure(definition.name, error);
            }
        }
    };
}
