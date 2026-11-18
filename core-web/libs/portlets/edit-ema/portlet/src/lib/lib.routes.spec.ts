import { Route, Routes } from '@angular/router';

import { dotEmaRoutes } from './lib.routes';

/**
 * Guards the coexistence half of #37005 (FR-019, FR-025).
 *
 * The entry-point switch selects which experiments experience the UVE Experiments navigation item
 * leads to; it does **not** remove either set of screens. So the legacy per-page experiments routes
 * have to stay mounted under `edit-page/experiments` for as long as the switch exists — retiring
 * them is #37008's job, not this work's.
 *
 * Why assert it here rather than trust it: nothing else fails if this mount disappears. With the
 * switch off, the navigation item would route to a path with no route behind it, and the symptom
 * would be a blank screen on the default configuration — the exact regression the switch exists to
 * prevent.
 */
describe('dotEmaRoutes', () => {
    const flatten = (routes: Routes): Route[] =>
        routes.flatMap((route) => [route, ...flatten(route.children ?? [])]);

    const allRoutes = flatten(dotEmaRoutes);

    describe('legacy per-page experiments screens', () => {
        const experimentsRoutes = allRoutes.filter((route) => route.path === 'experiments');

        it('should still be mounted exactly once under edit-page', () => {
            expect(experimentsRoutes).toHaveLength(1);
        });

        it('should still lazily load the legacy route array', () => {
            // `dotExperimentsRoutes` from `./old/` — distinct from the new portlet's
            // `dotExperimentsPortletRoutes`, which is mounted at `/experiments` by app.routes.ts.
            // Both are present in the same build (FR-025).
            expect(experimentsRoutes[0].loadChildren).toBeDefined();
        });
    });

    describe('sibling routes the navigation bar depends on', () => {
        // `EditEmaNavigationBarComponent.navigate()` prefixes `edit-page` for every relative href,
        // so each of these has to exist under this route array or the item leads nowhere. They are
        // listed because #37005 makes that prefix conditional, and a mistake there would show up
        // as one of these no longer resolving.
        it.each(['content', 'layout'])('should keep the `%s` route', (path) => {
            expect(allRoutes.some((route) => route.path === path)).toBe(true);
        });

        it('should keep the parameterised rules route', () => {
            expect(allRoutes.some((route) => route.path === 'rules/:pageId')).toBe(true);
        });
    });
});
