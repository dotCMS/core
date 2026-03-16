import { Params } from '@angular/router';

import { getComparisonLabel, isValidTab, paramsToTimeRange } from './filters.utils';

import { DASHBOARD_TABS, TIME_RANGE_OPTIONS } from '../constants';

describe('Filters Utils', () => {
    describe('isValidTab', () => {
        it('should return true for valid tab values', () => {
            expect(isValidTab(DASHBOARD_TABS.pageview)).toBe(true);
            expect(isValidTab(DASHBOARD_TABS.conversions)).toBe(true);
        });

        it('should return false for invalid tab values', () => {
            expect(isValidTab('invalid-tab')).toBe(false);
            expect(isValidTab('')).toBe(false);
            expect(isValidTab('pageviews')).toBe(false);
        });

        it('should work as a type guard', () => {
            const tab: string = DASHBOARD_TABS.pageview;

            if (isValidTab(tab)) {
                // TypeScript should know that tab is DashboardTab here
                expect(tab).toBe(DASHBOARD_TABS.pageview);
            }
        });
    });

    describe('paramsToTimeRange', () => {
        it('should return custom date range when time_range is custom and from/to are provided', () => {
            const params: Params = {
                time_range: TIME_RANGE_OPTIONS.custom,
                from: '2024-01-01',
                to: '2024-01-31'
            };

            const result = paramsToTimeRange(params);

            expect(result).toEqual(['2024-01-01', '2024-01-31']);
        });

        it('should return predefined time range when time_range is provided', () => {
            const params: Params = {
                time_range: TIME_RANGE_OPTIONS.last30days
            };

            const result = paramsToTimeRange(params);

            expect(result).toBe(TIME_RANGE_OPTIONS.last30days);
        });

        it('should return default time range when time_range is not provided', () => {
            const params: Params = {};

            const result = paramsToTimeRange(params);

            expect(result).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should return default time range when time_range is custom but from/to are missing', () => {
            const params: Params = {
                time_range: TIME_RANGE_OPTIONS.custom
            };

            const result = paramsToTimeRange(params);

            expect(result).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should return default time range when time_range is custom but only from is provided', () => {
            const params: Params = {
                time_range: TIME_RANGE_OPTIONS.custom,
                from: '2024-01-01'
            };

            const result = paramsToTimeRange(params);

            expect(result).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should return default time range when time_range is custom but only to is provided', () => {
            const params: Params = {
                time_range: TIME_RANGE_OPTIONS.custom,
                to: '2024-01-31'
            };

            const result = paramsToTimeRange(params);

            expect(result).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should handle null params', () => {
            const params: Params = null as unknown as Params;

            const result = paramsToTimeRange(params);

            expect(result).toBe(TIME_RANGE_OPTIONS.last7days);
        });
    });

    describe('getComparisonLabel', () => {
        it('should return "from previous 7 days" for last7days range', () => {
            expect(getComparisonLabel(TIME_RANGE_OPTIONS.last7days)).toBe('from previous 7 days');
        });

        it('should return "from previous 30 days" for last30days range', () => {
            expect(getComparisonLabel(TIME_RANGE_OPTIONS.last30days)).toBe('from previous 30 days');
        });

        it('should return "from previous range" for custom date range array', () => {
            expect(getComparisonLabel(['2024-01-01', '2024-01-31'])).toBe('from previous range');
        });

        it('should return "from previous range" for custom string range', () => {
            expect(getComparisonLabel(TIME_RANGE_OPTIONS.custom)).toBe('from previous range');
        });
    });
});
