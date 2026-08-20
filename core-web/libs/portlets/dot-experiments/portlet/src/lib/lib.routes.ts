import { Routes } from '@angular/router';

import { DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

/**
 * Routes for the Experiments portlet (registered under `/experiments`).
 *
 * This is the portlet that replaces the per-page UVE experiments screens, which live on
 * under `./old/` and keep serving `dotExperimentsRoutes` unchanged until they are retired.
 *
 * Only the list route is wired today. The `new`, `:id/configuration` and `:id/results`
 * screens are delivered by follow-up issues (#36990+) and are intentionally absent so
 * the router surfaces an honest 404 instead of falling back to the legacy UVE screens.
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
    }
];
