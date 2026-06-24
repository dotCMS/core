import { AbortError, HttpError, PolicyError, RuntimeError, ValidationError } from '../sandbox/errors';

/**
 * The single shared request core.
 *
 * "One adapter, one auth path, one allow-list" is only true if BOTH verbs of the runtime
 * route through one function that owns *all* of: path/method validation, the policy check,
 * auth injection, `fetch`, response decoding (incl. the binary envelope), the error model,
 * and abort/timeout. `runtime.request()` calls `requestCore` directly; the sandbox's
 * `api.request` is a thin worker→host bridge to this same function. Neither verb may add
 * behavior the other lacks — that is the contract, not an aspiration.
 */

interface FileFieldDescriptor {
    name: string; // filename, e.g. "logo.png"
    type: string; // MIME type, e.g. "image/png"
    data?: string; // base64-encoded content (mutually exclusive with url)
    url?: string; // URL to fetch content from (mutually exclusive with data)
}

type FormDataFieldValue = string | FileFieldDescriptor;

export interface RequestOptions {
    method?: string;
    path: string;
    query?: Record<string, string | number | boolean>;
    body?: unknown;
    formData?: Record<string, FormDataFieldValue>;
    headers?: Record<string, string>;
    // How to decode the response body. Defaults to content-type auto-detection:
    // JSON content types are parsed; textual types come back as strings; everything
    // else (images, fonts, etc.) comes back as a base64 binary envelope so the bytes
    // survive the JSON.stringify boundary in the consuming sandbox. Set 'base64' to
    // force the binary path regardless of the declared content-type.
    responseType?: 'auto' | 'base64';
}

/**
 * A policy hook consulted before a request reaches the wire. Return `false` (or throw) to
 * reject. The single place a caller-owned allow-list plugs in — both verbs honor it because
 * both flow through `requestCore`.
 */
export type RequestPolicy = (req: { method: string; path: string }) => boolean | void;

/** Host-side context the request core needs. Auth lives here, never in the caller's code. */
export interface RequestCoreContext {
    baseUrl: string;
    authToken: string;
    /** Optional policy/allow-list consulted before every call. */
    policy?: RequestPolicy;
    /** Aborts the in-flight fetch when the surrounding execution (e.g. sandbox) is torn down. */
    signal?: AbortSignal;
    /** Observability hook — fired around each call with redacted metadata (never the token). */
    onCall?: (event: RequestCallEvent) => void;
}

export interface RequestCallEvent {
    method: string;
    path: string;
    status?: number;
    durationMs: number;
    ok: boolean;
    errorCode?: string;
}

function isFileDescriptor(value: unknown): value is FileFieldDescriptor {
    const obj = value as Record<string, unknown>;
    return (
        typeof value === 'object' &&
        value !== null &&
        typeof obj.name === 'string' &&
        typeof obj.type === 'string' &&
        (typeof obj.data === 'string' || typeof obj.url === 'string')
    );
}

// Max size (bytes) for a remote file fetched via a `url` descriptor — guards
// against memory exhaustion from an attacker-controlled endpoint.
const MAX_REMOTE_FILE_BYTES = 25 * 1024 * 1024; // 25 MB
// Timeout (ms) for the remote fetch, so a slow/hanging URL cannot stall the host.
const REMOTE_FILE_FETCH_TIMEOUT_MS = 15000;

// Max size (bytes) for a binary response body returned as a base64 envelope.
// base64 inflates the payload ~33% and the whole thing flows through
// JSON.stringify in the consuming sandbox, so large assets can blow up memory
// and model context — cap it like the upload side already does.
const MAX_BINARY_RESPONSE_BYTES = 25 * 1024 * 1024; // 25 MB

/**
 * Tagged envelope returned for non-textual response bodies. The raw bytes are
 * base64-encoded so they survive the `JSON.stringify` serialization boundary in
 * the consuming sandbox intact — `response.text()` would corrupt any non-UTF-8 byte
 * into the U+FFFD replacement char. Consumers detect `__dotcmsBinary` and decode.
 */
export interface BinaryResponseEnvelope {
    __dotcmsBinary: true;
    contentType: string;
    base64: string;
    byteLength: number;
}

/**
 * Type guard for the binary response envelope. Consumers can use this to detect
 * a binary body and `Buffer.from(envelope.base64, 'base64')` to recover the bytes.
 */
export function isBinaryResponseEnvelope(value: unknown): value is BinaryResponseEnvelope {
    if (typeof value !== 'object' || value === null) {
        return false;
    }
    const obj = value as Record<string, unknown>;
    return (
        obj.__dotcmsBinary === true &&
        typeof obj.base64 === 'string' &&
        typeof obj.contentType === 'string' &&
        typeof obj.byteLength === 'number'
    );
}

/**
 * Decide whether a content-type should be decoded as text. Everything that is
 * not JSON (handled separately) and not in this textual set is treated as
 * binary and returned as a base64 envelope.
 */
function isTextualContentType(contentType: string): boolean {
    const ct = contentType.toLowerCase();
    return (
        ct.startsWith('text/') ||
        ct.includes('application/xml') ||
        ct.includes('application/javascript') ||
        ct.includes('application/x-www-form-urlencoded') ||
        ct.includes('+json') ||
        ct.includes('+xml')
    );
}

/**
 * Read a response body as a base64 binary envelope, enforcing the size cap.
 */
async function readBinaryResponse(
    response: Response,
    contentType: string
): Promise<BinaryResponseEnvelope> {
    // Reject early via Content-Length so we never buffer an oversized body into
    // memory. The header can be absent or lie, so the post-read check below stays
    // as the authoritative backstop.
    const declaredLength = Number(response.headers.get('content-length'));
    if (Number.isFinite(declaredLength) && declaredLength > MAX_BINARY_RESPONSE_BYTES) {
        throw new ValidationError(
            `Binary response (${declaredLength} bytes) exceeds the ${MAX_BINARY_RESPONSE_BYTES}-byte limit`
        );
    }
    const buffer = await response.arrayBuffer();
    if (buffer.byteLength > MAX_BINARY_RESPONSE_BYTES) {
        throw new ValidationError(
            `Binary response (${buffer.byteLength} bytes) exceeds the ${MAX_BINARY_RESPONSE_BYTES}-byte limit`
        );
    }
    return {
        __dotcmsBinary: true,
        contentType,
        base64: Buffer.from(buffer).toString('base64'),
        byteLength: buffer.byteLength
    };
}

/**
 * Validates a user-supplied file URL before fetching it, to mitigate SSRF.
 * Sandbox code can put any string in `desc.url`, and the fetch runs on the
 * host with host network access — so we restrict it to public http(s) targets
 * and reject loopback, link-local, and private (RFC 1918 / unique-local) hosts.
 *
 * KNOWN LIMITATION (DNS rebinding): this validates the literal hostname/IP in the URL, not
 * the address it ultimately resolves to. A hostname that resolves to a private/link-local/
 * metadata IP at fetch time bypasses this check. `redirect: 'error'` blocks the redirect
 * vector, but not rebinding. This matches the package threat model — capability confinement
 * for trusted code generators, not adversarial isolation. A consumer accepting genuinely
 * untrusted file URLs must front this with a hardened fetcher that resolves and pins the IP.
 */
function assertSafeRemoteUrl(rawUrl: string): URL {
    let parsed: URL;
    try {
        parsed = new URL(rawUrl);
    } catch {
        throw new ValidationError(`Invalid file URL: "${rawUrl}"`);
    }

    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
        throw new ValidationError(`File URL must use http(s); got "${parsed.protocol}"`);
    }

    const host = parsed.hostname.toLowerCase().replace(/^\[|\]$/g, '');

    // Block obvious metadata / loopback hostnames.
    if (
        host === 'localhost' ||
        host.endsWith('.localhost') ||
        host === 'metadata.google.internal'
    ) {
        throw new ValidationError(`File URL host "${host}" is not allowed`);
    }

    // IPv4 private / loopback / link-local / unspecified ranges.
    const ipv4 = host.match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/);
    if (ipv4) {
        const [a, b] = ipv4.slice(1).map(Number);
        const isPrivate =
            a === 0 || // 0.0.0.0/8 (unspecified)
            a === 10 || // 10.0.0.0/8
            a === 127 || // 127.0.0.0/8 (loopback)
            (a === 169 && b === 254) || // 169.254.0.0/16 (link-local, incl. cloud metadata)
            (a === 172 && b >= 16 && b <= 31) || // 172.16.0.0/12
            (a === 192 && b === 168); // 192.168.0.0/16
        if (isPrivate) {
            throw new ValidationError(`File URL resolves to a private/loopback address: "${host}"`);
        }
    }

    // IPv6 loopback (::1), unspecified (::) and unique-local (fc00::/7) / link-local (fe80::/10).
    if (host.includes(':')) {
        if (host === '::1' || host === '::' || /^f[cd]/.test(host) || /^fe[89ab]/.test(host)) {
            throw new ValidationError(
                `File URL resolves to a private/loopback IPv6 address: "${host}"`
            );
        }
    }

    return parsed;
}

async function resolveFileDescriptor(
    desc: FileFieldDescriptor,
    signal?: AbortSignal
): Promise<Blob> {
    if (desc.data) {
        const binary = Buffer.from(desc.data, 'base64');
        return new Blob([new Uint8Array(binary)], { type: desc.type });
    }
    if (desc.url) {
        const safeUrl = assertSafeRemoteUrl(desc.url);
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), REMOTE_FILE_FETCH_TIMEOUT_MS);
        // If the surrounding execution is aborted, also abort this nested fetch.
        const onParentAbort = () => controller.abort();
        signal?.addEventListener('abort', onParentAbort, { once: true });
        try {
            const response = await fetch(safeUrl.toString(), {
                signal: controller.signal,
                redirect: 'error' // a redirect could escape the SSRF guard
            });
            if (!response.ok) {
                throw new HttpError(
                    response.status,
                    response.statusText,
                    `Failed to fetch file from "${desc.url}"`
                );
            }
            const buffer = await response.arrayBuffer();
            if (buffer.byteLength > MAX_REMOTE_FILE_BYTES) {
                throw new ValidationError(
                    `Remote file "${desc.url}" exceeds the ${MAX_REMOTE_FILE_BYTES}-byte limit`
                );
            }
            return new Blob([buffer], { type: desc.type });
        } finally {
            clearTimeout(timer);
            signal?.removeEventListener('abort', onParentAbort);
        }
    }
    throw new ValidationError(`File descriptor "${desc.name}" must have either "data" (base64) or "url"`);
}

/**
 * Execute one authenticated request against dotCMS. This is the function both verbs share.
 * Auth is injected here, on the host side; the executing code never sees the token.
 */
export async function requestCore(
    options: RequestOptions,
    ctx: RequestCoreContext
): Promise<unknown> {
    const startedAt = performance.now();
    const method = (options.method || 'GET').toUpperCase();
    const urlPath = options.path || '/';

    const emit = (status: number | undefined, ok: boolean, errorCode?: string) => {
        ctx.onCall?.({
            method,
            path: urlPath,
            status,
            ok,
            errorCode,
            durationMs: performance.now() - startedAt
        });
    };

    try {
        if (!ctx.baseUrl) {
            throw new RuntimeError('dotcmsUrl is required');
        }
        if (!ctx.authToken) {
            throw new RuntimeError('authToken is required');
        }

        // Validate that the path is a relative path and cannot override the base URL.
        if (!urlPath.startsWith('/')) {
            throw new ValidationError("options.path must be a relative path starting with '/'");
        }
        // Explicitly reject protocol-relative URLs like "//attacker.example/path".
        if (urlPath.startsWith('//')) {
            throw new ValidationError('options.path must not be a protocol-relative URL');
        }
        // Reject values that look like they start with a URL scheme (e.g. "http:", "https:").
        if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(urlPath)) {
            throw new ValidationError('options.path must not be an absolute URL');
        }

        // Policy / allow-list check — before anything touches the wire.
        if (ctx.policy) {
            let allowed: boolean | void;
            try {
                allowed = ctx.policy({ method, path: urlPath });
            } catch (err) {
                throw new PolicyError(
                    err instanceof Error ? err.message : String(err),
                    method,
                    urlPath
                );
            }
            if (allowed === false) {
                throw new PolicyError(
                    `Request ${method} ${urlPath} rejected by policy`,
                    method,
                    urlPath
                );
            }
        }

        // Already aborted before we even start?
        if (ctx.signal?.aborted) {
            throw new AbortError(`Request ${method} ${urlPath} was aborted before it started`);
        }

        // Build URL with query params.
        const url = new URL(urlPath, ctx.baseUrl);
        if (options.query) {
            for (const [key, value] of Object.entries(options.query)) {
                url.searchParams.set(key, String(value));
            }
        }

        // Build headers — auth token injected here, never in sandbox.
        const headers: Record<string, string> = {
            Accept: 'application/json, */*;q=0.1',
            Origin: new URL(ctx.baseUrl).origin,
            ...options.headers,
            Authorization: `Bearer ${ctx.authToken}` // always last — cannot be overridden
        };

        const fetchOptions: RequestInit = { method, headers, signal: ctx.signal };

        if (options.formData && options.body) {
            throw new ValidationError("Cannot specify both 'body' and 'formData'");
        }

        if (options.formData && method !== 'GET' && method !== 'HEAD') {
            const form = new FormData();
            for (const [fieldName, fieldValue] of Object.entries(options.formData)) {
                if (typeof fieldValue === 'string') {
                    form.append(fieldName, fieldValue);
                } else if (isFileDescriptor(fieldValue)) {
                    const blob = await resolveFileDescriptor(fieldValue, ctx.signal);
                    form.append(fieldName, blob, fieldValue.name);
                } else {
                    throw new ValidationError(
                        `Invalid formData field "${fieldName}": must be a string or { name, type, data|url }`
                    );
                }
            }
            // Do NOT set Content-Type — fetch() auto-generates it with the multipart boundary.
            delete headers['Content-Type'];
            fetchOptions.body = form;
        } else if (options.body && method !== 'GET' && method !== 'HEAD') {
            headers['Content-Type'] = 'application/json';
            fetchOptions.body = JSON.stringify(options.body);
        }

        const response = await fetch(url.toString(), fetchOptions);

        const contentType = response.headers.get('content-type') || '';

        // On error, always read the body as text regardless of the declared
        // content-type — dotCMS errors come back as HTML/text and we want a
        // readable message, not a base64 envelope of the error page.
        if (!response.ok) {
            const errorBody = await response.text();
            throw new HttpError(response.status, response.statusText, errorBody);
        }

        const forceBinary = options.responseType === 'base64';

        let result: unknown;
        if (!forceBinary && contentType.includes('application/json')) {
            result = await response.json();
        } else if (!forceBinary && isTextualContentType(contentType)) {
            result = await response.text();
        } else {
            // Non-JSON, non-textual (or explicitly requested): return a base64
            // envelope so the raw bytes survive JSON.stringify intact.
            result = await readBinaryResponse(response, contentType);
        }

        emit(response.status, true);
        return result;
    } catch (err) {
        // Normalize fetch's AbortError (a DOMException with name "AbortError") into our typed one.
        if (err && typeof err === 'object' && (err as { name?: string }).name === 'AbortError') {
            const aborted = new AbortError(`Request ${method} ${urlPath} was aborted`);
            emit(undefined, false, aborted.code);
            throw aborted;
        }
        const code = (err as { code?: string }).code;
        const status = (err as HttpError).status;
        emit(typeof status === 'number' ? status : undefined, false, code);
        throw err;
    }
}
