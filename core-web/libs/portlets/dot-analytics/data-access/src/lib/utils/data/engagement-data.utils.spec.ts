import { toEngagementPlatformMetrics } from './engagement-data.utils';

import type { SessionEngagementGroupByData } from '../../types';

describe('engagement-data.utils', () => {
    describe('toEngagementPlatformMetrics', () => {
        it('should use API engagementRate (truncated to integer) and not recompute share of rows', () => {
            const rows: SessionEngagementGroupByData[] = [
                {
                    name: 'Chrome',
                    avgEngagedSessionTimeSeconds: 8,
                    engagedSessions: 1,
                    engagementRate: 11.5121212,
                    totalSessions: 1
                },
                {
                    name: 'Firefox',
                    avgEngagedSessionTimeSeconds: 9,
                    engagedSessions: 1,
                    engagementRate: 100,
                    totalSessions: 1
                }
            ];

            const result = toEngagementPlatformMetrics(rows);

            expect(result[0]).toEqual({
                name: 'Chrome',
                views: 1,
                percentage: 11,
                time: '0m 8s'
            });
            expect(result[1].percentage).toBe(100);
        });

        it('should return [] for null or empty rows', () => {
            expect(toEngagementPlatformMetrics(null)).toEqual([]);
            expect(toEngagementPlatformMetrics([])).toEqual([]);
        });

        it('should use 0 for non-finite engagementRate', () => {
            const rows: SessionEngagementGroupByData[] = [
                {
                    name: 'Other',
                    avgEngagedSessionTimeSeconds: 0,
                    engagedSessions: 0,
                    engagementRate: Number.NaN,
                    totalSessions: 0
                }
            ];

            expect(toEngagementPlatformMetrics(rows)[0].percentage).toBe(0);
        });
    });
});
