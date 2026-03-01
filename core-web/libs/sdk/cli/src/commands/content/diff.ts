import { defineCommand } from 'citty';
import consola from 'consola';
import { structuredPatch } from 'diff';

import * as childProcess from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';

import { resolveToken } from '../../core/auth';
import { getCachedContentType } from '../../core/cache';
import { loadConfig, resolveInstance } from '../../core/config';
import { createHttpClient, graphql } from '../../core/http';
import { fetchLanguageMap } from '../../core/languages';
import { getFileState, loadSnapshot } from '../../core/snapshot';
import {
    CACHE_DIR,
    DOTCLI_DIR,
    type ContentletRecord,
    type ContentTypeSchema
} from '../../core/types';
import { buildGraphQLQuery, parseContentFile, serializeContentlet } from '../../handlers/content';

export const diffCommand = defineCommand({
    meta: {
        name: 'diff',
        description: 'Show local vs server content differences'
    },
    args: {
        files: { type: 'positional', description: 'File to diff', required: false },
        from: { type: 'string', description: 'Source instance name' },
        local: {
            type: 'boolean',
            description: 'Compare against last-pulled snapshot (no API call)'
        }
    },
    async run({ args }) {
        const projectDir = process.cwd();
        const filePath = args.files ? path.resolve(projectDir, args.files as string) : undefined;

        if (!filePath) {
            consola.error('Please specify a file to diff.');
            return;
        }

        if (!fs.existsSync(filePath)) {
            consola.error(`File not found: ${filePath}`);
            return;
        }

        let config;
        try {
            config = loadConfig(projectDir);
        } catch {
            consola.error('No dotcli project found. Run `dotcli init` first.');
            return;
        }

        const instance = resolveInstance(config, args.from as string | undefined);
        const contentDir = path.dirname(filePath);
        const snapshot = loadSnapshot(contentDir);

        if (args.local) {
            // Local diff: compare hash against snapshot
            const state = getFileState(filePath, snapshot);
            const relativePath = path.relative(projectDir, filePath);
            consola.info(`${relativePath}: ${state}`);
            return;
        }

        // Server diff: fetch server version and compare via serialization
        const fileContent = fs.readFileSync(filePath, 'utf-8');
        const parsed = parseContentFile(filePath, fileContent);
        const identifier = parsed.frontmatter.identifier;

        if (!identifier) {
            consola.error('File has no identifier. Cannot diff against server.');
            return;
        }

        const contentType = parsed.frontmatter.contentType;
        const cacheDir = path.join(projectDir, DOTCLI_DIR, CACHE_DIR);
        const schema = getCachedContentType(cacheDir, contentType);

        if (!schema) {
            consola.error(`No cached schema for "${contentType}". Run a pull first.`);
            return;
        }

        const token = resolveToken(projectDir, instance.name);

        if (!token) {
            consola.error(`No auth token for instance "${instance.name}".`);
            return;
        }

        const client = createHttpClient({ baseURL: instance.url, token });

        // Fetch server version
        const serverRecord = await fetchServerContentlet(client, identifier, schema);
        if (!serverRecord) {
            consola.info('Contentlet not found on server (may be new).');
            return;
        }

        // Fetch language map and serialize server record to .md
        const languageMap = await fetchLanguageMap(client);
        const serverResult = serializeContentlet(serverRecord, schema, languageMap);
        const serverContent = serverResult.content;

        // Read local file content
        const localContent = fileContent;

        // Compute unified diff
        const relativePath = path.relative(projectDir, filePath);
        const patch = structuredPatch(
            `server (${instance.name})`,
            'local',
            serverContent,
            localContent,
            undefined,
            undefined,
            { context: 3 }
        );

        // Check if there are actual differences
        const hasDifferences = patch.hunks.length > 0;

        if (!hasDifferences) {
            consola.info('No differences found.');
            return;
        }

        // Format the diff output with colors
        const output = formatPatch(patch, relativePath, instance.name);

        // Page through less if TTY and output is large
        pageOutput(output);
    }
});

/**
 * Format a structured patch into colored unified diff output.
 */
function formatPatch(
    patch: ReturnType<typeof structuredPatch>,
    filePath: string,
    instanceName: string
): string {
    const lines: string[] = [];

    lines.push(`\x1b[1m${filePath}\x1b[0m`);
    lines.push(`\x1b[31m--- server (${instanceName})\x1b[0m`);
    lines.push(`\x1b[32m+++ local\x1b[0m`);

    for (const hunk of patch.hunks) {
        lines.push(
            `\x1b[36m@@ -${hunk.oldStart},${hunk.oldLines} +${hunk.newStart},${hunk.newLines} @@\x1b[0m`
        );

        for (const line of hunk.lines) {
            if (line.startsWith('+')) {
                lines.push(`\x1b[32m${line}\x1b[0m`);
            } else if (line.startsWith('-')) {
                lines.push(`\x1b[31m${line}\x1b[0m`);
            } else {
                lines.push(line);
            }
        }
    }

    return lines.join('\n');
}

/**
 * Page output through `less -R` if stdout is a TTY and content is large.
 * Falls back to direct print otherwise.
 */
function pageOutput(output: string): void {
    const isTTY = process.stdout.isTTY;
    const lineCount = output.split('\n').length;
    const terminalRows = process.stdout.rows || 24;

    if (isTTY && lineCount > terminalRows && process.platform !== 'win32') {
        try {
            const less = childProcess.spawnSync('less', ['-R'], {
                input: output,
                stdio: ['pipe', 'inherit', 'inherit']
            });
            if (less.status !== 0) {
                // Fallback if less fails
                // eslint-disable-next-line no-console
                console.log(output);
            }
        } catch {
            // eslint-disable-next-line no-console
            console.log(output);
        }
    } else {
        // eslint-disable-next-line no-console
        console.log(output);
    }
}

async function fetchServerContentlet(
    client: ReturnType<typeof createHttpClient>,
    identifier: string,
    schema: ContentTypeSchema
): Promise<ContentletRecord | null> {
    try {
        const gqlQuery = buildGraphQLQuery(schema, {
            query: `+identifier:${identifier}`,
            limit: 1
        });

        const data = await graphql<Record<string, ContentletRecord[]>>(client, gqlQuery);
        const collectionName = `${schema.variable}Collection`;
        const records = data[collectionName];
        return records?.[0] ?? null;
    } catch {
        return null;
    }
}
