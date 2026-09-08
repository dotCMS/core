import { Route, Routes } from '@angular/router';

import { MenuGuardService } from './api/services/guards/menu-guard.service';
import { appRoutes } from './app.routes';

/**
 * Guards the portlet route registration. `MenuGuardService` validates the FIRST url segment
 * against `/api/v1/menu`, so a portlet whose path drifts from the menu entry silently stops
 * resolving — a failure that is invisible until someone opens the portlet.
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

        it('should be guarded by MenuGuardService on activation and child activation', () => {
            expect(experimentsRoutes[0].canActivate).toContain(MenuGuardService);
            expect(experimentsRoutes[0].canActivateChild).toContain(MenuGuardService);
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
