import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { ChartData, TimeRangeInput } from '../../types';
import {
    EngagementKPIs,
    EngagementPlatforms,
    EngagementSparklineData
} from '../../types/engagement.types';

/**
 * Lifecycle events for each engagement metric: `*Requested` → `*Loaded` | `*Failed`.
 *
 * Three of the four handlers run multiple parallel queries via `forkJoin`
 * (current+previous period for KPIs and sparkline; device+browser+language
 * for platforms) and dispatch a single `*Loaded` event with the merged
 * payload. Reducers transition the matching state slice through
 * LOADING / LOADED / ERROR.
 */
export const engagementApiEvents = eventGroup({
    source: 'Analytics Engagement API',
    events: {
        // Engagement KPIs (forkJoin: current + previous period → merged EngagementKPIs)
        engagementKpisRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        engagementKpisLoaded: type<{ data: EngagementKPIs }>(),
        engagementKpisFailed: type<{ error: string }>(),

        // Engagement breakdown doughnut (engaged vs bounced) — single query
        engagementBreakdownRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        engagementBreakdownLoaded: type<{ data: ChartData }>(),
        engagementBreakdownFailed: type<{ error: string }>(),

        // Engagement sparkline (forkJoin: current + previous period)
        engagementSparklineRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        engagementSparklineLoaded: type<{ data: EngagementSparklineData }>(),
        engagementSparklineFailed: type<{ error: string }>(),

        // Engagement platforms (forkJoin: device + browser + language)
        engagementPlatformsRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        engagementPlatformsLoaded: type<{ data: EngagementPlatforms }>(),
        engagementPlatformsFailed: type<{ error: string }>()
    }
});
