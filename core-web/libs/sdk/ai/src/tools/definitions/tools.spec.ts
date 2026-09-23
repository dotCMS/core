import { vi } from 'vitest';
import { z } from 'zod';

import { downloadAssetsTool } from './download-assets';
import { executeTool } from './execute';
import { pageCreateTool } from './page-create';
import { pagePlaceContentTool } from './page-place-content';
import { pageVerifyTool } from './page-verify';
import { searchTool } from './search';
import { uploadAssetsTool } from './upload-assets';

import { isToolFailure, type ToolFailure } from '../toolkit/tool-runtime';

import type { DotCMSTool } from '../toolkit/types';

/** Build a minimal JSON Response stub. */
function jsonResponse(body: unknown): Response {
    return {
        ok: true,
        status: 200,
        statusText: 'OK',
        headers: {
            get: (n: string) => (n.toLowerCase() === 'content-type' ? 'application/json' : null)
        },
        json: async () => body,
        text: async () => JSON.stringify(body)
    } as unknown as Response;
}

/** A render response for a page that came out with content. */
const RENDERED_PAGE = {
    entity: {
        page: { rendered: '<main>hello</main>', pageURI: '/about-us' },
        containers: {},
        layout: { body: { rows: [] } }
    }
};

/** A fetch that never answers on its own, only rejects when its signal aborts — a wedged instance. */
function hangingFetch(_url: string, init?: RequestInit): Promise<Response> {
    return new Promise((_resolve, reject) => {
        init?.signal?.addEventListener('abort', () => {
            reject(Object.assign(new Error('The operation was aborted'), { name: 'AbortError' }));
        });
    });
}

/** Narrow a tool result to the failure it is expected to be. */
function expectFailure(result: unknown): ToolFailure {
    expect(isToolFailure(result)).toBe(true);

    return result as ToolFailure;
}

const CREDENTIALS = { url: 'https://demo.dotcms.com', token: 'secret-tok' };

const FACTORIES = {
    download_assets: downloadAssetsTool,
    execute: executeTool,
    page_create: pageCreateTool,
    page_place_content: pagePlaceContentTool,
    page_verify: pageVerifyTool,
    search: searchTool,
    upload_assets: uploadAssetsTool
};

describe('tool factories', () => {
    const fetchMock = vi.fn();

    beforeEach(() => {
        fetchMock.mockReset();
        global.fetch = fetchMock as unknown as typeof fetch;
        // Start every test from an environment with no credentials, so a test only sees the
        // ones it sets itself.
        vi.stubEnv('DOTCMS_URL', '');
        vi.stubEnv('AUTH_TOKEN', '');
    });

    afterEach(() => {
        vi.unstubAllEnvs();
    });

    describe('the tool objects', () => {
        it.each(Object.entries(FACTORIES))(
            '%s carries the name its description and its siblings expect',
            (name, factory) => {
                const tool: DotCMSTool = factory();

                expect(tool.name).toBe(name);
                expect(tool.title).toBeTruthy();
                expect(tool.description.length).toBeGreaterThan(100);
                expect(typeof tool.inputSchema.safeParse).toBe('function');
                expect(typeof tool.execute).toBe('function');
                expect(Object.keys(tool.annotations).sort()).toEqual([
                    'destructiveHint',
                    'idempotentHint',
                    'openWorldHint',
                    'readOnlyHint'
                ]);
            }
        );

        it('marks only the tools that cannot change the instance as read-only', () => {
            const readOnly = Object.entries(FACTORIES)
                .filter(([, factory]) => (factory() as DotCMSTool).annotations.readOnlyHint)
                .map(([name]) => name);

            expect(readOnly).toEqual(['page_verify', 'search']);
        });

        it('leaves defaulted fields optional in the input-side JSON Schema a model reads', () => {
            // `publish` and `verify` are lenient booleans. A preprocess hides their default from
            // JSON Schema, so without care they were listed as REQUIRED — telling the model it
            // must send an argument that omitting is the normal case for.
            const schema = z.toJSONSchema(uploadAssetsTool().inputSchema, { io: 'input' }) as {
                required?: string[];
            };

            expect(schema.required?.sort()).toEqual(['dest', 'src']);
        });

        it('does not read the environment or touch the network when it is created', async () => {
            // Created before any credentials exist, then given them: the call must use them. A
            // server builds its tools at startup, often before its config is final.
            const tool = pageVerifyTool();
            vi.stubEnv('DOTCMS_URL', CREDENTIALS.url);
            vi.stubEnv('AUTH_TOKEN', CREDENTIALS.token);
            expect(fetchMock).not.toHaveBeenCalled();

            fetchMock.mockResolvedValue(jsonResponse(RENDERED_PAGE));
            const manifest = await tool.execute({ path: '/about-us' });

            expect(isToolFailure(manifest)).toBe(false);
        });
    });

    describe('credentials', () => {
        it('falls back to DOTCMS_URL and AUTH_TOKEN, the names the MCP server has always read', async () => {
            vi.stubEnv('DOTCMS_URL', 'https://env.dotcms.com');
            vi.stubEnv('AUTH_TOKEN', 'env-tok');
            fetchMock.mockResolvedValue(jsonResponse(RENDERED_PAGE));

            await pageVerifyTool().execute({ path: '/about-us' });

            const [url, init] = fetchMock.mock.calls[0];
            expect(String(url)).toContain('https://env.dotcms.com/api/v1/page/render/about-us');
            expect((init.headers as Record<string, string>).Authorization).toBe('Bearer env-tok');
        });

        it('prefers the options it was given over the environment', async () => {
            vi.stubEnv('DOTCMS_URL', 'https://env.dotcms.com');
            vi.stubEnv('AUTH_TOKEN', 'env-tok');
            fetchMock.mockResolvedValue(jsonResponse(RENDERED_PAGE));

            await pageVerifyTool(CREDENTIALS).execute({ path: '/about-us' });

            const [url, init] = fetchMock.mock.calls[0];
            expect(String(url)).toContain('https://demo.dotcms.com/');
            expect((init.headers as Record<string, string>).Authorization).toBe(
                'Bearer secret-tok'
            );
        });

        it('reports missing credentials as CONFIGURATION, never as a bad call', async () => {
            const failure = expectFailure(await pageVerifyTool().execute({ path: '/about-us' }));

            expect(failure).toMatchObject({ code: 'CONFIGURATION', retryable: false });
            expect(failure.error).toContain('DOTCMS_URL / AUTH_TOKEN');
            expect(failure.error).toContain('do not look for credentials');
            expect(fetchMock).not.toHaveBeenCalled();
        });
    });

    describe('execute', () => {
        it('rejects input the schema does not accept, before any request', async () => {
            const failure = expectFailure(
                await pageVerifyTool(CREDENTIALS).execute({ path: 42 } as never)
            );

            expect(failure).toMatchObject({
                ok: false,
                operation: 'page_verify',
                code: 'VALIDATION',
                retryable: false
            });
            expect(failure.error).toContain('[dotCMS - page_verify]');
            expect(failure.error).toContain('path');
            expect(fetchMock).not.toHaveBeenCalled();
        });

        it('rejects missing input rather than throwing', async () => {
            const failure = expectFailure(
                await executeTool(CREDENTIALS).execute(undefined as never)
            );

            expect(failure.code).toBe('VALIDATION');
        });

        it('bounds a direct request with the configured deadline and reports it as retryable', async () => {
            fetchMock.mockImplementation(hangingFetch);

            const failure = expectFailure(
                await pageVerifyTool({ ...CREDENTIALS, requestTimeout: 20 }).execute({
                    path: '/about-us'
                })
            );

            expect(failure).toMatchObject({ code: 'TIMEOUT', retryable: true });
            expect(failure.error).toContain('20ms deadline');
        });

        it('bounds the model’s code in execute with the allow-list, before any request', async () => {
            // `allow` exists only on executeTool: there the MODEL picks the endpoints. The
            // fixed-purpose tools own theirs, so a consumer cannot misconfigure them.
            const result = await executeTool({ ...CREDENTIALS, allow: ['/api/v1/page'] }).execute({
                code: "return await api.request({ method: 'DELETE', path: '/api/v1/site/abc' });"
            });

            expect(result).toContain('PolicyError');
            expect(result).toContain('/api/v1/site/abc');
            // The DELETE never reached the wire. What did is the context load, which stays
            // permitted under any `allow` so the `sites` / `contentTypes` globals the code
            // relies on are never silently emptied by a narrow list.
            const fetched = fetchMock.mock.calls.map(([url, init]) => {
                const { pathname } = new URL(String(url));

                return `${(init as RequestInit).method} ${pathname}`;
            });
            expect(fetched.sort()).toEqual([
                'GET /api/v1/contenttype',
                'GET /api/v1/site',
                'GET /api/v1/users/current',
                'GET /api/v2/languages'
            ]);
        });

        it('keeps a fixed-purpose tool to the endpoints it owns', async () => {
            // search's sandbox carries the same `api` adapter as execute; its policy is what
            // makes "read-only" true. The model's write is refused before it reaches the wire.
            fetchMock.mockResolvedValue(jsonResponse({ entity: [] }));

            const result = await searchTool(CREDENTIALS).execute({
                code: "return await api.request({ method: 'DELETE', path: '/api/v1/site/abc' });"
            });

            expect(result).toContain('PolicyError');
            expect(result).toContain('The search tool can only reach the endpoints it owns');
            expect(result).toContain('Use the `execute` tool');
            const methods = fetchMock.mock.calls.map(([, init]) => (init as RequestInit).method);
            expect(methods.every((method) => method === 'GET')).toBe(true);
        });

        it('resolves a page tool to its manifest object', async () => {
            fetchMock.mockResolvedValue(jsonResponse(RENDERED_PAGE));

            const manifest = await pageVerifyTool(CREDENTIALS).execute({ path: '/about-us' });

            expect(manifest).toMatchObject({ path: '/about-us', pageRendered: true });
        });

        it('resolves execute to the formatted text of the model’s code', async () => {
            // Instance context (sites, languages, …) loads before the code runs; an empty answer
            // for every loader is enough for code that does not read it.
            fetchMock.mockResolvedValue(jsonResponse({ entity: [] }));

            const result = await executeTool(CREDENTIALS).execute({
                code: 'return { sum: 1 + 1 };'
            });

            expect(typeof result).toBe('string');
            expect(JSON.parse(result as string)).toEqual({ sum: 2 });
        });
    });
});
