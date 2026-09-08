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

import { isSuccessStatus } from './fetch-retry';

const DEFAULT_TIMEOUT_MS = 10000;

/**
 * A ceiling on what we will buffer from an arbitrary address.
 *
 * The first request a run makes goes to a host the developer typed, so "whatever it sends" is
 * not a safe amount of memory to accept.
 */
const MAX_BODY_BYTES = 10 * 1024 * 1024;

/**
 * Best-effort JSON. A health endpoint may answer 204, or plain text; neither is an error.
 *
 * Read through the stream rather than `response.text()` so the cap is enforced as bytes arrive
 * instead of after they are all in memory.
 */
async function readBody<T>(response: Response, url: string): Promise<T> {
    const tooLarge = () =>
        new HttpError(`Response from ${url} is too large (over ${MAX_BODY_BYTES} bytes)`, {
            status: response.status,
            code: 'EBODYTOOLARGE'
        });

    const declared = Number(response.headers.get('content-length'));
    if (Number.isFinite(declared) && declared > MAX_BODY_BYTES) throw tooLarge();

    let text = '';
    if (!response.body) {
        text = await response.text().catch(() => '');
    } else {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let size = 0;
        for (;;) {
            const { done, value } = await reader.read();
            if (done) break;
            if (!value) continue;
            size += value.byteLength;
            if (size > MAX_BODY_BYTES) {
                await reader.cancel().catch(() => undefined);
                throw tooLarge();
            }
            text += decoder.decode(value, { stream: true });
        }
        text += decoder.decode();
    }

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

    // The timer stays armed until the BODY has been read. Clearing it when the headers arrived
    // left `readBody` awaiting with no abort in place, so a host that answers 200 and then
    // trickles hung the process forever — the failure the timeout exists to prevent.
    try {
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
        }

        let data: T;
        try {
            data = await readBody<T>(response, url);
        } catch (error) {
            if (isHttpError(error)) throw error;
            const aborted = (error as Error)?.name === 'AbortError';
            throw new HttpError(
                aborted
                    ? `Request to ${url} timed out after ${timeoutMs}ms while reading the response`
                    : `Reading the response from ${url} failed: ${(error as Error)?.message ?? String(error)}`,
                { status: response.status, code: aborted ? 'ETIMEDOUT' : undefined }
            );
        }

        if (!isSuccessStatus(response.status) && !acceptAnyStatus) {
            throw new HttpError(`Request failed with status code ${response.status}`, {
                status: response.status,
                statusText: response.statusText
            });
        }

        return { status: response.status, data };
    } finally {
        clearTimeout(timer);
    }
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
