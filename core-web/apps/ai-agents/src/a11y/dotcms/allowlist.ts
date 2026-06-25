import type { Adapter, AdapterMethod } from '@dotcms/ai/sandbox';

/**
 * Path allowlist — the structural safety boundary (plan §3, stage B).
 *
 * The minted JWT carries the user's FULL permissions (it can publish/delete);
 * the boundary is therefore *what the agent can call*, not the token. This wraps
 * the `createApiAdapter` adapter so that only the four loop operations reach the
 * wire — every other call (publish, delete, workflow, config, …) is rejected
 * before `fetch`, even if a prompt-injected model tries to make it.
 *
 * It wraps the adapter's `request.execute`; the auth token lives inside that
 * function's closure and is never visible here. Pre-GA this becomes ~4 typed
 * tools (stage C); the rules below are the spec for that.
 */

type AllowRule = {
    /** HTTP method (upper-case). */
    method: 'GET' | 'POST' | 'PUT';
    /** The request path (the value of `options.path`, without query string). */
    path: string;
    /** exact: full equality. prefix: `path` must start with this (for `_render-sources/{uri}`). */
    match: 'exact' | 'prefix';
};

// The ONLY operations the a11y loop performs. Note `/api/v2/assets/save` is
// exact — a prefix rule would also admit `/publish`, `/delete`, etc.
export const ALLOW_RULES: readonly AllowRule[] = [
    // SCAN / RE-SCAN — POST { url } to the page-scanner proxy.
    { method: 'POST', path: '/api/v1/page-scanner/a11y/check', match: 'exact' },
    // LOCATE — GET /api/v1/page/_render-sources/{uri}
    { method: 'GET', path: '/api/v1/page/_render-sources', match: 'prefix' },
    // READ — GET /api/v2/assets?path=...
    { method: 'GET', path: '/api/v2/assets', match: 'exact' },
    // FETCH-STYLESHEET — GET the compiled theme stylesheet + inline sourcemap
    // (CSS-attribution, S1.5). GET-only, read-only theme assets; the SASS
    // preprocessor serves these from the theme folder, not /api/v2/assets.
    { method: 'GET', path: '/application/themes/', match: 'prefix' },
    // SAVE-WORKING — PUT /api/v2/assets/save (multipart, working only; never /publish)
    { method: 'PUT', path: '/api/v2/assets/save', match: 'exact' }
] as const;

export class DisallowedRequestError extends Error {
    constructor(method: string, path: string) {
        super(
            `Request not allowed by the a11y agent path allowlist: ${method} ${path}. ` +
                `Permitted: page-scanner/a11y/check, _render-sources GET, ` +
                `/api/v2/assets GET, /application/themes/ GET, /api/v2/assets/save PUT.`
        );
        this.name = 'DisallowedRequestError';
    }
}

/** Strip any query string the model may have appended to `path` (query belongs in `options.query`). */
function normalizePath(path: string): string {
    const q = path.indexOf('?');
    return q === -1 ? path : path.slice(0, q);
}

/** True if `method` + `path` matches a loop operation. */
export function isAllowed(method: string, path: string): boolean {
    const m = (method || 'GET').toUpperCase();
    const p = normalizePath(path);
    return ALLOW_RULES.some((rule) => {
        if (rule.method !== m) {
            return false;
        }
        return rule.match === 'exact' ? p === rule.path : p.startsWith(rule.path);
    });
}

type RequestOptions = { method?: string; path?: string };

/**
 * Wrap an `api` adapter so its `request` method enforces the allowlist before
 * delegating. Returns a new Adapter with the same shape (name/version/methods)
 * — the sandbox sees an identical `api.request(...)`, just guarded.
 */
export function withAllowlist(adapter: Adapter): Adapter {
    const inner = adapter.methods.get('request');
    if (!inner) {
        throw new Error('Adapter has no "request" method to guard');
    }

    const guarded: AdapterMethod = {
        ...inner,
        async execute(...args: unknown[]): Promise<unknown> {
            const options = (args[0] || {}) as RequestOptions;
            const method = options.method || 'GET';
            const path = options.path || '/';
            if (!isAllowed(method, path)) {
                throw new DisallowedRequestError(method.toUpperCase(), normalizePath(path));
            }
            return inner.execute(...args);
        }
    };

    return {
        ...adapter,
        methods: new Map([['request', guarded]])
    };
}
