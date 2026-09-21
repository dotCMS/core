import { DotAiIndex } from '@dotcms/dotcms-models';

import {
    CACHE_INDEX_NAME,
    stillBuildingSeeds,
    toIndexOptions,
    toRetrievalIndexes,
    withPendingIndexes
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

    describe('withPendingIndexes', () => {
        it('should stand in for a seeded build the server has not listed yet', () => {
            const result = withPendingIndexes([index({ name: 'a' })], new Set(['blogs']));

            expect(result.map((i) => i.name)).toEqual(['a', 'blogs']);
            expect(result.at(-1)?.fragments).toBe(0);
        });

        it('should return the same array when every seed is already listed', () => {
            // Identity matters: markIndexBuilding patches `indexes` with this, and a fresh
            // array there would churn every reader of the list on each poll.
            const indexes = [index({ name: 'blogs' })];

            expect(withPendingIndexes(indexes, new Set(['blogs']))).toBe(indexes);
        });
    });

    describe('stillBuildingSeeds', () => {
        it('should keep a seed whose index the server has not listed yet', () => {
            expect(stillBuildingSeeds([], {}, new Set(['a']))).toEqual(new Set(['a']));
        });

        it('should keep a seed with no snapshot to compare against', () => {
            expect(stillBuildingSeeds([index({ name: 'a' })], {}, new Set(['a']))).toEqual(
                new Set(['a'])
            );
        });

        it('should keep a seed while the fragment count is still moving', () => {
            const result = stillBuildingSeeds(
                [index({ name: 'a', fragments: 20 })],
                { a: 10 },
                new Set(['a'])
            );

            expect(result).toEqual(new Set(['a']));
        });

        it('should drop a seed once the fragment count stops changing', () => {
            const result = stillBuildingSeeds(
                [index({ name: 'a', fragments: 10 })],
                { a: 10 },
                new Set(['a'])
            );

            expect(result.size).toBe(0);
        });

        it('should settle each index on its own, not portlet-wide', () => {
            // The legacy portlet flipped every row at once off a single global delta.
            const result = stillBuildingSeeds(
                [index({ name: 'a', fragments: 20 }), index({ name: 'b', fragments: 5 })],
                { a: 10, b: 5 },
                new Set(['a', 'b'])
            );

            expect(result).toEqual(new Set(['a']));
        });

        it('should ignore an index nobody seeded a build for', () => {
            const result = stillBuildingSeeds(
                [index({ name: 'a', fragments: 20 })],
                { a: 10 },
                new Set()
            );

            expect(result.size).toBe(0);
        });
    });
});
