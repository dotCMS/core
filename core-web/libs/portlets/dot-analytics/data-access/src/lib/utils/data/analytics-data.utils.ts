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

import { TIME_RANGE_OPTIONS } from '../../constants';
import {
    ChartData,
    Granularity,
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    TablePageData,
    TimeRangeInput,
    TopPagePerformanceEntity,
    TopPerformaceTableEntity,
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
 * Extracts page views count from TotalPageViewsEntity
 */
export const extractPageViews = (data: TotalPageViewsEntity | null): number =>
    data ? Number(data['request.totalRequest'] ?? 0) : 0;

/**
 * Extracts unique sessions from UniqueVisitorsEntity
 */
export const extractSessions = (data: UniqueVisitorsEntity | null): number =>
    data ? Number(data['request.totalUsers']) : 0;

/**
 * Extracts top page performance value from TopPagePerformanceEntity
 */
export const extractTopPageValue = (data: TopPagePerformanceEntity | null): number =>
    data ? Number(data['request.totalRequest']) : 0;

/**
 * Extracts page title from TopPagePerformanceEntity
 */
export const extractPageTitle = (data: TopPagePerformanceEntity | null): string =>
    data?.['request.pageTitle'] || 'analytics.metrics.pageTitle.not-available';

/**
 * Transforms TopPerformaceTableEntity array to table-friendly format
 */
export const transformTopPagesTableData = (
    data: TopPerformaceTableEntity[] | null
): TablePageData[] => {
    if (!data || !Array.isArray(data)) {
        return [];
    }

    return data.map((item) => ({
        pageTitle: item['request.pageTitle'] || 'analytics.table.data.not-available',
        path: item['request.path'] || 'analytics.table.data.not-available',
        views: Number(item['request.totalRequest']) || 0
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
                    borderColor: '#3B82F6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4
                }
            ]
        };
    }

    const transformedData = data
        .map((item) => ({
            date: new Date(item['request.createdAt']),
            value: extractPageViews(item)
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
                borderColor: '#3B82F6',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                borderWidth: 2,
                fill: true,
                tension: 0.4,
                cubicInterpolationMode: 'monotone'
            }
        ]
    };
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
        const totalRequests = parseInt(item['request.totalRequest'] || '0', 10);

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
                    backgroundColor: ['#E5E7EB']
                }
            ]
        };
    }

    const labels = sortedBrowserDevices.map(([browserDevice]) => browserDevice);
    const chartData = sortedBrowserDevices.map(([, count]) => count);

    // Enhanced color palette for browser + device combinations
    const colorPalette = [
        '#3B82F6', // Chrome Desktop - Blue
        '#1E40AF', // Chrome Mobile - Dark Blue
        '#60A5FA', // Chrome Tablet - Light Blue
        '#8B5CF6', // Safari Desktop - Purple
        '#6D28D9', // Safari Mobile - Dark Purple
        '#A78BFA', // Safari Tablet - Light Purple
        '#10B981', // Firefox Desktop - Green
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
 * Fills missing dates in the data array based on the granularity
 * @param data - The data array to fill missing dates
 * @param granularity - The granularity of the data
 * @returns The data array with missing dates filled
 */
export const fillMissingDates = (
    data: PageViewTimeLineEntity[],
    timeRange: TimeRangeInput,
    granularity: Granularity
): PageViewTimeLineEntity[] => {
    if (!data || !Array.isArray(data)) {
        return [];
    }

    const [startDate, endDate] = getDateRange(timeRange);

    const dataMap = new Map();
    data.forEach((item) => {
        const dateKey = new Date(item['request.createdAt']).toISOString();
        dataMap.set(dateKey, item);
    });

    const filledData = [];
    let currentDate = startDate;
    while (currentDate <= endDate) {
        const currentDateKey = currentDate.toISOString();

        if (dataMap.has(currentDateKey)) {
            filledData.push(dataMap.get(currentDateKey));
        } else {
            filledData.push({
                'request.createdAt': currentDateKey,
                'request.totalRequest': '0'
            });
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
