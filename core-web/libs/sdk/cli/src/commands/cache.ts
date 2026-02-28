import { defineCommand } from 'citty';
import consola from 'consola';

import * as path from 'node:path';

import { clearCache } from '../core/cache';
import { CACHE_DIR, DOTCLI_DIR } from '../core/types';

export const cacheCommand = defineCommand({
    meta: {
        name: 'cache',
        description: 'Manage local caches'
    },
    subCommands: {
        clear: defineCommand({
            meta: { name: 'clear', description: 'Clear cached data' },
            args: {
                type: { type: 'string', description: 'Content type to clear cache for' }
            },
            async run({ args }) {
                const projectDir = process.cwd();
                const cacheDir = path.join(projectDir, DOTCLI_DIR, CACHE_DIR);
                const type = args.type as string | undefined;

                clearCache(cacheDir, type);

                if (type) {
                    consola.success(`Cache cleared for content type "${type}".`);
                } else {
                    consola.success('All caches cleared.');
                }
            }
        })
    }
});
