import {
    addDays,
    addHours,
    addMonths,
    differenceInDays,
    endOfDay,
    format,
    isSameDay,
    parse,
    startOfDay,
    subDays
} from 'date-fns';

import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    AnalyticsChartColors,
    BAR_CHART_STYLE,
    TIME_RANGE_API_MAPPING,
    TIME_RANGE_CUBEJS_MAPPING,
    TIME_RANGE_OPTIONS
} from '../../constants';
import {
    ApiGranularity,
    ApiRangeParams,
    BrowserBreakdownData,
    ChartData,
    ChartDataset,
    ContentAttributionData,
    DeviceBreakdownData,
    Granularity,
    PieChartEntry,
    RequestState,
    TablePageData,
    TimeRangeCubeJS,
    TimeRangeInput,
    TopContentData,
    TopPagePerformanceEntity,
    TotalEventsByDayData,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../types';

/**
 * Time formats for different chart types
 */
const TIME_FORMATS = {
    hour: 'HH:mm',
    day: 'MMM dd'
};

/**
 * Parses API calendar day strings (`yyyy-MM-dd` with no timezone).
 * `new Date("yyyy-MM-dd")` is parsed as UTC midnight and shifts chart labels in local TZ; {@link parse} uses the local calendar.
 */
function parseApiDateOnly(day: string): Date {
    return parse(day, 'yyyy-MM-dd', new Date());
}

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
 * Converts TimeRangeInput to the new analytics event API query params.
 * For predefined ranges returns `{ range: 'last_7_days' }`.
 * For custom date arrays returns `{ from: '2026-03-30', to: '2026-04-29' }`.
 *
 * @param timeRange - The time range input (predefined option or custom date array)
 * @returns Object with either `range` or `from`+`to` params
 */
export function toApiRangeParams(timeRange: TimeRangeInput): ApiRangeParams {
    if (Array.isArray(timeRange)) {
        return { from: timeRange[0], to: timeRange[1] };
    }

    return {
        range:
            TIME_RANGE_API_MAPPING[timeRange as keyof typeof TIME_RANGE_API_MAPPING] ||
            TIME_RANGE_API_MAPPING[TIME_RANGE_OPTIONS.last7days]
    };
}

/**
 * Extracts page views count from TotalPageViewsEntity
 */
export const extractPageViews = (data: TotalPageViewsEntity | null): number | null => {
    if (!data) return null;
    const value = data.totalEvents ?? 0;

    return value === 0 ? null : value;
};

/**
 * Extracts unique sessions from UniqueVisitorsEntity
 */
export const extractSessions = (data: UniqueVisitorsEntity | null): number | null => {
    if (!data) return null;
    const value = data.uniqueVisitors ?? 0;

    return value === 0 ? null : value;
};

/**
 * Extracts top page performance value from TopPagePerformanceEntity
 */
export const extractTopPageValue = (data: TopPagePerformanceEntity | null): number | null => {
    if (!data) return null;
    const value = data.totalEvents ?? 0;

    return value === 0 ? null : value;
};

/**
 * Extracts page title from TopPagePerformanceEntity
 */
export const extractPageTitle = (data: TopPagePerformanceEntity | null): string =>
    data?.title || 'analytics.metrics.pageTitle.not-available';

/**
 * Transforms TopContentData array to table-friendly format
 */
export const transformTopPagesTableData = (data: TopContentData[] | null): TablePageData[] => {
    if (!data || !Array.isArray(data)) {
        return [];
    }

    return data.map((item) => ({
        pageTitle: item.title || 'analytics.table.data.not-available',
        path: item.identifier || 'analytics.table.data.not-available',
        views: item.totalEvents
    }));
};

/**
 * Transforms TotalEventsByDayData array to Chart.js compatible format
 */
export const transformPageViewTimeLineData = (data: TotalEventsByDayData[] | null): ChartData => {
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
            date: parseApiDateOnly(item.day),
            value: item.totalEvents
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

/** One-day row for Traffic vs Conversions combo chart (merged unique visitors series). */
export interface TrafficVsConversionsDayData {
    day: string;
    uniqueVisitors: number;
    uniqueConvertingVisitors: number;
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
 * Transforms conversion trend time series ({@link TotalEventsByDayData}) to Chart.js format.
 */
export const transformConversionTrendData = (data: TotalEventsByDayData[] | null): ChartData => {
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
            date: parseApiDateOnly(item.day),
            value: item.totalEvents ?? 0
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
 * Transforms TrafficVsConversionsDayData array to Chart.js compatible format.
 * Creates a combo chart with bars (uniqueVisitors) and line (unique converting visitors).
 */
export const transformTrafficVsConversionsData = (
    data: TrafficVsConversionsDayData[] | null
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
            date: parseApiDateOnly(item.day),
            uniqueVisitors: item.uniqueVisitors ?? 0,
            uniqueConvertingVisitors: item.uniqueConvertingVisitors ?? 0
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
 * Transforms ContentAttributionData rows to table-friendly format.
 */
export const transformContentConversionsData = (
    data: ContentAttributionData[] | null
): ContentConversionRow[] => {
    if (!data || !Array.isArray(data) || data.length === 0) {
        return [];
    }

    return data.map((item) => {
        const events = item.events ?? 0;
        const conversions = item.attributionCount ?? 0;
        const conversionRate =
            events > 0
                ? Math.round((conversions / events) * 10000) / 100
                : (item.attributionRate ?? 0);

        return {
            eventType: item.eventType || '',
            identifier: item.identifier || '',
            title: item.title || item.identifier || '',
            events,
            conversions,
            conversionRate
        };
    });
};

/**
 * Default maximum pie slices when {@link PieSliceAggregationOptions.maxSlices} is omitted.
 */
const DEFAULT_PIE_SLICES_CAP = 10;

/**
 * Options for capping pie slices and aggregating the remainder into an "Other" slice.
 */
export interface PieSliceAggregationOptions {
    /**
     * Maximum number of slices (default {@link DEFAULT_PIE_SLICES_CAP}).
     */
    maxSlices?: number;
    /**
     * When non-empty and categories exceed the cap, the remainder is summed into one slice with this label.
     * When omitted or blank, overflow categories are omitted after the cap (legacy truncation).
     */
    otherLabel?: string;
}

/**
 * Maps sorted name→total pairs into {@link PieChartEntry}, applying optional cap and "Other" aggregation.
 *
 * @param sortedDescending - `[name, total]` pairs sorted by total descending.
 * @param options - Slice cap and optional remainder label.
 * @returns Rows for the pie chart.
 */
function mapSortedTotalsToPieChartEntries(
    sortedDescending: [string, number][],
    options?: PieSliceAggregationOptions
): PieChartEntry[] {
    const maxSlices = options?.maxSlices ?? DEFAULT_PIE_SLICES_CAP;
    const remainderLabel = options?.otherLabel?.trim();

    if (sortedDescending.length <= maxSlices) {
        return sortedDescending.map(([name, value]) => ({ name, value }));
    }

    if (remainderLabel) {
        const head = sortedDescending.slice(0, maxSlices - 1);
        const tail = sortedDescending.slice(maxSlices - 1);
        const otherValue = tail.reduce((sum, [, v]) => sum + v, 0);
        return [
            ...head.map(([name, value]) => ({ name, value })),
            { name: remainderLabel, value: otherValue }
        ];
    }

    return sortedDescending.slice(0, maxSlices).map(([name, value]) => ({ name, value }));
}

/**
 * Maps backend `groupBy=device` rows into {@link PieChartEntry} slices (sort, cap, optional "Other").
 */
export const transformDeviceBreakdownToPieChartEntries = (
    data: DeviceBreakdownData[] | null,
    options?: PieSliceAggregationOptions
): PieChartEntry[] => {
    if (!data?.length) {
        return [];
    }

    const sorted = [...data]
        .map((row): [string, number] => [row.device, row.total])
        .sort(([, a], [, b]) => b - a);
    return mapSortedTotalsToPieChartEntries(sorted, options);
};

/**
 * Maps backend `groupBy=browser` rows into {@link PieChartEntry} slices (sort, cap, optional "Other").
 */
export const transformBrowserBreakdownToPieChartEntries = (
    data: BrowserBreakdownData[] | null,
    options?: PieSliceAggregationOptions
): PieChartEntry[] => {
    if (!data?.length) {
        return [];
    }

    const sorted = [...data]
        .map((row): [string, number] => [row.browser, row.total])
        .sort(([, a], [, b]) => b - a);
    return mapSortedTotalsToPieChartEntries(sorted, options);
};

/**
 * Type for entities that have EventSummary.day and EventSummary.totalEvents fields
 */
/**
 * Base type for timeline entities that have a day dimension (legacy Cube row shape).
 */
type TimelineEntity = {
    'EventSummary.day': string;
    'EventSummary.day.day'?: string;
};

/**
 * Cube-shaped conversion trend row — retained for {@link fillMissingDates} unit tests.
 */
export type ConversionTrendEntity = TimelineEntity & {
    'EventSummary.totalEvents': string;
};

/**
 * Factory function type for creating empty entities
 */
type EmptyEntityFactory<T> = (date: Date, dateKey: string) => T;

/**
 * Generic factory for TimelineEntity types.
 * Used for PageViewTimeLineEntity and ConversionTrendEntity which share the same structure.
 */
export const createEmptyAnalyticsEntity = <
    T extends TimelineEntity & { 'EventSummary.totalEvents': string }
>(
    date: Date,
    dateKey: string
): T =>
    ({
        'EventSummary.day': dateKey,
        'EventSummary.day.day': format(date, 'yyyy-MM-dd'),
        'EventSummary.totalEvents': '0'
    }) as T;

/**
 * Factory for TrafficVsConversionsEntity
 */
export const createEmptyTrafficVsConversionsEntity = (
    date: Date,
    dateKey: string
): TimelineEntity & {
    'EventSummary.uniqueVisitors': string;
    'EventSummary.uniqueConvertingVisitors': string;
} => ({
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
        currentDate =
            granularity === Granularity.HOUR ? addHours(currentDate, 1) : addDays(currentDate, 1);
    }

    return filledData;
};

/** Base type for new API timeline entities */
type ApiTimelineEntity = { day: string };

/**
 * Fills missing dates for new API timeline data (shape: { day: string, ... }).
 * The API returns sparse data (only days with events), this fills gaps with zeros.
 *
 * Stepping follows {@link ApiGranularity}: **`day`** advances one calendar day per bucket;
 * **`month`** advances one calendar month per bucket (`day` keys are typically month starts).
 */
export const fillMissingApiDates = <T extends ApiTimelineEntity>(
    data: T[],
    timeRange: TimeRangeInput,
    granularity: ApiGranularity,
    createEmptyEntity: (date: Date) => T
): T[] => {
    if (!data || !Array.isArray(data)) {
        return [];
    }

    const [startDate, endDate] = getDateRange(timeRange);

    const dataMap = new Map<string, T>();
    data.forEach((item) => {
        dataMap.set(item.day, item);
    });

    const filledData: T[] = [];
    let currentDate = startDate;
    while (currentDate <= endDate) {
        const currentDateKey = format(currentDate, 'yyyy-MM-dd');

        const existing = dataMap.get(currentDateKey);
        if (existing) {
            filledData.push(existing);
        } else {
            filledData.push(createEmptyEntity(currentDate));
        }
        currentDate = granularity === 'month' ? addMonths(currentDate, 1) : addDays(currentDate, 1);
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

/**
 * Get the previous period of the same length as the given time range, ending the day before the current range starts.
 * Used for engagement trend comparison (current vs previous period).
 *
 * @param timeRange - The current time range (predefined or custom [from, to])
 * @returns The previous period as [from, to] date strings (yyyy-MM-dd) for Cube queries
 */
export const getPreviousPeriod = (timeRange: TimeRangeInput): [string, string] => {
    const [startDate, endDate] = getDateRange(timeRange);
    const days = differenceInDays(endDate, startDate) + 1;
    const previousEnd = subDays(startDate, 1);
    const previousStart = subDays(previousEnd, days - 1);

    return [format(previousStart, 'yyyy-MM-dd'), format(previousEnd, 'yyyy-MM-dd')];
};
