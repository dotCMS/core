import { createDotCMSRuntime } from './runtime';
import { HttpError, PolicyError } from './sandbox/errors';

/** Build a minimal JSON Response stub. */
function jsonResponse(body: unknown, init?: { ok?: boolean; status?: number }): Response {
    const ok = init?.ok ?? true;
    const status = init?.status ?? 200;
    return {
        ok,
        status,
        statusText: ok ? 'OK' : 'Error',
        headers: { get: (n: string) => (n.toLowerCase() === 'content-type' ? 'application/json' : null) },
        json: async () => body,
        text: async () => JSON.stringify(body)
    } as unknown as Response;
}

describe('createDotCMSRuntime.request (direct, no worker)', () => {
    const fetchMock = jest.fn();

    beforeEach(() => {
        fetchMock.mockReset();
        global.fetch = fetchMock as unknown as typeof fetch;
    });

    it('requires url and token', () => {
        expect(() => createDotCMSRuntime({ url: '', token: 't' })).toThrow(/url/);
        expect(() => createDotCMSRuntime({ url: 'https://x', token: '' })).toThrow(/token/);
    });

    it('injects the bearer token on the host side and returns parsed JSON', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ entity: [{ id: '1' }] }));
        const dotcms = createDotCMSRuntime({ url: 'https://demo.dotcms.com', token: 'secret-tok' });

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
        const dotcms = createDotCMSRuntime({ url: 'https://demo.dotcms.com', token: 't' });

        await expect(dotcms.request({ path: '/api/v1/missing' })).rejects.toBeInstanceOf(HttpError);
    });

    it('rejects a call that fails the allow-list with a PolicyError, before any fetch', async () => {
        const dotcms = createDotCMSRuntime({
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
        const dotcms = createDotCMSRuntime({
            url: 'https://demo.dotcms.com',
            token: 't',
            allow: ['/api/v1/site']
        });

        await expect(dotcms.request({ path: '/api/v1/site/123' })).resolves.toEqual({ ok: true });
    });

    it('fires the onCall observability hook without leaking the token', async () => {
        fetchMock.mockResolvedValue(jsonResponse({ ok: true }));
        const events: unknown[] = [];
        const dotcms = createDotCMSRuntime({
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
});
