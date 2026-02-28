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
        instance: { type: 'string', description: 'Server instance name' }
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

        const instance = resolveInstance(config, args.instance as string | undefined);
        const token = resolveToken(projectDir, instance.name);

        if (!token) {
            consola.error(`No auth token for instance "${instance.name}".`);
            return;
        }

        const client = createHttpClient({ baseURL: instance.url, token });

        // Archive via workflow fire API
        const url = '/api/v1/workflow/actions/fire?systemAction=ARCHIVE';
        await put(client, url, {
            contentlet: {
                identifier,
                contentType: parsed.frontmatter.contentType,
                inode: parsed.frontmatter.inode
            }
        });

        // Remove local file and snapshot entry
        fs.unlinkSync(filePath);
        removeSnapshotEntry(projectDir, filePath);

        consola.success(`Archived and removed: ${relativePath}`);
    }
});
