import type { createRuntime, RequestOptions } from '@dotcms/ai/runtime';

import { createPage, splitUrlPath } from './page-create';

type DotCMSRuntime = ReturnType<typeof createRuntime>;

describe('splitUrlPath', () => {
    it('splits a path with an explicit leaf into folder + leaf', () => {
        expect(splitUrlPath('/books/index')).toEqual({
            folder: '/books',
            url: 'index',
            fullPath: '/books/index'
        });
    });

    it('treats a non-index leaf as the page url', () => {
        expect(splitUrlPath('/store/checkout')).toEqual({
            folder: '/store',
            url: 'checkout',
            fullPath: '/store/checkout'
        });
    });

    it('treats a trailing slash as a folder whose page is index', () => {
        expect(splitUrlPath('/about-us/')).toEqual({
            folder: '/about-us',
            url: 'index',
            fullPath: '/about-us/index'
        });
    });

    it('treats a single bare segment as a leaf under root', () => {
        expect(splitUrlPath('/contact')).toEqual({
            folder: '/',
            url: 'contact',
            fullPath: '/contact'
        });
    });

    it('maps the site root to the root index page', () => {
        expect(splitUrlPath('/')).toEqual({ folder: '/', url: 'index', fullPath: '/index' });
    });

    it('handles a deep nested folder path', () => {
        expect(splitUrlPath('/store/books/scifi/index')).toEqual({
            folder: '/store/books/scifi',
            url: 'index',
            fullPath: '/store/books/scifi/index'
        });
    });

    it('rejects a path that does not start with /', () => {
        expect(() => splitUrlPath('books/index')).toThrow('must start with');
    });

    it('percent-decodes segments so the folder is named literally', () => {
        expect(splitUrlPath('/my%20books/index')).toEqual({
            folder: '/my books',
            url: 'index',
            fullPath: '/my books/index'
        });
    });

    it('collapses ./.. segments', () => {
        expect(splitUrlPath('/store/../books/index')).toEqual({
            folder: '/books',
            url: 'index',
            fullPath: '/books/index'
        });
    });

    it('strips a query string and fragment', () => {
        expect(splitUrlPath('/books/checkout?draft=1#top')).toEqual({
            folder: '/books',
            url: 'checkout',
            fullPath: '/books/checkout'
        });
    });
});

describe('createPage', () => {
    interface FakeField {
        variable: string;
        required?: boolean;
        fixed?: boolean;
        defaultValue?: unknown;
    }
    interface FakeContentType {
        id: string;
        variable: string;
        baseType: string;
        fields?: FakeField[];
    }

    // A standard page type with no user-added required fields — the default for tests that
    // don't care about content-type resolution.
    const HTMLPAGE_ASSET: FakeContentType = {
        id: 'ct-htmlpageasset',
        variable: 'htmlpageasset',
        baseType: 'HTMLPAGE',
        fields: [
            { variable: 'title', required: true },
            { variable: 'url', required: true },
            { variable: 'template', required: true }
        ]
    };

    // A request recorder standing in for the runtime, so we can assert ordering and payloads.
    // `contentTypes` seeds both loadContext() and the /contenttype/id/{} lookup.
    function fakeRuntime(handlers: {
        onCreateFolders?: (body: unknown) => unknown;
        onFire?: (body: unknown, query: unknown) => unknown;
        onLive?: () => unknown;
        contentTypes?: FakeContentType[];
    }) {
        const contentTypes = handlers.contentTypes ?? [HTMLPAGE_ASSET];
        const calls: Array<{ method?: string; path: string; body?: unknown; query?: unknown }> = [];
        const request = jest.fn(async (options: RequestOptions) => {
            calls.push({
                method: options.method,
                path: options.path,
                body: options.body,
                query: options.query
            });
            if (options.path.startsWith('/api/v1/contenttype/id/')) {
                const idOrVar = decodeURIComponent(
                    options.path.replace('/api/v1/contenttype/id/', '')
                );
                const ct = contentTypes.find(
                    (c) => c.id === idOrVar || c.variable === idOrVar
                );
                return { entity: ct ?? {} };
            }
            if (options.path.startsWith('/api/v1/folder/createfolders/')) {
                return handlers.onCreateFolders?.(options.body) ?? { entity: [] };
            }
            if (options.path.includes('/fire/')) {
                return handlers.onFire?.(options.body, options.query) ?? { entity: {} };
            }
            if (options.path.startsWith('/api/v1/content/')) {
                return handlers.onLive?.() ?? { entity: { live: true } };
            }
            return {};
        });
        const loadContext = jest.fn(async () => ({
            contentTypes: contentTypes.map((c) => ({
                id: c.id,
                name: c.variable,
                variable: c.variable,
                baseType: c.baseType
            })),
            sites: [],
            languages: [],
            currentUser: null
        }));
        return { runtime: { request, loadContext } as unknown as DotCMSRuntime, calls };
    }

    it('creates the parent folder BEFORE firing the page (trap #1)', async () => {
        const { runtime, calls } = fakeRuntime({
            onCreateFolders: () => ({ entity: [{ path: '/books', identifier: 'folder-123' }] }),
            onFire: () => ({ entity: { identifier: 'page-1', inode: 'inode-1', live: true } }),
            onLive: () => ({ entity: { live: true } })
        });

        await createPage({
            dotcms: runtime,
            site: 'demo.dotcms.com',
            urlPath: '/books/index',
            title: 'Books',
            template: 'tmpl-1'
        });

        const folderCall = calls.findIndex((c) => c.path.includes('createfolders'));
        const fireCall = calls.findIndex((c) => c.path.includes('/fire/'));
        expect(folderCall).toBeGreaterThanOrEqual(0);
        expect(fireCall).toBeGreaterThan(folderCall);
    });

    it('fires the page with the leaf url and the created folder id, not the full path', async () => {
        let firedBody: { contentlet: Record<string, unknown> } | undefined;
        const { runtime } = fakeRuntime({
            onCreateFolders: () => ({ entity: [{ path: '/books', identifier: 'folder-123' }] }),
            onFire: (body) => {
                firedBody = body as { contentlet: Record<string, unknown> };
                return { entity: { identifier: 'page-1', live: true } };
            }
        });

        await createPage({
            dotcms: runtime,
            site: 'demo.dotcms.com',
            urlPath: '/books/index',
            title: 'Books',
            template: 'tmpl-1'
        });

        // The url must be the bare leaf — never "/books/index", which dotCMS would collapse.
        const contentlet = firedBody?.contentlet ?? {};
        expect(contentlet.url).toBe('index');
        expect(contentlet.hostFolder).toBe('folder-123');
        expect(contentlet.contentType).toBe('htmlpageasset');
        expect(contentlet.template).toBe('tmpl-1');
    });

    it('fires with indexPolicy=WAIT_FOR', async () => {
        let firedQuery: Record<string, unknown> | undefined;
        const { runtime } = fakeRuntime({
            onCreateFolders: () => ({ entity: [{ path: '/x', identifier: 'f' }] }),
            onFire: (_body, query) => {
                firedQuery = query as Record<string, unknown>;
                return { entity: { identifier: 'p', live: true } };
            }
        });

        await createPage({
            dotcms: runtime,
            site: 'demo.dotcms.com',
            urlPath: '/x/index',
            title: 'X',
            template: 't'
        });

        expect(firedQuery?.indexPolicy).toBe('WAIT_FOR');
    });

    it('skips folder creation for a root page', async () => {
        const { runtime, calls } = fakeRuntime({
            onFire: () => ({ entity: { identifier: 'home', live: true } })
        });

        const manifest = await createPage({
            dotcms: runtime,
            site: 'demo.dotcms.com',
            urlPath: '/index',
            title: 'Home',
            template: 't'
        });

        expect(calls.some((c) => c.path.includes('createfolders'))).toBe(false);
        expect(manifest.folder).toBe('/');
        expect(manifest.url).toBe('index');
    });

    it('warns when the created page is not confirmed live (the blank-page trap #2)', async () => {
        const { runtime } = fakeRuntime({
            onCreateFolders: () => ({ entity: [{ path: '/p', identifier: 'f' }] }),
            onFire: () => ({ entity: { identifier: 'page-1', live: false } }),
            onLive: () => ({ entity: { live: false } })
        });

        const manifest = await createPage({
            dotcms: runtime,
            site: 'demo.dotcms.com',
            urlPath: '/p/index',
            title: 'P',
            template: 't'
        });

        expect(manifest.live).toBe(false);
        expect(manifest.warnings.length).toBeGreaterThan(0);
        expect(manifest.warnings[0]).toMatch(/blank|content/i);
    });

    describe('content type resolution', () => {
        const CUSTOM_PAGE: FakeContentType = {
            id: 'ct-landing',
            variable: 'landingPage',
            baseType: 'HTMLPAGE',
            fields: [
                { variable: 'title', required: true },
                { variable: 'url', required: true },
                // user-added required field with no default — must be supplied via extraFields
                { variable: 'campaign', required: true },
                // user-added required field WITH a default — does not need to be supplied
                { variable: 'region', required: true, defaultValue: 'global' },
                // optional user field — never required
                { variable: 'subtitle', required: false }
            ]
        };

        it('defaults to htmlpageasset and stamps it on the manifest', async () => {
            const { runtime } = fakeRuntime({
                onCreateFolders: () => ({ entity: [{ path: '/p', identifier: 'f' }] }),
                onFire: () => ({ entity: { identifier: 'page-1', live: true } })
            });

            const manifest = await createPage({
                dotcms: runtime,
                site: 'demo.dotcms.com',
                urlPath: '/p/index',
                title: 'P',
                template: 't'
            });

            expect(manifest.contentType).toBe('htmlpageasset');
        });

        it('fires a custom page type and merges extraFields', async () => {
            let firedBody: { contentlet: Record<string, unknown> } | undefined;
            const { runtime } = fakeRuntime({
                contentTypes: [CUSTOM_PAGE],
                onCreateFolders: () => ({ entity: [{ path: '/lp', identifier: 'f' }] }),
                onFire: (body) => {
                    firedBody = body as { contentlet: Record<string, unknown> };
                    return { entity: { identifier: 'page-1', live: true } };
                }
            });

            const manifest = await createPage({
                dotcms: runtime,
                site: 'demo.dotcms.com',
                urlPath: '/lp/index',
                title: 'Launch',
                template: 't',
                contentType: 'landingPage',
                extraFields: { campaign: 'summer', subtitle: 'Hot deals' }
            });

            expect(manifest.contentType).toBe('landingPage');
            const contentlet = firedBody?.contentlet ?? {};
            expect(contentlet.contentType).toBe('landingPage');
            expect(contentlet.campaign).toBe('summer');
            expect(contentlet.subtitle).toBe('Hot deals');
        });

        it('throws listing missing user-required fields before firing', async () => {
            const { runtime, calls } = fakeRuntime({
                contentTypes: [CUSTOM_PAGE],
                onFire: () => ({ entity: { identifier: 'page-1', live: true } })
            });

            await expect(
                createPage({
                    dotcms: runtime,
                    site: 'demo.dotcms.com',
                    urlPath: '/lp/index',
                    title: 'Launch',
                    template: 't',
                    contentType: 'landingPage'
                    // campaign missing
                })
            ).rejects.toThrow(/campaign/);

            // It must fail BEFORE any folder/fire side effect.
            expect(calls.some((c) => c.path.includes('createfolders'))).toBe(false);
            expect(calls.some((c) => c.path.includes('/fire/'))).toBe(false);
        });

        it('does not require a user field that has a default value', async () => {
            const { runtime } = fakeRuntime({
                contentTypes: [CUSTOM_PAGE],
                onCreateFolders: () => ({ entity: [{ path: '/lp', identifier: 'f' }] }),
                onFire: () => ({ entity: { identifier: 'page-1', live: true } })
            });

            // `region` is required but has a default — supplying only `campaign` is enough.
            await expect(
                createPage({
                    dotcms: runtime,
                    site: 'demo.dotcms.com',
                    urlPath: '/lp/index',
                    title: 'Launch',
                    template: 't',
                    contentType: 'landingPage',
                    extraFields: { campaign: 'summer' }
                })
            ).resolves.toMatchObject({ contentType: 'landingPage' });
        });

        it('rejects a content type that is not a page (wrong base type)', async () => {
            const { runtime } = fakeRuntime({
                contentTypes: [
                    { id: 'ct-blog', variable: 'Blog', baseType: 'CONTENT', fields: [] }
                ]
            });

            await expect(
                createPage({
                    dotcms: runtime,
                    site: 'demo.dotcms.com',
                    urlPath: '/b/index',
                    title: 'B',
                    template: 't',
                    contentType: 'Blog'
                })
            ).rejects.toThrow(/not a page|HTMLPAGE/i);
        });

        it('rejects an unknown content type', async () => {
            const { runtime } = fakeRuntime({ contentTypes: [HTMLPAGE_ASSET] });

            await expect(
                createPage({
                    dotcms: runtime,
                    site: 'demo.dotcms.com',
                    urlPath: '/x/index',
                    title: 'X',
                    template: 't',
                    contentType: 'doesNotExist'
                })
            ).rejects.toThrow(/not found/i);
        });

        it('cannot be overridden by extraFields on a typed page field', async () => {
            let firedBody: { contentlet: Record<string, unknown> } | undefined;
            const { runtime } = fakeRuntime({
                onCreateFolders: () => ({ entity: [{ path: '/p', identifier: 'f' }] }),
                onFire: (body) => {
                    firedBody = body as { contentlet: Record<string, unknown> };
                    return { entity: { identifier: 'page-1', live: true } };
                }
            });

            await createPage({
                dotcms: runtime,
                site: 'demo.dotcms.com',
                urlPath: '/p/index',
                title: 'Real Title',
                template: 'real-tmpl',
                extraFields: { title: 'HIJACK', template: 'evil', url: 'evil' }
            });

            const contentlet = firedBody?.contentlet ?? {};
            expect(contentlet.title).toBe('Real Title');
            expect(contentlet.template).toBe('real-tmpl');
            expect(contentlet.url).toBe('index');
        });
    });
});
