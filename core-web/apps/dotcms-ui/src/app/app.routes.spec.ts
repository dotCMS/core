import { Route, Routes } from '@angular/router';

import { MenuGuardService } from './api/services/guards/menu-guard.service';
import { appRoutes } from './app.routes';

/**
 * Guards the portlet route registration.
 *
 * The path always matters: `MenuGuardService`, on the routes that use it, validates the FIRST url
 * segment against `/api/v1/menu`, so a path that drifts from its menu entry silently stops
 * resolving — invisible until someone opens the portlet.
 *
 * Whether a route *should* carry that guard is per-portlet, and the two blocks below assert
 * opposite answers on purpose. `dotai` is registered in the menu, so the guard is what keeps its
 * path honest. `/experiments` is opt-in and legitimately absent from the menu on most instances,
 * so the same guard would reject it — see each test for the reasoning.
 */
describe('appRoutes', () => {
    const flatten = (routes: Routes): Route[] =>
        routes.flatMap((route) => [route, ...flatten(route.children ?? [])]);

    const allRoutes = flatten(appRoutes);

    describe('dotAI portlet', () => {
        const dotAiRoutes = allRoutes.filter((route) => route.path === 'dotai');

        it('should be registered exactly once', () => {
            expect(dotAiRoutes).toHaveLength(1);
        });

        it('should use `dotai` as its whole first segment', () => {
            // Must match the portlet id in /api/v1/menu exactly, or MenuGuardService rejects it.
            expect(dotAiRoutes[0].path).toBe('dotai');
        });

        it('should be guarded by MenuGuardService on activation and child activation', () => {
            expect(dotAiRoutes[0].canActivate).toContain(MenuGuardService);
            expect(dotAiRoutes[0].canActivateChild).toContain(MenuGuardService);
        });

        it('should not force `reuseRoute: false` on its subtree', () => {
            // Route `data` is inherited, and DotCustomReuseStrategyService returns
            // `data.reuseRoute !== false` — so this flag makes every tab change destroy and
            // recreate the shell AND the store hung off it. Measured before removing it:
            // three tab switches fired three /ai/completions/config requests, the chat draft
            // was lost on a round trip, and the not-configured banner flashed each time
            // because `isConfigured` reset to false while the config reloaded.
            expect(dotAiRoutes[0].data?.['reuseRoute']).toBeUndefined();
        });
    });

    describe('experiments portlet', () => {
        const experimentsRoutes = allRoutes.filter((route) => route.path === 'experiments');

        it('should be registered exactly once', () => {
            expect(experimentsRoutes).toHaveLength(1);
        });

        it('should use `experiments` as its whole first segment', () => {
            // MenuGuardService compares this against the menu entry, so it cannot be
            // `experiments/something` nor carry a prefix.
            expect(experimentsRoutes[0].path).toBe('experiments');
        });

        it('should NOT be guarded by MenuGuardService', () => {
            // The portlet is opt-in — declared in portlet.xml, added to a layout only by an
            // operator who wants it — so it is legitimately absent from `/api/v1/menu` on most
            // instances. The guard matches the first URL segment against that menu, so it turned
            // "nobody registered the portlet" into "the UVE Experiments item ejects the editor out
            // of UVE to the first portlet" (#37005). `/analytics` carries no guard for the same
            // reason and is the precedent followed here.
            expect(experimentsRoutes[0].canActivate).toBeUndefined();
            expect(experimentsRoutes[0].canActivateChild).toBeUndefined();
        });

        it('should not force `reuseRoute: false` on its subtree', () => {
            // Route `data` is inherited, so this flag would recreate the Configure shell — and
            // its store, mid-autosave — on the `new → :id/configuration` swap that follows
            // creating a draft. The portlet's own routes opt in per screen instead.
            expect(experimentsRoutes[0].data?.['reuseRoute']).toBeUndefined();
        });

        it('should lazily load the portlet route array', async () => {
            const loadChildren = experimentsRoutes[0].loadChildren;
            expect(loadChildren).toBeDefined();

            const loaded = await (loadChildren as () => Promise<Routes>)();

            expect(Array.isArray(loaded)).toBe(true);
            expect(loaded.some((route) => route.path === '')).toBe(true);
        });
    });

    describe('Query Tool portlet', () => {
        const queryToolRoutes = allRoutes.filter((route) => route.path === 'query-tool');

        it('should be registered exactly once', () => {
            expect(queryToolRoutes).toHaveLength(1);
        });

        it('should not force `reuseRoute: false` on its route', () => {
            // The route has no path params, so the flag only ever rebuilt the page on a
            // navigation to the Query Tool itself, which in practice is browser Back: the router
            // answers every Back with a navigation (`onSameUrlNavigation: 'reload'`). Rebuilt, the
            // page dropped the side panel open over it, its unsaved changes, and the URL the panel
            // service had just restored for its prompt (#38303, FR-047).
            expect(queryToolRoutes[0].data?.['reuseRoute']).toBeUndefined();
        });
    });

    describe('Publishing Queue portlet', () => {
        const publishingQueueRoutes = allRoutes.filter(
            (route) => route.path === 'publishing-queue-beta'
        );

        it('should be registered exactly once', () => {
            expect(publishingQueueRoutes).toHaveLength(1);
        });

        it('should not force `reuseRoute: false` on its route', () => {
            // Same rule as the Query Tool: no path params, so the flag only rebuilt the page on
            // browser Back, discarding the side panel open over the list and its prompt (#38303,
            // FR-047).
            expect(publishingQueueRoutes[0].data?.['reuseRoute']).toBeUndefined();
        });
    });

    /**
     * The path deep links redirect to the portlet's URL with the panel params (#38303, FR-049).
     * Velocity emits `/dotAdmin/#/c/content/<inode>` and workflow emails `/c/workflow/<task>`, so
     * the `/c/:id` children stay. `/pages/<inode>` and `/pages/new/<type>` were on main too, and
     * FR-026 covers every `/<portlet>/<inode>` link, so they stay as well (T158).
     */
    describe('deep links', () => {
        const legacyPortlet = allRoutes.find((route) => route.path === ':id' && route.children);

        it.each([':asset', 'new/:contentType', 'new'])(
            'should keep the `/c/:id/%s` child',
            (path) => {
                expect(legacyPortlet?.children?.map((child) => child.path)).toContain(path);
            }
        );

        /**
         * `/c/workflow/<task>` is a surface with a panel in its URL (FR-025a). Back with that panel
         * open is a query-only navigation, which `reuseRoute: false` would answer by rebuilding the
         * task dialog under the panel's prompt; the task follows its route's params instead.
         */
        it('should not force `reuseRoute: false` on the `/c/:id/:asset` child', () => {
            const asset = legacyPortlet?.children?.find((child) => child.path === ':asset');

            expect(asset).toBeDefined();
            expect(asset?.data?.['reuseRoute']).toBeUndefined();
        });

        it.each([':asset', 'new/:contentType'])(
            'should keep the `/pages/%s` child, redirecting through the deep-link component',
            async (path) => {
                const { dotPagesRoutes } = await import('./portlets/dot-pages/dot-pages.routes');
                const { DotEditContentDeepLinkComponent } =
                    await import('./portlets/dot-porlet-detail/dot-edit-content-deep-link/dot-edit-content-deep-link.component');

                expect(dotPagesRoutes).toHaveLength(1);
                const child = dotPagesRoutes[0].children?.find((route) => route.path === path);
                expect(child, `the /pages/${path} child`).toBeDefined();
                expect(await child?.loadComponent?.()).toBe(DotEditContentDeepLinkComponent);
            }
        );
    });
});
