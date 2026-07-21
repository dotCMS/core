import type { DotCMSRuntime, RequestOptions } from '@dotcms/ai/runtime';

import { buildManifest, verifyPage } from './page-verify';

const DEFAULT_CONTAINER = '//demo.dotcms.com/application/containers/default/';

/**
 * A render response with two slots on the default container (uuids "1" and "2"). Callers override
 * the per-slot rendered HTML / contentlets and the page.rendered to exercise each verdict.
 */
function renderResponse(opts: {
    pageRendered?: string;
    slot1Html?: string;
    slot1Content?: number;
    slot2Html?: string;
    slot2Content?: number;
    urlContentMap?: { identifier?: string } | null;
}) {
    const contentlets = (n: number) =>
        Array.from({ length: n }, (_, i) => ({ identifier: `c-${i}` }));

    return {
        entity: {
            page: { rendered: opts.pageRendered ?? '<html>ok</html>', pageURI: '/about-us' },
            containers: {
                [DEFAULT_CONTAINER]: {
                    rendered: {
                        '1': opts.slot1Html ?? '',
                        '2': opts.slot2Html ?? ''
                    },
                    contentlets: {
                        '1': contentlets(opts.slot1Content ?? 0),
                        '2': contentlets(opts.slot2Content ?? 0)
                    }
                }
            },
            layout: {
                body: {
                    rows: [
                        {
                            columns: [
                                {
                                    containers: [
                                        { identifier: DEFAULT_CONTAINER, uuid: '1' },
                                        { identifier: DEFAULT_CONTAINER, uuid: '2' }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            ...(opts.urlContentMap !== undefined ? { urlContentMap: opts.urlContentMap } : {})
        }
    };
}

function manifestFrom(
    body: ReturnType<typeof renderResponse>,
    over?: { status?: number; mode?: 'LIVE' | 'WORKING' }
) {
    return buildManifest({
        path: '/about-us',
        uri: '/about-us',
        siteLabel: 'demo.dotcms.com',
        mode: over?.mode ?? 'LIVE',
        languageId: 1,
        status: over?.status ?? 200,
        body
    });
}

function slot(m: ReturnType<typeof manifestFrom>, uuid: string) {
    return m.slots.find((s) => s.uuid === uuid);
}

describe('buildManifest verdicts', () => {
    it('verdict "ok" when the slot and the page both render', () => {
        // Both slots must render for the "all slots ok" diagnosis; an empty slot #2 would (correctly)
        // steer the diagnosis to the empty-no-content branch.
        const m = manifestFrom(
            renderResponse({
                slot1Html: '<div>hello</div>',
                slot1Content: 1,
                slot2Html: '<div>world</div>',
                slot2Content: 1
            })
        );
        expect(slot(m, '1')).toMatchObject({ rendered: true, verdict: 'ok', contentCount: 1 });
        expect(slot(m, '2')).toMatchObject({ rendered: true, verdict: 'ok' });
        expect(m.pageRendered).toBe(true);
        expect(m.diagnosis).toMatch(/rendered successfully/i);
    });

    it('verdict "empty-vtl-error" when content is placed but the slot rendered empty', () => {
        const m = manifestFrom(renderResponse({ slot1Html: '   ', slot1Content: 2 }));
        expect(slot(m, '1')).toMatchObject({ rendered: false, verdict: 'empty-vtl-error', contentCount: 2 });
        expect(m.warnings.some((w) => /vtl.*failed.*\/api\/vtl\/dynamic/i.test(w))).toBe(true);
        expect(m.diagnosis).toMatch(/VTL error.*\/api\/vtl\/dynamic/i);
    });

    it('verdict "empty-no-content" when the slot resolved but nothing is placed', () => {
        const m = manifestFrom(renderResponse({ slot1Html: '', slot1Content: 0 }));
        expect(slot(m, '1')).toMatchObject({ rendered: false, verdict: 'empty-no-content', contentCount: 0 });
        expect(m.warnings.some((w) => /no content.*page_place_content/i.test(w))).toBe(true);
    });

    it('verdict "cache-stale" when the slot renders but page.rendered is empty', () => {
        const m = manifestFrom(
            renderResponse({ pageRendered: '', slot1Html: '<div>hi</div>', slot1Content: 1 })
        );
        expect(slot(m, '1')).toMatchObject({ rendered: true, verdict: 'cache-stale' });
        expect(m.pageRendered).toBe(false);
        expect(m.diagnosis).toMatch(/cache is stale.*cachettl.*re-publish/i);
    });

    it('flags 200-but-empty as a swallowed #dotParse error (200 != rendered)', () => {
        const m = manifestFrom(
            renderResponse({ pageRendered: '   \n  ', slot1Html: '', slot1Content: 0 })
        );
        expect(m.httpStatus).toBe(200);
        expect(m.pageRendered).toBe(false);
        expect(m.warnings.some((w) => /200.*swallowed|#dotParse/i.test(w))).toBe(true);
    });

    it('reports byte length per slot and for the page', () => {
        const m = manifestFrom(renderResponse({ slot1Html: 'abcde', slot1Content: 1 }));
        expect(slot(m, '1')?.bytes).toBe(5);
        expect(m.pageBytes).toBeGreaterThan(0);
    });

    it('enumerates slots in layout order', () => {
        const m = manifestFrom(renderResponse({ slot1Html: 'x', slot1Content: 1 }));
        expect(m.slots.map((s) => s.uuid)).toEqual(['1', '2']);
    });

    it('a non-200 render is a verdict, not a crash', () => {
        const m = manifestFrom(renderResponse({}), { status: 404 });
        expect(m.httpStatus).toBe(404);
        expect(m.diagnosis).toMatch(/HTTP 404.*did not render/i);
    });

    it('WORKING mode that renders warns the result reflects unpublished edits', () => {
        const m = manifestFrom(
            renderResponse({ slot1Html: '<div>draft</div>', slot1Content: 1 }),
            { mode: 'WORKING' }
        );
        expect(m.warnings.some((w) => /WORKING.*unpublished.*LIVE/i.test(w))).toBe(true);
    });

    describe('urlMap', () => {
        it('is null for a plain page (no urlContentMap field)', () => {
            const m = manifestFrom(renderResponse({ slot1Html: 'x', slot1Content: 1 }));
            expect(m.urlMap).toBeNull();
        });

        it('reports resolved + contentletId for a URL-mapped detail page', () => {
            const m = manifestFrom(
                renderResponse({
                    slot1Html: '<article>post</article>',
                    slot1Content: 1,
                    urlContentMap: { identifier: 'detail-123' }
                })
            );
            expect(m.urlMap).toEqual({ resolved: true, contentletId: 'detail-123' });
        });
    });
});

describe('verifyPage', () => {
    const DEMO_SITE = {
        identifier: 'site-uuid-1',
        hostname: 'demo.dotcms.com',
        isDefault: true,
        archived: false
    };
    const OTHER_SITE = {
        identifier: 'site-uuid-2',
        hostname: 'other.example.com',
        isDefault: false,
        archived: false
    };

    function fakeRuntime(over?: { render?: unknown; sites?: unknown[]; renderThrows?: unknown }) {
        const calls: Array<{ path: string; query?: unknown }> = [];
        const request = jest.fn(async (options: RequestOptions) => {
            calls.push({ path: options.path, query: options.query });
            if (options.path.startsWith('/api/v1/page/render')) {
                if (over?.renderThrows) {
                    throw over.renderThrows;
                }
                return over?.render ?? renderResponse({ slot1Html: '<div>x</div>', slot1Content: 1 });
            }
            return {};
        });
        const loadContext = jest.fn(async () => ({
            contentTypes: [],
            sites: over?.sites ?? [DEMO_SITE, OTHER_SITE],
            languages: [],
            currentUser: null
        }));
        return { runtime: { request, loadContext } as unknown as DotCMSRuntime, calls };
    }

    it('renders the default site with NO host_id when site is omitted', async () => {
        const { runtime, calls } = fakeRuntime();

        const m = await verifyPage({ dotcms: runtime, path: '/about-us' });

        const render = calls.find((c) => c.path.startsWith('/api/v1/page/render'));
        expect((render?.query as Record<string, unknown>)?.host_id).toBeUndefined();
        expect((render?.query as Record<string, unknown>)?.mode).toBe('LIVE');
        expect(m.site).toBe('(default)');
    });

    it('resolves a hostname to host_id for a non-default site', async () => {
        const { runtime, calls } = fakeRuntime();

        const m = await verifyPage({
            dotcms: runtime,
            path: '/about-us',
            site: 'other.example.com'
        });

        const render = calls.find((c) => c.path.startsWith('/api/v1/page/render'));
        expect((render?.query as Record<string, unknown>)?.host_id).toBe('site-uuid-2');
        expect(m.site).toBe('other.example.com');
    });

    it('accepts a site passed as its identifier directly', async () => {
        const { runtime, calls } = fakeRuntime();

        await verifyPage({ dotcms: runtime, path: '/x', site: 'site-uuid-2' });

        const render = calls.find((c) => c.path.startsWith('/api/v1/page/render'));
        expect((render?.query as Record<string, unknown>)?.host_id).toBe('site-uuid-2');
    });

    it('throws a clear error for an unknown site', async () => {
        const { runtime } = fakeRuntime();

        await expect(
            verifyPage({ dotcms: runtime, path: '/x', site: 'nope.example.com' })
        ).rejects.toThrow(/not found.*hostname.*identifier/i);
    });

    it('passes languageId and mode through to the render call', async () => {
        const { runtime, calls } = fakeRuntime();

        await verifyPage({ dotcms: runtime, path: '/x', languageId: 2, mode: 'WORKING' });

        const render = calls.find((c) => c.path.startsWith('/api/v1/page/render'));
        expect((render?.query as Record<string, unknown>)?.language_id).toBe(2);
        expect((render?.query as Record<string, unknown>)?.mode).toBe('WORKING');
    });

    it('surfaces a 404 render as a manifest verdict, not a throw', async () => {
        const { runtime } = fakeRuntime({
            renderThrows: new Error('Request failed with status 404: not found')
        });

        const m = await verifyPage({ dotcms: runtime, path: '/missing' });
        expect(m.httpStatus).toBe(404);
        expect(m.diagnosis).toMatch(/HTTP 404/);
    });

    it('produces an end-to-end ok verdict on a healthy page', async () => {
        const { runtime } = fakeRuntime();

        const m = await verifyPage({ dotcms: runtime, path: '/about-us' });
        expect(m.pageRendered).toBe(true);
        expect(slot(m, '1')?.verdict).toBe('ok');
    });
});
