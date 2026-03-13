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

async function resolveFileDescriptor(desc: FileFieldDescriptor): Promise<Blob> {
    if (desc.data) {
        const binary = Buffer.from(desc.data, 'base64');
        return new Blob([binary], { type: desc.type });
    }
    if (desc.url) {
        const response = await fetch(desc.url);
        if (!response.ok) {
            throw new Error(`Failed to fetch file from "${desc.url}": ${response.status}`);
        }
        return new Blob([await response.arrayBuffer()], { type: desc.type });
    }
    throw new Error(`File descriptor "${desc.name}" must have either "data" (base64) or "url"`);
}

/**
 * Create the "api" adapter for making authenticated HTTP calls to dotCMS.
 * Auth tokens are injected by the main thread — never exposed to the sandbox.
 */
export function createApiAdapter(): Adapter {
    const baseUrl = process.env.DOTCMS_URL;
    const apiToken = process.env.AUTH_TOKEN;

    if (!baseUrl) {
        throw new Error('DOTCMS_URL environment variable is required');
    }
    if (!apiToken) {
        throw new Error('AUTH_TOKEN environment variable is required');
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
                throw new Error("options.path must not be a protocol-relative URL");
            }
            // Reject values that look like they start with a URL scheme (e.g. "http:", "https:")
            if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(urlPath)) {
                throw new Error("options.path must not be an absolute URL");
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

            // Parse response
            const contentType = response.headers.get('content-type') || '';
            let data: unknown;

            if (contentType.includes('application/json')) {
                data = await response.json();
            } else {
                data = await response.text();
            }

            if (!response.ok) {
                throw new Error(
                    `HTTP ${response.status} ${response.statusText}: ${typeof data === 'string' ? data : JSON.stringify(data)}`
                );
            }

            return data;
        }
    };

    return {
        name: 'api',
        description: 'Authenticated HTTP client for dotCMS REST API',
        version: '1.0.0',
        methods: new Map([['request', requestMethod]])
    };
}
