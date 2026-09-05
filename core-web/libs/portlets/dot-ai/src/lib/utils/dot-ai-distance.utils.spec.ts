import { toClosenessPercent } from './dot-ai-distance.utils';

describe('toClosenessPercent', () => {
    it('should read a near-zero distance as very close', () => {
        expect(toClosenessPercent(0)).toBe(100);
        expect(toClosenessPercent(0.1)).toBe(90);
    });

    it('should handle the negative distances inner product produces', () => {
        // Measured around -0.33 against a live index; a naive 0..1 bar renders empty.
        expect(toClosenessPercent(-0.33)).toBe(67);
        expect(toClosenessPercent(-1)).toBe(0);
    });

    it('should clamp magnitudes beyond 1, which cosine and L2 can exceed', () => {
        expect(toClosenessPercent(2)).toBe(0);
        expect(toClosenessPercent(-5)).toBe(0);
    });

    it('should be safe on a non-finite value', () => {
        expect(toClosenessPercent(Number.NaN)).toBe(0);
    });
});
