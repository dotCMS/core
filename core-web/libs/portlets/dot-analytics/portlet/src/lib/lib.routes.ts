import { Route } from '@angular/router';

import { analyticsHealthGuard } from './guards/analytics-health.guard';

export const dotAnalyticsRoutes: Route[] = [
    {
        path: 'error',
        title: 'analytics.error.title',
        loadComponent: () => import('./dot-analytics-error/dot-analytics-error.component')
    },
    {
        path: 'search',
        title: 'analytics.search.title',
        canMatch: [analyticsHealthGuard],
        loadComponent: () => import('./dot-analytics-search/dot-analytics-search.component')
    },
    {
        path: 'dashboard',
        canMatch: [analyticsHealthGuard],
        loadComponent: () => import('./dot-analytics-dashboard/dot-analytics-dashboard.component')
    },
    {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
    }
];
