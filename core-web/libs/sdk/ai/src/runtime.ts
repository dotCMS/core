import { ContextCache } from './adapter/context-cache';
import { createApiAdapter } from './adapter/http-client';
import {
    type RequestCallEvent,
    type RequestOptions,
    type RequestPolicy,
    requestCore
} from './adapter/request-core';
import { serializeError } from './sandbox/errors';
import { Executor } from './sandbox/executor';
import { getSpec } from './spec/spec';

import type { DotCMSContext } from './adapter/context';
import type { SandboxResult } from './sandbox/types';

/**
 * Policy controlling which requests are permitted. Either a list of allowed path prefixes
 * (a request is allowed if its path starts with any entry) or a predicate consulted per call.
 * Both verbs honor it, because both flow through the one shared request core.
 */
export type RuntimeAllow = string[] | RequestPolicy;

export interface DotCMSRuntimeConfig {
    /** dotCMS instance URL. */
    url: string;
    /** Server-side token. NEVER enters the sandbox — injected host-side on every call. */
    token: string;
    /** Optional allow-list / policy applied to every request. */
    allow?: RuntimeAllow;
    /** Context-cache + isolation key. Defaults to `__default__`. */
    sessionId?: string;
    /** Inject the OpenAPI `spec` global into `run(code)` (the search use case). */
    includeSpec?: boolean;
    /** Sandbox wall-clock timeout (ms) for `run(code)`. Defaults to 15000. */
    timeout?: number;
    /** Observability hook fired around each request (token/sensitive bodies never logged). */
    onCall?: (event: RequestCallEvent) => void;
    /** Error handler for instance-context load failures (per loader). */
    onContextError?: (label: string, error: unknown) => void;
    /**
     * Whether to leak stack traces (which can contain host paths) to executed/model code.
     * Defaults to false — stacks are withheld from `run(code)` results.
     */
    includeStacks?: boolean;
}

export interface DotCMSRuntime {
    /**
     * DIRECT — you write the call. No worker. Use this whenever you author the call yourself.
     * Routes through the same shared request core as `run`. Pass `opts.signal` to make the
     * call abortable (e.g. to wrap it in your own timeout) — the direct path has no surrounding
     * timeout of its own.
     */
    request(options: RequestOptions, opts?: { signal?: AbortSignal }): Promise<unknown>;
    /**
     * SANDBOXED — runs `code` you did NOT write (a model did) in a confined worker whose
     * `api.request` forwards to the same request core. Returns the structured sandbox result.
     */
    run<T = unknown>(code: string): Promise<SandboxResult<T>>;
    /** Load (or read cached) instance context for this runtime's session+url. */
    loadContext(): Promise<DotCMSContext>;
}

const DEFAULT_TIMEOUT_MS = 15000;
const DEFAULT_SESSION_ID = '__default__';

/** Normalize the `allow` config into the policy predicate the request core understands. */
function toPolicy(allow: RuntimeAllow | undefined): RequestPolicy | undefined {
    if (!allow) return undefined;
    if (typeof allow === 'function') return allow;
    const prefixes = allow;
    return ({ path }) => prefixes.some((prefix) => path.startsWith(prefix));
}

/**
 * The front door. One runtime, two verbs.
 *
 * `request` is the default — use it when you write the call. `run` is only for code you did
 * not write (a model did). `run(code)` is implemented *as* "spin a worker whose `api.request`
 * forwards to this runtime's request core", so the two verbs cannot drift: one is built on
 * the other, sharing one adapter, one auth path, one allow-list, one error model.
 *
 * Note: this is an execution runtime, not an agent. There is no LLM, no inference, no
 * prompting inside it. The agent is what the *user* builds on top.
 */
export function createDotCMSRuntime(config: DotCMSRuntimeConfig): DotCMSRuntime {
    if (!config.url) throw new Error('createDotCMSRuntime: `url` is required');
    if (!config.token) throw new Error('createDotCMSRuntime: `token` is required');

    const sessionId = config.sessionId ?? DEFAULT_SESSION_ID;
    const timeout = config.timeout ?? DEFAULT_TIMEOUT_MS;
    const policy = toPolicy(config.allow);

    // The runtime OWNS its context cache instance (keyed on sessionId+url internally), so two
    // runtimes for different instances never collide on a shared module singleton.
    const contextCache = new ContextCache({ onError: config.onContextError });

    // Single source of the adapter/request-core wiring — url→baseUrl/dotcmsUrl, token→authToken,
    // policy, and onCall live in ONE place so `request`, `run`, and `loadContext` can't drift
    // (the design's "one auth path"). Only the per-execution abort `signal` varies.
    const adapterConfig = (signal?: AbortSignal) => ({
        dotcmsUrl: config.url,
        authToken: config.token,
        policy,
        signal,
        onCall: config.onCall
    });

    const loadContext = (signal?: AbortSignal): Promise<DotCMSContext> =>
        contextCache.get(sessionId, config.url, createApiAdapter(adapterConfig(signal)));

    const request = (options: RequestOptions, opts?: { signal?: AbortSignal }): Promise<unknown> =>
        requestCore(options, {
            baseUrl: config.url,
            authToken: config.token,
            policy,
            signal: opts?.signal,
            onCall: config.onCall
        });

    async function run<T = unknown>(code: string): Promise<SandboxResult<T>> {
        // One AbortController per execution: when the sandbox times out (or tears down),
        // we abort it, which propagates through the adapter's signal to any in-flight fetch.
        const controller = new AbortController();

        const adapter = createApiAdapter(adapterConfig(controller.signal));

        // dotCMS instance context is THE defining feature of a dotCMS runtime, so the
        // runtime loads and injects it — absorbing the per-tool wiring consumers used to repeat.
        // Context loading is covered by the SAME timeout+abort as the sandbox run, so a hanging
        // context API request can't make run() hang past `timeout` (the load shares the
        // controller, whose signal the adapter already carries into the fetch).
        const loadTimer = setTimeout(() => controller.abort(), timeout);
        let context: DotCMSContext;
        try {
            context = await contextCache.get(sessionId, config.url, adapter);
        } catch (err) {
            return {
                success: false,
                error: serializeError(err),
                logs: [],
                executionTime: 0
            };
        } finally {
            clearTimeout(loadTimer);
        }

        const variables: Record<string, unknown> = {
            contentTypes: context.contentTypes,
            sites: context.sites,
            languages: context.languages,
            currentUser: context.currentUser
        };
        if (config.includeSpec) {
            variables.spec = getSpec();
        }

        const executor = new Executor({
            config: {
                adapters: [adapter],
                sandbox: {
                    timeout,
                    // A sandbox teardown (timeout, error, completion) aborts in-flight host work.
                    onTeardown: () => controller.abort()
                }
            }
        });

        const result = await executor.execute<T>(code, {
            adapters: ['api'],
            variables
        });

        // Withhold host stack traces from executed code unless explicitly opted in.
        if (!config.includeStacks && result.error?.stack) {
            delete result.error.stack;
        }

        return result;
    }

    return {
        request,
        run,
        loadContext: () => loadContext()
    };
}
