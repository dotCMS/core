import { DOT_AI_INDEX_STATUS, DotAiIndex, DotAiIndexStatus } from '@dotcms/dotcms-models';

/**
 * Internal cache index. It is real and shows in the Embeddings table, but it is not a
 * retrieval target — the legacy portlet had the same asymmetry and it is preserved.
 */
export const CACHE_INDEX_NAME = 'cache';

/** Indexes that can actually be searched — everything except the cache pseudo-index. */
export function toRetrievalIndexes(indexes: DotAiIndex[]): DotAiIndex[] {
    return indexes.filter((index) => index.name !== CACHE_INDEX_NAME);
}

/** Options for the retrieval index picker, labelled as the legacy portlet labelled them. */
export function toIndexOptions(indexes: DotAiIndex[]): { label: string; value: string }[] {
    return toRetrievalIndexes(indexes).map((index) => ({
        label: `${index.name} - (contents:${index.contents})`,
        value: index.name
    }));
}

/**
 * Build status per index, derived rather than read: `dot_embeddings` has no status column.
 *
 * An index counts as building while its fragment count is still moving. `buildSeeds` carries
 * the indexes a build was just requested for, which is what lets the very first poll report
 * BUILDING instead of guessing from a delta that has not appeared yet.
 *
 * Deliberately per index. The legacy portlet derived one portlet-wide flag, so starting a
 * build on one index made every row claim to be building.
 */
export function deriveIndexStatuses(
    indexes: DotAiIndex[],
    previousFragments: Record<string, number>,
    buildSeeds: Set<string>
): Record<string, DotAiIndexStatus> {
    return indexes.reduce<Record<string, DotAiIndexStatus>>((statuses, index) => {
        const previous = previousFragments[index.name];
        const moved = previous !== undefined && previous !== index.fragments;
        const building = buildSeeds.has(index.name) && (previous === undefined || moved);

        statuses[index.name] = building ? DOT_AI_INDEX_STATUS.BUILDING : DOT_AI_INDEX_STATUS.READY;

        return statuses;
    }, {});
}
