import { defineCommand } from 'citty';
import consola from 'consola';

import * as fs from 'node:fs';
import * as path from 'node:path';

import { resolveToken } from '../../core/auth';
import { getCachedContentType } from '../../core/cache';
import { loadConfig, resolveInstance } from '../../core/config';
import { createHttpClient } from '../../core/http';
import { resolveIdentifiersOnServer } from '../../core/resolve';
import { scanContentFiles } from '../../core/snapshot';
import { CACHE_DIR, DOTCLI_DIR, type FileState } from '../../core/types';
import { parseContentFile } from '../../handlers/content';

export const statusCommand = defineCommand({
    meta: {
        name: 'status',
        description: 'Show changed content files'
    },
    args: {
        from: { type: 'string', description: 'Source instance name' }
    },
    async run({ args }) {
        const projectDir = process.cwd();

        let config;
        try {
            config = loadConfig(projectDir);
        } catch {
            consola.error('No dotcli project found. Run `dotcli init` first.');
            return;
        }

        const instance = resolveInstance(config, args.from as string | undefined);
        const states = scanContentFiles(projectDir);

        const grouped: Record<FileState, string[]> = {
            unchanged: [],
            modified: [],
            new: [],
            deleted: []
        };

        // Track files that exist on server but aren't tracked locally
        const existsOnServer: string[] = [];

        for (const [file, state] of states) {
            grouped[state].push(file);
        }

        // Resolve "new" files against the target server to detect cross-instance content
        if (grouped.new.length > 0) {
            const token = resolveToken(projectDir, instance.name);
            if (token) {
                const client = createHttpClient({ baseURL: instance.url, token });
                const cacheDir = path.join(projectDir, DOTCLI_DIR, CACHE_DIR);

                // Collect identifiers from "new" files grouped by content type
                const byContentType = new Map<
                    string,
                    Array<{ file: string; identifier: string }>
                >();
                for (const file of grouped.new) {
                    try {
                        const fileContent = fs.readFileSync(file, 'utf-8');
                        const parsed = parseContentFile(file, fileContent);
                        const identifier = parsed.frontmatter.identifier;
                        const contentType = parsed.frontmatter.contentType;
                        if (identifier && contentType) {
                            const group = byContentType.get(contentType) ?? [];
                            group.push({ file, identifier });
                            byContentType.set(contentType, group);
                        }
                    } catch {
                        // Skip unparseable files
                    }
                }

                // Batch-resolve each content type
                const serverIdentifiers = new Set<string>();
                for (const [contentType, entries] of byContentType) {
                    const schema = getCachedContentType(cacheDir, contentType);
                    if (!schema) continue;

                    try {
                        const resolved = await resolveIdentifiersOnServer(
                            client,
                            schema.variable,
                            entries.map((e) => e.identifier)
                        );

                        for (const entry of entries) {
                            const match = resolved.get(entry.identifier);
                            if (match) {
                                serverIdentifiers.add(entry.file);
                            }
                        }
                    } catch {
                        // Failed resolution — treat all as new
                    }
                }

                // Reclassify: files found on server go to existsOnServer
                if (serverIdentifiers.size > 0) {
                    const trulyNew: string[] = [];
                    for (const file of grouped.new) {
                        if (serverIdentifiers.has(file)) {
                            existsOnServer.push(file);
                        } else {
                            trulyNew.push(file);
                        }
                    }
                    grouped.new = trulyNew;
                }
            }
        }

        consola.info(`Content status (instance: ${instance.name}):\n`);
        consola.log(`  ${grouped.unchanged.length} unchanged`);
        consola.log(`  ${grouped.modified.length} modified`);
        consola.log(`  ${grouped.new.length} new`);
        if (existsOnServer.length > 0) {
            consola.log(`  ${existsOnServer.length} update (exists on server, not yet tracked)`);
        }
        consola.log(`  ${grouped.deleted.length} deleted`);

        if (grouped.modified.length > 0) {
            consola.log('\nModified:');
            for (const file of grouped.modified) {
                consola.log(`  M  ${path.relative(projectDir, file)}`);
            }
        }

        if (grouped.new.length > 0) {
            consola.log('\nNew:');
            for (const file of grouped.new) {
                consola.log(`  +  ${path.relative(projectDir, file)}`);
            }
        }

        if (existsOnServer.length > 0) {
            consola.log('\nExists on server (not tracked):');
            for (const file of existsOnServer) {
                consola.log(`  ~  ${path.relative(projectDir, file)}`);
            }
        }

        if (grouped.deleted.length > 0) {
            consola.log('\nDeleted:');
            for (const file of grouped.deleted) {
                consola.log(`  -  ${path.relative(projectDir, file)}`);
            }
        }

        const totalChanged =
            grouped.modified.length +
            grouped.new.length +
            existsOnServer.length +
            grouped.deleted.length;
        if (totalChanged === 0) {
            consola.info('\nNo changes detected.');
        }
    }
});
