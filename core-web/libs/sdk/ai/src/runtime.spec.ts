import { vi } from 'vitest';

import { createRuntime } from './runtime';
import { AbortError, HttpError, NetworkError, PolicyError } from './sandbox/errors';

/** Build a minimal JSON Response stub. */
function jsonResponse(body: unknown, init?: { ok?: boolean; status?: number }): Response {
    const ok = init?.ok ?? true;
    const status = init?.status ?? 200;
    return {
        ok,
        status,
        statusText: ok ? 'OK' : 'Error',
        headers: {
            get: (n: string) => (n.toLowerCase() === 'content-type' ? 'application/json' : null)
        },
        json: async () => body,
        text: async () => JSON.stringify(body)
    } as unknown as Response;
}

describe('createRuntime.request (direct, no worker)', () => {
    const fetchMock = vi.fn();

    beforeEach(() => {
        fetchMock.mockReset();
        global.fetch = fetchMock as unknown as typeof fetch;
    });

    it('requires url and token', () => {
        expect(() => createRuntime({ url: '', token: 't' })).toThrow(/url/);
        expect(() => createRuntime({ url: 'https://x', token: '' })).toThrow(/token/);
    });

    it('injects the bearer token on the host side and returns parsed JSON', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ entity: [{ id: '1' }] }));
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 'secret-tok' });

        const result = await dotcms.request({ path: '/api/v1/site' });

        expect(result).toEqual({ entity: [{ id: '1' }] });
        const [, init] = fetchMock.mock.calls[0];
        expect((init.headers as Record<string, string>).Authorization).toBe('Bearer secret-tok');
    });

    it('maps a non-2xx response to a typed HttpError', async () => {
        fetchMock.mockResolvedValue({
            ok: false,
            status: 404,
            statusText: 'Not Found',
            headers: { get: () => 'text/html' },
            text: async () => 'nope'
        } as unknown as Response);
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't' });

        await expect(dotcms.request({ path: '/api/v1/missing' })).rejects.toBeInstanceOf(HttpError);
    });

    it('maps a request that never got a response to a typed NetworkError', async () => {
        // What undici throws for a refused connection: a TypeError with the reason under cause.
        fetchMock.mockRejectedValue(
            Object.assign(new TypeError('fetch failed'), { cause: { code: 'ECONNREFUSED' } })
        );
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't' });

        const error = await dotcms.request({ path: '/api/v1/site' }).catch((e: unknown) => e);

        expect(error).toBeInstanceOf(NetworkError);
        expect(error).toMatchObject({
            code: 'NETWORK',
            reason: 'ECONNREFUSED',
            path: '/api/v1/site'
        });
    });

    it('still reports an aborted request as AbortError, not a network failure', async () => {
        fetchMock.mockRejectedValue(
            Object.assign(new Error('The operation was aborted'), { name: 'AbortError' })
        );
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't' });

        await expect(dotcms.request({ path: '/api/v1/site' })).rejects.toBeInstanceOf(AbortError);
    });

    it('rejects a call that fails the allow-list with a PolicyError, before any fetch', async () => {
        const dotcms = createRuntime({
            url: 'https://demo.dotcms.com',
            token: 't',
            allow: ['/api/v1/site'] // only sites allowed
        });

        await expect(dotcms.request({ path: '/api/v1/contenttype' })).rejects.toBeInstanceOf(
            PolicyError
        );
        expect(fetchMock).not.toHaveBeenCalled();
    });

    it.each([
        ['a dot-segment', '/api/v1/site/../contenttype'],
        ['an encoded dot-segment', '/api/v1/site/%2e%2e/contenttype'],
        ['a backslash dot-segment', '/api/v1/site/..\\contenttype']
    ])('judges the resolved path, so %s cannot escape the allow-list', async (_label, path) => {
        // Each of these starts with the allowed prefix, and each is sent by `fetch` as
        // `/api/v1/contenttype`. Checked as raw strings they all passed.
        const dotcms = createRuntime({
            url: 'https://demo.dotcms.com',
            token: 't',
            allow: ['/api/v1/site']
        });

        const error = await dotcms.request({ path }).catch((e: unknown) => e);

        expect(error).toBeInstanceOf(PolicyError);
        expect((error as PolicyError).path).toBe('/api/v1/contenttype');
        expect((error as PolicyError).message).toContain('resolves to /api/v1/contenttype');
        expect(fetchMock).not.toHaveBeenCalled();
    });

    it('allows a call whose path matches an allow-list prefix', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ ok: true }));
        const dotcms = createRuntime({
            url: 'https://demo.dotcms.com',
            token: 't',
            allow: ['/api/v1/site']
        });

        await expect(dotcms.request({ path: '/api/v1/site/123' })).resolves.toEqual({ ok: true });
    });

    describe('matches whole path segments', () => {
        // An entry names an endpoint and everything under it — not every path that happens to
        // begin with the same characters. As raw `startsWith`, allowing `/api/v1/content` also
        // allowed `/api/v1/contenttype`, `/api/v1/contentrelationships`, and so on.
        const allow = ['/api/v1/content', '/api/v1/page/'];

        it.each([
            ['/api/v1/contenttype', 'a sibling that extends the last segment'],
            ['/api/v1/contentrelationships/abc', 'a sibling with a subpath'],
            ['/api/v1/pages', 'a sibling of an entry written with a trailing slash']
        ])('refuses %s (%s)', async (path) => {
            const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't', allow });

            await expect(dotcms.request({ path })).rejects.toBeInstanceOf(PolicyError);
            expect(fetchMock).not.toHaveBeenCalled();
        });

        it.each([
            ['/api/v1/content', 'the entry itself'],
            ['/api/v1/content/abc-123', 'a path under it'],
            ['/api/v1/content/abc/versions', 'a deeper path under it'],
            ['/api/v1/page', 'an entry written with a trailing slash, without it'],
            ['/api/v1/page/render/about-us', 'a path under an entry written with a trailing slash']
        ])('allows %s (%s)', async (path) => {
            fetchMock.mockResolvedValue(jsonResponse({ ok: true }));
            const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't', allow });

            await expect(dotcms.request({ path })).resolves.toEqual({ ok: true });
        });
    });

    it('fires the onCall observability hook without leaking the token', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ ok: true }));
        const events: unknown[] = [];
        const dotcms = createRuntime({
            url: 'https://demo.dotcms.com',
            token: 'super-secret',
            onCall: (e) => events.push(e)
        });

        await dotcms.request({ path: '/api/v1/site' });

        expect(events).toHaveLength(1);
        const serialized = JSON.stringify(events[0]);
        expect(serialized).not.toContain('super-secret');
        expect(serialized).toContain('/api/v1/site');
    });

    it('passes a caller-supplied AbortSignal through the direct request path', async () => {
        const controller = new AbortController();
        // fetch that rejects only when its signal aborts (a hanging request that honors abort).
        fetchMock.mockImplementation(
            (_url: string, init: RequestInit) =>
                new Promise((_resolve, reject) => {
                    init.signal?.addEventListener('abort', () =>
                        reject(Object.assign(new Error('aborted'), { name: 'AbortError' }))
                    );
                })
        );
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't' });

        const p = dotcms.request({ path: '/api/v1/site' }, { signal: controller.signal });
        controller.abort();
        await expect(p).rejects.toMatchObject({ code: 'ABORT' });
    });
});

describe('createRuntime.request — streaming file bodies (host only)', () => {
    // A file goes to dotCMS and comes back out without ever being held whole in memory: the
    // upload hands `fetch` a Blob it reads as it sends, the download hands the body stream
    // to a sink that writes it. Neither is reachable from sandboxed code — a Blob the host
    // opened from disk and a function are both things only the host can supply.
    const fetchMock = vi.fn();

    beforeEach(() => {
        fetchMock.mockReset();
        global.fetch = fetchMock as unknown as typeof fetch;
    });

    /** A 200 response whose body is `chunks`, and whose decoders fail the test if used. */
    function streamResponse(chunks: string[], signal?: AbortSignal | null, hang = false) {
        const encoder = new TextEncoder();
        const body = new ReadableStream<Uint8Array>({
            start(controller) {
                signal?.addEventListener('abort', () =>
                    controller.error(new DOMException('The operation was aborted', 'AbortError'))
                );
                for (const chunk of chunks) controller.enqueue(encoder.encode(chunk));
                if (!hang) controller.close();
            }
        });
        const unused = () => {
            throw new Error('the body was decoded instead of streamed');
        };

        return {
            ok: true,
            status: 200,
            statusText: 'OK',
            headers: {
                get: (n: string) =>
                    n.toLowerCase() === 'content-type' ? 'application/octet-stream' : null
            },
            body,
            json: vi.fn(unused),
            text: vi.fn(unused),
            arrayBuffer: vi.fn(unused)
        } as unknown as Response;
    }

    /** Everything a stream yields, as text. */
    async function readAll(stream: ReadableStream<Uint8Array>): Promise<string> {
        return new Response(stream).text();
    }

    it('sends a Blob file field as the multipart file part, unchanged', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ entity: { identifier: 'a-1' } }));
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't' });

        await dotcms.request({
            method: 'PUT',
            path: '/api/v2/assets/publish',
            formData: {
                path: '//demo.dotcms.com/application/style.css',
                file: { name: 'style.css', type: 'text/css', blob: new Blob(['.a{color:red}']) }
            }
        });

        const form = fetchMock.mock.calls[0][1].body as FormData;
        const part = form.get('file') as File;
        expect(part.name).toBe('style.css');
        expect(await part.text()).toBe('.a{color:red}');
    });

    it('hands a successful body to onBody and resolves to what it returns', async () => {
        const response = streamResponse(['hello ', 'world']);
        fetchMock.mockResolvedValue(response);
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't' });

        const result = await dotcms.request({
            path: '/api/v2/assets/a-1',
            onBody: async (body, info) => ({ text: await readAll(body), type: info.contentType })
        });

        expect(result).toEqual({ text: 'hello world', type: 'application/octet-stream' });
        expect(response.arrayBuffer).not.toHaveBeenCalled();
    });

    it('reports an error response as HttpError without calling onBody', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ message: 'nope' }, { ok: false, status: 404 }));
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't' });
        const onBody = vi.fn();

        await expect(
            dotcms.request({ path: '/api/v2/assets/missing', onBody })
        ).rejects.toBeInstanceOf(HttpError);
        expect(onBody).not.toHaveBeenCalled();
    });

    it('keeps the abort signal in force until the sink has consumed the body', async () => {
        // The body is still arriving when the caller aborts. Had the request settled at the
        // headers, the transfer would carry on unbounded after the deadline had "fired".
        fetchMock.mockImplementation(async (_url: string, init: RequestInit) =>
            streamResponse(['partial'], init.signal, true)
        );
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't' });
        const controller = new AbortController();
        setTimeout(() => controller.abort(), 20);

        const pending = dotcms.request(
            { path: '/api/v2/assets/a-1', onBody: (body) => readAll(body) },
            { signal: controller.signal }
        );

        await expect(pending).rejects.toBeInstanceOf(AbortError);
    });
});

describe('createRuntime.run — context-load timeout', () => {
    const fetchMock = vi.fn();

    beforeEach(() => {
        fetchMock.mockReset();
        global.fetch = fetchMock as unknown as typeof fetch;
    });

    it('does not hang when context loading stalls — the run timeout aborts the load', async () => {
        let aborts = 0;
        // Every context fetch hangs until its abort signal fires (a stalled instance).
        fetchMock.mockImplementation(
            (_url: string, init: RequestInit) =>
                new Promise((_resolve, reject) => {
                    init.signal?.addEventListener('abort', () => {
                        aborts++;
                        reject(Object.assign(new Error('aborted'), { name: 'AbortError' }));
                    });
                })
        );
        const timeout = 150;
        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't', timeout });

        // The run must RESOLVE (not hang) — the load timeout aborts the stalled context fetch.
        // The loaders degrade to empty context on abort, so the trivial body then runs fine.
        const start = Date.now();
        const result = await dotcms.run(`return 1;`);
        const elapsed = Date.now() - start;

        expect(aborts).toBeGreaterThan(0); // the stalled load WAS aborted
        expect(elapsed).toBeLessThan(2000); // resolved promptly, did not hang
        expect(result.value).toBe(1);
    }, 5000);
});

describe('createRuntime context freshness', () => {
    const fetchMock = vi.fn();

    beforeEach(() => {
        fetchMock.mockReset();
        global.fetch = fetchMock as unknown as typeof fetch;
    });

    it('loads all site states and reuses the snapshot within one runtime', async () => {
        let siteLoads = 0;
        fetchMock.mockImplementation(async (url: string) => {
            const parsed = new URL(url);
            if (parsed.pathname === '/api/v1/site') {
                siteLoads += 1;
                expect(parsed.searchParams.get('archive')).toBe('true');
                return jsonResponse({
                    entity: [
                        {
                            identifier: `site-${siteLoads}`,
                            siteName: 'demo.dotcms.com',
                            isDefault: true,
                            isArchived: siteLoads === 1,
                            isLive: siteLoads > 1
                        }
                    ]
                });
            }
            return jsonResponse({ entity: [] });
        });

        const dotcms = createRuntime({ url: 'https://demo.dotcms.com', token: 't' });
        const first = await dotcms.loadContext();
        const cached = await dotcms.loadContext();
        expect(first.sites[0]).toMatchObject({ archived: true, live: false });
        expect(cached.sites[0].identifier).toBe('site-1');
        expect(siteLoads).toBe(1);
    });

    it('paginates and de-duplicates the complete site catalog', async () => {
        fetchMock.mockImplementation(async (url: string) => {
            const parsed = new URL(url);
            if (parsed.pathname !== '/api/v1/site') {
                return jsonResponse({ entity: [] });
            }
            const page = Number(parsed.searchParams.get('page'));
            if (page === 0) {
                return jsonResponse({
                    entity: Array.from({ length: 200 }, (_, index) => ({
                        identifier: `site-${index}`,
                        siteName: `site-${index}.example.com`,
                        isLive: true
                    }))
                });
            }
            return jsonResponse({
                entity: [
                    { identifier: 'site-199', siteName: 'duplicate.example.com', isLive: false },
                    { identifier: 'site-200', siteName: 'last.example.com', isLive: false }
                ]
            });
        });

        const context = await createRuntime({
            url: 'https://demo.dotcms.com',
            token: 't'
        }).loadContext();

        expect(context.sites).toHaveLength(201);
        expect(context.sites.find((site) => site.identifier === 'site-199')?.hostname).toBe(
            'duplicate.example.com'
        );
        expect(context.sites.find((site) => site.identifier === 'site-200')?.live).toBe(false);
    });
});
