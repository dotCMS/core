import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';

export const ANALYTICS_SHOW_ENGAGEMENT_DASHBOARD_FLAG =
    'FEATURE_FLAG_ANALYTICS_SHOW_ENGAGEMENT_DASHBOARD';

/**
 * Resolve the engagement dashboard flag
 * @returns {Observable<boolean>}
 */
export const dotAnalyticsEngagementResolver: ResolveFn<boolean> = () => {
    return inject(DotPropertiesService)
        .getKey(ANALYTICS_SHOW_ENGAGEMENT_DASHBOARD_FLAG)
        .pipe(map((v) => v === 'true'));
};
