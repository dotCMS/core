/**
 * Snapshot tracking for @dotcms/cli.
 * Tracks content file states (hashing, change detection, conflict detection)
 * between pull and push operations.
 */
import matter from 'gray-matter';

import { createHash } from 'node:crypto';
import * as fs from 'node:fs';
import * as path from 'node:path';

import {
    DOTCLI_DIR,
    METADATA_KEYS,
    SNAPSHOT_FILE,
    type FileState,
    type SnapshotEntry,
    type SnapshotStore
} from './types';

// ─── Snapshot Persistence ───────────────────────────────────────────────────

/**
 * Resolves the path to the snapshot file for a project directory.
 */
function snapshotPath(projectDir: string): string {
    return path.join(projectDir, DOTCLI_DIR, SNAPSHOT_FILE);
}

/**
 * Load the snapshot store from `.dotcli/snapshot.json`.
 * Returns an empty object if the file doesn't exist.
 */
export function loadSnapshot(projectDir: string): SnapshotStore {
    const filePath = snapshotPath(projectDir);

    if (!fs.existsSync(filePath)) {
        return {};
    }

    const raw = fs.readFileSync(filePath, 'utf-8');
    return JSON.parse(raw) as SnapshotStore;
}

/**
 * Save a snapshot store to `.dotcli/snapshot.json`.
 * Creates the `.dotcli` directory if it doesn't exist.
 */
export function saveSnapshot(projectDir: string, snapshot: SnapshotStore): void {
    const filePath = snapshotPath(projectDir);
    const dir = path.dirname(filePath);

    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }

    fs.writeFileSync(filePath, JSON.stringify(snapshot, null, 2), 'utf-8');
}

/**
 * Update a single entry in the snapshot store.
 * Loads the current snapshot, updates the entry, and saves.
 */
export function updateSnapshotEntry(
    projectDir: string,
    filePath: string,
    entry: SnapshotEntry
): void {
    const snapshot = loadSnapshot(projectDir);
    snapshot[filePath] = entry;
    saveSnapshot(projectDir, snapshot);
}

/**
 * Remove a single entry from the snapshot store.
 * Loads the current snapshot, removes the entry, and saves.
 */
export function removeSnapshotEntry(projectDir: string, filePath: string): void {
    const snapshot = loadSnapshot(projectDir);
    delete snapshot[filePath];
    saveSnapshot(projectDir, snapshot);
}

// ─── Content Hashing ────────────────────────────────────────────────────────

/**
 * Set of metadata keys to strip from frontmatter before hashing.
 */
const metadataKeySet = new Set<string>(METADATA_KEYS);

/**
 * Compute a deterministic SHA-256 hash of the user-editable content in a .md file.
 *
 * The hash is independent of YAML formatting (key order, whitespace, quote style).
 * Algorithm:
 * 1. Parse .md with gray-matter → frontmatter + body
 * 2. Strip metadata keys from frontmatter
 * 3. Sort remaining keys alphabetically
 * 4. Add trimmed body as `__body__` key
 * 5. JSON.stringify the sorted object
 * 6. SHA-256 hash
 */
export function computeContentHash(filePath: string): string {
    const raw = fs.readFileSync(filePath, 'utf-8');
    const { data: frontmatter, content: body } = matter(raw);

    // Strip metadata keys, keep only user fields
    const userFields: Record<string, unknown> = {};
    const keys = Object.keys(frontmatter)
        .filter((key) => !metadataKeySet.has(key))
        .sort();

    for (const key of keys) {
        userFields[key] = frontmatter[key];
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
 *
 * - `'unchanged'` — hash matches snapshot entry
 * - `'modified'` — hash differs from snapshot entry
 * - `'new'` — file exists but no snapshot entry
 * - `'deleted'` — snapshot entry exists but file doesn't exist
 */
export function getFileState(filePath: string, snapshot: SnapshotStore): FileState {
    const entry = snapshot[filePath];
    const fileExists = fs.existsSync(filePath);

    if (!entry && fileExists) {
        return 'new';
    }

    if (entry && !fileExists) {
        return 'deleted';
    }

    if (!entry && !fileExists) {
        // No snapshot entry and file doesn't exist — treat as deleted
        return 'deleted';
    }

    // Both entry and file exist — compare hashes
    const currentHash = computeContentHash(filePath);
    return currentHash === entry.hash ? 'unchanged' : 'modified';
}

/**
 * Recursively collect all .md files under a directory.
 */
function walkMdFiles(dir: string): string[] {
    const results: string[] = [];
    const entries = fs.readdirSync(dir, { withFileTypes: true });

    for (const entry of entries) {
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
 * Scan all .md content files in a directory and compute their states.
 * Also detects deleted files (snapshot entries with no corresponding file).
 * Respects `.dotcliignore` if present.
 */
export function scanContentFiles(
    contentDir: string,
    snapshot: SnapshotStore
): Map<string, FileState> {
    const states = new Map<string, FileState>();

    // Build ignore patterns from .dotcliignore if present
    const ignorePatterns: string[] = [];
    const ignorePath = path.join(contentDir, '.dotcliignore');
    if (fs.existsSync(ignorePath)) {
        const ignoreContent = fs.readFileSync(ignorePath, 'utf-8');
        const lines = ignoreContent
            .split('\n')
            .map((line) => line.trim())
            .filter((line) => line.length > 0 && !line.startsWith('#'));
        ignorePatterns.push(...lines);
    }

    // Find all .md files
    const allFiles = walkMdFiles(contentDir);

    // Filter out ignored files and compute state for each
    for (const file of allFiles) {
        const relativePath = path.relative(contentDir, file);
        if (!matchesIgnorePattern(relativePath, ignorePatterns)) {
            states.set(file, getFileState(file, snapshot));
        }
    }

    // Detect deleted files: snapshot entries whose files no longer exist
    for (const snapshotFilePath of Object.keys(snapshot)) {
        if (!states.has(snapshotFilePath) && !fs.existsSync(snapshotFilePath)) {
            states.set(snapshotFilePath, 'deleted');
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
