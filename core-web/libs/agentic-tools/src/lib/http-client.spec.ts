import { createApiAdapter, isBinaryResponseEnvelope } from './http-client';

import type { Adapter, AdapterMethod } from './types';

/**
 * A real 1x1 red PNG. Its first byte is 0x89, which is not valid UTF-8 — the
 * exact kind of byte that `response.text()` corrupts into U+FFFD. This is the
 * regression fixture for the binary-response corruption bug.
 */
const PNG_BASE64 =
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';
const PNG_BYTES = Buffer.from(PNG_BASE64, 'base64');

const CONFIG = { dotcmsUrl: 'https://example.dotcms.com', authToken: 'test-token' };

function getRequestMethod(adapter: Adapter): AdapterMethod {
    const method = adapter.methods.get('request');
    if (!method) {
        throw new Error('request method not registered');
    }
    return method;
}

/** Build a Response-like stub backed by a fixed body buffer. */
function makeResponse(
    body: Buffer | string,
    { contentType, ok = true, status = 200, statusText = 'OK' }: {
        contentType: string;
        ok?: boolean;
        status?: number;
        statusText?: string;
    }
): Response {
    const buffer =
        typeof body === 'string' ? Buffer.from(body, 'utf-8') : body;
    return {
        ok,
        status,
        statusText,
        headers: { get: (name: string) => (name.toLowerCase() === 'content-type' ? contentType : null) },
        json: async () => JSON.parse(buffer.toString('utf-8')),
        text: async () => buffer.toString('utf-8'),
        arrayBuffer: async () =>
            buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.byteLength)
    } as unknown as Response;
}

describe('createApiAdapter response parsing', () => {
    const fetchMock = jest.fn();

    beforeEach(() => {
        fetchMock.mockReset();
        global.fetch = fetchMock as unknown as typeof fetch;
    });

    it('round-trips a binary (PNG) body as a base64 envelope without corrupting bytes', async () => {
        fetchMock.mockResolvedValue(makeResponse(PNG_BYTES, { contentType: 'image/png' }));

        const adapter = createApiAdapter(CONFIG);
        const result = await getRequestMethod(adapter).execute({ path: '/dA/abc123' });

        expect(isBinaryResponseEnvelope(result)).toBe(true);
        const envelope = result as {
            __dotcmsBinary: true;
            contentType: string;
            base64: string;
            byteLength: number;
        };
        expect(envelope.contentType).toBe('image/png');
        expect(envelope.byteLength).toBe(PNG_BYTES.byteLength);
        // The decoded bytes must be byte-exact to the source — the actual regression guard.
        expect(Buffer.from(envelope.base64, 'base64').equals(PNG_BYTES)).toBe(true);
    });

    it('parses JSON content as an object', async () => {
        fetchMock.mockResolvedValue(
            makeResponse(JSON.stringify({ hello: 'world' }), { contentType: 'application/json' })
        );

        const adapter = createApiAdapter(CONFIG);
        const result = await getRequestMethod(adapter).execute({ path: '/api/v1/x' });

        expect(result).toEqual({ hello: 'world' });
    });

    it('returns textual content types as strings', async () => {
        fetchMock.mockResolvedValue(
            makeResponse('<root/>', { contentType: 'application/xml; charset=utf-8' })
        );

        const adapter = createApiAdapter(CONFIG);
        const result = await getRequestMethod(adapter).execute({ path: '/api/x.xml' });

        expect(result).toBe('<root/>');
    });

    it('treats +json content types as textual strings', async () => {
        fetchMock.mockResolvedValue(
            makeResponse('{"a":1}', { contentType: 'application/vnd.api+json' })
        );

        const adapter = createApiAdapter(CONFIG);
        const result = await getRequestMethod(adapter).execute({ path: '/api/x' });

        expect(result).toBe('{"a":1}');
    });

    it('forces the binary path when responseType is "base64", even for JSON', async () => {
        fetchMock.mockResolvedValue(
            makeResponse(JSON.stringify({ hello: 'world' }), { contentType: 'application/json' })
        );

        const adapter = createApiAdapter(CONFIG);
        const result = await getRequestMethod(adapter).execute({
            path: '/api/v1/x',
            responseType: 'base64'
        });

        expect(isBinaryResponseEnvelope(result)).toBe(true);
    });

    it('reads the error body as text regardless of content-type', async () => {
        fetchMock.mockResolvedValue(
            makeResponse('<html>Not Found</html>', {
                contentType: 'text/html',
                ok: false,
                status: 404,
                statusText: 'Not Found'
            })
        );

        const adapter = createApiAdapter(CONFIG);
        await expect(getRequestMethod(adapter).execute({ path: '/dA/missing' })).rejects.toThrow(
            'HTTP 404 Not Found: <html>Not Found</html>'
        );
    });
});
