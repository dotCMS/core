/**
 * HTTP client wrapper for @dotcms/cli.
 * Uses ofetch for requests and provides GraphQL + REST helpers.
 */
import { ofetch, type $Fetch } from 'ofetch';

import type { GraphQLResponse, HttpClientOptions } from './types';

// ─── Client Creation ────────────────────────────────────────────────────────

/**
 * Create a configured ofetch instance with auth headers and defaults.
 */
export function createHttpClient(options: HttpClientOptions): $Fetch {
    return ofetch.create({
        baseURL: options.baseURL,
        timeout: options.timeout ?? 30_000,
        headers: {
            Authorization: `Bearer ${options.token}`,
            'Content-Type': 'application/json'
        },
        retry: 2,
        retryDelay: 1000
    });
}

// ─── GraphQL Helper ─────────────────────────────────────────────────────────

/**
 * Execute a GraphQL query against the dotCMS GraphQL endpoint.
 * Throws on GraphQL-level errors.
 */
export async function graphql<T = Record<string, unknown>>(
    client: $Fetch,
    query: string,
    variables?: Record<string, unknown>
): Promise<T> {
    const body: Record<string, unknown> = { query };
    if (variables) {
        body['variables'] = variables;
    }

    const response = await client<GraphQLResponse<T>>('/api/v1/graphql', {
        method: 'POST',
        body
    });

    if (response.errors?.length) {
        const messages = response.errors.map((e) => e.message).join('; ');
        throw new Error(`GraphQL error: ${messages}`);
    }

    return response.data;
}

// ─── REST Helpers ───────────────────────────────────────────────────────────

/**
 * Perform a GET request.
 */
export async function get<T>(
    client: $Fetch,
    url: string,
    options?: Record<string, unknown>
): Promise<T> {
    return client<T>(url, { method: 'GET', ...options });
}

/**
 * Perform a POST request.
 */
export async function post<T>(
    client: $Fetch,
    url: string,
    body?: Record<string, unknown>,
    options?: Record<string, unknown>
): Promise<T> {
    return client<T>(url, { method: 'POST', body, ...options });
}

/**
 * Perform a PUT request.
 */
export async function put<T>(
    client: $Fetch,
    url: string,
    body?: Record<string, unknown>,
    options?: Record<string, unknown>
): Promise<T> {
    return client<T>(url, { method: 'PUT', body, ...options });
}
