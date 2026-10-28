import { lookup as dnsLookup } from 'node:dns/promises';
import { request as httpRequest, type ClientRequest, type IncomingMessage } from 'node:http';
import { request as httpsRequest, type RequestOptions as HttpsRequestOptions } from 'node:https';
import { BlockList, isIP, type Socket } from 'node:net';
import { createBrotliDecompress, createGunzip, createInflate } from 'node:zlib';

import {
    AbortError,
    HttpError,
    NetworkError,
    ValidationError,
    isDotCMSError
} from '../sandbox/errors';

import type { Readable } from 'node:stream';

/**
 * Downloading a remote file for a `formData` field (`{ name, type, url }`).
 *
 * Sandbox code can put any string in `url`, and the download runs on the host with the host's
 * network access, so only public http(s) targets are accepted. The decision is made on the
 * addresses the host actually resolves to, not on the URL text: the host name is resolved
 * first, every resolved address must be public, and the connection is then opened to one of
 * those checked addresses directly, with the host name kept for TLS verification and the
 * `Host` header. Redirects are not followed.
 */

/** One resolved address, as `dns.lookup(host, { all: true })` returns it. */
export interface ResolvedAddress {
    address: string;
    family: 4 | 6;
}

/** Resolves a host name to every address it has. Replaceable for tests. */
export type AddressResolver = (hostname: string) => Promise<ResolvedAddress[]>;

/**
 * Opens the HTTP(S) request. Defaults to `node:http` / `node:https`. Replaceable for tests.
 * `options` carries the address to connect to (`hostname`, `port`) and the name for TLS
 * (`servername`, meaningful to `node:https` only) and the `Host` header.
 */
export type RequestTransport = (
    url: URL,
    options: HttpsRequestOptions,
    onResponse: (response: IncomingMessage) => void
) => ClientRequest;

export interface RemoteFileOptions {
    /** MIME type given to the returned Blob. */
    type: string;
    /** Largest body accepted, in bytes (inclusive), after any content decoding. */
    maxBytes: number;
    /**
     * Cancels the download; the request is destroyed and an `AbortError` is thrown, or the
     * signal's own reason when it is an SDK error (a `TimeoutError`, for instance).
     */
    signal?: AbortSignal;
    resolve?: AddressResolver;
    transport?: RequestTransport;
    /**
     * How long one resolved address may take to accept the connection before the next one is
     * tried (default 3000 ms). The last address has no such limit: `signal` bounds it.
     */
    connectAttemptMs?: number;
}

/** What the download identifies itself as; `fetch` used to send a user agent too. */
const REQUEST_HEADERS = { accept: '*/*', 'user-agent': '@dotcms/ai' } as const;

/**
 * Address ranges that are never a valid download source: unspecified, loopback, link-local,
 * private and shared-address space, benchmarking, documentation, multicast and reserved IPv4;
 * and their IPv6 counterparts, including every notation that embeds an IPv4 address.
 * `BlockList` applies the IPv4 rules to IPv4-mapped addresses (`::ffff:a.b.c.d`) itself;
 * IPv4-compatible (`::a.b.c.d`), 6to4 and NAT64 forms are listed explicitly.
 */
const NON_PUBLIC = new BlockList();
const NON_PUBLIC_IPV4: ReadonlyArray<[string, number]> = [
    ['0.0.0.0', 8],
    ['10.0.0.0', 8],
    ['100.64.0.0', 10],
    ['127.0.0.0', 8],
    ['169.254.0.0', 16],
    ['172.16.0.0', 12],
    ['192.0.0.0', 24],
    ['192.0.2.0', 24],
    ['192.88.99.0', 24],
    ['192.168.0.0', 16],
    ['198.18.0.0', 15],
    ['198.51.100.0', 24],
    ['203.0.113.0', 24],
    ['224.0.0.0', 4],
    ['240.0.0.0', 4]
];
const NON_PUBLIC_IPV6: ReadonlyArray<[string, number]> = [
    ['::', 96], // unspecified, loopback and IPv4-compatible
    ['64:ff9b::', 96], // NAT64
    ['64:ff9b:1::', 48], // local-use NAT64
    ['2001:db8::', 32], // documentation
    ['2002::', 16], // 6to4
    ['fc00::', 7], // unique-local
    ['fe80::', 10], // link-local
    ['fec0::', 10], // site-local
    ['ff00::', 8] // multicast
];
NON_PUBLIC_IPV4.forEach(([network, prefix]) => NON_PUBLIC.addSubnet(network, prefix, 'ipv4'));
NON_PUBLIC_IPV6.forEach(([network, prefix]) => NON_PUBLIC.addSubnet(network, prefix, 'ipv6'));

/**
 * Whether an IP address (v4 or v6, in any textual form `net.isIP` accepts) falls in a
 * non-public range. Anything that is not an IP address returns `false`: host names are
 * judged after resolution, by their addresses.
 */
export function isBlockedAddress(address: string): boolean {
    const family = isIP(address);
    if (family === 0) {
        return false;
    }
    return NON_PUBLIC.check(address, family === 4 ? 'ipv4' : 'ipv6');
}

/** The host name of a URL without the brackets an IPv6 literal carries or a trailing dot. */
function bareHostname(url: URL): string {
    return url.hostname
        .toLowerCase()
        .replace(/^\[|\]$/g, '')
        .replace(/\.$/, '');
}

/**
 * Validates the text of a file URL before anything is resolved: it must be http(s) without
 * credentials, must not name a local host, and when the host is an IP literal that address must
 * be public. A host name passes here and is judged by its resolved addresses in
 * `fetchRemoteFile`, which runs this check itself.
 */
export function assertSafeRemoteUrl(rawUrl: string): URL {
    let parsed: URL;
    try {
        parsed = new URL(rawUrl);
    } catch {
        throw new ValidationError(`Invalid file URL: "${rawUrl}"`);
    }

    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
        throw new ValidationError(`File URL must use http(s); got "${parsed.protocol}"`);
    }

    if (parsed.username || parsed.password) {
        throw new ValidationError('File URL must not carry credentials');
    }

    const host = bareHostname(parsed);

    if (
        host === 'localhost' ||
        host.endsWith('.localhost') ||
        host === 'metadata.google.internal'
    ) {
        throw new ValidationError(`File URL host "${host}" is not allowed`);
    }

    if (isBlockedAddress(host)) {
        throw new ValidationError(`File URL host "${host}" is not a public address`);
    }

    return parsed;
}

const defaultResolver: AddressResolver = async (hostname) =>
    (await dnsLookup(hostname, { all: true })).map(({ address, family }) => ({
        address,
        family: family === 6 ? 6 : 4
    }));

/**
 * Every address of the URL's host, all of them public IP addresses, or a `ValidationError`.
 * An IP literal is its own single address. A resolver answer that is not an IP address counts
 * as non-public: nothing is connected to on trust.
 */
async function resolvePublicAddresses(
    url: URL,
    resolve: AddressResolver
): Promise<ResolvedAddress[]> {
    const hostname = bareHostname(url);
    const literal = isIP(hostname);
    let addresses: ResolvedAddress[];
    if (literal) {
        addresses = [{ address: hostname, family: literal === 6 ? 6 : 4 }];
    } else {
        try {
            addresses = await resolve(hostname);
        } catch (err) {
            throw new NetworkError('GET', url.href, err, `the file host "${hostname}"`);
        }
    }
    if (addresses.length === 0) {
        throw new NetworkError(
            'GET',
            url.href,
            new Error('no address'),
            `the file host "${hostname}"`
        );
    }
    if (addresses.some(({ address }) => isIP(address) === 0 || isBlockedAddress(address))) {
        throw new ValidationError(
            `File URL host "${hostname}" resolves to an address that is not public`
        );
    }
    return addresses;
}

/** The error for an aborted `signal`: its own reason when that is an SDK error, else an `AbortError`. */
function abortedDownload(url: URL, signal?: AbortSignal): Error {
    const reason = signal?.reason;
    if (isDotCMSError(reason)) {
        return reason;
    }
    const why = reason instanceof Error ? `: ${reason.message}` : '';
    return new AbortError(`Download of "${url.href}" was aborted${why}`);
}

/** Maps a transport failure to the SDK error model; SDK errors pass through unchanged. */
function toRemoteFileError(err: unknown, url: URL, signal?: AbortSignal): Error {
    if (isDotCMSError(err)) {
        return err;
    }
    if ((err as { name?: string })?.name === 'AbortError') {
        return abortedDownload(url, signal);
    }
    return new NetworkError('GET', url.href, err, `the file host "${url.hostname}"`);
}

/**
 * The body stream to read for a response, decoding the content encoding the server applied
 * (`fetch` did this transparently). `null` when the encoding is not one that can be decoded.
 */
function decodedBody(res: IncomingMessage): Readable | null {
    const encoding = String(res.headers['content-encoding'] ?? 'identity')
        .trim()
        .toLowerCase();
    switch (encoding) {
        case '':
        case 'identity':
            return res;
        case 'gzip':
        case 'x-gzip':
            return res.pipe(createGunzip());
        case 'deflate':
            return res.pipe(createInflate());
        case 'br':
            return res.pipe(createBrotliDecompress());
        default:
            return null;
    }
}

/**
 * `work`, or the aborted-download error as soon as `signal` aborts, whichever comes first. The
 * resolver cannot be cancelled, so a late answer is ignored rather than waited for.
 */
function untilAborted<T>(work: Promise<T>, url: URL, signal?: AbortSignal): Promise<T> {
    if (!signal) {
        return work;
    }
    if (signal.aborted) {
        work.catch(() => undefined);
        return Promise.reject(abortedDownload(url, signal));
    }
    return new Promise<T>((resolve, reject) => {
        const onAbort = () => reject(abortedDownload(url, signal));
        signal.addEventListener('abort', onAbort, { once: true });
        work.then(
            (value) => {
                signal.removeEventListener('abort', onAbort);
                resolve(value);
            },
            (err: unknown) => {
                signal.removeEventListener('abort', onAbort);
                reject(err);
            }
        );
    });
}

/** How long one address may take to accept the connection while others remain to be tried. */
const CONNECT_ATTEMPT_MS = 3000;

/**
 * Downloads a file URL into a Blob of `options.type`. The URL text is validated
 * (`assertSafeRemoteUrl`), the host is resolved, every address is checked, and the request is
 * opened to a checked address with the host name kept for TLS and the `Host` header, so no
 * second resolution can change where the connection goes. The checked addresses are tried in
 * order: when one refuses the connection, cannot be reached, or does not accept it within
 * `connectAttemptMs`, the next one is tried; once a response has started, there is no retry.
 * Any status outside 2xx is an `HttpError` (redirects included: they are not followed); a body
 * over `maxBytes`, declared or received, is a `ValidationError`; a failed resolution, a failed
 * connection to every address, or a response cut short, is a `NetworkError`; an aborted
 * signal, at any point including while the host is being resolved, is an `AbortError` or the
 * signal's own SDK error.
 */
export async function fetchRemoteFile(
    rawUrl: string | URL,
    options: RemoteFileOptions
): Promise<Blob> {
    const url = assertSafeRemoteUrl(String(rawUrl));
    const { signal } = options;
    if (signal?.aborted) {
        throw abortedDownload(url, signal);
    }
    const addresses = await untilAborted(
        resolvePublicAddresses(url, options.resolve ?? defaultResolver),
        url,
        signal
    );
    const secure = url.protocol === 'https:';
    const send = options.transport ?? (secure ? httpsRequest : httpRequest);
    const hostname = bareHostname(url);
    const connectAttemptMs = options.connectAttemptMs ?? CONNECT_ATTEMPT_MS;
    // `hostname` (not `host`) is what the socket connects to when a URL is also given: the
    // URL's own host name would otherwise win and be resolved a second time.
    const optionsFor = (address: string): HttpsRequestOptions => ({
        method: 'GET',
        hostname: address,
        port: url.port || (secure ? 443 : 80),
        // TLS verifies the certificate against the name, never against the address.
        ...(isIP(hostname) ? {} : { servername: hostname }),
        headers: { ...REQUEST_HEADERS, host: url.host }
    });

    return new Promise<Blob>((resolve, reject) => {
        let settled = false;
        let req: ClientRequest | undefined;
        let connectTimer: ReturnType<typeof setTimeout> | undefined;
        const stopConnectTimer = () => {
            clearTimeout(connectTimer);
            connectTimer = undefined;
        };
        const fail = (err: unknown) => {
            if (settled) return;
            settled = true;
            stopConnectTimer();
            signal?.removeEventListener('abort', onAbort);
            reject(toRemoteFileError(err, url, signal));
            req?.destroy();
        };
        const onAbort = () => fail(abortedDownload(url, signal));

        const onResponse = (res: IncomingMessage) => {
            stopConnectTimer();
            const status = res.statusCode ?? 0;
            if (status < 200 || status >= 300) {
                const body =
                    status >= 300 && status < 400
                        ? `Redirects are not followed when fetching "${url.href}"`
                        : `Failed to fetch file from "${url.href}"`;
                fail(new HttpError(status, res.statusMessage ?? '', body));
                return;
            }
            // Refuse early on the declared size; the count below stays the backstop.
            const declaredLength = Number(res.headers['content-length']);
            if (Number.isFinite(declaredLength) && declaredLength > options.maxBytes) {
                fail(
                    new ValidationError(
                        `Remote file "${url.href}" (${declaredLength} bytes) exceeds the ${options.maxBytes}-byte limit`
                    )
                );
                return;
            }
            const body = decodedBody(res);
            if (!body) {
                fail(
                    new ValidationError(
                        `Remote file "${url.href}" uses an unsupported content encoding "${String(res.headers['content-encoding'])}"`
                    )
                );
                return;
            }
            const chunks: Buffer[] = [];
            let received = 0;
            body.on('data', (chunk: Buffer) => {
                if (settled) return;
                received += chunk.length;
                if (received > options.maxBytes) {
                    fail(
                        new ValidationError(
                            `Remote file "${url.href}" exceeds the ${options.maxBytes}-byte limit`
                        )
                    );
                    return;
                }
                chunks.push(chunk);
            });
            body.on('end', () => {
                if (settled) return;
                settled = true;
                signal?.removeEventListener('abort', onAbort);
                resolve(new Blob([Buffer.concat(chunks)], { type: options.type }));
            });
            body.on('error', fail);
            res.on('close', () => {
                if (!res.complete) {
                    fail(new Error('response ended before the body was complete'));
                }
            });
            res.on('error', fail);
        };

        /** Opens the request to `addresses[index]`, moving on to the next one if it cannot connect. */
        const attempt = (index: number) => {
            const { address } = addresses[index];
            const last = index === addresses.length - 1;
            let responded = false;
            let current: ClientRequest;
            try {
                current = send(url, optionsFor(address), (res) => {
                    if (settled || req !== current) {
                        res.resume();
                        return;
                    }
                    responded = true;
                    onResponse(res);
                });
            } catch (err) {
                fail(err);
                return;
            }
            req = current;
            current.on('error', (err) => {
                if (settled || req !== current) return;
                stopConnectTimer();
                if (!responded && !last) {
                    // Detach before destroying, so the abandoned request's own errors are ignored.
                    req = undefined;
                    current.destroy();
                    attempt(index + 1);
                    return;
                }
                fail(err);
            });
            if (!last) {
                connectTimer = setTimeout(
                    () =>
                        current.destroy(
                            Object.assign(new Error(`connection to ${address} timed out`), {
                                code: 'ETIMEDOUT'
                            })
                        ),
                    connectAttemptMs
                );
                current.once('socket', (socket: Socket) => {
                    if (socket.connecting) {
                        socket.once('connect', stopConnectTimer);
                    } else {
                        stopConnectTimer();
                    }
                });
            }
            current.end();
        };

        signal?.addEventListener('abort', onAbort, { once: true });
        attempt(0);
    });
}
