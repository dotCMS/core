import { CONTEXT_PATHS } from '../../adapter/context';
import { toRequestPolicy, type RequestPolicy } from '../../adapter/request-core';

import type { RuntimeAllow } from '../../runtime';

/**
 * One endpoint a tool may call: an HTTP method and a path pattern, e.g.
 * `'POST /api/v1/page/{pageId}/content'`.
 *
 * In the pattern, `{name}` matches exactly one path segment and a trailing `/**` matches
 * anything below that prefix. Patterns are matched against the RESOLVED path the request core
 * hands a policy (dot-segments and encoded dots already collapsed), so `/**` cannot be used
 * to climb out of its prefix.
 */
export type Endpoint = `${'GET' | 'POST' | 'PUT' | 'DELETE'} /${string}`;

/**
 * What loading instance context reads. The runtime does it on a tool's behalf, so every tool
 * that touches context includes these — they are the one endpoint list the toolkit owns.
 * Every other list lives next to the operation that makes those requests.
 */
export const CONTEXT_ENDPOINTS: readonly Endpoint[] = Object.values(CONTEXT_PATHS).map(
    (path): Endpoint => `GET ${path}`
);

interface CompiledEndpoint {
    method: string;
    pattern: RegExp;
}

function escapeRegExp(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function compile(endpoint: Endpoint): CompiledEndpoint {
    const space = endpoint.indexOf(' ');
    const method = endpoint.slice(0, space);
    const path = endpoint.slice(space + 1);

    const deep = path.endsWith('/**');
    const fixed = deep ? path.slice(0, -3) : path;
    const body = fixed
        .split(/(\{[^/}]+\})/)
        .map((part) => (/^\{[^/}]+\}$/.test(part) ? '[^/]+' : escapeRegExp(part)))
        .join('');

    return { method, pattern: new RegExp(`^${body}${deep ? '(?:/.*)?' : ''}$`) };
}

/** Whether `req` is one of `endpoints`. */
export function matchesEndpoint(
    endpoints: readonly Endpoint[],
    req: { method: string; path: string }
): boolean {
    return endpoints.some((endpoint) => {
        const { method, pattern } = compile(endpoint);

        return method === req.method.toUpperCase() && pattern.test(req.path);
    });
}

/**
 * The calls `endpoints` would refuse, as `METHOD path` strings — empty when every call is
 * covered. Paths are resolved the way the request core resolves them before its policy check.
 *
 * What keeps an allow-list honest: the operation specs run every call their fakes see through
 * this, so a request added to an operation without adding it to the tool's endpoints fails
 * the build instead of being refused in production.
 */
export function unlistedCalls(
    endpoints: readonly Endpoint[],
    calls: ReadonlyArray<{ method?: string; path: string }>
): string[] {
    return calls
        .map((call) => ({
            method: (call.method ?? 'GET').toUpperCase(),
            path: new URL(call.path, 'http://resolve.invalid').pathname
        }))
        .filter((req) => !matchesEndpoint(endpoints, req))
        .map((req) => `${req.method} ${req.path}`);
}

/**
 * The policy for a fixed-purpose tool: exactly the endpoints it owns, and nothing else.
 *
 * The consumer never configures this — the tool knows what it calls. It is least privilege by
 * construction: a bug in `page_verify`, or a page path shaped to reach somewhere else, cannot
 * get past the render and site-lookup endpoints the tool actually needs.
 */
export function toolPolicy(
    toolName: string,
    endpoints: readonly Endpoint[],
    hint?: string
): RequestPolicy {
    return (req) => {
        if (matchesEndpoint(endpoints, req)) {
            return true;
        }
        // Thrown rather than returned `false`, so the model reads WHY instead of a bare
        // "rejected by policy" — the request core turns this message into the PolicyError.
        throw new Error(
            `The ${toolName} tool can only reach the endpoints it owns, and ${req.method} ` +
                `${req.path} is not one of them.${hint ? ` ${hint}` : ''}`
        );
    };
}

/**
 * The policy for `execute`, where the MODEL chooses the endpoints: the consumer's `allow`
 * bounds its code, plus the context reads the runtime makes on the tool's behalf — so a narrow
 * `allow` never silently empties the `sites` / `contentTypes` globals the code relies on.
 * Those reads expose nothing the code does not already hold as globals.
 *
 * No `allow` means no policy: the model's code may call anything the token permits.
 */
export function modelChosenPolicy(allow: RuntimeAllow | undefined): RequestPolicy | undefined {
    const consumer = toRequestPolicy(allow);
    if (!consumer) {
        return undefined;
    }

    return (req) => matchesEndpoint(CONTEXT_ENDPOINTS, req) || consumer(req);
}
