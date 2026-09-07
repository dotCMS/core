import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';

import { buildEngagementDetailTableRows } from './dot-analytics-engagement-detail-table-dialog.models';

describe('buildEngagementDetailTableRows', () => {
    it('filters out rows with zero or non-finite totalSessions', () => {
        const rows: EngagementPlatformMetrics[] = [
            { name: 'A', views: 0, percentage: 0, totalSessions: 0, time: '0m 0s' },
            {
                name: 'B',
                views: 1,
                percentage: 50,
                totalSessions: 2,
                time: '1m'
            }
        ];
        const result = buildEngagementDetailTableRows(rows);
        expect(result.length).toBe(1);
        expect(result[0].dimensionLabel).toBe('B');
    });

    it('sorts rows by totalSessions descending', () => {
        const rows: EngagementPlatformMetrics[] = [
            { name: 'Low', views: 1, percentage: 10, totalSessions: 2, time: '1m' },
            { name: 'High', views: 5, percentage: 50, totalSessions: 10, time: '2m' }
        ];
        const result = buildEngagementDetailTableRows(rows);
        expect(result.map((r) => r.dimensionLabel)).toEqual(['High', 'Low']);
    });

    it('clamps percentage into 0–100', () => {
        const rows: EngagementPlatformMetrics[] = [
            {
                name: 'X',
                views: 1,
                percentage: NaN as unknown as number,
                totalSessions: 2,
                time: ''
            },
            { name: 'Y', views: 1, percentage: 150, totalSessions: 2, time: '' }
        ];
        const result = buildEngagementDetailTableRows(rows);
        expect(result[0].percentage).toBe(0);
        expect(result[1].percentage).toBe(100);
    });
});
