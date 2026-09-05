import { DOT_AI_INDEX_STATUS, DotAiIndex } from '@dotcms/dotcms-models';

import {
    CACHE_INDEX_NAME,
    deriveIndexStatuses,
    estimateIndexCost,
    toIndexOptions,
    toRetrievalIndexes
} from './dot-ai-index.utils';

const index = (overrides: Partial<DotAiIndex> = {}): DotAiIndex => ({
    name: 'default',
    fragments: 10,
    contents: 4,
    tokenTotal: 1000,
    tokensPerChunk: 100,
    contentTypes: ['Blog'],
    ...overrides
});

describe('dot-ai-index.utils', () => {
    describe('estimateIndexCost', () => {
        it('should apply the formula to any index', () => {
            // The legacy portlet computed this but only ever showed it for the index literally
            // named `cache`, which is why every other row read as free.
            expect(estimateIndexCost(index({ tokenTotal: 1000 }))).toBeCloseTo(0.0001);
            expect(estimateIndexCost(index({ tokenTotal: 2_000_000 }))).toBeCloseTo(0.2);
        });

        it('should be zero for an empty index', () => {
            expect(estimateIndexCost(index({ tokenTotal: 0 }))).toBe(0);
        });
    });

    describe('toRetrievalIndexes', () => {
        it('should exclude the cache pseudo-index', () => {
            const result = toRetrievalIndexes([
                index({ name: CACHE_INDEX_NAME }),
                index({ name: 'blogs' })
            ]);

            expect(result.map((i) => i.name)).toEqual(['blogs']);
        });
    });

    describe('toIndexOptions', () => {
        it('should format the label with the content count', () => {
            expect(toIndexOptions([index({ name: 'blogs', contents: 12 })])).toEqual([
                { label: 'blogs - (contents:12)', value: 'blogs' }
            ]);
        });

        it('should not offer the cache index as a retrieval target', () => {
            const options = toIndexOptions([
                index({ name: CACHE_INDEX_NAME }),
                index({ name: 'blogs' })
            ]);

            expect(options.map((o) => o.value)).toEqual(['blogs']);
        });
    });

    describe('deriveIndexStatuses', () => {
        it('should report READY when there is no previous snapshot', () => {
            const result = deriveIndexStatuses([index({ name: 'a' })], {}, new Set());

            expect(result['a']).toBe(DOT_AI_INDEX_STATUS.READY);
        });

        it('should report BUILDING when a build was just seeded for that index', () => {
            const result = deriveIndexStatuses([index({ name: 'a' })], {}, new Set(['a']));

            expect(result['a']).toBe(DOT_AI_INDEX_STATUS.BUILDING);
        });

        it('should keep BUILDING while the fragment count is still moving', () => {
            const result = deriveIndexStatuses(
                [index({ name: 'a', fragments: 20 })],
                { a: 10 },
                new Set(['a'])
            );

            expect(result['a']).toBe(DOT_AI_INDEX_STATUS.BUILDING);
        });

        it('should settle to READY once the fragment count stops changing', () => {
            const result = deriveIndexStatuses(
                [index({ name: 'a', fragments: 10 })],
                { a: 10 },
                new Set(['a'])
            );

            expect(result['a']).toBe(DOT_AI_INDEX_STATUS.READY);
        });

        it('should derive status per index, not portlet-wide', () => {
            // The legacy portlet flipped every row at once off a single global delta.
            const result = deriveIndexStatuses(
                [index({ name: 'a', fragments: 20 }), index({ name: 'b', fragments: 5 })],
                { a: 10, b: 5 },
                new Set(['a', 'b'])
            );

            expect(result['a']).toBe(DOT_AI_INDEX_STATUS.BUILDING);
            expect(result['b']).toBe(DOT_AI_INDEX_STATUS.READY);
        });
    });
});
