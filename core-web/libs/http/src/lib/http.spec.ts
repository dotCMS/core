/**
 * Contract spec for `src/utils/http.ts` — the CLI's only HTTP client.
 *
 * WHY THIS EXISTS. `semgrep-dotcms` flagged axios on this PR with two High findings, both the
 * same root cause: axios's Node adapter does not clear `Proxy-Authorization` when a request that
 * went through an authenticated proxy is redirected to a target that does not use that proxy, so
 * the proxy credentials leak to the redirect origin.
 *
 * Bumping axios would close those two CVEs. Removing it closes the class: this package was the
 * only lib in the workspace depending on axios, it is in esbuild's `external` list — so it is a
 * real install for anyone running `npx @dotcms/create-app` — and `compose-source.ts` in this same
 * package already used native `fetch`. Node >= 22.22.3 is required here (`.nvmrc`), where `fetch`
 * is stable.
 *
 * The fetch spec requires stripping `Authorization` on a cross-origin redirect, which is the
 * protection axios's Node adapter was missing.
 *
 * API PINNED
 *   export interface HttpResponse<T> { status: number; data: T }
 *   export class HttpError extends Error {
 *       status: number | null;             // null when the request never got a response
 *       code?: string;                     // ECONNREFUSED, ETIMEDOUT, ...
 *       response?: { status: number; statusText: string };
 *   }
 *   export function isHttpError(e: unknown): e is HttpError;
 *   export function httpGet<T>(url, opts?): Promise<HttpResponse<T>>;
 *   export function httpPost<T>(url, body, opts?): Promise<HttpResponse<T>>;
 *
 *   opts: { token?: string; timeoutMs?: number; acceptAnyStatus?: boolean }
 *
 * Throw-on-non-2xx is the default because that is what every existing call site expects;
 * `acceptAnyStatus` is the readiness probe's case, where a 503 is data rather than a failure.
 */

import { httpGet, httpPost, HttpError, isHttpError } from './http';

const URL_OK = 'http://localhost:8082/api/v1/thing';

function jsonResponse(status: number, body: unknown, statusText = 'OK') {
    return new Response(JSON.stringify(body), {
        status,
        statusText,
        headers: { 'Content-Type': 'application/json' }
    });
}

describe('http', () => {
    let fetchSpy: jest.SpyInstance;

    beforeEach(() => {
        fetchSpy = jest.spyOn(globalThis, 'fetch');
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('httpGet', () => {
        it('returns status and parsed body on success', async () => {
            fetchSpy.mockResolvedValue(jsonResponse(200, { entity: 'ok' }));

            await expect(httpGet(URL_OK)).resolves.toEqual({ status: 200, data: { entity: 'ok' } });
        });

        it('sends the bearer token when given one', async () => {
            fetchSpy.mockResolvedValue(jsonResponse(200, {}));

            await httpGet(URL_OK, { token: 'abc123' });

            const [, init] = fetchSpy.mock.calls[0];
            expect(new Headers(init.headers).get('authorization')).toBe('Bearer abc123');
        });

        it('sends no Authorization header when no token is given', async () => {
            fetchSpy.mockResolvedValue(jsonResponse(200, {}));

            await httpGet(URL_OK);

            const [, init] = fetchSpy.mock.calls[0];
            expect(new Headers(init.headers).has('authorization')).toBe(false);
        });

        it('throws an HttpError carrying the status on a non-2xx', async () => {
            fetchSpy.mockResolvedValue(jsonResponse(403, { message: 'forbidden' }, 'Forbidden'));

            const err = await httpGet(URL_OK).catch((e) => e);

            expect(isHttpError(err)).toBe(true);
            expect(err.status).toBe(403);
            // `response.status` is the shape configureUVE's statusOf() reads.
            expect(err.response?.status).toBe(403);
        });

        it.each([200, 201, 204, 299])('treats %i as success', async (status) => {
            fetchSpy.mockResolvedValue(new Response(null, { status }));

            await expect(httpGet(URL_OK)).resolves.toMatchObject({ status });
        });

        it('does not throw on a non-2xx when acceptAnyStatus is set', async () => {
            fetchSpy.mockResolvedValue(jsonResponse(503, {}, 'Service Unavailable'));

            // The readiness probe's case: a 503 means "still starting", not "request failed".
            await expect(httpGet(URL_OK, { acceptAnyStatus: true })).resolves.toMatchObject({
                status: 503
            });
        });

        it('surfaces a transport failure as an HttpError with a null status', async () => {
            fetchSpy.mockRejectedValue(
                Object.assign(new TypeError('fetch failed'), { cause: { code: 'ECONNREFUSED' } })
            );

            const err = await httpGet(URL_OK).catch((e) => e);

            expect(isHttpError(err)).toBe(true);
            expect(err.status).toBeNull();
            expect(err.code).toBe('ECONNREFUSED');
        });

        it('times out rather than hanging, and reports it as a timeout', async () => {
            fetchSpy.mockImplementation(
                (_url: string, init: RequestInit) =>
                    new Promise((_resolve, reject) => {
                        init.signal?.addEventListener('abort', () =>
                            reject(Object.assign(new Error('aborted'), { name: 'AbortError' }))
                        );
                    })
            );

            const err = await httpGet(URL_OK, { timeoutMs: 20 }).catch((e) => e);

            expect(isHttpError(err)).toBe(true);
            expect(err.code).toBe('ETIMEDOUT');
        });

        it('tolerates a body that is not JSON', async () => {
            fetchSpy.mockResolvedValue(new Response('plain text', { status: 200 }));

            await expect(httpGet(URL_OK)).resolves.toMatchObject({ status: 200 });
        });
    });

    describe('httpPost', () => {
        it('sends a JSON body and the content type', async () => {
            fetchSpy.mockResolvedValue(jsonResponse(200, { entity: 'Ok' }));

            await httpPost(URL_OK, { hello: 'world' }, { token: 'abc123' });

            const [, init] = fetchSpy.mock.calls[0];
            expect(init.method).toBe('POST');
            expect(init.body).toBe(JSON.stringify({ hello: 'world' }));
            expect(new Headers(init.headers).get('content-type')).toContain('application/json');
        });

        it('throws an HttpError on a non-2xx', async () => {
            fetchSpy.mockResolvedValue(jsonResponse(500, {}, 'Server Error'));

            const err = await httpPost(URL_OK, {}).catch((e) => e);

            expect(isHttpError(err)).toBe(true);
            expect(err.status).toBe(500);
        });
    });

    describe('isHttpError', () => {
        it('recognises its own errors and nothing else', () => {
            expect(isHttpError(new HttpError('x', { status: 404 }))).toBe(true);
            expect(isHttpError(new Error('plain'))).toBe(false);
            expect(isHttpError('a string')).toBe(false);
            expect(isHttpError(null)).toBe(false);
        });
    });
});
