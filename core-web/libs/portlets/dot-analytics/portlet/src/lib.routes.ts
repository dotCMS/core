import { Route } from '@angular/router';

import { DotAnalyticsDashboardStore } from '@dotcms/portlets/dot-analytics/data-access';

import { analyticsHealthGuard } from './lib/guards/analytics-health.guard';

export const DotAnalyticsRoutes: Route[] = [
    {
        path: 'error',
        title: 'analytics.error.title',
        loadComponent: () =>
            import('./lib/dot-analytics-error/dot-analytics-error.component').then((m) => m.default)
    },
    {
        path: 'search',
        title: 'analytics.search.title',
        canMatch: [analyticsHealthGuard],
        loadComponent: () =>
            import('./lib/dot-analytics-search/dot-analytics-search.component').then(
                (m) => m.default
            )
    },
    {
        path: 'dashboard',
        title: 'analytics.dashboard.title',
        canMatch: [analyticsHealthGuard],
        providers: [DotAnalyticsDashboardStore],
        loadComponent: () =>
            import('./lib/dot-analytics-dashboard/dot-analytics-dashboard.component').then(
                (m) => m.default
            )
    },
    {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
    }
];
