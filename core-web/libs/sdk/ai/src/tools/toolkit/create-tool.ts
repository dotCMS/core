import { z } from 'zod';

import { resolveConnection, type DotCMSConnection } from './connection';
import { modelChosenPolicy, toolPolicy, type Endpoint } from './endpoints';
import { toModelOutput } from './results';
import { createToolRuntime, toToolFailure } from './tool-runtime';

import { ValidationError, type DotCMSRuntime } from '../../runtime';

import type {
    DotCMSTool,
    DotCMSToolAnnotations,
    ExecuteToolOptions,
    RequestToolOptions
} from './types';

/** The union of every factory's options — what `createTool` may find on any of them. */
type AnyToolOptions = Partial<ExecuteToolOptions> & Partial<RequestToolOptions>;

/** What a tool's handler receives for one call. */
export interface ToolContext<TOptions extends object> {
    /** The options the tool's factory was called with. */
    options: TOptions;
    /**
     * Build this call's runtime from the resolved connection. Throws `ConfigurationError` when
     * the URL or token is empty, which `execute` turns into a `CONFIGURATION` failure.
     */
    runtime(runtimeOptions?: { timeout?: number; includeSpec?: boolean }): DotCMSRuntime;
}

/**
 * The static half of a tool — everything except the connection and options a consumer passes
 * its factory. `handler` receives input that already passed `inputSchema`, and anything it
 * throws becomes a `ToolFailure`.
 */
export interface ToolDefinition<TInput extends z.ZodObject, TResult, TOptions extends object> {
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
export function defineTool<TInput extends z.ZodObject, TResult, TOptions extends object = object>(
    definition: ToolDefinition<TInput, TResult, TOptions>
): ToolDefinition<TInput, TResult, TOptions> {
    return definition;
}

/**
 * Turn a definition, a connection and a consumer's options into the tool a framework
 * registers.
 *
 * Cheap and side-effect free: nothing is resolved, validated or fetched here. The connection is
 * resolved inside `execute`, on every call — so a host started without credentials still boots
 * and lists its tools, and each call reports the `CONFIGURATION` problem instead of the host
 * crashing at startup.
 */
export function createTool<TInput extends z.ZodObject, TResult, TOptions extends AnyToolOptions>(
    definition: ToolDefinition<TInput, TResult, TOptions>,
    connection: DotCMSConnection,
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
        toModelOutput,
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

            // A throw that escapes here reaches an MCP client as a PROTOCOL error, or an agent
            // loop as an exception, rather than as a result the model can read and act on.
            try {
                const { url, token, config } = await resolveConnection(connection);

                const ctx: ToolContext<TOptions> = {
                    options,
                    runtime: (runtimeOptions) =>
                        createToolRuntime(
                            {
                                url,
                                token,
                                allow: policy,
                                onCall: config.onCall,
                                onContextError: config.onContextError,
                                includeStacks: options.includeStacks
                            },
                            { ...runtimeOptions, requestTimeout: options.requestTimeout }
                        )
                };

                return await definition.handler(parsed.data, ctx);
            } catch (error) {
                return toToolFailure(definition.name, error);
            }
        }
    };
}
