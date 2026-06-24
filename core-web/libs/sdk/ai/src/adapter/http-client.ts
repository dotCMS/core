import {
    type RequestCallEvent,
    type RequestOptions,
    type RequestPolicy,
    requestCore
} from './request-core';

import type { Adapter, AdapterMethod } from '../sandbox/types';

export {
    type BinaryResponseEnvelope,
    type RequestOptions,
    type RequestPolicy,
    type RequestCallEvent,
    isBinaryResponseEnvelope
} from './request-core';

export interface ApiAdapterConfig {
    dotcmsUrl: string;
    authToken: string;
    /** Optional policy/allow-list consulted before each request reaches the wire. */
    policy?: RequestPolicy;
    /** Aborts in-flight requests when the surrounding execution (e.g. the sandbox) is torn down. */
    signal?: AbortSignal;
    /** Observability hook fired around each call (token/sensitive bodies never included). */
    onCall?: (event: RequestCallEvent) => void;
}

/**
 * Create the "api" adapter for making authenticated HTTP calls to dotCMS.
 *
 * This is the dotCMS door out of the sandbox. The adapter's single `request` method is a
 * thin wrapper over the shared {@link requestCore} — so a request driven by the sandboxed
 * `api.request` and one driven by the runtime's direct `request()` are byte-for-byte the
 * same code path (one auth path, one allow-list, one error model). Auth tokens are injected
 * by the host — never exposed to the sandbox.
 */
export function createApiAdapter(config: ApiAdapterConfig): Adapter {
    if (!config.dotcmsUrl) {
        throw new Error('dotcmsUrl is required');
    }
    if (!config.authToken) {
        throw new Error('authToken is required');
    }

    const requestMethod: AdapterMethod = {
        name: 'request',
        description: 'Make an authenticated HTTP request to the dotCMS API',
        parameters: [
            {
                name: 'options',
                type: 'object',
                description: 'Request options: { method, path, query, body, formData, headers }',
                required: true
            }
        ],
        execute(...args: unknown[]): Promise<unknown> {
            const options = (args[0] || {}) as RequestOptions;
            return requestCore(options, {
                baseUrl: config.dotcmsUrl,
                authToken: config.authToken,
                policy: config.policy,
                signal: config.signal,
                onCall: config.onCall
            });
        }
    };

    return {
        name: 'api',
        description: 'Authenticated HTTP client for dotCMS REST API',
        version: '1.0.0',
        methods: new Map([['request', requestMethod]])
    };
}

/**
 * The dotCMS adapter — the building block exported from `@dotcms/ai/adapter`.
 *
 * Returns both the underlying {@link Adapter} (for the sandbox) and a direct `request`
 * function (the no-worker path). A power-user can wrap this value — e.g. with an allow-list —
 * before handing it to the runtime, because it is a plain object you can compose.
 */
export interface DotCMSAdapter {
    /** The adapter object the sandbox executor consumes (`api.request` inside sandbox code). */
    adapter: Adapter;
    /** The direct, no-worker request path — the same shared core the sandbox bridges to. */
    request(options: RequestOptions): Promise<unknown>;
}

export function dotcmsAdapter(config: ApiAdapterConfig): DotCMSAdapter {
    const adapter = createApiAdapter(config);
    // Delegate the direct path to the adapter's own `request` method (which calls requestCore
    // with auth bound) — don't rebuild the request context the adapter already holds.
    const request = adapter.methods.get('request')?.execute;
    return {
        adapter,
        request: (options: RequestOptions): Promise<unknown> =>
            Promise.resolve(request?.(options))
    };
}
