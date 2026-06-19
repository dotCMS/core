import type { Adapter, AdapterMethod } from './types';

interface FileFieldDescriptor {
    name: string; // filename, e.g. "logo.png"
    type: string; // MIME type, e.g. "image/png"
    data?: string; // base64-encoded content (mutually exclusive with url)
    url?: string; // URL to fetch content from (mutually exclusive with data)
}

type FormDataFieldValue = string | FileFieldDescriptor;

interface RequestOptions {
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
 * `execute.ts` intact — `response.text()` would corrupt any non-UTF-8 byte into
 * the U+FFFD replacement char. Consumers detect `__dotcmsBinary` and decode.
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
    return (
        typeof value === 'object' &&
        value !== null &&
        (value as Record<string, unknown>).__dotcmsBinary === true &&
        typeof (value as Record<string, unknown>).base64 === 'string'
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
    const buffer = await response.arrayBuffer();
    if (buffer.byteLength > MAX_BINARY_RESPONSE_BYTES) {
        throw new Error(
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
 */
function assertSafeRemoteUrl(rawUrl: string): URL {
    let parsed: URL;
    try {
        parsed = new URL(rawUrl);
    } catch {
        throw new Error(`Invalid file URL: "${rawUrl}"`);
    }

    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') {
        throw new Error(`File URL must use http(s); got "${parsed.protocol}"`);
    }

    const host = parsed.hostname.toLowerCase().replace(/^\[|\]$/g, '');

    // Block obvious metadata / loopback hostnames.
    if (
        host === 'localhost' ||
        host.endsWith('.localhost') ||
        host === 'metadata.google.internal'
    ) {
        throw new Error(`File URL host "${host}" is not allowed`);
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
            throw new Error(`File URL resolves to a private/loopback address: "${host}"`);
        }
    }

    // IPv6 loopback (::1), unspecified (::) and unique-local (fc00::/7) / link-local (fe80::/10).
    if (host.includes(':')) {
        if (host === '::1' || host === '::' || /^f[cd]/.test(host) || /^fe[89ab]/.test(host)) {
            throw new Error(`File URL resolves to a private/loopback IPv6 address: "${host}"`);
        }
    }

    return parsed;
}

async function resolveFileDescriptor(desc: FileFieldDescriptor): Promise<Blob> {
    if (desc.data) {
        const binary = Buffer.from(desc.data, 'base64');
        return new Blob([new Uint8Array(binary)], { type: desc.type });
    }
    if (desc.url) {
        const safeUrl = assertSafeRemoteUrl(desc.url);
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), REMOTE_FILE_FETCH_TIMEOUT_MS);
        try {
            const response = await fetch(safeUrl.toString(), {
                signal: controller.signal,
                redirect: 'error' // a redirect could escape the SSRF guard
            });
            if (!response.ok) {
                throw new Error(`Failed to fetch file from "${desc.url}": ${response.status}`);
            }
            const buffer = await response.arrayBuffer();
            if (buffer.byteLength > MAX_REMOTE_FILE_BYTES) {
                throw new Error(
                    `Remote file "${desc.url}" exceeds the ${MAX_REMOTE_FILE_BYTES}-byte limit`
                );
            }
            return new Blob([buffer], { type: desc.type });
        } finally {
            clearTimeout(timer);
        }
    }
    throw new Error(`File descriptor "${desc.name}" must have either "data" (base64) or "url"`);
}

export interface ApiAdapterConfig {
    dotcmsUrl: string;
    authToken: string;
}

/**
 * Create the "api" adapter for making authenticated HTTP calls to dotCMS.
 * Auth tokens are injected by the main thread — never exposed to the sandbox.
 */
export function createApiAdapter(config: ApiAdapterConfig): Adapter {
    const baseUrl = config.dotcmsUrl;
    const apiToken = config.authToken;

    if (!baseUrl) {
        throw new Error('dotcmsUrl is required');
    }
    if (!apiToken) {
        throw new Error('authToken is required');
    }

    const requestMethod: AdapterMethod = {
        name: 'request',
        description: 'Make an authenticated HTTP request to the dotCMS API',
        parameters: [
            {
                name: 'options',
                type: 'object',
                description: 'Request options: { method, path, query, body, formData, headers }',
                required: true
            }
        ],
        async execute(...args: unknown[]): Promise<unknown> {
            const options = (args[0] || {}) as RequestOptions;
            const method = (options.method || 'GET').toUpperCase();
            const urlPath = options.path || '/';

            // Validate that the path is a relative path and cannot override the base URL
            if (!urlPath.startsWith('/')) {
                throw new Error("options.path must be a relative path starting with '/'");
            }
            // Explicitly reject protocol-relative URLs like "//attacker.example/path"
            if (urlPath.startsWith('//')) {
                throw new Error('options.path must not be a protocol-relative URL');
            }
            // Reject values that look like they start with a URL scheme (e.g. "http:", "https:")
            if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(urlPath)) {
                throw new Error('options.path must not be an absolute URL');
            }
            // Build URL with query params
            const url = new URL(urlPath, baseUrl);
            if (options.query) {
                for (const [key, value] of Object.entries(options.query)) {
                    url.searchParams.set(key, String(value));
                }
            }

            // Build headers — auth token injected here, never in sandbox
            const headers: Record<string, string> = {
                Accept: 'application/json, */*;q=0.1',
                Origin: new URL(baseUrl).origin,
                ...options.headers,
                Authorization: `Bearer ${apiToken}` // always last — cannot be overridden
            };

            // Build fetch options
            const fetchOptions: RequestInit = { method, headers };

            if (options.formData && options.body) {
                throw new Error("Cannot specify both 'body' and 'formData'");
            }

            if (options.formData && method !== 'GET' && method !== 'HEAD') {
                const form = new FormData();
                for (const [fieldName, fieldValue] of Object.entries(options.formData)) {
                    if (typeof fieldValue === 'string') {
                        form.append(fieldName, fieldValue);
                    } else if (isFileDescriptor(fieldValue)) {
                        const blob = await resolveFileDescriptor(fieldValue);
                        form.append(fieldName, blob, fieldValue.name);
                    } else {
                        throw new Error(
                            `Invalid formData field "${fieldName}": must be a string or { name, type, data|url }`
                        );
                    }
                }
                // Do NOT set Content-Type — fetch() auto-generates it with the multipart boundary
                delete headers['Content-Type'];
                fetchOptions.body = form;
            } else if (options.body && method !== 'GET' && method !== 'HEAD') {
                headers['Content-Type'] = 'application/json';
                fetchOptions.body = JSON.stringify(options.body);
            }

            const response = await fetch(url.toString(), fetchOptions);

            // Parse response.
            const contentType = response.headers.get('content-type') || '';

            // On error, always read the body as text regardless of the declared
            // content-type — dotCMS errors come back as HTML/text and we want a
            // readable message, not a base64 envelope of the error page.
            if (!response.ok) {
                const errorBody = await response.text();
                throw new Error(
                    `HTTP ${response.status} ${response.statusText}: ${errorBody}`
                );
            }

            const forceBinary = options.responseType === 'base64';

            if (!forceBinary && contentType.includes('application/json')) {
                return await response.json();
            }
            if (!forceBinary && isTextualContentType(contentType)) {
                return await response.text();
            }
            // Non-JSON, non-textual (or explicitly requested): return a base64
            // envelope so the raw bytes survive JSON.stringify intact.
            return await readBinaryResponse(response, contentType);
        }
    };

    return {
        name: 'api',
        description: 'Authenticated HTTP client for dotCMS REST API',
        version: '1.0.0',
        methods: new Map([['request', requestMethod]])
    };
}
