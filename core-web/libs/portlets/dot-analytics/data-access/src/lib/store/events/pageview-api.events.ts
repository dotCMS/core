import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import {
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    TimeRangeInput,
    TopPagePerformanceEntity,
    TopPerformanceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../types';

/**
 * Lifecycle events for each pageview metric: `*Requested` → `*Loaded` | `*Failed`.
 *
 * `*Requested` events are dispatched by the autoload handler when filters
 * change; HTTP handlers in `with-pageview.feature.ts` listen and dispatch
 * `*Loaded` (success) or `*Failed` (error). Reducers transition the
 * matching state slice through LOADING / LOADED / ERROR.
 */
export const pageviewApiEvents = eventGroup({
    source: 'Analytics Pageview API',
    events: {
        // Total page views
        totalPageViewsRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        totalPageViewsLoaded: type<{ data: TotalPageViewsEntity }>(),
        totalPageViewsFailed: type<{ error: string }>(),

        // Unique visitors
        uniqueVisitorsRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        uniqueVisitorsLoaded: type<{ data: UniqueVisitorsEntity }>(),
        uniqueVisitorsFailed: type<{ error: string }>(),

        // Top page performance (single top page)
        topPagePerformanceRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        topPagePerformanceLoaded: type<{ data: TopPagePerformanceEntity }>(),
        topPagePerformanceFailed: type<{ error: string }>(),

        // Page view timeline (line chart)
        pageViewTimeLineRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        pageViewTimeLineLoaded: type<{ data: PageViewTimeLineEntity[] }>(),
        pageViewTimeLineFailed: type<{ error: string }>(),

        // Page view device/browser breakdown
        pageViewDeviceBrowsersRequested: type<{
            timeRange: TimeRangeInput;
            currentSiteId: string;
        }>(),
        pageViewDeviceBrowsersLoaded: type<{ data: PageViewDeviceBrowsersEntity[] }>(),
        pageViewDeviceBrowsersFailed: type<{ error: string }>(),

        // Top pages table
        topPagesTableRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        topPagesTableLoaded: type<{ data: TopPerformanceTableEntity[] }>(),
        topPagesTableFailed: type<{ error: string }>()
    }
});
