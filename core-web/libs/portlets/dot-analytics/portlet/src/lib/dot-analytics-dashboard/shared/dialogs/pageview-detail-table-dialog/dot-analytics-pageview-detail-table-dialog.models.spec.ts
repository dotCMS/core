import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';

import { buildPageviewDetailTableRows } from './dot-analytics-pageview-detail-table-dialog.models';

describe('buildPageviewDetailTableRows', () => {
    it('filters out rows with zero views', () => {
        const data: EngagementPlatformMetrics[] = [
            { name: 'Chrome', views: 0, percentage: 0, totalSessions: 10, time: '' },
            { name: 'Firefox', views: 5, percentage: 50, totalSessions: 10, time: '' }
        ];
        const result = buildPageviewDetailTableRows(data);
        expect(result.length).toBe(1);
        expect(result[0].dimensionLabel).toBe('Firefox');
    });

    it('sorts rows by percentage descending', () => {
        const data: EngagementPlatformMetrics[] = [
            { name: 'Low', views: 5, percentage: 10, totalSessions: 50, time: '' },
            { name: 'High', views: 40, percentage: 80, totalSessions: 50, time: '' },
            { name: 'Mid', views: 20, percentage: 40, totalSessions: 50, time: '' }
        ];
        const result = buildPageviewDetailTableRows(data);
        expect(result.map((r) => r.dimensionLabel)).toEqual(['High', 'Mid', 'Low']);
    });

    it('maps views to totalViews', () => {
        const data: EngagementPlatformMetrics[] = [
            { name: 'Chrome', views: 17, percentage: 43, totalSessions: 40, time: '' }
        ];
        const result = buildPageviewDetailTableRows(data);
        expect(result[0].totalViews).toBe(17);
    });

    it('clamps percentage into 0–100', () => {
        const data: EngagementPlatformMetrics[] = [
            { name: 'A', views: 1, percentage: -5, totalSessions: 10, time: '' },
            { name: 'B', views: 1, percentage: 150, totalSessions: 10, time: '' }
        ];
        const result = buildPageviewDetailTableRows(data);
        expect(result.find((r) => r.dimensionLabel === 'A')?.percentage).toBe(0);
        expect(result.find((r) => r.dimensionLabel === 'B')?.percentage).toBe(100);
    });

    it('returns empty array when all rows have zero views', () => {
        const data: EngagementPlatformMetrics[] = [
            { name: 'Chrome', views: 0, percentage: 0, totalSessions: 10, time: '' }
        ];
        expect(buildPageviewDetailTableRows(data)).toEqual([]);
    });
});
