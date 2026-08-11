import { HttpError, type DotCMSRuntime, type RequestOptions } from '@dotcms/ai/runtime';

import { placeContent } from './page-place-content';

/**
 * A page with two slots on the default file container (uuids "1" and "2") and one system-container
 * slot (uuid "1"). Slot #1 already has one contentlet, slot #2 is empty, the system slot has one.
 * This mirrors the /api/v1/page/json shape: `containers` keyed by container key, each with a
 * `contentlets` map keyed by uuid; `layout.body.rows[].columns[].containers[]` for slot order.
 */
const DEFAULT_CONTAINER = '//demo.dotcms.com/application/containers/default/';
const SYSTEM_CONTAINER = 'SYSTEM_CONTAINER';

function pageJson() {
    return {
        entity: {
            page: { identifier: 'page-1', pageURI: '/about-us' },
            containers: {
                [DEFAULT_CONTAINER]: {
                    contentlets: {
                        'uuid-1': [{ identifier: 'existing-a' }],
                        'uuid-2': []
                    }
                },
                [SYSTEM_CONTAINER]: {
                    contentlets: {
                        'uuid-1': [{ identifier: 'sys-x' }]
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
                        },
                        {
                            columns: [{ containers: [{ identifier: SYSTEM_CONTAINER, uuid: '1' }] }]
                        }
                    ]
                }
            }
        }
    };
}

interface PostedEntry {
    identifier: string;
    uuid: string;
    contentletsId: string[];
}

function fakeRuntime(overrides?: {
    onPost?: (body: unknown, query: unknown) => unknown;
    page?: unknown;
}) {
    const calls: Array<{ method?: string; path: string; body?: unknown; query?: unknown }> = [];
    const request = jest.fn(async (options: RequestOptions) => {
        calls.push({
            method: options.method,
            path: options.path,
            body: options.body,
            query: options.query
        });
        if (options.path.startsWith('/api/v1/page/json')) {
            return overrides?.page ?? pageJson();
        }
        if (/\/api\/v1\/page\/.+\/content$/.test(options.path)) {
            return overrides?.onPost?.(options.body, options.query) ?? { entity: [] };
        }
        return {};
    });
    const loadContext = jest.fn(async () => ({
        contentTypes: [],
        sites: [],
        languages: [],
        currentUser: null
    }));
    return { runtime: { request, loadContext } as unknown as DotCMSRuntime, calls };
}

/** Pull the POSTed body (the full container array) out of the recorded calls. */
function postedBody(calls: Array<{ path: string; body?: unknown }>): PostedEntry[] {
    const post = calls.find((c) => /\/api\/v1\/page\/.+\/content$/.test(c.path));
    return (post?.body as PostedEntry[]) ?? [];
}

function slot(body: PostedEntry[], identifier: string, uuid: string): string[] {
    return body.find((e) => e.identifier === identifier && e.uuid === uuid)?.contentletsId ?? [];
}

describe('placeContent', () => {
    it('posts the COMPLETE container map, not just the touched slot', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: 1, contentlets: ['new-b'], op: 'append' }] // default container, uuid "1"
        });

        const body = postedBody(calls);
        // Every one of the page's three slots is present in the body.
        expect(body).toHaveLength(3);
        // Touched slot got the append (existing kept, new added, in order).
        expect(slot(body, DEFAULT_CONTAINER, '1')).toEqual(['existing-a', 'new-b']);
        // Untouched slots preserved verbatim — this is the anti-wipe guarantee.
        expect(slot(body, DEFAULT_CONTAINER, '2')).toEqual([]);
        expect(slot(body, SYSTEM_CONTAINER, '1')).toEqual(['sys-x']);
    });

    it('op "set" replaces the slot content exactly', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: 1, contentlets: ['only-this'], op: 'set' }]
        });

        expect(slot(postedBody(calls), DEFAULT_CONTAINER, '1')).toEqual(['only-this']);
    });

    it('op "set" with [] clears a slot but leaves others intact', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: 1, contentlets: [], op: 'set' }]
        });

        const body = postedBody(calls);
        expect(slot(body, DEFAULT_CONTAINER, '1')).toEqual([]);
        expect(slot(body, SYSTEM_CONTAINER, '1')).toEqual(['sys-x']);
    });

    it('op "remove" removes only the named ids', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: 1, contentlets: ['existing-a'], op: 'remove' }]
        });

        expect(slot(postedBody(calls), DEFAULT_CONTAINER, '1')).toEqual([]);
    });

    it('append de-duplicates ids already in the slot', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: 1, contentlets: ['existing-a', 'new-b'], op: 'append' }]
        });

        expect(slot(postedBody(calls), DEFAULT_CONTAINER, '1')).toEqual(['existing-a', 'new-b']);
    });

    it('defaults op to append when omitted', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: 1, contentlets: ['new-b'] }]
        });

        expect(slot(postedBody(calls), DEFAULT_CONTAINER, '1')).toEqual(['existing-a', 'new-b']);
    });

    it('addresses a slot by container + instance uuid', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: { container: 'default', instance: '2' }, contentlets: ['new-c'] }]
        });

        expect(slot(postedBody(calls), DEFAULT_CONTAINER, '2')).toEqual(['new-c']);
        // Sibling instance "1" of the same container is untouched.
        expect(slot(postedBody(calls), DEFAULT_CONTAINER, '1')).toEqual(['existing-a']);
    });

    it('errors (before any write) when a container instance is ambiguous', async () => {
        const { runtime, calls } = fakeRuntime();

        await expect(
            placeContent({
                dotcms: runtime,
                path: '/about-us',
                // 'default' appears in uuid "1" and "2" — ambiguous without an instance.
                slots: [{ slot: { container: 'default' }, contentlets: ['x'] }]
            })
        ).rejects.toThrow(/appears in 2 slots.*instances.*Pass slot\.instance/i);

        expect(calls.some((c) => /\/content$/.test(c.path))).toBe(false);
    });

    it('errors (before any write) for an out-of-range slot index, listing valid slots', async () => {
        const { runtime, calls } = fakeRuntime();

        await expect(
            placeContent({
                dotcms: runtime,
                path: '/about-us',
                slots: [{ slot: 9, contentlets: ['x'] }]
            })
        ).rejects.toThrow(/out of range.*3 slot/i);

        expect(calls.some((c) => /\/content$/.test(c.path))).toBe(false);
    });

    it('errors (before any write) for an unknown container', async () => {
        const { runtime, calls } = fakeRuntime();

        await expect(
            placeContent({
                dotcms: runtime,
                path: '/about-us',
                slots: [{ slot: { container: 'nope-not-here' }, contentlets: ['x'] }]
            })
        ).rejects.toThrow(/No slot.*uses container/i);

        expect(calls.some((c) => /\/content$/.test(c.path))).toBe(false);
    });

    it('applies multiple slot assignments in one write', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [
                { slot: 1, contentlets: ['a2'], op: 'append' },
                { slot: { container: 'default', instance: '2' }, contentlets: ['b1'], op: 'set' }
            ]
        });

        const body = postedBody(calls);
        expect(slot(body, DEFAULT_CONTAINER, '1')).toEqual(['existing-a', 'a2']);
        expect(slot(body, DEFAULT_CONTAINER, '2')).toEqual(['b1']);
        // Untouched system slot preserved.
        expect(slot(body, SYSTEM_CONTAINER, '1')).toEqual(['sys-x']);
    });

    it('mode "replace" clears every slot the caller does not set', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: 1, contentlets: ['only-a'], op: 'set' }],
            mode: 'replace'
        });

        const body = postedBody(calls);
        expect(slot(body, DEFAULT_CONTAINER, '1')).toEqual(['only-a']);
        // Replace wipes the rest.
        expect(slot(body, DEFAULT_CONTAINER, '2')).toEqual([]);
        expect(slot(body, SYSTEM_CONTAINER, '1')).toEqual([]);
    });

    it('reports a before/after diff and flags a slot that lost content', async () => {
        const { runtime } = fakeRuntime();

        const manifest = await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: 1, contentlets: [], op: 'set' }] // clears slot #1
        });

        const slot1 = manifest.slots.find(
            (s) => s.uuid === '1' && s.identifier === DEFAULT_CONTAINER
        );
        expect(slot1).toMatchObject({ before: ['existing-a'], after: [], changed: true });
        expect(manifest.warnings.some((w) => /lost 1 contentlet.*existing-a/i.test(w))).toBe(true);
    });

    it('rejects an empty slots array', async () => {
        const { runtime, calls } = fakeRuntime();

        await expect(
            placeContent({ dotcms: runtime, path: '/about-us', slots: [] })
        ).rejects.toThrow(/`slots` is required.*at least one/i);

        expect(calls.some((c) => /\/content$/.test(c.path))).toBe(false);
    });

    it('throws a clear error when the page is not found', async () => {
        const { runtime } = fakeRuntime({ page: { entity: {} } });

        await expect(
            placeContent({
                dotcms: runtime,
                path: '/nope',
                slots: [{ slot: 1, contentlets: ['a'] }]
            })
        ).rejects.toThrow(/not found/i);
    });

    it('translates a net-loss 409 into an actionable message', async () => {
        const { runtime } = fakeRuntime({
            onPost: () => {
                throw new HttpError(409, 'Conflict', 'net content loss exceeds threshold');
            }
        });

        await expect(
            placeContent({
                dotcms: runtime,
                path: '/about-us',
                slots: [{ slot: 1, contentlets: ['a'] }]
            })
        ).rejects.toThrow(/net-loss conflict.*Re-read the page and retry/i);
    });

    it('names the likely cause on a 400 instead of relaying the raw error', async () => {
        // The most common failure of this tool in a placement loop. Without naming it, the
        // model cannot tell that the fix is a different container or a different content
        // type, so it retries the identical call and fails identically.
        const { runtime } = fakeRuntime({
            onPost: () => {
                throw new HttpError(400, 'Bad Request', 'invalid contentlet for container');
            }
        });

        await expect(
            placeContent({
                dotcms: runtime,
                path: '/about-us',
                slots: [{ slot: 1, contentlets: ['a'] }]
            })
        ).rejects.toThrow(/CONTENT TYPE is not permitted in the container/i);
    });

    it('does not report a non-409 as a conflict just because its body says "conflict"', async () => {
        // The old test was a regex over the message, so any body containing "conflict" (or the
        // digits 409 anywhere) was reported as a net-loss conflict — telling the model to
        // re-read and retry a page that in fact hit a server error.
        const { runtime } = fakeRuntime({
            onPost: () => {
                throw new HttpError(500, 'Server Error', 'unexpected conflict in module 409x');
            }
        });

        await expect(
            placeContent({
                dotcms: runtime,
                path: '/about-us',
                slots: [{ slot: 1, contentlets: ['a'] }]
            })
        ).rejects.toThrow(/Failed to save page content/i);
    });

    it('passes variantName and languageId through to both the read and the write', async () => {
        const { runtime, calls } = fakeRuntime();

        await placeContent({
            dotcms: runtime,
            path: '/about-us',
            slots: [{ slot: 1, contentlets: ['a'] }],
            variantName: 'my-variant',
            languageId: 2
        });

        const read = calls.find((c) => c.path.startsWith('/api/v1/page/json'));
        const write = calls.find((c) => /\/content$/.test(c.path));
        expect((read?.query as Record<string, unknown>)?.language_id).toBe(2);
        expect((write?.query as Record<string, unknown>)?.variantName).toBe('my-variant');
        expect((write?.query as Record<string, unknown>)?.language_id).toBe(2);
    });
});
