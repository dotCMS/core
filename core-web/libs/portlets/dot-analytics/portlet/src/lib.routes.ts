import { Route } from '@angular/router';

import { DotAnalyticsDashboardStore } from '@dotcms/portlets/dot-analytics/data-access';

export const DotAnalyticsRoutes: Route[] = [
    {
        path: 'search',
        loadComponent: () => import('./lib/dot-analytics-search/dot-analytics-search.component')
    },
    {
        path: 'dashboard',
        providers: [DotAnalyticsDashboardStore],
        loadComponent: () =>
            import('./lib/dot-analytics-dashboard/dot-analytics-dashboard.component')
    }
];
