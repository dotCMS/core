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
