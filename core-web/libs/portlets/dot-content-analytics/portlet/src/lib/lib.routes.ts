import { Route } from '@angular/router';

import { DotEnterpriseLicenseResolver, DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { DotContentAnalyticsComponent } from './dot-content-analytics/dot-content-analytics.component';

export const DotContentAnalyticsRoutes: Route[] = [
    {
        path: '',
        component: DotContentAnalyticsComponent,
        providers: [DotPushPublishEnvironmentsResolver, DotEnterpriseLicenseResolver],
        resolve: {
            pushPublishEnvironments: DotPushPublishEnvironmentsResolver,
            isEnterprise: DotEnterpriseLicenseResolver
        }
    }
];
