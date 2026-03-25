import { Route } from '@angular/router';

import DotAnalyticsDashboardComponent from './dot-analytics-dashboard/dot-analytics-dashboard.component';
import { analyticsHealthGuard } from './guards/analytics-health.guard';

export const dotAnalyticsRoutes: Route[] = [
    {
        path: 'error',
        title: 'analytics.error.title',
        loadComponent: () =>
            import('./dot-analytics-error/dot-analytics-error.component').then((m) => m.default)
    },
    {
        path: 'search',
        title: 'analytics.search.title',
        canMatch: [analyticsHealthGuard],
        loadComponent: () =>
            import('./dot-analytics-search/dot-analytics-search.component').then((m) => m.default)
    },
    {
        path: 'dashboard',
        canMatch: [analyticsHealthGuard],
        component: DotAnalyticsDashboardComponent
    },
    {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
    }
];
