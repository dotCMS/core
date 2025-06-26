import { Route } from '@angular/router';

export const DotAnalyticsRoutes: Route[] = [
    {
        path: 'search',
        loadComponent: () =>
            import('./lib/dot-analytics-search/dot-analytics-search.component')
    },
    {
        path: 'dashboard',
        loadComponent: () =>
            import('./lib/dot-analytics-dashboard/dot-analytics-dashboard.component')
    }
];
