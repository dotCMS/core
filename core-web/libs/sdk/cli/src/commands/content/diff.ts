import { defineCommand } from 'citty';
import consola from 'consola';

import * as fs from 'node:fs';
import * as path from 'node:path';

import { resolveToken } from '../../core/auth';
import { getCachedContentType } from '../../core/cache';
import { loadConfig, resolveInstance } from '../../core/config';
import { createHttpClient, graphql } from '../../core/http';
import { getFileState, loadSnapshot } from '../../core/snapshot';
import {
    CACHE_DIR,
    DOTCLI_DIR,
    type ContentletRecord,
    type ContentTypeSchema
} from '../../core/types';
import { buildGraphQLQuery, getUserFields, parseContentFile } from '../../handlers/content';

export const diffCommand = defineCommand({
    meta: {
        name: 'diff',
        description: 'Show local vs server content differences'
    },
    args: {
        files: { type: 'positional', description: 'File to diff', required: false },
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

        const snapshot = loadSnapshot(projectDir);

        if (args.local) {
            // Local diff: compare hash against snapshot
            const state = getFileState(filePath, snapshot);
            const relativePath = path.relative(projectDir, filePath);
            consola.info(`${relativePath}: ${state}`);
            return;
        }

        // Server diff: fetch server version and compare field-by-field
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

        let config;
        try {
            config = loadConfig(projectDir);
        } catch {
            consola.error('No dotcli project found. Run `dotcli init` first.');
            return;
        }

        const instance = resolveInstance(config);
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

        // Compare field-by-field
        const userFields = getUserFields(schema);
        const relativePath = path.relative(projectDir, filePath);
        consola.info(`Diff: ${relativePath}\n`);

        let hasDifferences = false;

        for (const field of userFields) {
            const localValue = parsed.frontmatter[field.variable] ?? '';
            const serverValue = serverRecord[field.variable] ?? '';

            const localStr = String(localValue);
            const serverStr = String(serverValue);

            if (localStr !== serverStr) {
                hasDifferences = true;
                consola.log(`  ${field.variable}:`);
                consola.log(`    - server: ${truncate(serverStr, 80)}`);
                consola.log(`    + local:  ${truncate(localStr, 80)}`);
            }
        }

        // Compare body
        const bodyFieldVar = parsed.frontmatter.bodyField;
        if (bodyFieldVar) {
            const localBody = parsed.body.trim();
            const serverBody = String(serverRecord[bodyFieldVar] ?? '').trim();

            if (localBody !== serverBody) {
                hasDifferences = true;
                consola.log(`  ${bodyFieldVar} (body):`);
                consola.log(`    - server: ${truncate(serverBody, 80)}`);
                consola.log(`    + local:  ${truncate(localBody, 80)}`);
            }
        }

        if (!hasDifferences) {
            consola.info('No differences found.');
        }
    }
});

function truncate(str: string, maxLen: number): string {
    if (str.length <= maxLen) return str;
    return str.slice(0, maxLen) + '...';
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
