import { Route } from '@angular/router';

import DotAnalyticsDashboardComponent from './dot-analytics-dashboard/dot-analytics-dashboard.component';
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
        canActivate: [analyticsHealthGuard],
        loadComponent: () => import('./dot-analytics-search/dot-analytics-search.component')
    },
    {
        path: 'dashboard',
        canActivate: [analyticsHealthGuard],
        component: DotAnalyticsDashboardComponent,
        children: [
            {
                // Default landing tab — Engagement, per product decision (was Pageview).
                path: '',
                redirectTo: 'engagement',
                pathMatch: 'full'
            },
            {
                path: 'pageview',
                loadComponent: () =>
                    import('./dot-analytics-dashboard/reports/pageview/dot-analytics-pageview-report/dot-analytics-pageview-report.component')
            },
            {
                path: 'conversions',
                loadComponent: () =>
                    import('./dot-analytics-dashboard/reports/conversions/dot-analytics-conversions-report/dot-analytics-conversions-report.component')
            },
            {
                path: 'engagement',
                loadComponent: () =>
                    import('./dot-analytics-dashboard/reports/engagement/dot-analytics-engagement-report/dot-analytics-engagement-report.component')
            }
        ]
    },
    {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
    }
];
