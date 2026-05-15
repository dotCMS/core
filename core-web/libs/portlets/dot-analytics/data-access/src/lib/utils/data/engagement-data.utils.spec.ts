import {
    toEngagementBreakdownPieEntries,
    toEngagementBreakdownPieScheme,
    toEngagementPlatformMetrics
} from './engagement-data.utils';

import type { ChartData, SessionEngagementGroupByData } from '../../types';

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
