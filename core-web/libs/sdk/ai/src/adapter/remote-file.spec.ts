import { EventEmitter } from 'node:events';
import { Agent as HttpAgent, type ClientRequest, type IncomingMessage } from 'node:http';
import { Agent as HttpsAgent, type RequestOptions as HttpsRequestOptions } from 'node:https';
import { isIP, Socket } from 'node:net';
import { PassThrough } from 'node:stream';
import { brotliCompressSync, deflateSync, gzipSync } from 'node:zlib';

import {
    assertSafeRemoteUrl,
    fetchRemoteFile,
    isBlockedAddress,
    type AddressResolver,
    type RequestTransport,
    type ResolvedAddress
} from './remote-file';

import {
    AbortError,
    HttpError,
    NetworkError,
    TimeoutError,
    ValidationError
} from '../sandbox/errors';

/**
 * Remote file descriptors (`formData` fields with a `url`) are downloaded on the host.
 * These specs pin what the download accepts: only public http(s) targets, judged on the
 * addresses the host actually resolves to, with redirects refused and the size cap kept.
 */

const PUBLIC_V4 = '93.184.216.34';
const PUBLIC_V6 = '2606:4700::1111';

describe('isBlockedAddress', () => {
    it.each([PUBLIC_V4, '1.1.1.1', '8.8.8.8', PUBLIC_V6])(
        'lets public addresses through: %s',
        (address) => {
            expect(isBlockedAddress(address)).toBe(false);
        }
    );

    it.each(['::ffff:8.8.8.8', '::ffff:5db8:d822'])(
        'lets an IPv4-mapped form of a public address through: %s',
        (address) => {
            expect(isBlockedAddress(address)).toBe(false);
        }
    );

    it.each([
        ['0.0.0.0', 'unspecified'],
        ['10.0.0.1', 'private 10/8'],
        ['100.100.100.200', 'shared address space 100.64/10'],
        ['127.0.0.1', 'loopback'],
        ['169.254.1.1', 'link-local'],
        ['172.16.0.1', 'private 172.16/12'],
        ['192.0.0.1', 'IETF protocol assignments'],
        ['192.0.2.1', 'documentation'],
        ['192.88.99.1', '6to4 relay'],
        ['192.168.1.1', 'private 192.168/16'],
        ['198.18.0.1', 'benchmarking 198.18/15'],
        ['198.51.100.1', 'documentation'],
        ['203.0.113.1', 'documentation'],
        ['224.0.0.1', 'multicast'],
        ['240.0.0.1', 'reserved']
    ])('blocks each non-public IPv4 range: %s (%s)', (address) => {
        expect(isBlockedAddress(address)).toBe(true);
    });

    it.each([
        ['172.15.255.255', false],
        ['172.16.0.0', true],
        ['172.31.255.255', true],
        ['172.32.0.0', false],
        ['100.63.255.255', false],
        ['100.64.0.0', true],
        ['100.127.255.255', true],
        ['100.128.0.0', false],
        ['198.17.255.255', false],
        ['198.18.0.0', true],
        ['198.19.255.255', true],
        ['198.20.0.0', false],
        ['223.255.255.255', false],
        ['224.0.0.0', true]
    ])('applies the exact prefix length at each range edge: %s → %s', (address, blocked) => {
        expect(isBlockedAddress(address)).toBe(blocked);
    });

    it.each([
        ['::1', 'loopback'],
        ['::', 'unspecified'],
        ['fc00::1', 'unique-local'],
        ['fd12:3456::1', 'unique-local'],
        ['fe80::1', 'link-local'],
        ['fec0::1', 'site-local'],
        ['ff02::1', 'multicast'],
        ['2001:db8::1', 'documentation'],
        ['2002:7f00:1::1', '6to4 embedding loopback'],
        ['::ffff:127.0.0.1', 'IPv4-mapped loopback'],
        ['::ffff:7f00:1', 'IPv4-mapped loopback, hex'],
        ['::ffff:a9fe:a9fe', 'IPv4-mapped link-local'],
        ['::7f00:1', 'IPv4-compatible loopback'],
        ['64:ff9b::a9fe:a9fe', 'NAT64'],
        ['64:ff9b:1::1', 'local-use NAT64']
    ])('blocks each non-public IPv6 range and IPv4-embedding form: %s (%s)', (address) => {
        expect(isBlockedAddress(address)).toBe(true);
    });

    it('ignores values that are not IP addresses', () => {
        expect(isBlockedAddress('example.com')).toBe(false);
        expect(isBlockedAddress('')).toBe(false);
    });
});

describe('assertSafeRemoteUrl', () => {
    it('rejects text that is not a URL', () => {
        expect(() => assertSafeRemoteUrl('not a url')).toThrow(ValidationError);
    });

    it('rejects schemes other than http and https', () => {
        expect(() => assertSafeRemoteUrl('ftp://example.com/f')).toThrow(ValidationError);
    });

    it('rejects a URL that carries credentials, without echoing them', () => {
        expect(() => assertSafeRemoteUrl('https://user:s3cret@example.com/f')).toThrow(
            ValidationError
        );
        expect(() => assertSafeRemoteUrl('https://user:s3cret@example.com/f')).not.toThrow(
            /s3cret/
        );
    });

    it.each([
        'http://localhost/',
        'http://LOCALHOST/',
        'http://localhost./',
        'http://a.localhost/',
        'http://metadata.google.internal/'
    ])('rejects local host names: %s', (url) => {
        expect(() => assertSafeRemoteUrl(url)).toThrow(ValidationError);
    });

    it.each(['http://127.0.0.1/', 'http://169.254.1.1/', 'http://10.0.0.5/', 'http://100.64.0.1/'])(
        'rejects non-public IPv4 literals: %s',
        (url) => {
            expect(() => assertSafeRemoteUrl(url)).toThrow(ValidationError);
        }
    );

    it.each([
        'http://[::1]/',
        'http://[::ffff:127.0.0.1]/',
        'http://[::ffff:169.254.1.1]/',
        'http://[::ffff:192.168.0.1]/',
        'http://[::ffff:100.64.0.1]/',
        'http://[::127.0.0.1]/',
        'http://[64:ff9b::a9fe:a9fe]/'
    ])('rejects non-public IPv6 literals in every notation: %s', (url) => {
        expect(() => assertSafeRemoteUrl(url)).toThrow(ValidationError);
    });

    it('returns the parsed URL for public targets', () => {
        expect(assertSafeRemoteUrl('https://cdn.example.com/logo.png').href).toBe(
            'https://cdn.example.com/logo.png'
        );
        expect(assertSafeRemoteUrl(`http://${PUBLIC_V4}/x`).hostname).toBe(PUBLIC_V4);
        expect(assertSafeRemoteUrl('http://[::ffff:8.8.8.8]/x').hostname).toBe('[::ffff:808:808]');
    });
});

/** What the fake transport answers with. */
interface Reply {
    status?: number;
    statusMessage?: string;
    headers?: Record<string, string>;
    /** Body chunks, written in order. */
    chunks?: Buffer[];
    /** Keep the body open until the request is destroyed. */
    hang?: boolean;
    /** Close the response after the chunks without ending it (connection cut). */
    truncate?: boolean;
    /** Emit this error on the response after the chunks. */
    responseError?: Error;
    /** Emit this error on the request instead of answering. */
    requestError?: Error;
    /** Throw this synchronously from the transport. */
    throwSync?: Error;
    /** Never answer: no response, no error (an address that does not accept the connection). */
    silent?: boolean;
    /** Overrides for the request opened to a given address (`options.hostname`). */
    perAddress?: Record<string, Omit<Reply, 'perAddress'>>;
}

interface FakeRequest extends EventEmitter {
    destroyed: boolean;
    destroy: (err?: Error) => FakeRequest;
    end: () => FakeRequest;
}

/**
 * A transport that never opens a socket: it records the call and replies from `reply`.
 * `destroy()` without an error emits ECONNRESET, as Node's `ClientRequest` does.
 */
function fakeTransport(defaults: Reply = {}) {
    const calls: { url: URL; options: HttpsRequestOptions }[] = [];
    const requests: FakeRequest[] = [];
    let markDelivered: () => void = () => undefined;
    /** Resolves once a response and its chunks have been handed to the download. */
    const delivered = new Promise<void>((resolve) => (markDelivered = resolve));
    const transport: RequestTransport = (url, options, onResponse) => {
        const reply: Reply = {
            ...defaults,
            ...(defaults.perAddress?.[String(options.hostname)] ?? {})
        };
        if (reply.throwSync) {
            throw reply.throwSync;
        }
        calls.push({ url, options });
        const req = new EventEmitter() as FakeRequest;
        req.destroyed = false;
        req.destroy = (err?: Error) => {
            if (req.destroyed) return req;
            req.destroyed = true;
            req.emit(
                'error',
                err ?? Object.assign(new Error('socket hang up'), { code: 'ECONNRESET' })
            );
            return req;
        };
        req.end = () => {
            if (reply.silent) {
                return req;
            }
            setImmediate(() => {
                if (reply.requestError) {
                    req.emit('error', reply.requestError);
                    return;
                }
                const res = new PassThrough() as unknown as IncomingMessage & PassThrough;
                res.statusCode = reply.status ?? 200;
                res.statusMessage = reply.statusMessage ?? 'OK';
                res.headers = reply.headers ?? {};
                res.complete = false;
                onResponse(res);
                if (req.destroyed) {
                    return;
                }
                for (const chunk of reply.chunks ?? []) {
                    res.write(chunk);
                }
                markDelivered();
                if (reply.responseError) {
                    res.emit('error', reply.responseError);
                } else if (reply.truncate) {
                    res.destroy();
                } else if (!reply.hang) {
                    res.complete = true;
                    res.end();
                }
            });
            return req;
        };
        requests.push(req);
        return req as unknown as ClientRequest;
    };
    return { transport, calls, requests, delivered };
}

const resolvingTo =
    (...addresses: ResolvedAddress[]): AddressResolver =>
    async () =>
        addresses;

const publicHost = resolvingTo({ address: PUBLIC_V4, family: 4 });
const url = new URL('https://files.example/logo.png');
const base = { type: 'text/plain', maxBytes: 1024, resolve: publicHost };

describe('fetchRemoteFile', () => {
    it('downloads from a host whose addresses are all public, connecting to the checked address', async () => {
        const bytes = Buffer.from('logo-bytes');
        const { transport, calls } = fakeTransport({ chunks: [bytes] });

        const blob = await fetchRemoteFile(url, { ...base, type: 'image/png', transport });

        expect(blob.type).toBe('image/png');
        expect(Buffer.from(await blob.arrayBuffer()).equals(bytes)).toBe(true);
        // The socket goes to the address that was checked (`hostname` is what Node connects to
        // when a URL is also given); the name is kept for TLS and the Host header.
        expect(calls).toHaveLength(1);
        expect(calls[0].options).toMatchObject({
            method: 'GET',
            hostname: PUBLIC_V4,
            port: 443,
            servername: 'files.example',
            headers: { host: 'files.example' }
        });
    });

    it('uses the URL port and plain http defaults', async () => {
        const { transport, calls } = fakeTransport({ chunks: [Buffer.from('x')] });

        await fetchRemoteFile('http://files.example:8080/f', { ...base, transport });
        await fetchRemoteFile('http://files.example/f', { ...base, transport });

        expect(calls[0].options).toMatchObject({
            port: '8080',
            headers: { host: 'files.example:8080' }
        });
        expect(calls[1].options).toMatchObject({ port: 80 });
    });

    it('identifies itself and accepts any content type', async () => {
        const { transport, calls } = fakeTransport({ chunks: [Buffer.from('x')] });

        await fetchRemoteFile(url, { ...base, transport });

        expect(calls[0].options.headers).toMatchObject({ accept: '*/*' });
        expect(calls[0].options.headers).toHaveProperty('user-agent');
    });

    it('connects to an IPv6 address as resolved', async () => {
        const { transport, calls } = fakeTransport({ chunks: [Buffer.from('x')] });

        await fetchRemoteFile(url, {
            ...base,
            resolve: resolvingTo({ address: PUBLIC_V6, family: 6 }),
            transport
        });

        expect(calls[0].options).toMatchObject({
            hostname: PUBLIC_V6,
            servername: 'files.example'
        });
    });

    it('connects to the first of several public addresses', async () => {
        const { transport, calls } = fakeTransport({ chunks: [Buffer.from('x')] });

        await fetchRemoteFile(url, {
            ...base,
            resolve: resolvingTo(
                { address: PUBLIC_V6, family: 6 },
                { address: PUBLIC_V4, family: 4 }
            ),
            transport
        });

        expect(calls[0].options.hostname).toBe(PUBLIC_V6);
    });

    it('moves on to the next checked address when one refuses the connection', async () => {
        const OTHER_V4 = '93.184.216.35';
        const { transport, calls } = fakeTransport({
            chunks: [Buffer.from('ok')],
            perAddress: {
                [PUBLIC_V6]: {
                    requestError: Object.assign(new Error('connect ENETUNREACH'), {
                        code: 'ENETUNREACH'
                    })
                },
                [PUBLIC_V4]: {
                    requestError: Object.assign(new Error('connect ECONNREFUSED'), {
                        code: 'ECONNREFUSED'
                    })
                }
            }
        });

        const blob = await fetchRemoteFile(url, {
            ...base,
            resolve: resolvingTo(
                { address: PUBLIC_V6, family: 6 },
                { address: PUBLIC_V4, family: 4 },
                { address: OTHER_V4, family: 4 }
            ),
            transport
        });

        expect(await blob.text()).toBe('ok');
        expect(calls.map((c) => c.options.hostname)).toEqual([PUBLIC_V6, PUBLIC_V4, OTHER_V4]);
        expect(calls.every((c) => c.options.servername === 'files.example')).toBe(true);
    });

    it('moves on when an address does not accept the connection in time', async () => {
        const { transport, calls, requests } = fakeTransport({
            chunks: [Buffer.from('ok')],
            perAddress: { [PUBLIC_V6]: { silent: true } }
        });

        const blob = await fetchRemoteFile(url, {
            ...base,
            connectAttemptMs: 20,
            resolve: resolvingTo(
                { address: PUBLIC_V6, family: 6 },
                { address: PUBLIC_V4, family: 4 }
            ),
            transport
        });

        expect(await blob.text()).toBe('ok');
        expect(calls.map((c) => c.options.hostname)).toEqual([PUBLIC_V6, PUBLIC_V4]);
        expect(requests[0].destroyed).toBe(true);
    });

    it('gives the last address no connect limit of its own', async () => {
        const { transport, calls } = fakeTransport({ silent: true });
        const controller = new AbortController();

        const download = fetchRemoteFile(url, {
            ...base,
            connectAttemptMs: 20,
            signal: controller.signal,
            transport
        });
        const outcome = download.then(
            () => 'resolved',
            () => 'rejected'
        );
        await new Promise((r) => setTimeout(r, 80));

        expect(calls).toHaveLength(1);
        expect(await Promise.race([outcome, Promise.resolve('pending')])).toBe('pending');
        controller.abort();
        await expect(download).rejects.toBeInstanceOf(AbortError);
    });

    it('reports the last connection error when no address can be reached', async () => {
        const { transport, calls } = fakeTransport({
            requestError: Object.assign(new Error('connect ECONNREFUSED'), {
                code: 'ECONNREFUSED'
            })
        });

        const failure = fetchRemoteFile(url, {
            ...base,
            resolve: resolvingTo(
                { address: PUBLIC_V6, family: 6 },
                { address: PUBLIC_V4, family: 4 }
            ),
            transport
        });

        await expect(failure).rejects.toBeInstanceOf(NetworkError);
        await expect(failure).rejects.toMatchObject({ reason: 'ECONNREFUSED' });
        expect(calls).toHaveLength(2);
    });

    it('does not try another address once a response has started', async () => {
        const { transport, calls } = fakeTransport({
            chunks: [Buffer.from('partial')],
            responseError: Object.assign(new Error('read ECONNRESET'), { code: 'ECONNRESET' })
        });

        await expect(
            fetchRemoteFile(url, {
                ...base,
                resolve: resolvingTo(
                    { address: PUBLIC_V4, family: 4 },
                    { address: PUBLIC_V6, family: 6 }
                ),
                transport
            })
        ).rejects.toBeInstanceOf(NetworkError);
        expect(calls).toHaveLength(1);
    });

    it('does not try another address after an HTTP error status', async () => {
        const { transport, calls } = fakeTransport({ status: 503, statusMessage: 'Unavailable' });

        await expect(
            fetchRemoteFile(url, {
                ...base,
                resolve: resolvingTo(
                    { address: PUBLIC_V4, family: 4 },
                    { address: PUBLIC_V6, family: 6 }
                ),
                transport
            })
        ).rejects.toMatchObject({ status: 503 });
        expect(calls).toHaveLength(1);
    });

    it('uses an IP literal host as its own address without resolving it or naming it for TLS', async () => {
        const { transport, calls } = fakeTransport({ chunks: [Buffer.from('x')] });
        const resolve = vi.fn<AddressResolver>();

        await fetchRemoteFile(`https://${PUBLIC_V4}/f`, { ...base, resolve, transport });

        expect(resolve).not.toHaveBeenCalled();
        expect(calls[0].options.hostname).toBe(PUBLIC_V4);
        expect(calls[0].options.servername).toBeUndefined();
    });

    it.each([
        'not a url',
        'ftp://example.com/f',
        'https://user:s3cret@example.com/f',
        'http://localhost/f',
        'http://metadata.google.internal/f',
        'http://127.0.0.1/f',
        'http://[::ffff:7f00:1]/f'
    ])('applies the text-level checks itself: %s', async (target) => {
        const { transport, calls } = fakeTransport();
        const resolve = vi.fn<AddressResolver>();

        await expect(
            fetchRemoteFile(target, { ...base, resolve, transport })
        ).rejects.toBeInstanceOf(ValidationError);
        expect(resolve).not.toHaveBeenCalled();
        expect(calls).toHaveLength(0);
    });

    it('refuses a host that resolves to a non-public address', async () => {
        const { transport, calls } = fakeTransport();

        await expect(
            fetchRemoteFile(url, {
                ...base,
                resolve: resolvingTo({ address: '127.0.0.1', family: 4 }),
                transport
            })
        ).rejects.toBeInstanceOf(ValidationError);
        expect(calls).toHaveLength(0);
    });

    it('refuses a host when any resolved address is non-public', async () => {
        const { transport, calls } = fakeTransport();

        await expect(
            fetchRemoteFile(url, {
                ...base,
                resolve: resolvingTo(
                    { address: PUBLIC_V4, family: 4 },
                    { address: '10.0.0.5', family: 4 }
                ),
                transport
            })
        ).rejects.toBeInstanceOf(ValidationError);
        expect(calls).toHaveLength(0);
    });

    it('refuses a resolved address that is not an IP address', async () => {
        const { transport, calls } = fakeTransport();

        await expect(
            fetchRemoteFile(url, {
                ...base,
                resolve: resolvingTo({ address: 'internal.host', family: 4 }),
                transport
            })
        ).rejects.toBeInstanceOf(ValidationError);
        expect(calls).toHaveLength(0);
    });

    it('reports a host with no address as a network error', async () => {
        const { transport, calls } = fakeTransport();

        await expect(
            fetchRemoteFile(url, { ...base, resolve: resolvingTo(), transport })
        ).rejects.toBeInstanceOf(NetworkError);
        expect(calls).toHaveLength(0);
    });

    it('reports an unresolvable host as a network error naming the host and the URL', async () => {
        const { transport } = fakeTransport();
        const resolve: AddressResolver = async () => {
            throw Object.assign(new Error('getaddrinfo ENOTFOUND files.example'), {
                code: 'ENOTFOUND'
            });
        };

        const failure = fetchRemoteFile(url, { ...base, resolve, transport });

        await expect(failure).rejects.toBeInstanceOf(NetworkError);
        await expect(failure).rejects.toMatchObject({ reason: 'ENOTFOUND', path: url.href });
        await expect(failure).rejects.toThrow(/files\.example/);
        await expect(failure).rejects.not.toThrow(/dotCMS instance/);
    });

    it('does not follow redirects and says so', async () => {
        const { transport, calls, requests } = fakeTransport({
            status: 302,
            statusMessage: 'Found'
        });

        const failure = fetchRemoteFile(url, { ...base, transport });

        await expect(failure).rejects.toBeInstanceOf(HttpError);
        await expect(failure).rejects.toMatchObject({
            status: 302,
            body: expect.stringContaining('Redirects are not followed')
        });
        expect(calls).toHaveLength(1);
        expect(requests[0].destroyed).toBe(true);
    });

    it('reports a non-2xx status as an HTTP error and drops the connection', async () => {
        const { transport, requests } = fakeTransport({ status: 404, statusMessage: 'Not Found' });

        const failure = fetchRemoteFile(url, { ...base, transport });

        await expect(failure).rejects.toBeInstanceOf(HttpError);
        await expect(failure).rejects.toMatchObject({ status: 404 });
        expect(requests[0].destroyed).toBe(true);
    });

    it('returns an empty Blob for an empty body', async () => {
        const { transport } = fakeTransport();

        const blob = await fetchRemoteFile(url, { ...base, transport });

        expect(blob.size).toBe(0);
    });

    it('concatenates chunks in order', async () => {
        const { transport } = fakeTransport({
            chunks: [Buffer.from('ab'), Buffer.from('cd'), Buffer.from('ef')]
        });

        const blob = await fetchRemoteFile(url, { ...base, transport });

        expect(await blob.text()).toBe('abcdef');
    });

    it.each([
        ['gzip', gzipSync],
        ['deflate', deflateSync],
        ['br', brotliCompressSync]
    ] as const)('decodes a %s-encoded body', async (encoding, compress) => {
        const { transport } = fakeTransport({
            headers: { 'content-encoding': encoding },
            chunks: [Buffer.from(compress(Buffer.from('hello, compressed')))]
        });

        const blob = await fetchRemoteFile(url, { ...base, transport });

        expect(await blob.text()).toBe('hello, compressed');
    });

    it('applies the size limit to the decoded bytes', async () => {
        const { transport } = fakeTransport({
            headers: { 'content-encoding': 'gzip' },
            chunks: [Buffer.from(gzipSync(Buffer.alloc(64)))]
        });

        await expect(
            fetchRemoteFile(url, { ...base, maxBytes: 16, transport })
        ).rejects.toBeInstanceOf(ValidationError);
    });

    it('refuses a content encoding it cannot decode', async () => {
        const { transport, requests } = fakeTransport({
            headers: { 'content-encoding': 'zstd' },
            chunks: [Buffer.from('x')]
        });

        await expect(fetchRemoteFile(url, { ...base, transport })).rejects.toBeInstanceOf(
            ValidationError
        );
        expect(requests[0].destroyed).toBe(true);
    });

    it('refuses a declared size over the limit before reading the body', async () => {
        const { transport, requests } = fakeTransport({
            headers: { 'content-length': '500' },
            chunks: [Buffer.alloc(500)]
        });

        const failure = fetchRemoteFile(url, { ...base, maxBytes: 16, transport });

        await expect(failure).rejects.toBeInstanceOf(ValidationError);
        await expect(failure).rejects.toThrow(/500 bytes/);
        expect(requests[0].destroyed).toBe(true);
    });

    it('accepts a body at the size limit and rejects one byte over', async () => {
        const atLimit = fakeTransport({ chunks: [Buffer.alloc(16)] });
        const blob = await fetchRemoteFile(url, {
            ...base,
            maxBytes: 16,
            transport: atLimit.transport
        });
        expect(blob.size).toBe(16);

        const over = fakeTransport({ chunks: [Buffer.alloc(17)] });
        await expect(
            fetchRemoteFile(url, { ...base, maxBytes: 16, transport: over.transport })
        ).rejects.toBeInstanceOf(ValidationError);
        expect(over.requests[0].destroyed).toBe(true);
    });

    it('counts the size limit across chunks', async () => {
        const { transport, requests } = fakeTransport({
            chunks: [Buffer.alloc(10), Buffer.alloc(10)]
        });

        await expect(
            fetchRemoteFile(url, { ...base, maxBytes: 16, transport })
        ).rejects.toBeInstanceOf(ValidationError);
        expect(requests[0].destroyed).toBe(true);
    });

    it('reports a response that fails mid-stream as a network error', async () => {
        const { transport } = fakeTransport({
            chunks: [Buffer.from('partial')],
            responseError: Object.assign(new Error('read ECONNRESET'), { code: 'ECONNRESET' })
        });

        const failure = fetchRemoteFile(url, { ...base, transport });

        await expect(failure).rejects.toBeInstanceOf(NetworkError);
        await expect(failure).rejects.toMatchObject({ reason: 'ECONNRESET' });
    });

    it('reports a response cut off before its end as a network error', async () => {
        const { transport } = fakeTransport({ chunks: [Buffer.from('partial')], truncate: true });

        await expect(fetchRemoteFile(url, { ...base, transport })).rejects.toBeInstanceOf(
            NetworkError
        );
    });

    it('reports a request that fails before any response as a network error', async () => {
        const { transport } = fakeTransport({
            requestError: Object.assign(new Error('connect ECONNREFUSED'), {
                code: 'ECONNREFUSED'
            })
        });

        const failure = fetchRemoteFile(url, { ...base, transport });

        await expect(failure).rejects.toBeInstanceOf(NetworkError);
        await expect(failure).rejects.toMatchObject({ reason: 'ECONNREFUSED', path: url.href });
    });

    it('reports a request the transport itself aborted as an AbortError', async () => {
        const { transport } = fakeTransport({
            requestError: Object.assign(new Error('The operation was aborted'), {
                name: 'AbortError'
            })
        });

        await expect(fetchRemoteFile(url, { ...base, transport })).rejects.toBeInstanceOf(
            AbortError
        );
    });

    it('reports a transport that throws while opening the request as a network error', async () => {
        const { transport } = fakeTransport({ throwSync: new TypeError('bad option') });

        await expect(fetchRemoteFile(url, { ...base, transport })).rejects.toBeInstanceOf(
            NetworkError
        );
    });

    it('rejects without resolving or connecting when the signal is already aborted', async () => {
        const { transport, calls } = fakeTransport();
        const resolve = vi.fn<AddressResolver>();
        const controller = new AbortController();
        controller.abort();

        await expect(
            fetchRemoteFile(url, { ...base, signal: controller.signal, resolve, transport })
        ).rejects.toBeInstanceOf(AbortError);
        expect(resolve).not.toHaveBeenCalled();
        expect(calls).toHaveLength(0);
    });

    it('honors an abort that happens while the host is being resolved, with its reason', async () => {
        const { transport, calls } = fakeTransport();
        const controller = new AbortController();
        const resolve: AddressResolver = async () => {
            controller.abort(new Error('sandbox torn down'));
            return [{ address: PUBLIC_V4, family: 4 }];
        };

        const failure = fetchRemoteFile(url, {
            ...base,
            signal: controller.signal,
            resolve,
            transport
        });

        await expect(failure).rejects.toBeInstanceOf(AbortError);
        await expect(failure).rejects.toThrow(/sandbox torn down/);
        expect(calls).toHaveLength(0);
    });

    it('rejects at once when aborted during a resolution that does not answer', async () => {
        const { transport, calls } = fakeTransport();
        const controller = new AbortController();
        let resolverCalled = false;
        const resolve: AddressResolver = () => {
            resolverCalled = true;
            return new Promise(() => undefined);
        };

        const download = fetchRemoteFile(url, {
            ...base,
            signal: controller.signal,
            resolve,
            transport
        });
        await new Promise((r) => setImmediate(r));
        expect(resolverCalled).toBe(true);
        const timeout = new TimeoutError('timed out after 15000 ms', 15000);
        controller.abort(timeout);

        await expect(download).rejects.toBe(timeout);
        expect(calls).toHaveLength(0);
    });

    it('surfaces an SDK error used as the abort reason as that error', async () => {
        const { transport, delivered } = fakeTransport({
            chunks: [Buffer.from('partial')],
            hang: true
        });
        const controller = new AbortController();

        const download = fetchRemoteFile(url, { ...base, signal: controller.signal, transport });
        // Abort while the body is arriving: the response and its first chunk are delivered.
        await delivered;
        const timeout = new TimeoutError('timed out after 15000 ms', 15000);
        controller.abort(timeout);

        await expect(download).rejects.toBe(timeout);
    });

    it('aborts the download while the body is arriving', async () => {
        const { transport, requests, delivered } = fakeTransport({
            chunks: [Buffer.from('partial')],
            hang: true
        });
        const controller = new AbortController();

        const download = fetchRemoteFile(url, { ...base, signal: controller.signal, transport });
        await delivered;
        controller.abort();

        await expect(download).rejects.toBeInstanceOf(AbortError);
        expect(requests[0].destroyed).toBe(true);
        // A late error from the destroyed request changes nothing and throws nowhere.
        expect(() => requests[0].emit('error', new Error('late'))).not.toThrow();
    });
});

/**
 * The same download through the real `node:http` / `node:https` request code. Only the agent's
 * connect step is replaced: it records what the socket would be opened to and refuses, so no
 * network is used. This pins that the connection goes to the checked addresses, in order, and
 * that nothing hands the host name to the connect step to be resolved again.
 */
describe('fetchRemoteFile with the default transport', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    /** What the agent's connect step receives. */
    interface ConnectOptions {
        host?: string;
        port?: unknown;
        servername?: string;
    }

    /** Replaces `createConnection` on an agent class with one that records and refuses. */
    function refuseConnections(agent: typeof HttpAgent | typeof HttpsAgent) {
        const connections: ConnectOptions[] = [];
        const target = agent.prototype as unknown as {
            createConnection: (options: ConnectOptions) => Socket;
        };
        vi.spyOn(target, 'createConnection').mockImplementation((options) => {
            connections.push({
                host: options.host,
                port: options.port,
                servername: options.servername
            });
            const socket = new Socket();
            process.nextTick(() =>
                socket.destroy(
                    Object.assign(new Error('connection refused by test'), {
                        code: 'ECONNREFUSED'
                    })
                )
            );
            return socket;
        });
        return connections;
    }

    it('connects over http to each checked address in turn, never to the name', async () => {
        const OTHER_V4 = '93.184.216.35';
        const connections = refuseConnections(HttpAgent);

        const failure = fetchRemoteFile('http://files.example:8080/logo.png', {
            ...base,
            resolve: resolvingTo(
                { address: PUBLIC_V4, family: 4 },
                { address: OTHER_V4, family: 4 }
            )
        });

        await expect(failure).rejects.toBeInstanceOf(NetworkError);
        await expect(failure).rejects.toMatchObject({ reason: 'ECONNREFUSED' });
        expect(connections.map((c) => c.host)).toEqual([PUBLIC_V4, OTHER_V4]);
        expect(connections.every((c) => isIP(c.host ?? '') !== 0)).toBe(true);
        expect(connections.every((c) => String(c.port) === '8080')).toBe(true);
    });

    it('connects over https to the checked address and verifies the certificate for the name', async () => {
        const connections = refuseConnections(HttpsAgent);

        await expect(fetchRemoteFile(url, { ...base })).rejects.toBeInstanceOf(NetworkError);

        expect(connections).toEqual([{ host: PUBLIC_V4, port: 443, servername: 'files.example' }]);
    });
});
