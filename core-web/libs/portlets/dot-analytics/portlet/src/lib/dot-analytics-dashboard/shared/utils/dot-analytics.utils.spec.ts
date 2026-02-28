import { hexToRgba, isValidCustomDateRange } from './dot-analytics.utils';

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

    // ============================================================================
    // COLOR UTILITIES
    // ============================================================================

    describe('hexToRgba', () => {
        it('should convert hex color to rgba', () => {
            expect(hexToRgba('#1243e3', 0.5)).toBe('rgba(18, 67, 227, 0.5)');
            expect(hexToRgba('#ff0000', 1)).toBe('rgba(255, 0, 0, 1)');
            expect(hexToRgba('#00ff00', 0)).toBe('rgba(0, 255, 0, 0)');
        });

        it('should convert rgb color to rgba', () => {
            expect(hexToRgba('rgb(255, 0, 0)', 0.3)).toBe('rgba(255, 0, 0, 0.3)');
            expect(hexToRgba('rgb(0, 128, 255)', 0.75)).toBe('rgba(0, 128, 255, 0.75)');
        });

        it('should replace alpha in existing rgba color', () => {
            expect(hexToRgba('rgba(255, 0, 0, 1)', 0.5)).toBe('rgba(255, 0, 0, 0.5)');
            expect(hexToRgba('rgba(0, 128, 255, 0.8)', 0.2)).toBe('rgba(0, 128, 255, 0.2)');
        });

        it('should return fallback for invalid color', () => {
            expect(hexToRgba('invalid', 0.5)).toBe('rgba(18, 67, 227, 0.5)');
            expect(hexToRgba('', 0.5)).toBe('rgba(18, 67, 227, 0.5)');
        });

        it('should handle different alpha values', () => {
            expect(hexToRgba('#000000', 0)).toBe('rgba(0, 0, 0, 0)');
            expect(hexToRgba('#000000', 0.5)).toBe('rgba(0, 0, 0, 0.5)');
            expect(hexToRgba('#000000', 1)).toBe('rgba(0, 0, 0, 1)');
        });
    });
});
