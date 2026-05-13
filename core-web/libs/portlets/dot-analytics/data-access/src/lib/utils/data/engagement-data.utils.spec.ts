import {
    toEngagementBreakdownPieEntries,
    toEngagementBreakdownPieScheme,
    toEngagementPlatformMetrics,
    toEngagementPlatformPieEntries
} from './engagement-data.utils';

import type {
    ChartData,
    EngagementPlatformMetrics,
    SessionEngagementGroupByData
} from '../../types';

const MOCK_BREAKDOWN_CHART: ChartData = {
    labels: ['Engaged Sessions (65%)', 'Bounced Sessions (35%)'],
    datasets: [
        { label: 'Engagement Breakdown', data: [65, 35], backgroundColor: ['#6366F1', '#000000'] }
    ]
};

describe('engagement-data.utils', () => {
    describe('toEngagementPlatformMetrics', () => {
        it('should use API engagementRate (rounded to integer) and not recompute share of rows', () => {
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
                percentage: 12,
                time: '0m 8s'
            });
            expect(result[1].percentage).toBe(100);
        });

        it('should round toward nearest integer (not truncate), e.g. 11.7% → 12', () => {
            const rows: SessionEngagementGroupByData[] = [
                {
                    name: 'Safari',
                    avgEngagedSessionTimeSeconds: 1,
                    engagedSessions: 1,
                    engagementRate: 11.7,
                    totalSessions: 1
                }
            ];

            expect(toEngagementPlatformMetrics(rows)[0].percentage).toBe(12);
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

    describe('toEngagementPlatformPieEntries', () => {
        it('should map device metrics to pie entries using views', () => {
            const metrics: EngagementPlatformMetrics[] = [
                { name: 'Desktop', views: 100, percentage: 60, time: '1m' },
                { name: 'Mobile', views: 50, percentage: 40, time: '1m' }
            ];
            expect(toEngagementPlatformPieEntries(metrics)).toEqual([
                { name: 'Desktop', value: 100 },
                { name: 'Mobile', value: 50 }
            ]);
        });

        it('should omit rows with zero or non-finite views', () => {
            expect(
                toEngagementPlatformPieEntries([
                    { name: 'A', views: 0, percentage: 0, time: '0m' },
                    { name: 'B', views: 10, percentage: 100, time: '1m' }
                ])
            ).toEqual([{ name: 'B', value: 10 }]);
        });

        it('should return [] for null, undefined, or empty', () => {
            expect(toEngagementPlatformPieEntries(null)).toEqual([]);
            expect(toEngagementPlatformPieEntries(undefined)).toEqual([]);
            expect(toEngagementPlatformPieEntries([])).toEqual([]);
        });
    });

    describe('toEngagementBreakdownPieEntries', () => {
        it('should return [] for null, empty labels, or empty data', () => {
            expect(toEngagementBreakdownPieEntries(null)).toEqual([]);
            expect(toEngagementBreakdownPieEntries({ labels: [], datasets: [] })).toEqual([]);
            expect(
                toEngagementBreakdownPieEntries({
                    labels: ['A'],
                    datasets: [{ label: 'x', data: [] }]
                })
            ).toEqual([]);
        });

        it('should zip labels with first dataset values', () => {
            expect(toEngagementBreakdownPieEntries(MOCK_BREAKDOWN_CHART)).toEqual([
                { name: 'Engaged Sessions (65%)', value: 65 },
                { name: 'Bounced Sessions (35%)', value: 35 }
            ]);
        });
    });

    describe('toEngagementBreakdownPieScheme', () => {
        it('should return undefined when there are no pie entries', () => {
            expect(toEngagementBreakdownPieScheme(null)).toBeUndefined();
            expect(toEngagementBreakdownPieScheme({ labels: [], datasets: [] })).toBeUndefined();
        });

        it('should return undefined when backgroundColor is missing, not an array, or too short', () => {
            expect(
                toEngagementBreakdownPieScheme({
                    labels: ['A', 'B'],
                    datasets: [{ label: 'x', data: [1, 2] }]
                })
            ).toBeUndefined();
            expect(
                toEngagementBreakdownPieScheme({
                    labels: ['A', 'B'],
                    datasets: [{ label: 'x', data: [1, 2], backgroundColor: '#fff' }]
                })
            ).toBeUndefined();
            expect(
                toEngagementBreakdownPieScheme({
                    labels: ['A', 'B'],
                    datasets: [{ label: 'x', data: [1, 2], backgroundColor: ['#a'] }]
                })
            ).toBeUndefined();
        });

        it('should slice domain to match pie entry count when backgroundColor is string[]', () => {
            expect(toEngagementBreakdownPieScheme(MOCK_BREAKDOWN_CHART)).toEqual({
                domain: ['#6366F1', '#000000']
            });
        });
    });
});
