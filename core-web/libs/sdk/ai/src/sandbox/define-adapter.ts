import { z } from 'zod';

import { ValidationError } from './errors';

import type { Adapter, AdapterMethod } from './types';

/**
 * The host capabilities the runtime injects into every adapter handler. `request` is the
 * shared request core with auth already bound — a handler reaches dotCMS through it, never
 * through a free-floating global. `signal` aborts in-flight work when the sandbox times out.
 */
export interface AdapterContext {
    request: (opts: RequestLike) => Promise<unknown>;
    signal?: AbortSignal;
}

/**
 * Minimal request shape an adapter handler passes to `ctx.request`. Kept structural (not a
 * hard import of the dotCMS RequestOptions) so `/sandbox` stays free of dotCMS specifics.
 */
export interface RequestLike {
    method?: string;
    path: string;
    query?: Record<string, string | number | boolean>;
    body?: unknown;
    formData?: Record<string, unknown>;
    headers?: Record<string, string>;
    responseType?: 'auto' | 'base64';
}

/**
 * A single method definition on an adapter. `input` is mandatory — it is the *trust*
 * boundary: arguments arrive from model-authored code inside the sandbox and must be
 * validated before the handler runs. `output` is the *tool-contract* boundary: required for
 * any model-facing adapter (it becomes the result schema the LLM plans against), optional
 * only for internal host-to-host plumbing the model never sees.
 */
export interface AdapterMethodDef<
    TInput extends z.ZodTypeAny = z.ZodTypeAny,
    TOutput extends z.ZodTypeAny = z.ZodTypeAny
> {
    description: string;
    input: TInput;
    output?: TOutput;
    handler: (input: z.infer<TInput>, ctx: AdapterContext) => Promise<unknown> | unknown;
}

/* eslint-disable @typescript-eslint/no-explicit-any */
export interface AdapterDef<
    TMethods extends Record<string, AdapterMethodDef<any, any>> = Record<
        string,
        AdapterMethodDef<any, any>
    >
> {
    name: string;
    description?: string;
    version?: string;
    methods: TMethods;
}
/* eslint-enable @typescript-eslint/no-explicit-any */

/**
 * A defined adapter. It IS a plain `Adapter` (so the executor consumes it unchanged) plus:
 *  - `__schemas`: the per-method Zod input/output, used to auto-generate tool definitions;
 *  - `withContext(ctx)`: binds the injected host capabilities, returning a runnable adapter.
 *
 * The schemas being declared is what lets the runtime *describe the adapter to an LLM
 * automatically* — that declaration IS the tool definition (the bridge to MCP/tool-calling).
 */
export interface DefinedAdapter extends Adapter {
    readonly __defined: true;
    readonly __schemas: Record<
        string,
        { description: string; input: z.ZodTypeAny; output?: z.ZodTypeAny }
    >;
    /** Every method declares an `output` schema → safe to expose to a model. */
    readonly modelExposable: boolean;
    /** Bind host capabilities (the injected `ctx`) and return a runnable adapter. */
    withContext(ctx: AdapterContext): Adapter;
}

function validateInput(methodName: string, schema: z.ZodTypeAny, raw: unknown): unknown {
    const result = schema.safeParse(raw);
    if (!result.success) {
        throw new ValidationError(
            `Invalid arguments for "${methodName}": ${result.error.message}`,
            result.error.issues
        );
    }
    return result.data;
}

function validateOutput(schema: z.ZodTypeAny | undefined, value: unknown): unknown {
    if (!schema) return value;
    const result = schema.safeParse(value);
    if (!result.success) {
        // Output drift is a contract problem, not untrusted input — surface it as a
        // validation error so a dotCMS response change shows up at the boundary rather
        // than as a silent `undefined` downstream.
        throw new ValidationError(
            `Adapter result did not match its output schema: ${result.error.message}`,
            result.error.issues
        );
    }
    return result.data;
}

/**
 * Build a typed, schema-validated adapter. Handlers get `(input, ctx)` — `input` already
 * validated, `ctx.request` already auth-bound by the runtime.
 *
 * The returned value is a plain `Adapter` (the `methods` Map is populated with thunks that
 * throw until `withContext` is called), so it can be wrapped (e.g. by an allow-list) before
 * it reaches the runtime, exactly like the built-in `dotcmsAdapter`.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function defineAdapter<TMethods extends Record<string, AdapterMethodDef<any, any>>>(
    def: AdapterDef<TMethods>
): DefinedAdapter {
    const schemas: DefinedAdapter['__schemas'] = {};
    let modelExposable = true;

    for (const [methodName, m] of Object.entries(def.methods)) {
        schemas[methodName] = { description: m.description, input: m.input, output: m.output };
        if (!m.output) modelExposable = false;
    }

    const makeMethods = (ctx?: AdapterContext): Map<string, AdapterMethod> => {
        const methods = new Map<string, AdapterMethod>();
        for (const [methodName, m] of Object.entries(def.methods)) {
            methods.set(methodName, {
                name: methodName,
                description: m.description,
                parameters: [
                    {
                        name: 'input',
                        type: 'object',
                        description: m.description,
                        required: true
                    }
                ],
                async execute(...args: unknown[]): Promise<unknown> {
                    if (!ctx) {
                        throw new ValidationError(
                            `Adapter "${def.name}" was used without a bound context. ` +
                                `Call adapter.withContext(ctx) before running it.`
                        );
                    }
                    const input = validateInput(methodName, m.input, args[0]);
                    const result = await m.handler(input as never, ctx);
                    return validateOutput(m.output, result);
                }
            });
        }
        return methods;
    };

    const base: DefinedAdapter = {
        name: def.name,
        description: def.description,
        version: def.version ?? '1.0.0',
        methods: makeMethods(),
        __defined: true,
        __schemas: schemas,
        modelExposable,
        withContext(ctx: AdapterContext): Adapter {
            return {
                name: def.name,
                description: def.description,
                version: def.version ?? '1.0.0',
                methods: makeMethods(ctx)
            };
        }
    };

    return base;
}

/** Type guard: distinguishes a `defineAdapter` result from a hand-built `Adapter`. */
export function isDefinedAdapter(adapter: Adapter): adapter is DefinedAdapter {
    return (adapter as DefinedAdapter).__defined === true;
}

/**
 * Render a defined adapter's methods as JSON-Schema-ish tool definitions an LLM can consume.
 * This is the bridge to MCP / tool-calling: the declared Zod schemas become the tool's
 * input/output contract, replacing hand-written description prose.
 *
 * Only methods that declare an `output` schema are emitted — a method without one is treated
 * as internal plumbing and withheld from the model.
 */
export function describeAdapterForLLM(adapter: DefinedAdapter): Array<{
    name: string;
    description: string;
    inputSchema: unknown;
    outputSchema?: unknown;
}> {
    const tools: Array<{
        name: string;
        description: string;
        inputSchema: unknown;
        outputSchema?: unknown;
    }> = [];
    for (const [methodName, s] of Object.entries(adapter.__schemas)) {
        if (!s.output) continue; // internal-only method — not model-exposable
        tools.push({
            name: `${adapter.name}.${methodName}`,
            description: s.description,
            inputSchema: z.toJSONSchema(s.input),
            outputSchema: z.toJSONSchema(s.output)
        });
    }
    return tools;
}
