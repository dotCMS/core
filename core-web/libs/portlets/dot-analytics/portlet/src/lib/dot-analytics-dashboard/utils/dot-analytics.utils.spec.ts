import { isValidCustomDateRange } from './dot-analytics.utils';

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
});
