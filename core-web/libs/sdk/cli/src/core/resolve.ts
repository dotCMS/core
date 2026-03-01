/**
 * Server-side identifier resolution for cross-instance push safety.
 *
 * Before pushing content to a target instance, batch-queries the server
 * by identifier to determine whether content already exists. This prevents
 * creating duplicates when pushing content pulled from a different instance.
 */
import { graphql } from './http';

import type { $Fetch } from 'ofetch';

// ─── Constants ──────────────────────────────────────────────────────────────

/** Maximum identifiers per GraphQL query to stay within URL/body limits */
const BATCH_SIZE = 50;

// ─── Types ──────────────────────────────────────────────────────────────────

export interface ResolvedIdentifier {
    identifier: string;
    inode: string;
}

// ─── Public API ─────────────────────────────────────────────────────────────

/**
 * Batch-query the target server to check which identifiers already exist.
 *
 * Uses Lucene OR syntax: `+identifier:(id1 OR id2 OR id3)` via GraphQL
 * collection queries. Results are returned as a Map where:
 * - Found identifiers map to `{ identifier, inode }`
 * - Not-found identifiers map to `null`
 *
 * Failed batches are silently caught — their identifiers default to `null`
 * (treated as new content).
 */
export async function resolveIdentifiersOnServer(
    client: $Fetch,
    contentType: string,
    identifiers: string[]
): Promise<Map<string, ResolvedIdentifier | null>> {
    const result = new Map<string, ResolvedIdentifier | null>();

    if (identifiers.length === 0) return result;

    // Deduplicate
    const unique = [...new Set(identifiers)];

    // Split into batches
    for (let i = 0; i < unique.length; i += BATCH_SIZE) {
        const batch = unique.slice(i, i + BATCH_SIZE);

        try {
            const found = await queryBatch(client, contentType, batch);
            // Mark found identifiers
            for (const entry of found) {
                result.set(entry.identifier, entry);
            }
            // Mark not-found identifiers as null
            for (const id of batch) {
                if (!result.has(id)) {
                    result.set(id, null);
                }
            }
        } catch {
            // Failed lookup — treat all as not found (will create as new)
            for (const id of batch) {
                if (!result.has(id)) {
                    result.set(id, null);
                }
            }
        }
    }

    return result;
}

// ─── Internal ───────────────────────────────────────────────────────────────

/**
 * Query a batch of identifiers via GraphQL collection query.
 */
async function queryBatch(
    client: $Fetch,
    contentType: string,
    identifiers: string[]
): Promise<ResolvedIdentifier[]> {
    const idList = identifiers.join(' OR ');
    const query = `{ ${contentType}Collection(query: "+identifier:(${idList})", limit: ${identifiers.length}) { identifier inode } }`;

    const data = await graphql<Record<string, Array<{ identifier: string; inode: string }>>>(
        client,
        query
    );

    const collectionName = `${contentType}Collection`;
    const records = data[collectionName] ?? [];

    return records.map((r) => ({
        identifier: r.identifier,
        inode: r.inode
    }));
}
