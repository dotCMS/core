import { Route } from '@angular/router';

import { DotAnalyticsDashboardStore } from '@dotcms/portlets/dot-analytics/data-access';

import DotAnalyticsDashboardComponent from './lib/dot-analytics-dashboard/dot-analytics-dashboard.component';
import { analyticsHealthGuard } from './lib/guards/analytics-health.guard';
import { dotAnalyticsEngagementResolver } from './lib/resolvers/dot-analytics-engagement.resolver';

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
        canMatch: [analyticsHealthGuard],
        providers: [DotAnalyticsDashboardStore],
        component: DotAnalyticsDashboardComponent,
        // TODO: Remove this resolver when the feature flag is removed
        resolve: {
            engagementEnabled: dotAnalyticsEngagementResolver
        }
    },
    {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
    }
];
