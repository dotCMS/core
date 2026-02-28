import { defineCommand } from 'citty';
import consola from 'consola';

import * as path from 'node:path';

import { loadSnapshot, scanContentFiles } from '../../core/snapshot';

import type { FileState } from '../../core/types';

export const statusCommand = defineCommand({
    meta: {
        name: 'status',
        description: 'Show changed content files'
    },
    async run() {
        const projectDir = process.cwd();
        const snapshot = loadSnapshot(projectDir);
        const states = scanContentFiles(projectDir, snapshot);

        const grouped: Record<FileState, string[]> = {
            unchanged: [],
            modified: [],
            new: [],
            deleted: []
        };

        for (const [file, state] of states) {
            grouped[state].push(file);
        }

        consola.info('Content status:\n');
        consola.log(`  ${grouped.unchanged.length} unchanged`);
        consola.log(`  ${grouped.modified.length} modified`);
        consola.log(`  ${grouped.new.length} new`);
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

        if (grouped.deleted.length > 0) {
            consola.log('\nDeleted:');
            for (const file of grouped.deleted) {
                consola.log(`  -  ${path.relative(projectDir, file)}`);
            }
        }

        const totalChanged = grouped.modified.length + grouped.new.length + grouped.deleted.length;
        if (totalChanged === 0) {
            consola.info('\nNo changes detected.');
        }
    }
});
