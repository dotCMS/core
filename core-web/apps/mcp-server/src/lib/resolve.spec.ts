import { HttpError, type DotCMSRuntime, type RequestOptions } from '@dotcms/ai/runtime';

import { resolveLanguageId, resolveSite } from './resolve';

const DEMO_SITE = {
    identifier: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    hostname: 'demo.dotcms.com',
    isDefault: true,
    archived: false,
    live: true
};

/**
 * A runtime whose context cache can be empty — the reported failure mode. Every loader inside
 * `loadContext()` swallows its own error and returns `[]`, so one bad response during the
 * single per-session load leaves the cache empty for the whole session.
 */
function fakeRuntime(options?: {
    cachedSites?: unknown[];
    cachedLanguages?: unknown[];
    onRequest?: (options: RequestOptions) => unknown;
    contextThrows?: boolean;
}) {
    const calls: RequestOptions[] = [];
    const request = jest.fn(async (opts: RequestOptions) => {
        calls.push(opts);

        return options?.onRequest ? options.onRequest(opts) : {};
    });
    const loadContext = jest.fn(async () => {
        if (options?.contextThrows) {
            throw new Error('context boom');
        }

        return {
            contentTypes: [],
            sites: options?.cachedSites ?? [],
            languages: options?.cachedLanguages ?? [],
            currentUser: null
        };
    });

    return { runtime: { request, loadContext } as unknown as DotCMSRuntime, calls };
}

describe('resolveSite', () => {
    it('uses the cache when it has the site, without any extra request', async () => {
        const { runtime, calls } = fakeRuntime({ cachedSites: [DEMO_SITE] });

        const site = await resolveSite(runtime, 'demo.dotcms.com');

        expect(site).toEqual({ identifier: DEMO_SITE.identifier, hostname: 'demo.dotcms.com' });
        expect(calls).toHaveLength(0);
    });

    describe('when the session cache is empty (the reported failure)', () => {
        // Reported from a real session: page_create / page_place_content / page_verify all
        // failed with "Available sites: (none found)" — for the default site, and even when
        // passed a correct site identifier. An empty cache is not evidence a site is missing.
        it('resolves an identifier directly instead of rejecting it', async () => {
            const { runtime, calls } = fakeRuntime({
                cachedSites: [],
                onRequest: (opts) =>
                    opts.path === `/api/v1/site/${DEMO_SITE.identifier}`
                        ? { entity: DEMO_SITE }
                        : {}
            });

            const site = await resolveSite(runtime, DEMO_SITE.identifier);

            expect(site.identifier).toBe(DEMO_SITE.identifier);
            expect(site.hostname).toBe('demo.dotcms.com');
            expect(calls[0].path).toBe(`/api/v1/site/${DEMO_SITE.identifier}`);
        });

        it('resolves a hostname via the filtered site list', async () => {
            const { runtime } = fakeRuntime({
                cachedSites: [],
                onRequest: (opts) => (opts.path === '/api/v1/site' ? { entity: [DEMO_SITE] } : {})
            });

            const site = await resolveSite(runtime, 'demo.dotcms.com');

            expect(site.identifier).toBe(DEMO_SITE.identifier);
        });

        it('still resolves when loadContext itself throws', async () => {
            const { runtime } = fakeRuntime({
                contextThrows: true,
                onRequest: (opts) => (opts.path === '/api/v1/site' ? { entity: [DEMO_SITE] } : {})
            });

            await expect(resolveSite(runtime, 'demo.dotcms.com')).resolves.toEqual({
                identifier: DEMO_SITE.identifier,
                hostname: 'demo.dotcms.com'
            });
        });

        it('explains that the context did not load rather than claiming no sites exist', async () => {
            const { runtime } = fakeRuntime({ cachedSites: [], onRequest: () => ({}) });

            await expect(resolveSite(runtime, 'ghost.example.com')).rejects.toThrow(
                /site list is empty[\s\S]*context load failed/i
            );
        });
    });

    it('does not accept a substring near-miss from the filtered list', async () => {
        // The list endpoint filters by substring, so `demo` also returns `demo-backup`.
        // Accepting a near-miss would write content to the wrong site.
        const { runtime } = fakeRuntime({
            cachedSites: [],
            onRequest: (opts) =>
                opts.path === '/api/v1/site'
                    ? { entity: [{ identifier: 'other-id', hostname: 'demo-backup.dotcms.com' }] }
                    : {}
        });

        await expect(resolveSite(runtime, 'demo.dotcms.com')).rejects.toThrow(/not found|empty/i);
    });

    it('reports a genuine miss with the sites it does know about', async () => {
        const { runtime } = fakeRuntime({ cachedSites: [DEMO_SITE], onRequest: () => ({}) });

        await expect(resolveSite(runtime, 'ghost.example.com')).rejects.toThrow(
            /Available sites: demo\.dotcms\.com/
        );
    });

    it('rethrows a non-404 lookup failure instead of reporting "not found"', async () => {
        // A 500 while looking up a site says nothing about whether that site exists.
        const { runtime } = fakeRuntime({
            cachedSites: [],
            onRequest: () => {
                throw new HttpError(500, 'Server Error', 'boom');
            }
        });

        await expect(resolveSite(runtime, DEMO_SITE.identifier)).rejects.toThrow(/500|boom/i);
    });
});

describe('resolveLanguageId', () => {
    const EN = { id: 1, isoCode: 'en-us' };
    const ES = { id: 2, isoCode: 'es-es' };

    it('accepts an id present in the cache', async () => {
        const { runtime } = fakeRuntime({ cachedLanguages: [EN, ES] });
        await expect(resolveLanguageId(runtime, 2)).resolves.toBe(2);
    });

    it('rejects an id the instance does not have', async () => {
        // dotCMS silently falls back to the default language rather than rejecting, so an
        // unknown id would write to the WRONG language while the manifest echoed the id asked for.
        const { runtime } = fakeRuntime({ cachedLanguages: [EN, ES] });
        await expect(resolveLanguageId(runtime, 12)).rejects.toThrow(/does not exist/i);
    });

    it('trusts the caller when the language list could not be loaded', async () => {
        // Same rule as sites: an empty list is a failed load, not proof of absence. Refusing
        // here would block every call for the rest of the session.
        const { runtime } = fakeRuntime({ cachedLanguages: [], onRequest: () => ({}) });
        await expect(resolveLanguageId(runtime, 12)).resolves.toBe(12);
    });

    it('falls back to a live read when the cache is empty', async () => {
        const { runtime } = fakeRuntime({
            cachedLanguages: [],
            onRequest: (opts) => (opts.path === '/api/v2/languages' ? { entity: [EN, ES] } : {})
        });

        await expect(resolveLanguageId(runtime, 12)).rejects.toThrow(/does not exist/i);
        await expect(resolveLanguageId(runtime, 2)).resolves.toBe(2);
    });

    it("defaults to the instance's first language rather than a hardcoded 1", async () => {
        const { runtime } = fakeRuntime({ cachedLanguages: [{ id: 7, isoCode: 'fr-fr' }] });
        await expect(resolveLanguageId(runtime, undefined)).resolves.toBe(7);
    });
});
