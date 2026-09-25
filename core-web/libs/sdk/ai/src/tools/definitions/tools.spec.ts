import { expectTypeOf, vi } from 'vitest';
import { z } from 'zod';

import { mkdtemp, readdir, rm, truncate, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { downloadAssetsTool } from './download-assets';
import { executeTool } from './execute';
import { pageCreateTool } from './page-create';
import { pagePlaceContentTool } from './page-place-content';
import { pageVerifyTool } from './page-verify';
import { searchTool } from './search';
import { uploadAssetsTool } from './upload-assets';

import { dotcmsConnection, type DotCMSConnection } from '../toolkit/connection';
import { isToolFailure, type ToolFailure } from '../toolkit/tool-runtime';

import type { CodeToolResult } from '../toolkit/results';
import type { AnyDotCMSTool } from '../toolkit/types';
import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { ToolSet } from 'ai';

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

/** The URL and bearer token of the n-th request fetch saw. */
function sent(fetchMock: ReturnType<typeof vi.fn>, n = 0): { url: string; auth: string } {
    const [url, init] = fetchMock.mock.calls[n];

    return {
        url: String(url),
        auth: ((init as RequestInit).headers as Record<string, string>)['Authorization']
    };
}

const DOTCMS = dotcmsConnection({ url: 'https://demo.dotcms.com', token: 'secret-tok' });

const FACTORIES: Record<string, (connection: DotCMSConnection) => AnyDotCMSTool> = {
    download_assets: (connection) => downloadAssetsTool(connection, { root: '/' }),
    execute: executeTool,
    page_create: pageCreateTool,
    page_place_content: pagePlaceContentTool,
    page_verify: pageVerifyTool,
    search: searchTool,
    upload_assets: (connection) => uploadAssetsTool(connection, { root: '/' })
};

describe('tool factories', () => {
    const fetchMock = vi.fn();

    beforeEach(() => {
        fetchMock.mockReset();
        global.fetch = fetchMock as unknown as typeof fetch;
    });

    afterEach(() => {
        vi.unstubAllEnvs();
    });

    describe('the tool objects', () => {
        it.each(Object.entries(FACTORIES))(
            '%s carries the name its description and its siblings expect',
            (name, factory) => {
                const tool = factory(DOTCMS);

                expect(tool.name).toBe(name);
                expect(tool.title).toBeTruthy();
                expect(tool.description.length).toBeGreaterThan(100);
                expect(typeof tool.inputSchema.safeParse).toBe('function');
                expect(typeof tool.execute).toBe('function');
                expect(typeof tool.toModelOutput).toBe('function');
                expect(typeof tool.toText).toBe('function');
                expect(Object.keys(tool.annotations).sort()).toEqual([
                    'destructiveHint',
                    'idempotentHint',
                    'openWorldHint',
                    'readOnlyHint'
                ]);
            }
        );

        it('types execute’s input from the tool’s own schema', () => {
            // Compile-time: a caller writing the call gets completion, and a wrong field is a
            // type error rather than a VALIDATION failure found at run time.
            const tool = pageVerifyTool(DOTCMS);

            expectTypeOf(tool.execute)
                .parameter(0)
                .toEqualTypeOf<z.input<typeof tool.inputSchema>>();
            // @ts-expect-error — `path` is a string.
            const wrong = () => tool.execute({ path: 42 });
            expect(typeof wrong).toBe('function');
        });

        it('holds tools with different schemas in one AnyDotCMSTool[]', () => {
            // A registry dispatches whatever the model picked, so its element type takes any input.
            const tools: AnyDotCMSTool[] = [pageVerifyTool(DOTCMS), searchTool(DOTCMS)];

            expectTypeOf(tools[0].execute).parameter(0).toBeUnknown();
            expect(tools.map((tool) => tool.name)).toEqual(['page_verify', 'search']);
        });

        it('type-checks as the AI SDK’s tools and as an MCP registerTool config, as the README shows', () => {
            // Compile-time: the README's framework examples pass the tool objects straight in,
            // so a type the frameworks reject breaks every consumer that copies them.
            const aiSdk: ToolSet = {
                search: searchTool(DOTCMS),
                page_verify: pageVerifyTool(DOTCMS)
            };
            // Never called: it exists so the compiler checks the tool against registerTool's config.
            const register = (server: McpServer) => {
                const tool = pageVerifyTool(DOTCMS);
                server.registerTool(tool.name, tool, async (args) => ({
                    content: [{ type: 'text', text: tool.toText(await tool.execute(args)) }]
                }));
            };

            expect(Object.keys(aiSdk)).toEqual(['search', 'page_verify']);
            expect(typeof register).toBe('function');
        });

        it('marks only the tools that cannot change the instance as read-only', () => {
            const readOnly = Object.entries(FACTORIES)
                .filter(([, factory]) => factory(DOTCMS).annotations.readOnlyHint)
                .map(([name]) => name);

            expect(readOnly).toEqual(['page_verify', 'search']);
        });

        it('leaves defaulted fields optional in the input-side JSON Schema a model reads', () => {
            // `publish` and `verify` are lenient booleans. A preprocess hides their default from
            // JSON Schema, so without care they were listed as REQUIRED — telling the model it
            // must send an argument that omitting is the normal case for.
            const schema = z.toJSONSchema(uploadAssetsTool(DOTCMS, { root: '/' }).inputSchema, {
                io: 'input'
            }) as { required?: string[] };

            expect(schema.required?.sort()).toEqual(['dest', 'src']);
        });
    });

    describe('the connection', () => {
        it('sends the url and token it was given — injected host-side, never through the model', async () => {
            fetchMock.mockResolvedValue(jsonResponse(RENDERED_PAGE));

            await pageVerifyTool(DOTCMS).execute({ path: '/about-us' });

            expect(sent(fetchMock)).toEqual({
                url: expect.stringContaining('https://demo.dotcms.com/api/v1/page/render/about-us'),
                auth: 'Bearer secret-tok'
            });
        });

        it('never reads credentials from the environment itself', async () => {
            // The consumer owns its configuration. A host that wants environment variables reads
            // them in its own resolvers — the SDK knows no variable names.
            vi.stubEnv('DOTCMS_URL', 'https://env.dotcms.com');
            vi.stubEnv('AUTH_TOKEN', 'env-tok');

            const empty = dotcmsConnection({ url: '', token: '' });
            const failure = expectFailure(
                await pageVerifyTool(empty).execute({ path: '/about-us' })
            );

            expect(failure.code).toBe('CONFIGURATION');
            expect(fetchMock).not.toHaveBeenCalled();
        });

        it('reads resolvers on every call, not when the tool is created', async () => {
            // A rotating token: each call must use whatever the resolver returns right then.
            let current = 'first-tok';
            const tool = pageVerifyTool(
                dotcmsConnection({ url: 'https://demo.dotcms.com', token: async () => current })
            );
            fetchMock.mockResolvedValue(jsonResponse(RENDERED_PAGE));

            await tool.execute({ path: '/about-us' });
            current = 'second-tok';
            await tool.execute({ path: '/about-us' });

            expect(sent(fetchMock, 0).auth).toBe('Bearer first-tok');
            expect(sent(fetchMock, 1).auth).toBe('Bearer second-tok');
        });

        it('boots without credentials and reports each call as CONFIGURATION', async () => {
            // A host that builds its tools at startup, before its config exists — e.g. a
            // resolver over an unset environment variable.
            const unset = dotcmsConnection({ url: () => undefined, token: () => undefined });

            const failure = expectFailure(
                await pageVerifyTool(unset).execute({ path: '/about-us' })
            );

            expect(failure).toMatchObject({ code: 'CONFIGURATION', retryable: false });
            expect(failure.error).toContain('dotCMS connection');
            expect(failure.error).toContain('do not look for credentials');
            expect(fetchMock).not.toHaveBeenCalled();
        });

        describe('a failing resolver', () => {
            // A secrets manager's error can carry paths, key names or identifiers — it must reach
            // the host, never the model's context.
            const SECRET_DETAIL = 'vault read failed: secret/prod/dotcms-admin-token (10.0.3.7)';
            const vaultDown = () => {
                throw new Error(SECRET_DETAIL);
            };

            it('is reported as CONFIGURATION without the resolver’s own message', async () => {
                const broken = dotcmsConnection({
                    url: 'https://demo.dotcms.com',
                    token: vaultDown
                });

                const failure = expectFailure(
                    await pageVerifyTool(broken).execute({ path: '/about-us' })
                );

                expect(failure).toMatchObject({ code: 'CONFIGURATION', retryable: false });
                expect(failure.error).toContain('could not resolve its token');
                expect(JSON.stringify(failure)).not.toContain('secret/prod');
                expect(JSON.stringify(failure)).not.toContain('10.0.3.7');
                expect(fetchMock).not.toHaveBeenCalled();
            });

            it('hands the host the real error through onResolveError', async () => {
                const onResolveError = vi.fn();
                const broken = dotcmsConnection({
                    url: 'https://demo.dotcms.com',
                    token: vaultDown,
                    onResolveError
                });

                await pageVerifyTool(broken).execute({ path: '/about-us' });

                expect(onResolveError).toHaveBeenCalledWith(
                    'token',
                    expect.objectContaining({ message: SECRET_DETAIL })
                );
            });

            it('still reports CONFIGURATION when the hook itself throws', async () => {
                const broken = dotcmsConnection({
                    url: 'https://demo.dotcms.com',
                    token: vaultDown,
                    onResolveError: () => {
                        throw new Error('logger down');
                    }
                });

                const failure = expectFailure(
                    await pageVerifyTool(broken).execute({ path: '/about-us' })
                );

                expect(failure.code).toBe('CONFIGURATION');
                expect(failure.error).not.toContain('logger down');
            });
        });

        it('reports a tool created without a connection as CONFIGURATION rather than throwing', async () => {
            // Only reachable from JavaScript or through a cast — TypeScript requires one.
            const failure = expectFailure(
                await pageVerifyTool(undefined as never).execute({ path: '/about-us' })
            );

            expect(failure.code).toBe('CONFIGURATION');
            expect(failure.error).toContain('created without a dotCMS connection');
        });

        it('fires the connection’s onCall hook for every request', async () => {
            const onCall = vi.fn();
            fetchMock.mockResolvedValue(jsonResponse(RENDERED_PAGE));

            await pageVerifyTool(
                dotcmsConnection({ url: 'https://demo.dotcms.com', token: 't', onCall })
            ).execute({ path: '/about-us' });

            expect(onCall).toHaveBeenCalledWith(
                expect.objectContaining({ method: 'GET', path: '/api/v1/page/render/about-us' })
            );
        });
    });

    describe('execute', () => {
        it('rejects input the schema does not accept, before any request', async () => {
            const failure = expectFailure(
                await pageVerifyTool(DOTCMS).execute({ path: 42 } as never)
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
            const failure = expectFailure(await executeTool(DOTCMS).execute(undefined as never));

            expect(failure.code).toBe('VALIDATION');
        });

        // Input the schema accepts but the operation refuses. The model gets the same code as
        // for a schema failure — the call is wrong and retrying it unchanged cannot help —
        // not UNKNOWN, which says nothing about whether to fix the input or try again.
        it.each([
            ['download_assets', { path: '/application/themes', dest: 'relative/dir' }],
            ['download_assets', { path: 'application/themes', dest: '/tmp/out' }],
            ['upload_assets', { src: '/tmp', dest: '/application/themes/travel' }],
            ['upload_assets', { src: 'relative/dir', dest: '//demo.dotcms.com/application' }],
            [
                'page_create',
                { site: 'ghost.example.com', urlPath: '/books', title: 'Books', template: 't-1' }
            ],
            [
                'page_place_content',
                { path: '/about-us', slots: [{ slot: 1, contentlets: ['c-1'] }] }
            ],
            ['page_verify', { path: '/a/my%2Fbooks' }]
        ])('reports %s input dotCMS cannot act on as VALIDATION', async (name, input) => {
            // Every instance-context loader answers empty: no site, no type, no language.
            fetchMock.mockResolvedValue(jsonResponse({ entity: [] }));

            const failure = expectFailure(await FACTORIES[name](DOTCMS).execute(input));

            expect(failure).toMatchObject({
                operation: name,
                code: 'VALIDATION',
                retryable: false
            });
        });

        it('bounds a direct request with the configured deadline and reports it as retryable', async () => {
            fetchMock.mockImplementation(hangingFetch);

            const failure = expectFailure(
                await pageVerifyTool(DOTCMS, { requestTimeout: 20 }).execute({ path: '/about-us' })
            );

            expect(failure).toMatchObject({ code: 'TIMEOUT', retryable: true });
            expect(failure.error).toContain('20ms deadline');
        });

        describe('upload_assets limits', () => {
            // The model picks `src`; the MCP server's root is the whole disk. So the TOOL is
            // bounded by default, while the direct operation stays unbounded unless told.
            let dir: string;

            beforeEach(async () => {
                dir = await mkdtemp(join(tmpdir(), 'dot-limits-'));
            });

            afterEach(async () => {
                await rm(dir, { recursive: true, force: true });
            });

            it('refuses a file over 100 MB by default, without sending it', async () => {
                // Sparse: 100 MB + 1 byte on paper, nothing on disk.
                await truncate(join(dir, 'huge.bin'), 100 * 1024 * 1024 + 1).catch(async () => {
                    await writeFile(join(dir, 'huge.bin'), '');
                    await truncate(join(dir, 'huge.bin'), 100 * 1024 * 1024 + 1);
                });

                const manifest = (await uploadAssetsTool(DOTCMS, { root: dir }).execute({
                    src: dir,
                    dest: '//demo.dotcms.com/app'
                })) as { failures: Array<{ path: string; error: string }> };

                expect(manifest.failures).toEqual([
                    { path: 'huge.bin', error: expect.stringContaining('upload limit') }
                ]);
                expect(fetchMock).not.toHaveBeenCalled();
            });

            it('refuses more than 1,000 files by default, as VALIDATION', async () => {
                await Promise.all(
                    Array.from({ length: 1001 }, (_, n) => writeFile(join(dir, `f${n}.css`), 'x'))
                );

                const failure = expectFailure(
                    await uploadAssetsTool(DOTCMS, { root: dir }).execute({
                        src: dir,
                        dest: '//demo.dotcms.com/app'
                    })
                );

                expect(failure).toMatchObject({ code: 'VALIDATION', retryable: false });
                expect(fetchMock).not.toHaveBeenCalled();
            });

            it('takes a host’s own limits', async () => {
                await writeFile(join(dir, 'a.css'), 'x');
                await writeFile(join(dir, 'b.css'), 'x');

                const failure = expectFailure(
                    await uploadAssetsTool(DOTCMS, { root: dir, maxFiles: 1 }).execute({
                        src: dir,
                        dest: '//demo.dotcms.com/app'
                    })
                );

                expect(failure.code).toBe('VALIDATION');
            });
        });

        it('keeps the deadline on a download while its body is still arriving', async () => {
            // The asset answers at once and then stalls mid-body. The bytes stream to disk
            // inside the request, so the same deadline ends the transfer — and the file that
            // never finished is not left behind.
            const dir = await mkdtemp(join(tmpdir(), 'dot-deadline-'));
            fetchMock.mockImplementation(async (url: string, init?: RequestInit) => {
                if (String(url).includes('/api/content/_search')) {
                    return jsonResponse({
                        entity: {
                            jsonObjectView: {
                                contentlets: [
                                    { identifier: 'a1', path: '//demo.dotcms.com/app/big.bin' }
                                ]
                            }
                        }
                    });
                }
                if (!String(url).includes('/api/v2/assets/')) {
                    // The site lookup and context reads the folder search is scoped by.
                    return jsonResponse({
                        entity: String(url).includes('/api/v1/site')
                            ? [
                                  {
                                      identifier: 'site-demo',
                                      hostname: 'demo.dotcms.com',
                                      isDefault: true
                                  }
                              ]
                            : []
                    });
                }

                const body = new ReadableStream<Uint8Array>({
                    start(controller) {
                        controller.enqueue(new Uint8Array([1, 2, 3]));
                        init?.signal?.addEventListener('abort', () =>
                            controller.error(new DOMException('aborted', 'AbortError'))
                        );
                    }
                });

                return {
                    ok: true,
                    status: 200,
                    statusText: 'OK',
                    headers: { get: () => 'application/octet-stream' },
                    body
                } as unknown as Response;
            });

            try {
                const manifest = (await downloadAssetsTool(DOTCMS, {
                    root: dir,
                    requestTimeout: 20
                }).execute({ path: '//demo.dotcms.com/app', dest: dir })) as {
                    failures: Array<{ path: string; error: string }>;
                };

                expect(manifest.failures).toEqual([
                    { path: 'big.bin', error: expect.stringContaining('20ms deadline') }
                ]);
                expect(await readdir(dir)).toEqual([]);
            } finally {
                await rm(dir, { recursive: true, force: true });
            }
        });

        it('refuses an executeTool allow entry that is not a path, when the tool is created', () => {
            // The host's own configuration, so it fails at startup rather than quietly opening
            // the whole API to the model's code: `''` used to match every path.
            expect(() => executeTool(DOTCMS, { allow: [''] })).toThrow(/allow/);
        });

        it('bounds the model’s code in execute with the allow-list, before any request', async () => {
            // `allow` exists only on executeTool: there the MODEL picks the endpoints. The
            // fixed-purpose tools own theirs, so a consumer cannot misconfigure them.
            const { result } = (await executeTool(DOTCMS, { allow: ['/api/v1/page'] }).execute({
                code: "return await api.request({ method: 'DELETE', path: '/api/v1/site/abc' });"
            })) as CodeToolResult;

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

            const { result } = (await searchTool(DOTCMS).execute({
                code: "return await api.request({ method: 'DELETE', path: '/api/v1/site/abc' });"
            })) as CodeToolResult;

            expect(result).toContain('PolicyError');
            expect(result).toContain('The search tool can only reach the endpoints it owns');
            expect(result).toContain('Use the `execute` tool');
            const methods = fetchMock.mock.calls.map(([, init]) => (init as RequestInit).method);
            expect(methods.every((method) => method === 'GET')).toBe(true);
        });

        it('resolves a page tool to its manifest object', async () => {
            fetchMock.mockResolvedValue(jsonResponse(RENDERED_PAGE));

            const manifest = await pageVerifyTool(DOTCMS).execute({ path: '/about-us' });

            expect(manifest).toMatchObject({ path: '/about-us', pageRendered: true });
        });

        it('resolves execute to an object carrying the formatted text of the model’s code', async () => {
            // An object, not a bare string: Google ADK requires tool results to be objects.
            // Instance context loads before the code runs; an empty answer for every loader is
            // enough for code that does not read it.
            fetchMock.mockResolvedValue(jsonResponse({ entity: [] }));

            const output = await executeTool(DOTCMS).execute({ code: 'return { sum: 1 + 1 };' });

            expect(Object.keys(output as object)).toEqual(['result']);
            expect(JSON.parse((output as CodeToolResult).result)).toEqual({ sum: 2 });
        });
    });

    describe('what the model sees', () => {
        const codeResult: CodeToolResult = { result: 'line one\nline two' };
        const manifest = { path: '/about-us', pageRendered: true };

        it('shows a code tool’s text as text, and a manifest as structured JSON', () => {
            const tool = searchTool(DOTCMS);

            expect(tool.toModelOutput({ output: codeResult })).toEqual({
                type: 'text',
                value: 'line one\nline two'
            });
            expect(pageVerifyTool(DOTCMS).toModelOutput({ output: manifest as never })).toEqual({
                type: 'json',
                value: manifest
            });
        });

        it('renders a result as the text an MCP host sends — code text unescaped', () => {
            expect(searchTool(DOTCMS).toText(codeResult)).toBe('line one\nline two');
            expect(pageVerifyTool(DOTCMS).toText(manifest as never)).toBe(
                JSON.stringify(manifest, null, 2)
            );
        });

        it('shows text only for the tools that declare a text form', () => {
            // Asked with the SAME code-shaped value, only search and execute answer with text:
            // the choice is the tool's declaration, not a guess from the value's shape.
            const textTools = Object.entries(FACTORIES)
                .filter(
                    ([, factory]) =>
                        factory(DOTCMS).toModelOutput({ output: codeResult }).type === 'text'
                )
                .map(([name]) => name);

            expect(textTools).toEqual(['execute', 'search']);
        });
    });
});
