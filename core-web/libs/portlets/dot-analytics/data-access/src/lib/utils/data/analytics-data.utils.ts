import {
    ChartData,
    Granularity,
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    TablePageData,
    TimeRange,
    TopPagePerformanceEntity,
    TopPerformaceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../types';
import { parseUserAgent } from '../browser/userAgentParser';

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
export function determineGranularityForTimeRange(timeRange: TimeRange): Granularity {
    switch (timeRange) {
        case 'today':

        // falls through
        case 'yesterday':
            // For today/yesterday, use hourly granularity for detailed intraday analysis
            return 'hour';

        case 'from 7 days ago to now':
            // For last 7 days, use daily granularity
            return 'day';

        case 'from 30 days ago to now':
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
    data ? Number(data['request.totalRequest']) : 0;

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

    // Sort by date to ensure correct order
    const sortedData = [...data].sort(
        (a, b) =>
            new Date(a['request.createdAt']).getTime() - new Date(b['request.createdAt']).getTime()
    );

    const labels = sortedData.map((item) => {
        const date = new Date(item['request.createdAt']);

        // Format as short weekday + date (e.g., "Mon 21", "Tue 22")
        return date.toLocaleDateString('en-US', {
            weekday: 'short',
            day: 'numeric'
        });
    });

    const chartData = sortedData.map((item) => Number(item['request.totalRequest']) || 0);

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
                tension: 0.4
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
