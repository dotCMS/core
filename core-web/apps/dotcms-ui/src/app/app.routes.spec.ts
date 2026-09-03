import { Route, Routes } from '@angular/router';

import { appRoutes } from './app.routes';

/**
 * Guards the portlet route registration.
 *
 * The path itself still matters: `MenuGuardService`, where a route uses it, validates the FIRST
 * url segment against `/api/v1/menu`, so a path that drifts from its menu entry silently stops
 * resolving. `/experiments` deliberately does not use that guard — see the test below.
 */
describe('appRoutes', () => {
    const flatten = (routes: Routes): Route[] =>
        routes.flatMap((route) => [route, ...flatten(route.children ?? [])]);

    const allRoutes = flatten(appRoutes);

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
});
