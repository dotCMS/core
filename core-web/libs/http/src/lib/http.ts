/**
 * The CLI's only HTTP client, built on Node's native `fetch`.
 *
 * This package used axios, which `semgrep-dotcms` flagged with two High findings on #37264 —
 * both the same root cause: axios's Node adapter does not clear `Proxy-Authorization` when a
 * request that went through an authenticated proxy is redirected to a target that does not use
 * that proxy, leaking the proxy credentials to the redirect origin.
 *
 * Bumping axios closes those two CVEs; removing it closes the class. This package was the only
 * lib in the workspace depending on axios, and it sits in esbuild's `external` list — so it is a
 * real install for everyone running `npx @dotcms/create-app`, not just a build-time concern.
 * `compose-source.ts` here already used native `fetch`, so the inconsistency was ours.
 *
 * Node >= 22.22.3 is required (`.nvmrc`), where `fetch` is stable. The fetch spec requires
 * stripping `Authorization` on a cross-origin redirect — the protection axios's Node adapter
 * was missing.
 */

export interface HttpResponse<T = unknown> {
    status: number;
    data: T;
}

export interface HttpOptions {
    /** Sent as `Authorization: Bearer <token>`. */
    token?: string;
    timeoutMs?: number;
    /**
     * Return non-2xx responses instead of throwing. The readiness probe needs this: a 503 from
     * `/dotmgt/readyz` means "still starting", which is data, not a failed request.
     */
    acceptAnyStatus?: boolean;
}

export class HttpError extends Error {
    /** HTTP status, or null when the request never got a response at all. */
    readonly status: number | null;
    /** Transport-level code — ECONNREFUSED, ETIMEDOUT — when there was no response. */
    readonly code?: string;
    /** Kept in axios's shape so existing `error.response.status` readers keep working. */
    readonly response?: { status: number; statusText: string };

    constructor(
        message: string,
        init: { status?: number | null; code?: string; statusText?: string }
    ) {
        super(message);
        this.name = 'HttpError';
        this.status = init.status ?? null;
        this.code = init.code;

        if (typeof init.status === 'number') {
            this.response = { status: init.status, statusText: init.statusText ?? '' };
        }
    }
}

export function isHttpError(error: unknown): error is HttpError {
    return error instanceof HttpError;
}

const DEFAULT_TIMEOUT_MS = 10000;

function isSuccess(status: number): boolean {
    return status >= 200 && status < 300;
}

/** Best-effort JSON. A health endpoint may answer 204, or plain text; neither is an error. */
async function readBody<T>(response: Response): Promise<T> {
    const text = await response.text().catch(() => '');

    if (!text) {
        return undefined as T;
    }

    try {
        return JSON.parse(text) as T;
    } catch {
        return text as unknown as T;
    }
}

async function request<T>(
    url: string,
    init: RequestInit,
    { token, timeoutMs = DEFAULT_TIMEOUT_MS, acceptAnyStatus = false }: HttpOptions
): Promise<HttpResponse<T>> {
    // fetch has no timeout of its own; without this a dead instance hangs the CLI.
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    const headers = new Headers(init.headers);

    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    let response: Response;

    try {
        response = await fetch(url, { ...init, headers, signal: controller.signal });
    } catch (error) {
        const aborted = (error as Error)?.name === 'AbortError';
        const cause = (error as { cause?: { code?: string } })?.cause;

        throw new HttpError(
            aborted
                ? `Request to ${url} timed out after ${timeoutMs}ms`
                : `Request to ${url} failed: ${(error as Error)?.message ?? String(error)}`,
            { status: null, code: aborted ? 'ETIMEDOUT' : cause?.code }
        );
    } finally {
        clearTimeout(timer);
    }

    const data = await readBody<T>(response);

    if (!isSuccess(response.status) && !acceptAnyStatus) {
        throw new HttpError(`Request failed with status code ${response.status}`, {
            status: response.status,
            statusText: response.statusText
        });
    }

    return { status: response.status, data };
}

export function httpGet<T = unknown>(url: string, options: HttpOptions = {}) {
    return request<T>(url, { method: 'GET' }, options);
}

export function httpPost<T = unknown>(url: string, body: unknown, options: HttpOptions = {}) {
    return request<T>(
        url,
        {
            method: 'POST',
            body: JSON.stringify(body),
            headers: { 'Content-Type': 'application/json' }
        },
        options
    );
}
