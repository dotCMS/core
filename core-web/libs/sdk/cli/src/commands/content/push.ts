import { defineCommand } from 'citty';
import consola from 'consola';

import * as fs from 'node:fs';
import * as path from 'node:path';

import { resolveToken } from '../../core/auth';
import { getCachedContentType } from '../../core/cache';
import { loadConfig, resolveInstance } from '../../core/config';
import { createHttpClient, graphql, put } from '../../core/http';
import {
    checkConflict,
    computeContentHash,
    loadSnapshot,
    scanContentFiles,
    updateSnapshotEntry
} from '../../core/snapshot';
import {
    CACHE_DIR,
    DOTCLI_DIR,
    PUSH_ERRORS_FILE,
    type ContentletRecord,
    type ContentTypeSchema,
    type PushErrorLog,
    type PushResult,
    type SnapshotEntry,
    type SnapshotStore
} from '../../core/types';
import { buildMultipartPayload } from '../../handlers/binary';
import { buildPushPayload, parseContentFile, validateContentFile } from '../../handlers/content';

import type { $Fetch } from 'ofetch';

export const pushCommand = defineCommand({
    meta: {
        name: 'push',
        description: 'Push changed contentlets to dotCMS'
    },
    args: {
        files: { type: 'positional', description: 'File or directory to push', required: false },
        instance: { type: 'string', description: 'Server instance name' },
        action: { type: 'string', description: 'Workflow system action (default: PUBLISH)' },
        'dry-run': { type: 'boolean', description: 'Show what would be pushed without pushing' },
        force: { type: 'boolean', description: 'Skip conflict check' },
        delete: {
            type: 'boolean',
            description: 'Also delete server content for locally deleted files'
        },
        retry: { type: 'boolean', description: 'Retry previously failed pushes' },
        bail: { type: 'boolean', description: 'Stop on first error' },
        'ignore-schema-changes': {
            type: 'boolean',
            description: 'Skip content type drift warnings'
        }
    },
    async run({ args }) {
        const projectDir = process.cwd();
        const systemAction = (args.action as string) || 'PUBLISH';
        const dryRun = !!args['dry-run'];
        const force = !!args.force;
        const bail = !!args.bail;
        const retryMode = !!args.retry;

        // 1. Load config and resolve instance
        let config;
        try {
            config = loadConfig(projectDir);
        } catch {
            consola.error('No dotcli project found. Run `dotcli init` first.');
            return;
        }

        const instance = resolveInstance(config, args.instance as string | undefined);
        const token = resolveToken(projectDir, instance.name);

        if (!token) {
            consola.error(
                `No auth token for instance "${instance.name}". Run \`dotcli auth add\`.`
            );
            return;
        }

        const client = createHttpClient({ baseURL: instance.url, token });
        const cacheDir = path.join(projectDir, DOTCLI_DIR, CACHE_DIR);
        const snapshot = loadSnapshot(projectDir);

        // 2. Determine files to push
        let filesToPush: string[];

        if (retryMode) {
            filesToPush = loadRetryFiles(projectDir);
            if (filesToPush.length === 0) {
                consola.info('No previously failed pushes to retry.');
                return;
            }
            consola.info(`Retrying ${filesToPush.length} previously failed file(s).`);
        } else if (args.files) {
            const target = path.resolve(projectDir, args.files as string);
            if (fs.statSync(target).isDirectory()) {
                const states = scanContentFiles(target, snapshot);
                filesToPush = [...states.entries()]
                    .filter(([, state]) => state === 'modified' || state === 'new')
                    .map(([file]) => file);
            } else {
                filesToPush = [target];
            }
        } else {
            // Scan entire project for changed files
            const states = scanContentFiles(projectDir, snapshot);
            filesToPush = [...states.entries()]
                .filter(([, state]) => state === 'modified' || state === 'new')
                .map(([file]) => file);
        }

        if (filesToPush.length === 0) {
            consola.info('No changes to push.');
            return;
        }

        // 3. Dry run
        if (dryRun) {
            consola.info(`Would push ${filesToPush.length} file(s):`);
            for (const file of filesToPush) {
                consola.log(`  ${path.relative(projectDir, file)}`);
            }
            return;
        }

        // 4. Push each file
        const results: PushResult[] = [];
        const failures: Array<{ file: string; error: string; identifier?: string }> = [];

        for (const filePath of filesToPush) {
            try {
                const result = await pushFile(
                    client,
                    projectDir,
                    cacheDir,
                    snapshot,
                    filePath,
                    systemAction,
                    force
                );
                results.push(result);

                if (result.status === 'error') {
                    failures.push({
                        file: result.file,
                        error: result.error || 'Unknown error',
                        identifier: result.identifier
                    });
                    if (bail) {
                        consola.error(`Bailing: ${result.error}`);
                        break;
                    }
                }
            } catch (error) {
                const errorMsg = (error as Error).message;
                failures.push({ file: filePath, error: errorMsg });
                results.push({ file: filePath, status: 'error', error: errorMsg });

                if (bail) {
                    consola.error(`Bailing: ${errorMsg}`);
                    break;
                }
            }
        }

        // 5. Write errors file
        if (failures.length > 0) {
            const errLog: PushErrorLog = {
                timestamp: new Date().toISOString(),
                failed: failures
            };
            const errDir = path.join(projectDir, DOTCLI_DIR);
            if (!fs.existsSync(errDir)) {
                fs.mkdirSync(errDir, { recursive: true });
            }
            const errPath = path.join(errDir, PUSH_ERRORS_FILE);
            fs.writeFileSync(errPath, JSON.stringify(errLog, null, 2), 'utf-8');
        }

        // 6. Log summary
        const created = results.filter((r) => r.status === 'created').length;
        const updated = results.filter((r) => r.status === 'updated').length;
        const skipped = results.filter((r) => r.status === 'skipped').length;
        const errors = results.filter((r) => r.status === 'error').length;

        consola.info(
            `Push complete: ${created} created, ${updated} updated, ${skipped} skipped, ${errors} errors.`
        );
    }
});

function loadRetryFiles(projectDir: string): string[] {
    const errPath = path.join(projectDir, DOTCLI_DIR, PUSH_ERRORS_FILE);
    if (!fs.existsSync(errPath)) {
        return [];
    }

    const raw = fs.readFileSync(errPath, 'utf-8');
    const errLog = JSON.parse(raw) as PushErrorLog;
    return errLog.failed.map((f) => f.file);
}

async function pushFile(
    client: $Fetch,
    projectDir: string,
    cacheDir: string,
    snapshot: SnapshotStore,
    filePath: string,
    systemAction: string,
    force: boolean
): Promise<PushResult> {
    const fileContent = fs.readFileSync(filePath, 'utf-8');
    const parsed = parseContentFile(filePath, fileContent);
    const contentType = parsed.frontmatter.contentType;
    const identifier = parsed.frontmatter.identifier;

    // Get schema from cache
    const schema = getCachedContentType(cacheDir, contentType);
    if (!schema) {
        return {
            file: filePath,
            status: 'error',
            identifier,
            error: `No cached schema for "${contentType}". Run a pull first to populate the cache.`
        };
    }

    // Validate
    const validation = validateContentFile(parsed, schema);
    if (!validation.valid) {
        return {
            file: filePath,
            status: 'error',
            identifier,
            error: `Validation failed: ${validation.errors.join('; ')}`
        };
    }

    // Conflict check
    if (!force && identifier && snapshot[filePath]) {
        const serverInode = await fetchServerInode(client, identifier, schema);
        if (serverInode) {
            const conflict = checkConflict(snapshot[filePath], serverInode);
            if (conflict.hasConflict) {
                return {
                    file: filePath,
                    status: 'skipped',
                    identifier,
                    error: `Conflict: ${conflict.reason}. Use --force to override.`
                };
            }
        }
    }

    // Build payload
    const { contentlet, binaries } = buildPushPayload(parsed, schema);
    const isNew = !identifier;

    // Push via workflow fire API (default action)
    // Remove inode — let the server resolve it from identifier
    delete contentlet['inode'];
    const url = `/api/v1/workflow/actions/default/fire/${systemAction}`;

    let responseData: Record<string, unknown>;

    // Filter binaries to only include files that actually exist on disk
    const existingBinaries = binaries.filter((b) => {
        const absPath = path.resolve(path.dirname(filePath), b.localPath);
        return fs.existsSync(absPath);
    });

    if (existingBinaries.length > 0) {
        const formData = await buildMultipartPayload(contentlet, existingBinaries, filePath);
        responseData = await client(url, {
            method: 'PUT',
            body: formData,
            headers: { 'Content-Type': undefined as unknown as string }
        });
    } else {
        responseData = await put<Record<string, unknown>>(client, url, { contentlet });
    }

    // Extract response entity
    const entity = (responseData as { entity?: ContentletRecord }).entity;
    const newInode = (entity?.['inode'] as string) || '';
    const newIdentifier = (entity?.['identifier'] as string) || identifier || '';

    // Update snapshot
    const hash = computeContentHash(filePath);
    const snapshotEntry: SnapshotEntry = {
        hash,
        pulledAt: new Date().toISOString(),
        inode: newInode,
        identifier: newIdentifier
    };
    updateSnapshotEntry(projectDir, filePath, snapshotEntry);

    const relativePath = path.relative(projectDir, filePath);
    consola.success(`${isNew ? 'Created' : 'Updated'}: ${relativePath}`);

    return {
        file: filePath,
        status: isNew ? 'created' : 'updated',
        identifier: newIdentifier
    };
}

async function fetchServerInode(
    client: $Fetch,
    identifier: string,
    schema: ContentTypeSchema
): Promise<string | null> {
    try {
        const query = `{ ${schema.variable}Collection(query: "+identifier:${identifier}") { inode } }`;
        const data = await graphql<Record<string, Array<{ inode: string }>>>(client, query);
        const collectionName = `${schema.variable}Collection`;
        const records = data[collectionName];
        return records?.[0]?.inode ?? null;
    } catch {
        return null;
    }
}
