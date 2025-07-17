import { DashboardMockData, TableColumn } from '../types';

/** Table column configuration for top pages analytics table */
export const mockTopPagesTableConfig: TableColumn[] = [
    {
        field: 'path',
        header: 'analytics.table.headers.page-url',
        type: 'link',
        alignment: 'left'
    },
    {
        field: 'title',
        header: 'analytics.table.headers.title',
        type: 'text',
        alignment: 'left'
    },
    {
        field: 'views',
        header: 'analytics.table.headers.pageviews',
        type: 'number',
        alignment: 'center'
    }
];

/**
 * Complete mock data for analytics dashboard demonstration.
 * Includes realistic metrics, charts, and table data for development and testing.
 */
export const DASHBOARD_MOCK_DATA: DashboardMockData = {
    metrics: [
        {
            name: 'analytics.metrics.total-pageviews',
            value: 85230,
            subtitle: 'analytics.metrics.total-pageviews.subtitle',
            icon: 'pi-eye'
        },
        {
            name: 'analytics.metrics.unique-visitors',
            value: 12450,
            subtitle: 'analytics.metrics.unique-visitors.subtitle',
            icon: 'pi-users'
        },
        {
            name: 'analytics.metrics.top-page-performance',
            value: '18.2k',
            subtitle: 'analytics.metrics.top-page-performance.subtitle',
            icon: 'pi-chart-bar'
        }
    ],
    pageviewsTimeline: {
        labels: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'],
        datasets: [
            {
                label: 'analytics.charts.pageviews-timeline.dataset-label',
                data: [4200, 4800, 5100, 4900, 4600, 3200, 2800],
                borderColor: '#3B82F6',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                borderWidth: 2,
                fill: true,
                tension: 0.4
            }
        ]
    },
    deviceBreakdown: {
        labels: [
            'Chrome (Desktop)',
            'Safari (Mobile)',
            'Chrome (Mobile)',
            'Firefox (Desktop)',
            'Safari (Desktop)',
            'Edge (Desktop)',
            'Other'
        ],
        datasets: [
            {
                data: [38.1, 22.2, 18.4, 10, 6.2, 3.4, 1.7],
                backgroundColor: [
                    '#3B82F6', // Blue
                    '#10B981', // Green
                    '#8B5CF6', // Purple
                    '#F59E0B', // Orange
                    '#EF4444', // Red
                    '#6B7280', // Gray
                    '#84CC16' // Lime
                ]
            }
        ]
    },
    topPages: [
        {
            path: '/solutions/headless-cms',
            title: 'Headless CMS Solutions',
            views: 18200,
            percentage: 18.1
        },
        {
            path: '/resources/whitepapers',
            title: 'Resource Whitepapers',
            views: 12450,
            percentage: 14.5
        },
        {
            path: '/blog/modern-web-strategy',
            title: 'Modern Web Strategy Blog',
            views: 7880,
            percentage: 10.8
        },
        {
            path: '/products/content-management',
            title: 'Content Management Products',
            views: 6340,
            percentage: 9.3
        },
        {
            path: '/enterprise/solutions',
            title: 'Enterprise Solutions',
            views: 5120,
            percentage: 7.7
        },
        {
            path: '/developers/documentation',
            title: 'Developer Documentation',
            views: 4890,
            percentage: 6.7
        },
        {
            path: '/features/digital-experience',
            title: 'Digital Experience Features',
            views: 4380,
            percentage: 5.1
        },
        {
            path: '/api/documentation',
            title: 'API Documentation',
            views: 3420,
            percentage: 4.0
        },
        {
            path: '/support/help-center',
            title: 'Support Help Center',
            views: 2890,
            percentage: 3.4
        },
        {
            path: '/login',
            title: 'User Login',
            views: 2350,
            percentage: 2.8
        }
    ],
    topPagesTableConfig: mockTopPagesTableConfig
};
