import { defineCommand } from 'citty';
import consola from 'consola';

import * as fs from 'node:fs';
import * as path from 'node:path';

import { resolveToken } from '../../core/auth';
import { cacheContentType, getCachedContentType } from '../../core/cache';
import { loadConfig, resolveInstance } from '../../core/config';
import { createHttpClient, get, graphql } from '../../core/http';
import { fetchLanguageMap } from '../../core/languages';
import {
    computeContentHash,
    findEntryByFile,
    loadSnapshot,
    saveSnapshot,
    updateSnapshotEntry
} from '../../core/snapshot';
import {
    CACHE_DIR,
    DOTCLI_DIR,
    type ContentletRecord,
    type ContentTypeSchema,
    type LanguageMap,
    type PullConfigEntry,
    type SnapshotEntry
} from '../../core/types';
import { downloadContentBinaries } from '../../handlers/binary';
import {
    buildGraphQLQuery,
    serializeContentlet,
    type GraphQLQueryOptions
} from '../../handlers/content';

import type { $Fetch } from 'ofetch';

const DEFAULT_PAGE_SIZE = 25;

export const pullCommand = defineCommand({
    meta: {
        name: 'pull',
        description: 'Pull contentlets from dotCMS'
    },
    args: {
        type: { type: 'string', description: 'Content type variable name' },
        site: { type: 'string', description: 'Site hostname' },
        id: { type: 'string', description: 'Specific contentlet identifier' },
        from: { type: 'string', description: 'Source instance name' },
        query: { type: 'string', description: 'Lucene query filter' },
        language: { type: 'string', description: 'Language code (e.g., es-ES)' },
        'with-binaries': { type: 'boolean', description: 'Download binary sidecar files' }
    },
    async run({ args }) {
        const projectDir = process.cwd();

        // 1. Load config and resolve instance
        let config;
        try {
            config = loadConfig(projectDir);
        } catch {
            consola.error('No dotcli project found. Run `dotcli init` first.');
            return;
        }

        const instance = resolveInstance(config, args.from as string | undefined);
        const token = resolveToken(projectDir, instance.name);

        if (!token) {
            consola.error(
                `No auth token for instance "${instance.name}". Run \`dotcli auth add\`.`
            );
            return;
        }

        // 2. Create HTTP client
        const client = createHttpClient({ baseURL: instance.url, token });
        const cacheDir = path.join(projectDir, DOTCLI_DIR, CACHE_DIR);

        // 3. Fetch language map
        const languageMap = await fetchLanguageMap(client);

        // 4. Pull by ID or by content types
        if (args.id) {
            await pullSingleContentlet(
                client,
                projectDir,
                cacheDir,
                languageMap,
                args.id as string,
                !!args['with-binaries'],
                instance.name
            );
        } else {
            const pullEntries = buildPullEntries(config.pull, args);

            if (pullEntries.length === 0) {
                consola.error(
                    'No content types specified. Use --type or configure pull entries in config.yml.'
                );
                return;
            }

            let totalPulled = 0;
            for (const entry of pullEntries) {
                const count = await pullContentType(
                    client,
                    projectDir,
                    cacheDir,
                    languageMap,
                    entry,
                    !!args['with-binaries'],
                    instance.name
                );
                totalPulled += count;
            }

            consola.success(`Pulled ${totalPulled} contentlet(s).`);
        }
    }
});

function buildPullEntries(
    configPull: PullConfigEntry[] | undefined,
    args: Record<string, unknown>
): PullConfigEntry[] {
    if (args.type) {
        return [
            {
                type: args.type as string,
                site: args.site as string | undefined,
                query: args.query as string | undefined,
                language: args.language as string | undefined
            }
        ];
    }

    return configPull ?? [];
}

async function fetchOrCacheSchema(
    client: $Fetch,
    cacheDir: string,
    contentType: string
): Promise<ContentTypeSchema> {
    const cached = getCachedContentType(cacheDir, contentType);
    if (cached) {
        return cached;
    }

    const response = await get<{ entity: ContentTypeSchema }>(
        client,
        `/api/v1/contenttype/id/${contentType}`
    );

    const schema = response.entity;
    cacheContentType(cacheDir, contentType, schema);
    return schema;
}

async function pullSingleContentlet(
    client: $Fetch,
    projectDir: string,
    cacheDir: string,
    languageMap: LanguageMap,
    identifier: string,
    withBinaries: boolean,
    instanceName: string
): Promise<void> {
    // Fetch contentlet by identifier using REST API to get content type first
    const response = await get<{ entity: ContentletRecord }>(
        client,
        `/api/v1/content/${identifier}`
    );

    const record = response.entity;
    const contentType = record['contentType'] as string;

    if (!contentType) {
        consola.error(`Contentlet ${identifier} has no content type.`);
        return;
    }

    const schema = await fetchOrCacheSchema(client, cacheDir, contentType);
    const hostObj = record['host'] as Record<string, unknown> | undefined;
    const hostName = (hostObj?.['hostName'] || record['hostName'] || 'default') as string;

    const result = serializeContentlet(record, schema, languageMap);
    const contentDir = path.join(projectDir, hostName, 'content', contentType);
    fs.mkdirSync(contentDir, { recursive: true });

    const filePath = path.join(contentDir, result.filename);

    // Warn if overwriting local modifications
    if (fs.existsSync(filePath)) {
        const snapshot = loadSnapshot(contentDir);
        const match = findEntryByFile(snapshot, result.filename);
        if (match) {
            const currentHash = computeContentHash(filePath);
            if (currentHash !== match[1].hash) {
                consola.warn(`Overwriting local changes: ${path.relative(projectDir, filePath)}`);
            }
        }
    }

    fs.writeFileSync(filePath, result.content, 'utf-8');

    // Update snapshot
    const hash = computeContentHash(filePath);
    const snapshotEntry: SnapshotEntry = {
        file: result.filename,
        title: (record['title'] as string) || '',
        hash,
        pulledAt: new Date().toISOString(),
        inode: record['inode'] as string,
        source: instanceName
    };
    updateSnapshotEntry(contentDir, identifier, snapshotEntry);

    if (withBinaries && result.binaries.length > 0) {
        await downloadContentBinaries(client, record, schema, contentDir);
    }

    consola.success(`Pulled: ${filePath}`);
}

async function pullContentType(
    client: $Fetch,
    projectDir: string,
    cacheDir: string,
    languageMap: LanguageMap,
    entry: PullConfigEntry,
    withBinaries: boolean,
    instanceName: string
): Promise<number> {
    const schema = await fetchOrCacheSchema(client, cacheDir, entry.type);

    consola.start(`Pulling ${entry.type}...`);

    let offset = 0;
    const limit = entry.limit ?? DEFAULT_PAGE_SIZE;
    let totalPulled = 0;
    let hasMore = true;

    // Track identifiers pulled in this run to clean up stale entries afterwards
    const pulledIdentifiers = new Set<string>();
    // Track content dirs we write to (for cleanup pass)
    const touchedContentDirs = new Set<string>();

    while (hasMore) {
        const queryOptions: GraphQLQueryOptions = {
            site: entry.site,
            query: entry.query,
            limit,
            offset
        };

        const gqlQuery = buildGraphQLQuery(schema, queryOptions);

        const data = await graphql<Record<string, ContentletRecord[]>>(client, gqlQuery);
        const collectionName = `${schema.variable}Collection`;
        const records = data[collectionName] ?? [];

        for (const record of records) {
            const host = record['host'] as Record<string, unknown> | undefined;
            const hostName = (host?.['hostName'] || record['hostName'] || 'default') as string;
            const contentDir = path.join(projectDir, hostName, 'content', entry.type);
            fs.mkdirSync(contentDir, { recursive: true });
            touchedContentDirs.add(contentDir);

            const result = serializeContentlet(record, schema, languageMap);
            const filePath = path.join(contentDir, result.filename);

            // Warn if overwriting local modifications
            if (fs.existsSync(filePath)) {
                const snapshot = loadSnapshot(contentDir);
                const match = findEntryByFile(snapshot, result.filename);
                if (match) {
                    const currentHash = computeContentHash(filePath);
                    if (currentHash !== match[1].hash) {
                        consola.warn(
                            `Overwriting local changes: ${path.relative(projectDir, filePath)}`
                        );
                    }
                }
            }

            fs.writeFileSync(filePath, result.content, 'utf-8');

            // Update snapshot
            const hash = computeContentHash(filePath);
            const recordIdentifier = record['identifier'] as string;
            const snapshotEntry: SnapshotEntry = {
                file: result.filename,
                title: (record['title'] as string) || '',
                hash,
                pulledAt: new Date().toISOString(),
                inode: record['inode'] as string,
                source: instanceName
            };
            updateSnapshotEntry(contentDir, recordIdentifier, snapshotEntry);
            pulledIdentifiers.add(recordIdentifier);

            if (withBinaries && result.binaries.length > 0) {
                await downloadContentBinaries(client, record, schema, contentDir);
            }

            totalPulled++;
        }

        hasMore = records.length === limit;
        offset += limit;
    }

    // Clean up stale entries: remove snapshot entries and files that were not
    // part of this pull (e.g., content from a different instance or deleted on server)
    for (const contentDir of touchedContentDirs) {
        const snapshot = loadSnapshot(contentDir);
        const staleIds = Object.keys(snapshot).filter((id) => !pulledIdentifiers.has(id));

        if (staleIds.length > 0) {
            for (const staleId of staleIds) {
                const staleEntry = snapshot[staleId];
                const stalePath = path.join(contentDir, staleEntry.file);

                // Remove the file if it exists
                if (fs.existsSync(stalePath)) {
                    fs.unlinkSync(stalePath);
                    consola.info(
                        `Removed stale: ${path.relative(projectDir, stalePath)} (not in ${instanceName})`
                    );
                }

                delete snapshot[staleId];
            }

            saveSnapshot(contentDir, snapshot);
        }
    }

    consola.success(`Pulled ${totalPulled} ${entry.type} contentlet(s).`);
    return totalPulled;
}
