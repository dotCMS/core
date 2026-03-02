/**
 * Snapshot tracking for @dotcms/cli.
 * Tracks content file states (hashing, change detection, conflict detection)
 * between pull and push operations.
 *
 * Snapshots are co-located with content: each content type directory has a
 * `.snapshot.json` keyed by identifier.
 */
import matter from 'gray-matter';

import { createHash } from 'node:crypto';
import * as fs from 'node:fs';
import * as path from 'node:path';

import {
    DOTCLI_DIR,
    METADATA_KEYS,
    SNAPSHOT_FILE,
    type ContentletRecord,
    type FileState,
    type SnapshotEntry,
    type SnapshotStore
} from './types';

// ─── Snapshot Persistence ───────────────────────────────────────────────────

/**
 * Load snapshot from {contentDir}/.snapshot.json.
 * Returns an empty object if the file doesn't exist.
 */
export function loadSnapshot(contentDir: string): SnapshotStore {
    const filePath = path.join(contentDir, SNAPSHOT_FILE);

    if (!fs.existsSync(filePath)) {
        return {};
    }

    const raw = fs.readFileSync(filePath, 'utf-8');
    return JSON.parse(raw) as SnapshotStore;
}

/**
 * Save snapshot to {contentDir}/.snapshot.json.
 */
export function saveSnapshot(contentDir: string, snapshot: SnapshotStore): void {
    fs.mkdirSync(contentDir, { recursive: true });

    const filePath = path.join(contentDir, SNAPSHOT_FILE);
    fs.writeFileSync(filePath, JSON.stringify(snapshot, null, 2), 'utf-8');
}

/**
 * Update a single entry keyed by identifier in the co-located snapshot.
 */
export function updateSnapshotEntry(
    contentDir: string,
    identifier: string,
    entry: SnapshotEntry
): void {
    const snapshot = loadSnapshot(contentDir);
    snapshot[identifier] = entry;
    saveSnapshot(contentDir, snapshot);
}

/**
 * Build a SnapshotEntry from a pulled contentlet record.
 */
export function buildSnapshotEntry(
    record: ContentletRecord,
    filename: string,
    hash: string,
    instanceName: string
): SnapshotEntry {
    return {
        file: filename,
        title: (record['title'] as string) || '',
        hash,
        pulledAt: new Date().toISOString(),
        inode: record['inode'] as string,
        source: instanceName,
        modDate: (record['modDate'] as string) || ''
    };
}

/**
 * Remove an entry by identifier from the co-located snapshot.
 */
export function removeSnapshotEntry(contentDir: string, identifier: string): void {
    const snapshot = loadSnapshot(contentDir);
    delete snapshot[identifier];
    saveSnapshot(contentDir, snapshot);
}

/**
 * Find a snapshot entry by matching filename against entry.file values.
 * Returns [identifier, entry] or null.
 */
export function findEntryByFile(
    snapshot: SnapshotStore,
    filename: string
): [string, SnapshotEntry] | null {
    for (const [identifier, entry] of Object.entries(snapshot)) {
        if (entry.file === filename) {
            return [identifier, entry];
        }
    }

    return null;
}

/**
 * Find a snapshot entry by looking up an identifier across all
 * .snapshot.json files under a root directory.
 */
export function findSnapshotEntry(
    projectDir: string,
    identifier: string
): { contentDir: string; entry: SnapshotEntry } | null {
    const snapshotFiles = walkSnapshotFiles(projectDir);

    for (const snapshotFile of snapshotFiles) {
        const raw = fs.readFileSync(snapshotFile, 'utf-8');
        const snapshot = JSON.parse(raw) as SnapshotStore;

        if (snapshot[identifier]) {
            return {
                contentDir: path.dirname(snapshotFile),
                entry: snapshot[identifier]
            };
        }
    }

    return null;
}

// ─── Content Hashing ────────────────────────────────────────────────────────

/**
 * Set of metadata keys to strip from frontmatter before hashing.
 */
const metadataKeySet = new Set<string>(METADATA_KEYS);

/**
 * Compute a deterministic SHA-256 hash of the user-editable content in a .md file,
 * including the contents of any referenced binary sidecar files.
 *
 * The hash is independent of YAML formatting (key order, whitespace, quote style).
 * Algorithm:
 * 1. Parse .md with gray-matter → frontmatter + body
 * 2. Strip metadata keys from frontmatter
 * 3. Sort remaining keys alphabetically
 * 4. For values starting with `./` (binary sidecar refs), hash the file contents
 *    and use the hash as the value instead of the path string
 * 5. Add trimmed body as `__body__` key
 * 6. JSON.stringify the sorted object
 * 7. SHA-256 hash
 */
export function computeContentHash(filePath: string): string {
    const raw = fs.readFileSync(filePath, 'utf-8');

    return computeContentHashFromString(raw, path.dirname(filePath));
}

/**
 * Compute a deterministic SHA-256 hash from an in-memory content string.
 * Avoids a disk read when the content is already available (e.g. right after writing a file).
 */
export function computeContentHashFromString(content: string, fileDir: string): string {
    const { data: frontmatter, content: body } = matter(content);

    // Strip metadata keys, keep only user fields
    const userFields: Record<string, unknown> = {};
    const keys = Object.keys(frontmatter)
        .filter((key) => !metadataKeySet.has(key))
        .sort();

    for (const key of keys) {
        const value = frontmatter[key];

        // If value is a local binary sidecar reference, hash the file contents
        if (typeof value === 'string' && value.startsWith('./')) {
            const absPath = path.resolve(fileDir, value);
            if (fs.existsSync(absPath)) {
                userFields[key] = `__binary__:${computeBinaryHash(absPath)}`;
            } else {
                userFields[key] = value;
            }
        } else {
            userFields[key] = value;
        }
    }

    // Add body content
    userFields['__body__'] = body.trim();

    const serialized = JSON.stringify(userFields);
    return createHash('sha256').update(serialized).digest('hex');
}

/**
 * Compute a SHA-256 hash of a binary file's contents.
 */
export function computeBinaryHash(filePath: string): string {
    const buffer = fs.readFileSync(filePath);
    return createHash('sha256').update(buffer).digest('hex');
}

// ─── File State Detection ───────────────────────────────────────────────────

/**
 * Determine the state of a content file relative to the snapshot.
 * The snapshot is keyed by identifier, so we look up by matching entry.file
 * against the filename.
 *
 * - `'unchanged'` — hash matches snapshot entry
 * - `'modified'` — hash differs from snapshot entry
 * - `'new'` — file exists but no snapshot entry
 * - `'deleted'` — snapshot entry exists but file doesn't exist
 */
export function getFileState(filePath: string, snapshot: SnapshotStore): FileState {
    const filename = path.basename(filePath);
    const match = findEntryByFile(snapshot, filename);
    const fileExists = fs.existsSync(filePath);

    if (!match && fileExists) {
        return 'new';
    }

    if (match && !fileExists) {
        return 'deleted';
    }

    if (!match && !fileExists) {
        return 'deleted';
    }

    // Both entry and file exist — compare hashes
    const currentHash = computeContentHash(filePath);
    return currentHash === match![1].hash ? 'unchanged' : 'modified';
}

/**
 * Recursively collect all .md files under a directory.
 */
function walkMdFiles(dir: string): string[] {
    const results: string[] = [];

    if (!fs.existsSync(dir)) {
        return results;
    }

    const entries = fs.readdirSync(dir, { withFileTypes: true });

    for (const entry of entries) {
        if (entry.name.startsWith('.')) {
            continue;
        }
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            results.push(...walkMdFiles(fullPath));
        } else if (entry.isFile() && entry.name.endsWith('.md')) {
            results.push(fullPath);
        }
    }

    return results;
}

/**
 * Recursively find all .snapshot.json files under a directory.
 */
function walkSnapshotFiles(dir: string): string[] {
    const results: string[] = [];

    if (!fs.existsSync(dir)) {
        return results;
    }

    const entries = fs.readdirSync(dir, { withFileTypes: true });

    for (const entry of entries) {
        if (entry.name.startsWith('.') && entry.name !== SNAPSHOT_FILE) {
            continue;
        }
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            results.push(...walkSnapshotFiles(fullPath));
        } else if (entry.name === SNAPSHOT_FILE) {
            results.push(fullPath);
        }
    }

    return results;
}

/**
 * Check if a file path (relative to contentDir) matches any ignore pattern.
 * Supports simple glob patterns: `dir/**`, `*.ext`, `dir/file`.
 */
function matchesIgnorePattern(relativePath: string, patterns: string[]): boolean {
    for (const pattern of patterns) {
        // Pattern ending with /** matches anything under that directory
        if (pattern.endsWith('/**')) {
            const prefix = pattern.slice(0, -3);
            if (relativePath.startsWith(prefix + '/') || relativePath === prefix) {
                return true;
            }
        }
        // Pattern starting with * matches by extension
        else if (pattern.startsWith('*.')) {
            const ext = pattern.slice(1);
            if (relativePath.endsWith(ext)) {
                return true;
            }
        }
        // Exact match
        else if (relativePath === pattern) {
            return true;
        }
    }

    return false;
}

/**
 * Scan all .md content files under a root directory and compute their states.
 * Walks all content type directories, loads co-located .snapshot.json for each,
 * and computes states for .md files. Also detects deleted files.
 * Respects `.dotcliignore` if present.
 */
export function scanContentFiles(rootDir: string): Map<string, FileState> {
    const states = new Map<string, FileState>();

    // Build ignore patterns from .dotcliignore if present
    const ignorePatterns: string[] = [];
    const ignorePath = path.join(rootDir, '.dotcliignore');
    if (fs.existsSync(ignorePath)) {
        const ignoreContent = fs.readFileSync(ignorePath, 'utf-8');
        const lines = ignoreContent
            .split('\n')
            .map((line) => line.trim())
            .filter((line) => line.length > 0 && !line.startsWith('#'));
        ignorePatterns.push(...lines);
    }

    // Find all .md files
    const allFiles = walkMdFiles(rootDir);

    // Group files by their content type directory (parent dir)
    const filesByDir = new Map<string, string[]>();
    for (const file of allFiles) {
        const dir = path.dirname(file);
        const group = filesByDir.get(dir) ?? [];
        group.push(file);
        filesByDir.set(dir, group);
    }

    // For each directory with .md files, load its snapshot and compute states
    for (const [dir, files] of filesByDir) {
        const snapshot = loadSnapshot(dir);

        for (const file of files) {
            const relativePath = path.relative(rootDir, file);
            if (!matchesIgnorePattern(relativePath, ignorePatterns)) {
                states.set(file, getFileState(file, snapshot));
            }
        }

        // Detect deleted files: snapshot entries whose files no longer exist in this dir
        for (const [, entry] of Object.entries(snapshot)) {
            const fullPath = path.join(dir, entry.file);
            if (!states.has(fullPath) && !fs.existsSync(fullPath)) {
                states.set(fullPath, 'deleted');
            }
        }
    }

    // Also scan directories that have .snapshot.json but may have no .md files left
    const snapshotFiles = walkSnapshotFiles(rootDir);
    for (const snapshotFile of snapshotFiles) {
        const dir = path.dirname(snapshotFile);
        if (!filesByDir.has(dir)) {
            // This dir has a snapshot but no .md files — all entries are deleted
            const snapshot = loadSnapshot(dir);
            for (const [, entry] of Object.entries(snapshot)) {
                const fullPath = path.join(dir, entry.file);
                if (!states.has(fullPath)) {
                    states.set(fullPath, 'deleted');
                }
            }
        }
    }

    return states;
}

// ─── Conflict Detection ─────────────────────────────────────────────────────

/**
 * Check if a snapshot entry conflicts with the server state.
 * A conflict occurs when the server inode has changed since the last pull,
 * indicating someone else modified the content on the server.
 */
export function checkConflict(
    entry: SnapshotEntry,
    serverInode: string
): { hasConflict: boolean; reason?: string } {
    if (entry.inode !== serverInode) {
        return {
            hasConflict: true,
            reason: `Server inode changed: local=${entry.inode}, server=${serverInode}`
        };
    }

    return { hasConflict: false };
}

// ─── Migration ──────────────────────────────────────────────────────────────

/**
 * One-time migration from .dotcli/snapshots/*.json to co-located .snapshot.json files.
 * Reads old per-instance snapshot files, parses each entry's file path to determine
 * the content type directory, and writes co-located snapshot files.
 *
 * Returns true if migration occurred.
 * Does NOT delete old files — let the user clean up.
 */
export function migrateFromDotcli(projectDir: string): boolean {
    const oldSnapshotsDir = path.join(projectDir, DOTCLI_DIR, 'snapshots');

    if (!fs.existsSync(oldSnapshotsDir)) {
        return false;
    }

    const files = fs.readdirSync(oldSnapshotsDir).filter((f) => f.endsWith('.json'));
    if (files.length === 0) {
        return false;
    }

    let migrated = false;

    for (const file of files) {
        const filePath = path.join(oldSnapshotsDir, file);
        const raw = fs.readFileSync(filePath, 'utf-8');

        let oldSnapshot: Record<
            string,
            { hash: string; pulledAt: string; inode: string; identifier: string }
        >;
        try {
            oldSnapshot = JSON.parse(raw);
        } catch {
            continue;
        }

        // Group entries by their content type directory
        const byDir = new Map<string, SnapshotStore>();

        for (const [oldFilePath, oldEntry] of Object.entries(oldSnapshot)) {
            // Old filePath is absolute, e.g. /project/default/content/Blog/e5e92e.md
            // We need to determine the contentDir from it
            const contentDir = path.dirname(oldFilePath);
            const filename = path.basename(oldFilePath);

            const dirSnapshot = byDir.get(contentDir) ?? {};
            dirSnapshot[oldEntry.identifier] = {
                file: filename,
                title: '', // Old snapshots don't have title
                hash: oldEntry.hash,
                pulledAt: oldEntry.pulledAt,
                inode: oldEntry.inode,
                source: '', // Old snapshots don't track source
                modDate: '' // Old snapshots don't track modDate
            };
            byDir.set(contentDir, dirSnapshot);
        }

        // Write co-located snapshots
        for (const [contentDir, snapshot] of byDir) {
            const snapshotPath = path.join(contentDir, SNAPSHOT_FILE);
            if (!fs.existsSync(snapshotPath)) {
                if (fs.existsSync(contentDir)) {
                    saveSnapshot(contentDir, snapshot);
                    migrated = true;
                }
            }
        }
    }

    return migrated;
}
