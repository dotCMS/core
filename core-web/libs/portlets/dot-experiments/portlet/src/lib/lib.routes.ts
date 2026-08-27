import { Routes, UrlMatchResult, UrlSegment } from '@angular/router';

import { ExperimentsConfigProperties } from '@dotcms/dotcms-models';
import { DotExperimentsConfigResolver } from '@dotcms/portlets/dot-experiments/data-access';
import { DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { experimentsUnsavedChangesGuard } from './guards/unsaved-changes.guard';
import { CONFIGURATION_SEGMENT, NEW_EXPERIMENT_SEGMENT } from './shared/constants';

/**
 * Matches the two URLs the Configure screen answers on: `new` and `:experimentId/configuration`.
 *
 * They are deliberately **one** route config rather than two. Creating a draft swaps
 * `/experiments/new` for `/experiments/:experimentId/configuration` with `replaceUrl`, and the
 * router reuses a component only while the route config stays the same — two configs would tear
 * the shell (and with it the store) down in the middle of that swap, dropping the unsaved form
 * and the just-created experiment held in memory. With a single config only the params
 * change, which is exactly what the store expects: it reads the route once on init and owns the
 * experiment from then on.
 *
 * `experimentId` is exposed as a positional parameter on the second form only — its absence is
 * what tells the store it is on the creation screen.
 *
 * @param segments - Segments left to match under `/experiments`
 * @returns The consumed segments (plus `experimentId` when present), or `null` to let the list
 * route and any future sibling — `:experimentId/reports` — match instead
 */
export function experimentsConfigureMatcher(segments: UrlSegment[]): UrlMatchResult | null {
    if (segments.length === 1 && segments[0].path === NEW_EXPERIMENT_SEGMENT) {
        return { consumed: segments };
    }

    if (segments.length === 2 && segments[1].path === CONFIGURATION_SEGMENT) {
        return { consumed: segments, posParams: { experimentId: segments[0] } };
    }

    return null;
}

/**
 * Routes for the Experiments portlet (registered under `/experiments`).
 *
 * This is the portlet that replaces the per-page UVE experiments screens, which live on
 * under `./old/` and keep serving `dotExperimentsRoutes` unchanged until they are retired.
 *
 * The list and the Configure screen are wired. `:id/results` is delivered by a follow-up issue
 * and is intentionally absent so the router surfaces an honest 404 instead of falling back to
 * the legacy UVE screens.
 */
export const dotExperimentsPortletRoutes: Routes = [
    {
        path: '',
        title: 'experiment.container.list.title',
        // `DotPushPublishEnvironmentsResolver` is `@Injectable()` without `providedIn: 'root'`,
        // so referencing it in `resolve` is not enough — it has to be provided on the route or
        // the router throws NG0201 on activation.
        providers: [DotPushPublishEnvironmentsResolver],
        resolve: {
            pushPublishEnvironments: DotPushPublishEnvironmentsResolver
        },
        loadComponent: () =>
            import('./dot-experiments-list/dot-experiments-list.component').then(
                (m) => m.DotExperimentsListComponent
            )
    },
    {
        matcher: experimentsConfigureMatcher,
        title: 'experiment.container.configuration.title',
        // The screen only writes when Save Draft is pressed, so leaving it is the moment unsaved
        // work is lost. Does not fire on the `new` → `:experimentId/configuration` swap, which is
        // a reused route — and which follows a successful save anyway.
        canDeactivate: [experimentsUnsavedChangesGuard],
        // Both resolvers are `@Injectable()` without `providedIn: 'root'` — see the list route.
        providers: [DotExperimentsConfigResolver, DotPushPublishEnvironmentsResolver],
        resolve: {
            config: DotExperimentsConfigResolver,
            // The header's kebab offers Push Publish, which needs the environments the same way
            // the list's row menu does.
            pushPublishEnvironments: DotPushPublishEnvironmentsResolver
        },
        data: {
            // Read by `DotExperimentsConfigResolver` to bound the Scheduling card's dates.
            experimentsConfigProps: [
                ExperimentsConfigProperties.EXPERIMENTS_MIN_DURATION,
                ExperimentsConfigProperties.EXPERIMENTS_MAX_DURATION
            ],
            // Route `data` is inherited, so the mount point in `app.routes.ts` must not force
            // `reuseRoute: false` on this subtree: that would recreate the shell on the
            // `new → :experimentId/configuration` swap the matcher above exists to survive.
            reuseRoute: true
        },
        loadComponent: () =>
            import('./dot-experiments-configure/dot-experiments-configure.component').then(
                (m) => m.DotExperimentsConfigureComponent
            )
    }
];
