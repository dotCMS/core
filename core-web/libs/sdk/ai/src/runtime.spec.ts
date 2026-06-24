import { createRuntime } from './runtime';
import { HttpError, PolicyError } from './sandbox/errors';

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
    const fetchMock = jest.fn();

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

    it('allows a call whose path matches an allow-list prefix', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ ok: true }));
        const dotcms = createRuntime({
            url: 'https://demo.dotcms.com',
            token: 't',
            allow: ['/api/v1/site']
        });

        await expect(dotcms.request({ path: '/api/v1/site/123' })).resolves.toEqual({ ok: true });
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

describe('createRuntime.run — context-load timeout', () => {
    const fetchMock = jest.fn();

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
