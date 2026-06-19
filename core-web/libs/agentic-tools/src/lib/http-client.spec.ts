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
    {
        contentType,
        ok = true,
        status = 200,
        statusText = 'OK',
        contentLength
    }: {
        contentType: string;
        ok?: boolean;
        status?: number;
        statusText?: string;
        // Override the Content-Length header independently of the actual body —
        // lets us simulate a server that advertises an oversized response.
        contentLength?: string;
    }
): Response {
    const buffer = typeof body === 'string' ? Buffer.from(body, 'utf-8') : body;
    const headerValues: Record<string, string | null> = {
        'content-type': contentType,
        'content-length': contentLength ?? String(buffer.byteLength)
    };
    return {
        ok,
        status,
        statusText,
        headers: { get: (name: string) => headerValues[name.toLowerCase()] ?? null },
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

    it('rejects an oversized binary response via Content-Length before buffering', async () => {
        const oversized = String(26 * 1024 * 1024); // 26MB > 25MB cap
        const arrayBuffer = jest.fn();
        fetchMock.mockResolvedValue({
            ok: true,
            status: 200,
            statusText: 'OK',
            headers: {
                get: (name: string) =>
                    name.toLowerCase() === 'content-type'
                        ? 'application/octet-stream'
                        : name.toLowerCase() === 'content-length'
                          ? oversized
                          : null
            },
            arrayBuffer
        } as unknown as Response);

        const adapter = createApiAdapter(CONFIG);
        await expect(getRequestMethod(adapter).execute({ path: '/dA/huge' })).rejects.toThrow(
            'exceeds the'
        );
        // The body must never be buffered when Content-Length already exceeds the cap.
        expect(arrayBuffer).not.toHaveBeenCalled();
    });

    describe('isBinaryResponseEnvelope', () => {
        it('accepts a fully-formed envelope', () => {
            expect(
                isBinaryResponseEnvelope({
                    __dotcmsBinary: true,
                    contentType: 'image/png',
                    base64: 'AA==',
                    byteLength: 1
                })
            ).toBe(true);
        });

        it('rejects an envelope missing contentType or byteLength', () => {
            expect(isBinaryResponseEnvelope({ __dotcmsBinary: true, base64: 'AA==' })).toBe(false);
            expect(
                isBinaryResponseEnvelope({
                    __dotcmsBinary: true,
                    base64: 'AA==',
                    contentType: 'image/png'
                })
            ).toBe(false);
        });

        it('rejects non-envelope values', () => {
            expect(isBinaryResponseEnvelope(null)).toBe(false);
            expect(isBinaryResponseEnvelope('string')).toBe(false);
            expect(isBinaryResponseEnvelope({ hello: 'world' })).toBe(false);
        });
    });
});
