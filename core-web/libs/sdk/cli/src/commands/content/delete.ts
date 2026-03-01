import * as prompts from '@clack/prompts';
import { defineCommand } from 'citty';
import consola from 'consola';

import * as fs from 'node:fs';
import * as path from 'node:path';

import { resolveToken } from '../../core/auth';
import { loadConfig, resolveInstance } from '../../core/config';
import { createHttpClient, put } from '../../core/http';
import { removeSnapshotEntry } from '../../core/snapshot';
import { parseContentFile } from '../../handlers/content';

export const deleteCommand = defineCommand({
    meta: {
        name: 'delete',
        description: 'Delete contentlet from server'
    },
    args: {
        files: { type: 'positional', description: 'File to delete', required: true },
        to: { type: 'string', description: 'Target instance name' }
    },
    async run({ args }) {
        const projectDir = process.cwd();
        const filePath = path.resolve(projectDir, args.files as string);

        if (!fs.existsSync(filePath)) {
            consola.error(`File not found: ${filePath}`);
            return;
        }

        // Parse file to get identifier
        const fileContent = fs.readFileSync(filePath, 'utf-8');
        const parsed = parseContentFile(filePath, fileContent);
        const identifier = parsed.frontmatter.identifier;

        if (!identifier) {
            consola.error('File has no identifier — it may not have been pushed yet.');
            return;
        }

        // Confirm
        const relativePath = path.relative(projectDir, filePath);
        const confirmed = await prompts.confirm({
            message: `Archive contentlet ${identifier} (${relativePath})?`
        });

        if (prompts.isCancel(confirmed) || !confirmed) {
            consola.info('Aborted.');
            return;
        }

        // Load config and auth
        let config;
        try {
            config = loadConfig(projectDir);
        } catch {
            consola.error('No dotcli project found. Run `dotcli init` first.');
            return;
        }

        const instance = resolveInstance(config, args.to as string | undefined);
        const token = resolveToken(projectDir, instance.name);

        if (!token) {
            consola.error(`No auth token for instance "${instance.name}".`);
            return;
        }

        const client = createHttpClient({ baseURL: instance.url, token });

        // Archive via workflow fire API — omit inode to let the server resolve it
        const url = '/api/v1/workflow/actions/fire?systemAction=ARCHIVE';
        try {
            await put(client, url, {
                contentlet: {
                    identifier,
                    contentType: parsed.frontmatter.contentType
                }
            });
        } catch (error) {
            const err = error as Error & { data?: unknown };
            const detail = err.data ? ` — ${JSON.stringify(err.data)}` : '';
            consola.error(`Archive failed: ${err.message}${detail}`);
            return;
        }

        // Remove local file and snapshot entry
        fs.unlinkSync(filePath);
        const contentDir = path.dirname(filePath);
        removeSnapshotEntry(contentDir, identifier);

        consola.success(`Archived and removed: ${relativePath}`);
    }
});
