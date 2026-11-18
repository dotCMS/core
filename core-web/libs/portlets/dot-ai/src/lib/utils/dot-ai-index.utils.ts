import { DotAiIndex } from '@dotcms/dotcms-models';

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
 * A build that has been requested but has not reached `indexCount` yet.
 *
 * Embedding is asynchronous — `EmbeddingsRunner` writes one row per contentlet as it finishes —
 * so for the first second or two after a build the index genuinely exists but has nothing in
 * `dot_embeddings`, and `indexCount` does not return it. Standing in for it with a zeroed row
 * is what puts it in the table immediately, rather than leaving the user to reload the page.
 */
function toPendingIndex(name: string): DotAiIndex {
    return { name, fragments: 0, contents: 0, tokenTotal: 0, tokensPerChunk: 0, contentTypes: [] };
}

/**
 * The server's list plus a placeholder row for every seeded build it has not caught up with.
 *
 * Ordered with the pending ones last so an in-flight build does not reshuffle the table.
 */
export function withPendingIndexes(indexes: DotAiIndex[], buildSeeds: Set<string>): DotAiIndex[] {
    const listed = new Set(indexes.map((index) => index.name));
    const pending = [...buildSeeds].filter((name) => !listed.has(name));

    return pending.length ? [...indexes, ...pending.map(toPendingIndex)] : indexes;
}

/**
 * The seeded builds that are still running.
 *
 * `dot_embeddings` has no status column, so this is derived: a build is finished when its
 * index's fragment count stops moving. The seeds are what let the very first poll report a
 * build instead of guessing from a delta that has not appeared yet.
 *
 * A seeded index the server has not listed yet has no `previousFragments` entry — the snapshot
 * is taken from the server's own response, never from the placeholder rows — so it keeps
 * building rather than settling off a fragment count of zero that never moves.
 *
 * Deliberately per index. The legacy portlet derived one portlet-wide flag, so starting a
 * build on one index made every row claim to be building.
 */
export function stillBuildingSeeds(
    indexes: DotAiIndex[],
    previousFragments: Record<string, number>,
    buildSeeds: Set<string>
): Set<string> {
    const fragments = new Map(indexes.map((index) => [index.name, index.fragments]));

    return new Set(
        [...buildSeeds].filter((name) => {
            const current = fragments.get(name);
            const previous = previousFragments[name];

            // Unlisted, or never snapshotted: there is nothing to compare, so it cannot have
            // settled. Otherwise it is building exactly while the count is moving.
            return current === undefined || previous === undefined || previous !== current;
        })
    );
}
