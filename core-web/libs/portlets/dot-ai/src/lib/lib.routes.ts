import { Route } from '@angular/router';

import { DOT_AI_TABS } from './models/dot-ai-portlet.models';

/**
 * Routes for the dotAI portlet.
 *
 * Every tab is its own route so it is bookmarkable and shareable (FR-005). The tab list is
 * generated from `DOT_AI_TABS`, so the tab bar and the routes cannot drift apart.
 *
 * Mounted in `app.routes.ts` under the path `dotai`, which must match the portlet id exactly:
 * `DotRouterService.getPortletId` takes the first URL segment after filtering `''`/`'#'`/`'c'`
 * and matches it against `/api/v1/menu`.
 */
export const dotAiRoutes: Route[] = [
    {
        path: '',
        loadComponent: () => import('./dot-ai-shell/dot-ai-shell.component'),
        children: [
            {
                path: '',
                redirectTo: DOT_AI_TABS[0].id,
                pathMatch: 'full'
            },
            // Placeholders — each is replaced by its real tab component as that tab lands.
            ...DOT_AI_TABS.map(
                (tab): Route => ({
                    path: tab.id,
                    data: { tab },
                    loadComponent: () =>
                        import('./dot-ai-tab-placeholder/dot-ai-tab-placeholder.component')
                })
            )
        ]
    }
];
