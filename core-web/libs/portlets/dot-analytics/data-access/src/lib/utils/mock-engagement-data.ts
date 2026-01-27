import { AnalyticsChartColors, BAR_CHART_STYLE } from '../constants/dot-analytics.constants';
import { ChartData } from '../types/entities.types';

export const MOCK_ENGAGEMENT_DATA = {
    kpis: {
        engagementRate: {
            value: 45,
            trend: 8,
            subtitle: '29,203 Engaged Sessions',
            label: 'Engagement Rate',
            sparklineData: [
                { date: 'Oct 1', value: 32 },
                { date: 'Oct 2', value: 35 },
                { date: 'Oct 3', value: 38 },
                { date: 'Oct 4', value: 36 },
                { date: 'Oct 5', value: 40 },
                { date: 'Oct 6', value: 42 },
                { date: 'Oct 7', value: 39 },
                { date: 'Oct 8', value: 43 },
                { date: 'Oct 9', value: 41 },
                { date: 'Oct 10', value: 44 },
                { date: 'Oct 11', value: 42 },
                { date: 'Oct 12', value: 45 }
            ]
        },
        avgInteractions: { value: 6.4, trend: 18, label: 'Avg Interactions (Engaged)' },
        avgSessionTime: { value: '2m 34s', trend: 12, label: 'Average Session Time' },
        conversionRate: { value: '3.2%', trend: -0.3, label: 'Conversion Rate' }
    },
    trend: {
        labels: ['Oct1', 'Oct2', 'Oct3', 'Oct4', 'Oct5', 'Nov1', 'Nov2', 'Nov3'],
        datasets: [
            {
                label: 'Trend',
                data: [40, 35, 45, 30, 50, 45, 48, 48],
                backgroundColor: AnalyticsChartColors.primary.line,
                ...BAR_CHART_STYLE
            }
        ]
    } as ChartData,
    breakdown: {
        labels: ['Engaged Sessions (65%)', 'Bounced Sessions (35%)'],
        datasets: [
            {
                label: 'Engagement Breakdown',
                data: [65, 35],
                backgroundColor: [AnalyticsChartColors.primary.line, '#000000']
            }
        ]
    } as ChartData,
    platforms: {
        device: [
            { name: 'Desktop', views: 77053, percentage: 72, time: '2m 45s' },
            { name: 'Mobile', views: 16071, percentage: 20, time: '1m 47s' },
            { name: 'Tablet', views: 2531, percentage: 8, time: '2m 00s' }
        ],
        browser: [
            { name: 'Chrome', views: 60000, percentage: 65, time: '2m 50s' },
            { name: 'Safari', views: 20000, percentage: 25, time: '2m 30s' },
            { name: 'Firefox', views: 10000, percentage: 10, time: '2m 40s' }
        ],
        language: [
            { name: 'English', views: 80000, percentage: 80, time: '2m 55s' },
            { name: 'Spanish', views: 10000, percentage: 10, time: '2m 20s' },
            { name: 'French', views: 5000, percentage: 5, time: '2m 10s' }
        ]
    }
};
