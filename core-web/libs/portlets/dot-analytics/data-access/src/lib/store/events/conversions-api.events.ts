import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import {
    ContentAttributionEntity,
    ConversionsOverviewEntity,
    ConvertingVisitorsEntity,
    TimeRangeInput,
    TotalConversionsEntity
} from '../../types';
import {
    ConversionTrendEntity,
    TrafficVsConversionsEntity
} from '../../utils/data/analytics-data.utils';

/**
 * Lifecycle events for each conversions metric: `*Requested` → `*Loaded` | `*Failed`.
 *
 * Six metrics: totalConversions, conversionTrend, convertingVisitors,
 * trafficVsConversions, contentConversions, conversionsOverview.
 * (The orphan `conversionRate` state slice is dropped during the migration —
 * no loader exists for it and consumers compute the rate locally.)
 */
export const conversionsApiEvents = eventGroup({
    source: 'Analytics Conversions API',
    events: {
        // Total conversions
        totalConversionsRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        totalConversionsLoaded: type<{ data: TotalConversionsEntity }>(),
        totalConversionsFailed: type<{ error: string }>(),

        // Conversion trend (line chart over time)
        conversionTrendRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        conversionTrendLoaded: type<{ data: ConversionTrendEntity[] }>(),
        conversionTrendFailed: type<{ error: string }>(),

        // Converting visitors (unique + unique converting)
        convertingVisitorsRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        convertingVisitorsLoaded: type<{ data: ConvertingVisitorsEntity }>(),
        convertingVisitorsFailed: type<{ error: string }>(),

        // Traffic vs conversions (combo chart)
        trafficVsConversionsRequested: type<{
            timeRange: TimeRangeInput;
            currentSiteId: string;
        }>(),
        trafficVsConversionsLoaded: type<{ data: TrafficVsConversionsEntity[] }>(),
        trafficVsConversionsFailed: type<{ error: string }>(),

        // Content conversions table (per-content attribution)
        contentConversionsRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        contentConversionsLoaded: type<{ data: ContentAttributionEntity[] }>(),
        contentConversionsFailed: type<{ error: string }>(),

        // Conversions overview table (per-conversion-name with attributed content)
        conversionsOverviewRequested: type<{ timeRange: TimeRangeInput; currentSiteId: string }>(),
        conversionsOverviewLoaded: type<{ data: ConversionsOverviewEntity[] }>(),
        conversionsOverviewFailed: type<{ error: string }>()
    }
});
