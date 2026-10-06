import { DOT_AI_VECTOR_OPERATOR } from '@dotcms/dotcms-models';

import { toClosenessPercent } from './dot-ai-distance.utils';

const COSINE = DOT_AI_VECTOR_OPERATOR.COSINE;
const INNER_PRODUCT = DOT_AI_VECTOR_OPERATOR.INNER_PRODUCT;

describe('toClosenessPercent', () => {
    describe('cosine and L2, where smaller is closer', () => {
        it('should read a near-zero distance as very close', () => {
            expect(toClosenessPercent(0, COSINE)).toBe(100);
            expect(toClosenessPercent(0.1, COSINE)).toBe(90);
        });

        it('should clamp magnitudes beyond 1, which cosine and L2 can exceed', () => {
            expect(toClosenessPercent(2, COSINE)).toBe(0);
        });
    });

    describe('inner product, where more negative is closer', () => {
        it('should recognise the symbol the response actually echoes', () => {
            // The request says `innerProduct`; the response comes back with `<#>`. Testing only
            // the request form left this green while the live bars stayed inverted.
            expect(toClosenessPercent(-0.65, '<#>')).toBe(65);
            expect(toClosenessPercent(-0.65, '<#>')).toBe(toClosenessPercent(-0.65, INNER_PRODUCT));
        });

        it('should read the more negative distance as the closer match', () => {
            // The property that matters: `<#>` returns the negated inner product, so the bars
            // must rank the same way the results do. Inverting the magnitude for every
            // operator drew the emptiest bar on the best hit.
            expect(toClosenessPercent(-0.65, INNER_PRODUCT)).toBeGreaterThan(
                toClosenessPercent(-0.09, INNER_PRODUCT)
            );
        });

        it('should keep the whole ranking monotonic', () => {
            const ranked = [-0.65, -0.27, -0.2, -0.19, -0.14, -0.09];
            const bars = ranked.map((d) => toClosenessPercent(d, INNER_PRODUCT));

            expect(bars).toEqual([...bars].sort((a, b) => b - a));
        });

        it('should measure similarity as the distance below zero', () => {
            expect(toClosenessPercent(-0.65, INNER_PRODUCT)).toBe(65);
            expect(toClosenessPercent(-1, INNER_PRODUCT)).toBe(100);
        });

        it('should read a positive value as dissimilar, not close', () => {
            expect(toClosenessPercent(0.3, INNER_PRODUCT)).toBe(0);
        });

        it('should clamp beyond the normalised range', () => {
            expect(toClosenessPercent(-5, INNER_PRODUCT)).toBe(100);
        });
    });

    it('should be safe on a non-finite value', () => {
        expect(toClosenessPercent(Number.NaN, COSINE)).toBe(0);
    });
});
