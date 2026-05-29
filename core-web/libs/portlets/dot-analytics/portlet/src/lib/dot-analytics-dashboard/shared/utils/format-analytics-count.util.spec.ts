import { formatAnalyticsCount } from './format-analytics-count.util';

describe('formatAnalyticsCount', () => {
    describe('compact mode', () => {
        it('should return 0 for non-finite values', () => {
            expect(formatAnalyticsCount(Number.NaN)).toBe('0');
            expect(formatAnalyticsCount(Number.POSITIVE_INFINITY)).toBe('0');
        });

        it('should return 0 for zero and negative values', () => {
            expect(formatAnalyticsCount(0)).toBe('0');
            expect(formatAnalyticsCount(-5)).toBe('0');
        });

        it('should return integer string below 1000', () => {
            expect(formatAnalyticsCount(804)).toBe('804');
            expect(formatAnalyticsCount(23)).toBe('23');
            expect(formatAnalyticsCount(1)).toBe('1');
        });

        it('should use compact notation from 1000', () => {
            const compact = formatAnalyticsCount(1128);
            expect(compact).not.toBe('1128');
            expect(compact).toMatch(/K/i);
        });

        it('should use compact notation for millions', () => {
            const compact = formatAnalyticsCount(1_000_000);
            expect(compact).toMatch(/M/i);
        });
    });

    describe('full mode', () => {
        it('should group digits without decimals', () => {
            const formatted = formatAnalyticsCount(1128, 'full');
            expect(formatted).toContain('1');
            expect(formatted).toContain('128');
            expect(formatted).not.toMatch(/[kKmM]/);
        });

        it('should return 0 for non-finite values', () => {
            expect(formatAnalyticsCount(Number.NaN, 'full')).toBe('0');
        });

        it('should return 0 for zero and negative values', () => {
            expect(formatAnalyticsCount(0, 'full')).toBe('0');
            expect(formatAnalyticsCount(-5, 'full')).toBe('0');
        });
    });
});
