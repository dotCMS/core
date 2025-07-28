import {
    formatDateForUrl,
    fromUrlFriendly,
    getDefaultTimePeriod,
    getTimePeriodOptions,
    isValidCustomDateRange,
    isValidDate,
    isValidDateOrder,
    isValidTimeRange,
    parseDateFromUrl,
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

        it('should return false for same dates', () => {
            expect(isValidCustomDateRange('2024-01-01', '2024-01-01')).toBe(false);
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

    describe('isValidDate', () => {
        it('should return true for valid Date objects', () => {
            expect(isValidDate(new Date('2024-01-01'))).toBe(true);
            expect(isValidDate(new Date())).toBe(true);
        });

        it('should return true for valid date strings', () => {
            expect(isValidDate('2024-01-01')).toBe(true);
            expect(isValidDate('2023-12-31')).toBe(true);
            expect(isValidDate('January 1, 2024')).toBe(true);
        });

        it('should return false for invalid Date objects', () => {
            expect(isValidDate(new Date('invalid-date'))).toBe(false);
        });

        it('should return false for invalid date strings', () => {
            expect(isValidDate('invalid-date')).toBe(false);
            expect(isValidDate('not-a-date')).toBe(false);
            expect(isValidDate('')).toBe(false);
        });
    });

    describe('isValidDateOrder', () => {
        it('should return true when from date is before to date', () => {
            expect(isValidDateOrder('2024-01-01', '2024-01-31')).toBe(true);
            expect(isValidDateOrder(new Date('2024-01-01'), new Date('2024-01-31'))).toBe(true);
            expect(isValidDateOrder('2023-12-01', new Date('2024-01-01'))).toBe(true);
        });

        it('should return false when from date is after to date', () => {
            expect(isValidDateOrder('2024-01-31', '2024-01-01')).toBe(false);
            expect(isValidDateOrder(new Date('2024-01-31'), new Date('2024-01-01'))).toBe(false);
        });

        it('should return false when dates are equal', () => {
            expect(isValidDateOrder('2024-01-01', '2024-01-01')).toBe(false);
            expect(isValidDateOrder(new Date('2024-01-01'), new Date('2024-01-01'))).toBe(false);
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
    // DATE FORMATTING UTILITIES
    // ============================================================================

    describe('formatDateForUrl', () => {
        it('should format dates to YYYY-MM-DD format', () => {
            const date1 = new Date('2024-01-01T10:30:00Z');
            expect(formatDateForUrl(date1)).toBe('2024-01-01');

            const date2 = new Date('2023-12-31T23:59:59Z');
            expect(formatDateForUrl(date2)).toBe('2023-12-31');
        });

        it('should handle different date inputs consistently', () => {
            const date = new Date(2024, 0, 15); // January 15, 2024
            expect(formatDateForUrl(date)).toBe('2024-01-15');
        });
    });

    describe('parseDateFromUrl', () => {
        it('should parse valid date strings', () => {
            const result1 = parseDateFromUrl('2024-01-01');
            expect(result1).toBeInstanceOf(Date);
            expect(result1).not.toBeNull();

            const result2 = parseDateFromUrl('2023-12-31');
            expect(result2).toBeInstanceOf(Date);
            expect(result2).not.toBeNull();

            // Test that parsing and formatting work together
            const testDate = new Date('2024-06-15T12:00:00Z');
            const formatted = formatDateForUrl(testDate);
            const parsed = parseDateFromUrl(formatted);
            expect(parsed).toBeInstanceOf(Date);
            expect(parsed).not.toBeNull();
        });

        it('should return null for invalid date strings', () => {
            expect(parseDateFromUrl('invalid-date')).toBeNull();
            expect(parseDateFromUrl('not-a-date')).toBeNull();
            expect(parseDateFromUrl('2024-13-01')).toBeNull(); // Invalid month
        });

        it('should return null for empty strings', () => {
            expect(parseDateFromUrl('')).toBeNull();
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

        it('should handle date parsing and formatting round-trip', () => {
            const testDates = [
                new Date('2024-01-01'),
                new Date('2023-12-31'),
                new Date('2024-06-15')
            ];

            testDates.forEach((originalDate) => {
                const formatted = formatDateForUrl(originalDate);
                const parsed = parseDateFromUrl(formatted);

                expect(parsed).not.toBeNull();
                expect(parsed?.getFullYear()).toBe(originalDate.getFullYear());
                expect(parsed?.getMonth()).toBe(originalDate.getMonth());
                expect(parsed?.getDate()).toBe(originalDate.getDate());
            });
        });

        it('should validate complete custom date range workflow', () => {
            const validFrom = '2024-01-01';
            const validTo = '2024-01-31';
            const invalidFrom = 'invalid-date';
            const reversedTo = '2023-12-01';

            // Valid case
            expect(isValidCustomDateRange(validFrom, validTo)).toBe(true);

            // Invalid date case
            expect(isValidCustomDateRange(invalidFrom, validTo)).toBe(false);

            // Reversed order case
            expect(isValidCustomDateRange(validTo, reversedTo)).toBe(false);

            // Individual date validation
            expect(isValidDate(validFrom)).toBe(true);
            expect(isValidDate(invalidFrom)).toBe(false);

            // Date order validation
            expect(isValidDateOrder(validFrom, validTo)).toBe(true);
            expect(isValidDateOrder(validTo, reversedTo)).toBe(false);
        });
    });
});
