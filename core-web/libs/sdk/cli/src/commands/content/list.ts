import { defineCommand } from 'citty';
import consola from 'consola';

import * as fs from 'node:fs';
import * as path from 'node:path';

import { SNAPSHOT_FILE, type SnapshotStore } from '../../core/types';

export const listCommand = defineCommand({
    meta: {
        name: 'list',
        description: 'List local content from snapshots (offline)'
    },
    args: {
        type: { type: 'string', description: 'Filter by content type' }
    },
    async run({ args }) {
        const projectDir = process.cwd();
        const typeFilter = args.type as string | undefined;

        // Walk project looking for .snapshot.json files
        const snapshots = findSnapshots(projectDir);

        if (snapshots.length === 0) {
            consola.info('No content found. Run `dotcli content pull` first.');
            return;
        }

        let totalContentlets = 0;

        for (const { contentType, contentDir, snapshot } of snapshots) {
            if (typeFilter && contentType !== typeFilter) {
                continue;
            }

            const entries = Object.entries(snapshot);
            if (entries.length === 0) continue;

            totalContentlets += entries.length;
            const label = entries.length === 1 ? 'contentlet' : 'contentlets';
            consola.log(`\n${contentType} (${entries.length} ${label})`);

            for (const [identifier, entry] of entries) {
                const shortId = identifier.slice(0, 6);
                const relativePath = path.relative(projectDir, path.join(contentDir, entry.file));
                const title = entry.title || '(untitled)';
                consola.log(`  ${shortId}  ${title.padEnd(24)}  ${relativePath}`);
            }
        }

        if (totalContentlets === 0) {
            if (typeFilter) {
                consola.info(`No content found for type "${typeFilter}".`);
            } else {
                consola.info('No content found. Run `dotcli content pull` first.');
            }
        }
    }
});

interface SnapshotInfo {
    contentType: string;
    contentDir: string;
    snapshot: SnapshotStore;
}

/**
 * Walk projectDir looking for .snapshot.json files.
 * Extract content type from the directory path ({host}/content/{type}/.snapshot.json).
 */
function findSnapshots(dir: string): SnapshotInfo[] {
    const results: SnapshotInfo[] = [];
    walkForSnapshots(dir, results);
    return results;
}

function walkForSnapshots(dir: string, results: SnapshotInfo[]): void {
    if (!fs.existsSync(dir)) return;

    const entries = fs.readdirSync(dir, { withFileTypes: true });

    for (const entry of entries) {
        // Skip hidden dirs except we need to check for .snapshot.json files
        if (entry.name.startsWith('.') && entry.name !== SNAPSHOT_FILE) {
            continue;
        }

        const fullPath = path.join(dir, entry.name);

        if (entry.isDirectory()) {
            walkForSnapshots(fullPath, results);
        } else if (entry.name === SNAPSHOT_FILE) {
            try {
                const raw = fs.readFileSync(fullPath, 'utf-8');
                const snapshot = JSON.parse(raw) as SnapshotStore;

                // Extract content type from path: .../content/{type}/.snapshot.json
                const contentDir = path.dirname(fullPath);
                const contentType = path.basename(contentDir);

                results.push({ contentType, contentDir, snapshot });
            } catch {
                // Skip unreadable snapshots
            }
        }
    }
}
