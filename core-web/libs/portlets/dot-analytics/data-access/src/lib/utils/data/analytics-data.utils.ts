import {
    addDays,
    addHours,
    endOfDay,
    format,
    isSameDay,
    isSameMonth,
    parse,
    startOfDay,
    subDays
} from 'date-fns';

import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    AnalyticsChartColors,
    BAR_CHART_STYLE,
    TIME_RANGE_CUBEJS_MAPPING,
    TIME_RANGE_OPTIONS
} from '../../constants';
import {
    ChartData,
    ChartDataset,
    ContentAttributionEntity,
    Granularity,
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    RequestState,
    TablePageData,
    TimeRangeCubeJS,
    TimeRangeInput,
    TopPagePerformanceEntity,
    TopPerformanceTableEntity,
    TotalConversionsEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../types';
import { parseUserAgent } from '../browser/userAgentParser';

/**
 * Time formats for different chart types
 */
const TIME_FORMATS = {
    hour: 'HH:mm',
    day: 'MMM dd'
};

/**
 * Creates a typed initial request state.
 * Use this function when you need type-safe initialization of RequestState.
 *
 * @returns A new RequestState object with INIT status and null data/error
 */
export function createInitialRequestState<T>(): RequestState<T> {
    return {
        status: ComponentStatus.INIT,
        data: null,
        error: null
    };
}

/**
 * Helper functions to extract numeric values from analytics entities
 */

/**
 * Determines the appropriate granularity for analytics queries based on the time range.
 *
 * This utility centralizes the logic for selecting granularity levels to ensure
 * optimal data visualization and performance across different time periods.
 *
 * @param timeRange - The time range for the analytics query
 * @returns The appropriate granularity level for the given time range
 */
export function determineGranularityForTimeRange(timeRange: TimeRangeInput): Granularity {
    if (Array.isArray(timeRange)) {
        const [fromDate, toDate] = timeRange.map((date) => parse(date, 'yyyy-MM-dd', new Date()));

        if (isSameDay(fromDate, toDate)) {
            return 'hour';
        } else if (isSameMonth(fromDate, toDate)) {
            return 'day';
        } else {
            return 'month';
        }
    }

    switch (timeRange) {
        case TIME_RANGE_OPTIONS.today:

        // falls through
        case TIME_RANGE_OPTIONS.yesterday:
            // For today/yesterday, use hourly granularity for detailed intraday analysis
            return 'hour';

        case TIME_RANGE_OPTIONS.last7days:
            // For last 7 days, use daily granularity
            return 'day';

        case TIME_RANGE_OPTIONS.last30days:
            // For last 30 days, use daily granularity
            return 'day';

        default: {
            // For custom ranges or other periods, extract days and decide
            const daysMatch = timeRange.match(/from (\d+) days ago to now/);
            if (daysMatch) {
                const numDays = parseInt(daysMatch[1], 10);
                if (numDays > 90) {
                    return 'month';
                } else if (numDays > 30) {
                    return 'week';
                } else {
                    return 'day';
                }
            } else {
                // For custom date ranges, default to day
                return 'day';
            }
        }
    }
}

/**
 * Converts TimeRangeInput to TimeRangeCubeJS format for CubeJS queries.
 *
 * @param timeRange - The time range input (predefined option or custom date array)
 * @returns The CubeJS-compatible time range value
 */
export function toTimeRangeCubeJS(timeRange: TimeRangeInput): TimeRangeCubeJS {
    if (Array.isArray(timeRange)) {
        return timeRange;
    }

    return (
        TIME_RANGE_CUBEJS_MAPPING[timeRange as keyof typeof TIME_RANGE_CUBEJS_MAPPING] ||
        TIME_RANGE_CUBEJS_MAPPING.last7days
    );
}

/**
 * Extracts page views count from TotalPageViewsEntity
 */
export const extractPageViews = (data: TotalPageViewsEntity | null): number =>
    data ? Number(data['EventSummary.totalEvents'] ?? 0) : 0;

/**
 * Extracts unique sessions from UniqueVisitorsEntity
 */
export const extractSessions = (data: UniqueVisitorsEntity | null): number =>
    data ? Number(data['EventSummary.uniqueVisitors']) : 0;

/**
 * Extracts top page performance value from TopPagePerformanceEntity
 */
export const extractTopPageValue = (data: TopPagePerformanceEntity | null): number =>
    data ? Number(data['EventSummary.totalEvents']) : 0;

/**
 * Extracts page title from TopPagePerformanceEntity
 */
export const extractPageTitle = (data: TopPagePerformanceEntity | null): string =>
    data?.['EventSummary.title'] || 'analytics.metrics.pageTitle.not-available';

/**
 * Aggregates total conversions from an array of TotalConversionsEntity.
 * Sums all EventSummary.totalEvents values and returns a single TotalConversionsEntity with the total.
 *
 * @param entities - Array of TotalConversionsEntity (one per day/period)
 * @returns A single TotalConversionsEntity with the sum of all events, or null if array is empty
 */
export const aggregateTotalConversions = (
    entities: TotalConversionsEntity[]
): TotalConversionsEntity | null => {
    if (!entities || entities.length === 0) {
        return null;
    }

    const totalEvents = entities.reduce((sum, entity) => {
        const events = parseInt(entity['EventSummary.totalEvents'] || '0', 10);

        return sum + events;
    }, 0);

    return {
        'EventSummary.totalEvents': totalEvents.toString()
    };
};

/**
 * Transforms TopPerformanceTableEntity array to table-friendly format
 */
export const transformTopPagesTableData = (
    data: TopPerformanceTableEntity[] | null
): TablePageData[] => {
    if (!data || !Array.isArray(data)) {
        return [];
    }

    return data.map((item) => ({
        pageTitle: item['EventSummary.title'] || 'analytics.table.data.not-available',
        path: item['EventSummary.identifier'] || 'analytics.table.data.not-available',
        views: Number(item['EventSummary.totalEvents']) || 0
    }));
};

/**
 * Transforms PageViewTimeLineEntity array to Chart.js compatible format
 */
export const transformPageViewTimeLineData = (data: PageViewTimeLineEntity[] | null): ChartData => {
    if (!data || !Array.isArray(data)) {
        return {
            labels: [],
            datasets: [
                {
                    label: 'analytics.charts.pageviews-timeline.dataset-label',
                    data: [],
                    borderColor: AnalyticsChartColors.primary.line,
                    backgroundColor: AnalyticsChartColors.primary.fill,
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4
                }
            ]
        };
    }

    const transformedData = data
        .map((item) => ({
            date: new Date(item['EventSummary.day']),
            value: Number(item['EventSummary.totalEvents'] || '0')
        }))
        .sort((a, b) => a.date.getTime() - b.date.getTime());

    // Check if all data points are from the same day (in user's local timezone)
    const allDatesAreSameDay = transformedData.every((item, _, arr) => {
        if (arr.length < 2) return true;
        const currentDate = item.date;
        const firstDate = arr[0].date;

        return isSameDay(firstDate, currentDate);
    });

    const labels = transformedData.map((item) =>
        format(item.date, allDatesAreSameDay ? TIME_FORMATS.hour : TIME_FORMATS.day)
    );

    const chartData = transformedData.map((item) => item.value);

    return {
        labels,
        datasets: [
            {
                label: 'analytics.charts.pageviews-timeline.dataset-label',
                data: chartData,
                borderColor: AnalyticsChartColors.primary.line,
                backgroundColor: AnalyticsChartColors.primary.fill,
                borderWidth: 2,
                fill: true,
                tension: 0.4,
                cubicInterpolationMode: 'monotone'
            }
        ]
    };
};

/**
 * Conversion trend data point entity from EventSummary cube.
 */
export interface ConversionTrendEntity {
    'EventSummary.totalEvents': string;
    'EventSummary.day': string;
    'EventSummary.day.day': string;
}

/**
 * Extended ChartDataset type for Conversion Trend chart with Chart.js specific properties
 * to make zero values visible in the chart.
 */
export interface ConversionTrendChartDataset extends ChartDataset {
    spanGaps?: boolean;
    pointRadius?: number | number[];
    pointHoverRadius?: number | number[];
    pointBackgroundColor?: string | string[];
    pointBorderColor?: string | string[];
}

/**
 * Transforms ConversionTrendEntity array to Chart.js compatible format
 */
export const transformConversionTrendData = (data: ConversionTrendEntity[] | null): ChartData => {
    if (!data || !Array.isArray(data)) {
        return {
            labels: [],
            datasets: [
                {
                    label: 'analytics.charts.conversion-trend.dataset-label',
                    data: [],
                    borderColor: AnalyticsChartColors.secondary.line,
                    backgroundColor: AnalyticsChartColors.secondary.fill,
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4
                }
            ]
        };
    }

    const transformedData = data
        .map((item) => ({
            date: new Date(item['EventSummary.day']),
            value: parseInt(item['EventSummary.totalEvents'] || '0', 10)
        }))
        .sort((a, b) => a.date.getTime() - b.date.getTime());

    // Check if all data points are from the same day
    const allDatesAreSameDay = transformedData.every((item, _, arr) => {
        if (arr.length < 2) return true;

        return isSameDay(arr[0].date, item.date);
    });

    const labels = transformedData.map((item) =>
        format(item.date, allDatesAreSameDay ? TIME_FORMATS.hour : TIME_FORMATS.day)
    );

    const chartData = transformedData.map((item) => item.value);

    return {
        labels,
        datasets: [
            {
                label: 'analytics.charts.conversion-trend.dataset-label',
                data: chartData,
                borderColor: AnalyticsChartColors.secondary.line,
                backgroundColor: AnalyticsChartColors.secondary.fill,
                borderWidth: 2,
                fill: true,
                tension: 0.4,
                cubicInterpolationMode: 'monotone',
                // Use type assertion to allow Chart.js specific properties
                spanGaps: false,
                pointRadius: chartData.map((value) => (value === 0 ? 4 : 0)),
                pointHoverRadius: chartData.map((value) => (value === 0 ? 6 : 4)),
                pointBackgroundColor: chartData.map((value) =>
                    value === 0
                        ? AnalyticsChartColors.secondary.line
                        : AnalyticsChartColors.secondary.fill
                ),
                pointBorderColor: AnalyticsChartColors.secondary.line
            } as ConversionTrendChartDataset
        ]
    };
};

/**
 * Traffic vs Conversions chart data entity per day.
 */
export interface TrafficVsConversionsEntity {
    'EventSummary.uniqueVisitors': string;
    'EventSummary.uniqueConvertingVisitors': string;
    'EventSummary.day': string;
    'EventSummary.day.day': string;
}

/**
 * Transforms TrafficVsConversionsEntity array to Chart.js compatible format.
 * Creates a combo chart with bars (uniqueVisitors) and line (conversions).
 */
export const transformTrafficVsConversionsData = (
    data: TrafficVsConversionsEntity[] | null
): ChartData => {
    if (!data || !Array.isArray(data) || data.length === 0) {
        return {
            labels: [],
            datasets: [
                {
                    type: 'bar',
                    label: 'analytics.charts.unique-visitors',
                    data: [],
                    ...BAR_CHART_STYLE,
                    backgroundColor: AnalyticsChartColors.primary.line,
                    order: 2
                },
                {
                    type: 'line',
                    label: 'analytics.charts.conversions',
                    data: [],
                    borderColor: AnalyticsChartColors.secondary.line,
                    borderWidth: 2,
                    fill: false,
                    tension: 0.4,
                    order: 1
                }
            ]
        };
    }

    const transformedData = data
        .map((item) => ({
            date: new Date(item['EventSummary.day']),
            uniqueVisitors: parseInt(item['EventSummary.uniqueVisitors'] || '0', 10),
            uniqueConvertingVisitors: parseInt(
                item['EventSummary.uniqueConvertingVisitors'] || '0',
                10
            )
        }))
        .sort((a, b) => a.date.getTime() - b.date.getTime());

    // Check if all data points are from the same day
    const allDatesAreSameDay = transformedData.every((item, _, arr) => {
        if (arr.length < 2) return true;

        return isSameDay(arr[0].date, item.date);
    });

    const labels = transformedData.map((item) =>
        format(item.date, allDatesAreSameDay ? TIME_FORMATS.hour : TIME_FORMATS.day)
    );

    const visitorsData = transformedData.map((item) => item.uniqueVisitors);

    const conversionsData = transformedData.map((item) => item.uniqueConvertingVisitors);

    return {
        labels,
        datasets: [
            {
                type: 'bar',
                label: 'analytics.charts.unique-visitors',
                data: visitorsData,
                ...BAR_CHART_STYLE,
                backgroundColor: AnalyticsChartColors.primary.line,
                order: 2
            },
            {
                type: 'line',
                label: 'analytics.charts.conversions',
                data: conversionsData,
                borderColor: AnalyticsChartColors.secondary.line,
                borderWidth: 2,
                fill: false,
                tension: 0.4,
                order: 1
            }
        ]
    };
};

/**
 * Transformed content conversion row for table display.
 */
export interface ContentConversionRow {
    eventType: string;
    identifier: string;
    title: string;
    events: number;
    conversions: number;
    conversionRate: number;
}

/**
 * Transforms ContentAttributionEntity array to table-friendly format.
 * Calculates conversion rate as (conversions / events) * 100.
 */
export const transformContentConversionsData = (
    data: ContentAttributionEntity[] | null
): ContentConversionRow[] => {
    if (!data || !Array.isArray(data) || data.length === 0) {
        return [];
    }

    return data.map((item) => {
        const events = parseInt(item['ContentAttribution.sumEvents'] || '0', 10);
        const conversions = parseInt(item['ContentAttribution.sumConversions'] || '0', 10);
        const conversionRate = events > 0 ? Math.round((conversions / events) * 10000) / 100 : 0;

        return {
            eventType: item['ContentAttribution.eventType'] || '',
            identifier: item['ContentAttribution.identifier'] || '',
            title: item['ContentAttribution.title'] || item['ContentAttribution.identifier'] || '',
            events,
            conversions,
            conversionRate
        };
    });
};

/**
 * Transforms PageViewDeviceBrowsersEntity array to pie chart ChartData format
 */
export const transformDeviceBrowsersData = (
    data: PageViewDeviceBrowsersEntity[] | null
): ChartData => {
    if (!data || data.length === 0) {
        return {
            labels: [],
            datasets: [
                {
                    label: 'analytics.charts.device-breakdown.dataset-label',
                    data: [],
                    backgroundColor: []
                }
            ]
        };
    }

    // Group data by browser + device type combination
    const browserDeviceGroups = new Map<string, number>();

    data.forEach((item) => {
        const userAgent = item['request.userAgent'];
        const totalRequests = parseInt(item['request.count'] || '0', 10);

        if (userAgent && totalRequests > 0) {
            const parsed = parseUserAgent(userAgent);
            const browserName = parsed.browser.name;
            const deviceType = parsed.device.type;

            // Create combined label: "Chrome (Mobile)", "Safari (Desktop)", etc.
            // Note: Device labels are hardcoded as they go directly to chart library
            const deviceLabel =
                deviceType === 'mobile' ? 'Mobile' : deviceType === 'tablet' ? 'Tablet' : 'Desktop';
            const combinedLabel = `${browserName} (${deviceLabel})`;

            const currentTotal = browserDeviceGroups.get(combinedLabel) || 0;
            browserDeviceGroups.set(combinedLabel, currentTotal + totalRequests);
        }
    });

    // Convert map to arrays and sort by usage
    const sortedBrowserDevices = Array.from(browserDeviceGroups.entries())
        .sort(([, a], [, b]) => b - a)
        .slice(0, 10); // Increase limit to 10 for more device combinations

    if (sortedBrowserDevices.length === 0) {
        return {
            labels: ['No Data'],
            datasets: [
                {
                    label: 'analytics.charts.device-breakdown.dataset-label',
                    data: [1],
                    backgroundColor: [AnalyticsChartColors.neutral.line]
                }
            ]
        };
    }

    const labels = sortedBrowserDevices.map(([browserDevice]) => browserDevice);
    const chartData = sortedBrowserDevices.map(([, count]) => count);

    // Enhanced color palette for browser + device combinations
    const colorPalette = [
        AnalyticsChartColors.primary.line, // Chrome Desktop - Blue
        '#1E40AF', // Chrome Mobile - Dark Blue
        '#60A5FA', // Chrome Tablet - Light Blue
        '#8B5CF6', // Safari Desktop - Purple
        '#6D28D9', // Safari Mobile - Dark Purple
        '#A78BFA', // Safari Tablet - Light Purple
        AnalyticsChartColors.secondary.line, // Firefox Desktop - Green
        '#047857', // Firefox Mobile - Dark Green
        '#34D399', // Firefox Tablet - Light Green
        '#F59E0B', // Edge Desktop - Orange
        '#D97706', // Edge Mobile - Dark Orange
        '#FBBF24', // Edge Tablet - Light Orange
        '#EF4444', // Others Desktop - Red
        '#DC2626', // Others Mobile - Dark Red
        '#F87171' // Others Tablet - Light Red
    ];

    return {
        labels,
        datasets: [
            {
                label: 'analytics.charts.device-breakdown.dataset-label',
                data: chartData,
                backgroundColor: colorPalette.slice(0, labels.length)
            }
        ]
    };
};

/**
 * Type for entities that have EventSummary.day and EventSummary.totalEvents fields
 */
/**
 * Base type for timeline entities that have a day dimension
 */
type TimelineEntity = {
    'EventSummary.day': string;
    'EventSummary.day.day'?: string;
};

/**
 * Factory function type for creating empty entities
 */
type EmptyEntityFactory<T> = (date: Date, dateKey: string) => T;

/**
 * Generic factory for TimelineEntity types.
 * Used for PageViewTimeLineEntity and ConversionTrendEntity which share the same structure.
 */
export const createEmptyAnalyticsEntity = <T extends TimelineEntity>(
    date: Date,
    dateKey: string
): T =>
    ({
        'EventSummary.day': dateKey,
        'EventSummary.day.day': format(date, 'yyyy-MM-dd'),
        'EventSummary.totalEvents': '0'
    }) as unknown as T;

/**
 * Factory for TrafficVsConversionsEntity
 */
export const createEmptyTrafficVsConversionsEntity = (
    date: Date,
    dateKey: string
): TrafficVsConversionsEntity => ({
    'EventSummary.day': dateKey,
    'EventSummary.day.day': format(date, 'yyyy-MM-dd'),
    'EventSummary.uniqueVisitors': '0',
    'EventSummary.uniqueConvertingVisitors': '0'
});

/**
 * Fills missing dates in the data array based on the granularity
 * Works with any timeline entity type by using a factory function
 * @param data - The data array to fill missing dates
 * @param timeRange - The time range for the query
 * @param granularity - The granularity of the data
 * @param createEmptyEntity - Factory function to create empty entities for missing dates
 * @returns The data array with missing dates filled with zero values
 */
export const fillMissingDates = <T extends TimelineEntity>(
    data: T[],
    timeRange: TimeRangeInput,
    granularity: Granularity,
    createEmptyEntity: EmptyEntityFactory<T>
): T[] => {
    if (!data || !Array.isArray(data)) {
        return [];
    }

    const [startDate, endDate] = getDateRange(timeRange);

    const dataMap = new Map<string, T>();
    data.forEach((item) => {
        const dateKey = new Date(item['EventSummary.day']).toISOString();
        dataMap.set(dateKey, item);
    });

    const filledData: T[] = [];
    let currentDate = startDate;
    while (currentDate <= endDate) {
        const currentDateKey = currentDate.toISOString();

        if (dataMap.has(currentDateKey)) {
            const existingItem = dataMap.get(currentDateKey);
            if (existingItem) {
                filledData.push(existingItem);
            }
        } else {
            filledData.push(createEmptyEntity(currentDate, currentDateKey));
        }
        currentDate = granularity === 'hour' ? addHours(currentDate, 1) : addDays(currentDate, 1);
    }

    return filledData;
};

/**
 * Get the date range for the given time range
 * @param timeRange - The time range to get the date range for
 * @returns The date range
 */
export const getDateRange = (timeRange: TimeRangeInput): [Date, Date] => {
    const today = new Date();

    if (Array.isArray(timeRange)) {
        const startDate = startOfDay(parse(timeRange[0], 'yyyy-MM-dd', today));
        const endDate = endOfDay(parse(timeRange[1], 'yyyy-MM-dd', today));

        return [startDate, endDate];
    }

    switch (timeRange) {
        case TIME_RANGE_OPTIONS.today:
            return [startOfDay(today), endOfDay(today)];
        case TIME_RANGE_OPTIONS.yesterday: {
            const yesterday = subDays(today, 1);

            return [startOfDay(yesterday), endOfDay(yesterday)];
        }

        case TIME_RANGE_OPTIONS.last7days: {
            const sevenDaysAgo = subDays(today, 6);

            return [startOfDay(sevenDaysAgo), endOfDay(today)];
        }

        case TIME_RANGE_OPTIONS.last30days: {
            const thirtyDaysAgo = subDays(today, 29);

            return [startOfDay(thirtyDaysAgo), endOfDay(today)];
        }

        default:
            return [startOfDay(today), endOfDay(today)];
    }
};
