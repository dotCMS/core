import {
    fromUrlFriendly,
    getDefaultTimePeriod,
    getTimePeriodOptions,
    isValidCustomDateRange,
    isValidTimeRange,
    toUrlFriendly
} from './dot-analytics.utils';

import { CUSTOM_TIME_RANGE, DEFAULT_TIME_PERIOD, TIME_PERIOD_OPTIONS } from '../constants';

describe('Analytics Utils', () => {
    // ============================================================================
    // DATE VALIDATION UTILITIES
    // ============================================================================

    describe('isValidCustomDateRange', () => {
        it('should return true for valid date range', () => {
            expect(isValidCustomDateRange('2024-01-01', '2024-01-31')).toBe(true);
            expect(isValidCustomDateRange('2023-12-01', '2024-01-01')).toBe(true);
        });

        it('should return false for invalid dates', () => {
            expect(isValidCustomDateRange('invalid-date', '2024-01-31')).toBe(false);
            expect(isValidCustomDateRange('2024-01-01', 'invalid-date')).toBe(false);
            expect(isValidCustomDateRange('not-a-date', 'also-not-a-date')).toBe(false);
        });

        it('should return false for reversed date order', () => {
            expect(isValidCustomDateRange('2024-01-31', '2024-01-01')).toBe(false);
            expect(isValidCustomDateRange('2024-12-31', '2024-01-01')).toBe(false);
        });

        it('should return true for same dates', () => {
            expect(isValidCustomDateRange('2024-01-01', '2024-01-01')).toBe(true);
        });

        it('should return false for empty or null dates', () => {
            expect(isValidCustomDateRange('', '2024-01-31')).toBe(false);
            expect(isValidCustomDateRange('2024-01-01', '')).toBe(false);
            expect(isValidCustomDateRange('', '')).toBe(false);
        });
    });

    describe('isValidTimeRange', () => {
        it('should return true for valid time ranges', () => {
            expect(isValidTimeRange('today')).toBe(true);
            expect(isValidTimeRange('yesterday')).toBe(true);
            expect(isValidTimeRange('from 7 days ago to now')).toBe(true);
            expect(isValidTimeRange('from 30 days ago to now')).toBe(true);
            expect(isValidTimeRange(CUSTOM_TIME_RANGE)).toBe(true);
        });

        it('should return false for invalid time ranges', () => {
            expect(isValidTimeRange('invalid-range')).toBe(false);
            expect(isValidTimeRange('from 100 days ago')).toBe(false);
            expect(isValidTimeRange('')).toBe(false);
            expect(isValidTimeRange('random-string')).toBe(false);
        });
    });

    // ============================================================================
    // URL MAPPING UTILITIES
    // ============================================================================

    describe('toUrlFriendly', () => {
        it('should convert internal values to URL-friendly values', () => {
            const testCases = [
                { internal: 'today', urlFriendly: 'today' },
                { internal: 'yesterday', urlFriendly: 'yesterday' },
                { internal: 'from 7 days ago to now', urlFriendly: 'last7days' },
                { internal: 'from 30 days ago to now', urlFriendly: 'last30days' },
                { internal: CUSTOM_TIME_RANGE, urlFriendly: 'custom' }
            ];

            testCases.forEach(({ internal, urlFriendly }) => {
                expect(toUrlFriendly(internal)).toBe(urlFriendly);
            });
        });

        it('should return original value for unknown internal values', () => {
            expect(toUrlFriendly('unknown-value')).toBe('unknown-value');
            expect(toUrlFriendly('random-string')).toBe('random-string');
            expect(toUrlFriendly('')).toBe('');
        });
    });

    describe('fromUrlFriendly', () => {
        it('should convert URL-friendly values to internal values', () => {
            const testCases = [
                { urlFriendly: 'today', internal: 'today' },
                { urlFriendly: 'yesterday', internal: 'yesterday' },
                { urlFriendly: 'last7days', internal: 'from 7 days ago to now' },
                { urlFriendly: 'last30days', internal: 'from 30 days ago to now' },
                { urlFriendly: 'custom', internal: CUSTOM_TIME_RANGE }
            ];

            testCases.forEach(({ urlFriendly, internal }) => {
                expect(fromUrlFriendly(urlFriendly)).toBe(internal);
            });
        });

        it('should return original value for unknown URL values', () => {
            expect(fromUrlFriendly('unknown-url-value')).toBe('unknown-url-value');
            expect(fromUrlFriendly('random-string')).toBe('random-string');
            expect(fromUrlFriendly('')).toBe('');
        });
    });

    // ============================================================================
    // CONSTANTS UTILITIES
    // ============================================================================

    describe('getDefaultTimePeriod', () => {
        it('should return the default time period', () => {
            expect(getDefaultTimePeriod()).toBe(DEFAULT_TIME_PERIOD);
        });

        it('should return a string', () => {
            expect(typeof getDefaultTimePeriod()).toBe('string');
        });
    });

    describe('getTimePeriodOptions', () => {
        it('should return the time period options array', () => {
            const options = getTimePeriodOptions();
            expect(options).toBe(TIME_PERIOD_OPTIONS);
            expect(Array.isArray(options)).toBe(true);
        });

        it('should include expected options', () => {
            const options = getTimePeriodOptions();
            expect(options.length).toBeGreaterThan(0);

            // Check that each option has required properties
            options.forEach((option) => {
                expect(option).toHaveProperty('label');
                expect(option).toHaveProperty('value');
                expect(typeof option.label).toBe('string');
                expect(typeof option.value).toBe('string');
            });
        });

        it('should include CUSTOM_TIME_RANGE as an option', () => {
            const options = getTimePeriodOptions();
            const hasCustomOption = options.some((option) => option.value === CUSTOM_TIME_RANGE);
            expect(hasCustomOption).toBe(true);
        });
    });

    // ============================================================================
    // INTEGRATION TESTS
    // ============================================================================

    describe('Integration tests', () => {
        it('should handle round-trip URL mapping correctly', () => {
            const internalValues = [
                'today',
                'yesterday',
                'from 7 days ago to now',
                'from 30 days ago to now',
                CUSTOM_TIME_RANGE
            ];

            internalValues.forEach((internal) => {
                const urlFriendly = toUrlFriendly(internal);
                const backToInternal = fromUrlFriendly(urlFriendly);
                expect(backToInternal).toBe(internal);
            });
        });
    });
});
