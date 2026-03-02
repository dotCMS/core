import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

import {
    checkConflict,
    computeBinaryHash,
    computeContentHash,
    findEntryByFile,
    findSnapshotEntry,
    getFileState,
    loadSnapshot,
    migrateFromDotcli,
    removeSnapshotEntry,
    saveSnapshot,
    scanContentFiles,
    updateSnapshotEntry
} from '../snapshot';

import type { SnapshotEntry, SnapshotStore } from '../types';

describe('snapshot', () => {
    let tmpDir: string;

    beforeEach(() => {
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-snapshot-'));
    });

    afterEach(() => {
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    // ─── Helper Functions ───────────────────────────────────────────────────

    function writeContentFile(filePath: string, frontmatter: string, body: string): void {
        const dir = path.dirname(filePath);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
        fs.writeFileSync(filePath, `---\n${frontmatter}---\n${body}`, 'utf-8');
    }

    function makeEntry(overrides: Partial<SnapshotEntry> = {}): SnapshotEntry {
        return {
            file: 'abc123.md',
            title: 'Test Post',
            hash: 'abc123',
            pulledAt: '2025-01-01T00:00:00Z',
            inode: 'inode-001',
            source: 'demo',
            modDate: '2025-01-01T00:00:00Z',
            ...overrides
        };
    }

    // ─── loadSnapshot / saveSnapshot ────────────────────────────────────────

    describe('loadSnapshot', () => {
        it('should return empty object when snapshot file does not exist', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });
            const result = loadSnapshot(contentDir);
            expect(result).toEqual({});
        });

        it('should load an existing snapshot file', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });

            const snapshot: SnapshotStore = {
                'id-001': makeEntry()
            };
            fs.writeFileSync(
                path.join(contentDir, '.snapshot.json'),
                JSON.stringify(snapshot),
                'utf-8'
            );

            const result = loadSnapshot(contentDir);
            expect(result).toEqual(snapshot);
            expect(result['id-001'].hash).toBe('abc123');
        });
    });

    describe('saveSnapshot', () => {
        it('should create dir and write snapshot', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            const snapshot: SnapshotStore = {
                'id-001': makeEntry()
            };

            saveSnapshot(contentDir, snapshot);

            const filePath = path.join(contentDir, '.snapshot.json');
            expect(fs.existsSync(filePath)).toBe(true);

            const loaded = loadSnapshot(contentDir);
            expect(loaded).toEqual(snapshot);
        });

        it('should overwrite an existing snapshot', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            saveSnapshot(contentDir, { 'id-a': makeEntry({ file: 'a.md' }) });
            saveSnapshot(contentDir, { 'id-b': makeEntry({ file: 'b.md', hash: 'xyz' }) });

            const loaded = loadSnapshot(contentDir);
            expect(loaded['id-a']).toBeUndefined();
            expect(loaded['id-b'].hash).toBe('xyz');
        });
    });

    describe('loadSnapshot / saveSnapshot round-trip', () => {
        it('should preserve all entry fields through round-trip', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            const entry: SnapshotEntry = {
                file: 'e5e92e.md',
                title: 'My Post',
                hash: 'sha256-hash-value',
                pulledAt: '2025-06-15T10:30:00Z',
                inode: 'abc-def-123',
                source: 'staging',
                modDate: '2025-06-15T10:30:00Z'
            };
            const snapshot: SnapshotStore = { 'id-456-789': entry };

            saveSnapshot(contentDir, snapshot);
            const loaded = loadSnapshot(contentDir);

            expect(loaded['id-456-789']).toEqual(entry);
        });
    });

    // ─── updateSnapshotEntry / removeSnapshotEntry ──────────────────────────

    describe('updateSnapshotEntry', () => {
        it('should add a new entry to an empty snapshot', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });

            const entry = makeEntry({ hash: 'new-hash' });
            updateSnapshotEntry(contentDir, 'id-new', entry);

            const loaded = loadSnapshot(contentDir);
            expect(loaded['id-new']).toEqual(entry);
        });

        it('should update an existing entry', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            saveSnapshot(contentDir, { 'id-1': makeEntry({ hash: 'old' }) });

            updateSnapshotEntry(contentDir, 'id-1', makeEntry({ hash: 'updated' }));

            const loaded = loadSnapshot(contentDir);
            expect(loaded['id-1'].hash).toBe('updated');
        });

        it('should preserve other entries when updating one', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            saveSnapshot(contentDir, {
                'id-a': makeEntry({ file: 'a.md', hash: 'a-hash' }),
                'id-b': makeEntry({ file: 'b.md', hash: 'b-hash' })
            });

            updateSnapshotEntry(contentDir, 'id-a', makeEntry({ file: 'a.md', hash: 'a-updated' }));

            const loaded = loadSnapshot(contentDir);
            expect(loaded['id-a'].hash).toBe('a-updated');
            expect(loaded['id-b'].hash).toBe('b-hash');
        });
    });

    describe('removeSnapshotEntry', () => {
        it('should remove an entry from the snapshot', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            saveSnapshot(contentDir, {
                'id-a': makeEntry({ file: 'a.md', hash: 'a' }),
                'id-b': makeEntry({ file: 'b.md', hash: 'b' })
            });

            removeSnapshotEntry(contentDir, 'id-a');

            const loaded = loadSnapshot(contentDir);
            expect(loaded['id-a']).toBeUndefined();
            expect(loaded['id-b'].hash).toBe('b');
        });

        it('should be a no-op if entry does not exist', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            saveSnapshot(contentDir, { 'id-a': makeEntry() });

            removeSnapshotEntry(contentDir, 'nonexistent');

            const loaded = loadSnapshot(contentDir);
            expect(loaded['id-a']).toBeDefined();
        });
    });

    // ─── findEntryByFile ───────────────────────────────────────────────────

    describe('findEntryByFile', () => {
        it('should find entry by filename', () => {
            const snapshot: SnapshotStore = {
                'id-1': makeEntry({ file: 'abc123.md' }),
                'id-2': makeEntry({ file: 'def456.md' })
            };

            const result = findEntryByFile(snapshot, 'def456.md');
            expect(result).not.toBeNull();
            expect(result![0]).toBe('id-2');
            expect(result![1].file).toBe('def456.md');
        });

        it('should return null when not found', () => {
            const snapshot: SnapshotStore = {
                'id-1': makeEntry({ file: 'abc123.md' })
            };

            const result = findEntryByFile(snapshot, 'nonexistent.md');
            expect(result).toBeNull();
        });
    });

    // ─── findSnapshotEntry ─────────────────────────────────────────────────

    describe('findSnapshotEntry', () => {
        it('should find entry across multiple snapshot files', () => {
            const blogDir = path.join(tmpDir, 'default', 'content', 'Blog');
            const authorDir = path.join(tmpDir, 'default', 'content', 'Author');

            saveSnapshot(blogDir, { 'blog-id': makeEntry({ file: 'blog.md' }) });
            saveSnapshot(authorDir, { 'author-id': makeEntry({ file: 'author.md' }) });

            const result = findSnapshotEntry(tmpDir, 'author-id');
            expect(result).not.toBeNull();
            expect(result!.contentDir).toBe(authorDir);
            expect(result!.entry.file).toBe('author.md');
        });

        it('should return null when identifier not found', () => {
            const blogDir = path.join(tmpDir, 'default', 'content', 'Blog');
            saveSnapshot(blogDir, { 'blog-id': makeEntry() });

            const result = findSnapshotEntry(tmpDir, 'nonexistent');
            expect(result).toBeNull();
        });
    });

    // ─── computeContentHash ─────────────────────────────────────────────────

    describe('computeContentHash', () => {
        it('should produce a SHA-256 hex string', () => {
            const filePath = path.join(tmpDir, 'test.md');
            writeContentFile(
                filePath,
                'title: Hello\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'This is the body.'
            );

            const hash = computeContentHash(filePath);
            expect(hash).toMatch(/^[a-f0-9]{64}$/);
        });

        it('should produce the same hash regardless of YAML key order', () => {
            const file1 = path.join(tmpDir, 'order1.md');
            const file2 = path.join(tmpDir, 'order2.md');

            writeContentFile(
                file1,
                'title: Hello\nauthor: Alice\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body content.'
            );
            writeContentFile(
                file2,
                'author: Alice\ntitle: Hello\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body content.'
            );

            expect(computeContentHash(file1)).toBe(computeContentHash(file2));
        });

        it('should produce the same hash regardless of YAML whitespace', () => {
            const file1 = path.join(tmpDir, 'ws1.md');
            const file2 = path.join(tmpDir, 'ws2.md');

            writeContentFile(
                file1,
                'title: Hello\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );
            writeContentFile(
                file2,
                'title:   Hello\ncontentType:   Blog\nidentifier:   id1\nlanguage:   en\ninode:   abc\n',
                'Body.'
            );

            expect(computeContentHash(file1)).toBe(computeContentHash(file2));
        });

        it('should produce the same hash regardless of YAML quote style', () => {
            const file1 = path.join(tmpDir, 'quote1.md');
            const file2 = path.join(tmpDir, 'quote2.md');

            writeContentFile(
                file1,
                'title: Hello World\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );
            writeContentFile(
                file2,
                "title: 'Hello World'\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n",
                'Body.'
            );

            expect(computeContentHash(file1)).toBe(computeContentHash(file2));
        });

        it('should change hash when a user field value changes', () => {
            const file1 = path.join(tmpDir, 'val1.md');
            const file2 = path.join(tmpDir, 'val2.md');

            writeContentFile(
                file1,
                'title: Hello\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );
            writeContentFile(
                file2,
                'title: Goodbye\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );

            expect(computeContentHash(file1)).not.toBe(computeContentHash(file2));
        });

        it('should change hash when body changes', () => {
            const file1 = path.join(tmpDir, 'body1.md');
            const file2 = path.join(tmpDir, 'body2.md');

            writeContentFile(
                file1,
                'title: Same\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Original body.'
            );
            writeContentFile(
                file2,
                'title: Same\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Changed body.'
            );

            expect(computeContentHash(file1)).not.toBe(computeContentHash(file2));
        });

        it('should NOT change hash when only metadata keys change', () => {
            const file1 = path.join(tmpDir, 'meta1.md');
            const file2 = path.join(tmpDir, 'meta2.md');

            writeContentFile(
                file1,
                'title: Same\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: inode-111\n',
                'Same body.'
            );
            writeContentFile(
                file2,
                'title: Same\ncontentType: Article\nidentifier: id2\nlanguage: fr\ninode: inode-222\n',
                'Same body.'
            );

            expect(computeContentHash(file1)).toBe(computeContentHash(file2));
        });

        it('should NOT change hash when bodyField metadata changes', () => {
            const file1 = path.join(tmpDir, 'bf1.md');
            const file2 = path.join(tmpDir, 'bf2.md');

            writeContentFile(
                file1,
                'title: Same\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\nbodyField: body\n',
                'Same.'
            );
            writeContentFile(
                file2,
                'title: Same\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\nbodyField: content\n',
                'Same.'
            );

            expect(computeContentHash(file1)).toBe(computeContentHash(file2));
        });

        it('should ignore trailing whitespace differences in body', () => {
            const file1 = path.join(tmpDir, 'trim1.md');
            const file2 = path.join(tmpDir, 'trim2.md');

            writeContentFile(
                file1,
                'title: Same\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body text.\n'
            );
            writeContentFile(
                file2,
                'title: Same\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body text.\n\n\n'
            );

            expect(computeContentHash(file1)).toBe(computeContentHash(file2));
        });
    });

    // ─── computeBinaryHash ──────────────────────────────────────────────────

    describe('computeBinaryHash', () => {
        it('should produce a SHA-256 hex string for binary content', () => {
            const filePath = path.join(tmpDir, 'image.png');
            const buffer = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
            fs.writeFileSync(filePath, buffer);

            const hash = computeBinaryHash(filePath);
            expect(hash).toMatch(/^[a-f0-9]{64}$/);
        });

        it('should produce different hashes for different content', () => {
            const file1 = path.join(tmpDir, 'bin1');
            const file2 = path.join(tmpDir, 'bin2');
            fs.writeFileSync(file1, Buffer.from('content-a'));
            fs.writeFileSync(file2, Buffer.from('content-b'));

            expect(computeBinaryHash(file1)).not.toBe(computeBinaryHash(file2));
        });

        it('should produce same hash for identical content', () => {
            const file1 = path.join(tmpDir, 'same1');
            const file2 = path.join(tmpDir, 'same2');
            fs.writeFileSync(file1, Buffer.from('identical'));
            fs.writeFileSync(file2, Buffer.from('identical'));

            expect(computeBinaryHash(file1)).toBe(computeBinaryHash(file2));
        });
    });

    // ─── getFileState ───────────────────────────────────────────────────────

    describe('getFileState', () => {
        it('should return "new" when file exists but has no snapshot entry', () => {
            const filePath = path.join(tmpDir, 'new-file.md');
            writeContentFile(
                filePath,
                'title: New\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );

            const state = getFileState(filePath, {});
            expect(state).toBe('new');
        });

        it('should return "deleted" when snapshot entry exists but file does not', () => {
            const filePath = path.join(tmpDir, 'deleted.md');
            const snapshot: SnapshotStore = {
                'id-1': makeEntry({ file: 'deleted.md' })
            };

            const state = getFileState(filePath, snapshot);
            expect(state).toBe('deleted');
        });

        it('should return "unchanged" when file hash matches snapshot', () => {
            const filePath = path.join(tmpDir, 'unchanged.md');
            writeContentFile(
                filePath,
                'title: Hello\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );

            const hash = computeContentHash(filePath);
            const snapshot: SnapshotStore = {
                'id-1': makeEntry({ file: 'unchanged.md', hash })
            };

            const state = getFileState(filePath, snapshot);
            expect(state).toBe('unchanged');
        });

        it('should return "modified" when file hash differs from snapshot', () => {
            const filePath = path.join(tmpDir, 'modified.md');
            writeContentFile(
                filePath,
                'title: Hello\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );

            const snapshot: SnapshotStore = {
                'id-1': makeEntry({ file: 'modified.md', hash: 'stale-hash' })
            };

            const state = getFileState(filePath, snapshot);
            expect(state).toBe('modified');
        });
    });

    // ─── scanContentFiles ───────────────────────────────────────────────────

    describe('scanContentFiles', () => {
        it('should find all .md files and categorize them', () => {
            const contentDir = path.join(tmpDir, 'content', 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });

            const newFile = path.join(contentDir, 'new-post.md');
            writeContentFile(
                newFile,
                'title: New Post\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'New body.'
            );

            const unchangedFile = path.join(contentDir, 'old-post.md');
            writeContentFile(
                unchangedFile,
                'title: Old Post\ncontentType: Blog\nidentifier: id2\nlanguage: en\ninode: def\n',
                'Old body.'
            );
            const unchangedHash = computeContentHash(unchangedFile);

            // Create co-located snapshot
            saveSnapshot(contentDir, {
                id2: makeEntry({ file: 'old-post.md', hash: unchangedHash }),
                'id-deleted': makeEntry({ file: 'gone.md', hash: 'deleted-hash' })
            });

            const states = scanContentFiles(path.join(tmpDir, 'content'));

            expect(states.get(newFile)).toBe('new');
            expect(states.get(unchangedFile)).toBe('unchanged');
            expect(states.get(path.join(contentDir, 'gone.md'))).toBe('deleted');
        });

        it('should detect modified files', () => {
            const contentDir = path.join(tmpDir, 'content', 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });

            const filePath = path.join(contentDir, 'modified.md');
            writeContentFile(
                filePath,
                'title: Original\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );

            saveSnapshot(contentDir, {
                id1: makeEntry({ file: 'modified.md', hash: 'old-hash-that-no-longer-matches' })
            });

            const states = scanContentFiles(path.join(tmpDir, 'content'));
            expect(states.get(filePath)).toBe('modified');
        });

        it('should respect .dotcliignore patterns', () => {
            const rootDir = path.join(tmpDir, 'content');
            const contentDir = path.join(rootDir, 'Blog');
            fs.mkdirSync(path.join(rootDir, 'drafts'), { recursive: true });
            fs.mkdirSync(contentDir, { recursive: true });

            const includedFile = path.join(contentDir, 'included.md');
            writeContentFile(
                includedFile,
                'title: Included\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );

            const ignoredFile = path.join(rootDir, 'drafts', 'ignored.md');
            writeContentFile(
                ignoredFile,
                'title: Ignored\ncontentType: Blog\nidentifier: id2\nlanguage: en\ninode: def\n',
                'Draft body.'
            );

            // Create .dotcliignore
            fs.writeFileSync(path.join(rootDir, '.dotcliignore'), 'drafts/**\n', 'utf-8');

            const states = scanContentFiles(rootDir);

            expect(states.has(includedFile)).toBe(true);
            expect(states.has(ignoredFile)).toBe(false);
        });

        it('should skip hidden directories like .dotcli and .git', () => {
            const projectDir = path.join(tmpDir, 'project');
            const contentDir = path.join(projectDir, 'default', 'content', 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });
            fs.mkdirSync(path.join(projectDir, '.dotcli', 'cache'), { recursive: true });
            fs.mkdirSync(path.join(projectDir, '.git', 'objects'), { recursive: true });

            const visibleFile = path.join(contentDir, 'post.md');
            writeContentFile(
                visibleFile,
                'title: Visible\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );

            const hiddenFile1 = path.join(projectDir, '.dotcli', 'cache', 'demo.md');
            fs.writeFileSync(hiddenFile1, '---\ntitle: Hidden\n---\nShould be skipped.\n', 'utf-8');

            const hiddenFile2 = path.join(projectDir, '.git', 'objects', 'pack.md');
            fs.writeFileSync(hiddenFile2, '---\ntitle: Git\n---\nShould be skipped.\n', 'utf-8');

            const states = scanContentFiles(projectDir);

            expect(states.has(visibleFile)).toBe(true);
            expect(states.has(hiddenFile1)).toBe(false);
            expect(states.has(hiddenFile2)).toBe(false);
        });

        it('should handle empty content directory', () => {
            const contentDir = path.join(tmpDir, 'empty-content');
            fs.mkdirSync(contentDir, { recursive: true });

            const states = scanContentFiles(contentDir);
            expect(states.size).toBe(0);
        });

        it('should handle .dotcliignore with comments and blank lines', () => {
            const rootDir = path.join(tmpDir, 'content');
            const contentDir = path.join(rootDir, 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });
            fs.mkdirSync(path.join(rootDir, 'temp'), { recursive: true });

            const includedFile = path.join(contentDir, 'keep.md');
            writeContentFile(
                includedFile,
                'title: Keep\ncontentType: Blog\nidentifier: id1\nlanguage: en\ninode: abc\n',
                'Body.'
            );

            const ignoredFile = path.join(rootDir, 'temp', 'skip.md');
            writeContentFile(
                ignoredFile,
                'title: Skip\ncontentType: Blog\nidentifier: id2\nlanguage: en\ninode: def\n',
                'Body.'
            );

            fs.writeFileSync(
                path.join(rootDir, '.dotcliignore'),
                '# This is a comment\n\ntemp/**\n\n# Another comment\n',
                'utf-8'
            );

            const states = scanContentFiles(rootDir);
            expect(states.has(includedFile)).toBe(true);
            expect(states.has(ignoredFile)).toBe(false);
        });
    });

    // ─── checkConflict ──────────────────────────────────────────────────────

    describe('checkConflict', () => {
        it('should detect conflict when inodes differ', () => {
            const entry = makeEntry({ inode: 'local-inode-123' });
            const result = checkConflict(entry, 'server-inode-456');

            expect(result.hasConflict).toBe(true);
            expect(result.reason).toContain('local-inode-123');
            expect(result.reason).toContain('server-inode-456');
        });

        it('should report no conflict when inodes match', () => {
            const entry = makeEntry({ inode: 'same-inode' });
            const result = checkConflict(entry, 'same-inode');

            expect(result.hasConflict).toBe(false);
            expect(result.reason).toBeUndefined();
        });
    });

    // ─── migrateFromDotcli ─────────────────────────────────────────────────

    describe('migrateFromDotcli', () => {
        it('should return false when no old snapshots dir exists', () => {
            const result = migrateFromDotcli(tmpDir);
            expect(result).toBe(false);
        });

        it('should migrate old per-instance snapshots to co-located files', () => {
            // Create old-style snapshot
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });
            writeContentFile(
                path.join(contentDir, 'abc123.md'),
                'title: Test\ncontentType: Blog\n',
                'Body.'
            );

            const oldSnapshotsDir = path.join(tmpDir, '.dotcli', 'snapshots');
            fs.mkdirSync(oldSnapshotsDir, { recursive: true });

            const filePath = path.join(contentDir, 'abc123.md');
            const oldSnapshot = {
                [filePath]: {
                    hash: 'old-hash',
                    pulledAt: '2025-01-01T00:00:00Z',
                    inode: 'inode-1',
                    identifier: 'id-abc123'
                }
            };
            fs.writeFileSync(
                path.join(oldSnapshotsDir, 'demo.json'),
                JSON.stringify(oldSnapshot),
                'utf-8'
            );

            const result = migrateFromDotcli(tmpDir);
            expect(result).toBe(true);

            // Verify co-located snapshot was created
            const newSnapshot = loadSnapshot(contentDir);
            expect(newSnapshot['id-abc123']).toBeDefined();
            expect(newSnapshot['id-abc123'].file).toBe('abc123.md');
            expect(newSnapshot['id-abc123'].hash).toBe('old-hash');
        });

        it('should set empty source for migrated entries', () => {
            // Create old-style snapshot
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });
            writeContentFile(
                path.join(contentDir, 'abc123.md'),
                'title: Test\ncontentType: Blog\n',
                'Body.'
            );

            const oldSnapshotsDir = path.join(tmpDir, '.dotcli', 'snapshots');
            fs.mkdirSync(oldSnapshotsDir, { recursive: true });

            const filePath = path.join(contentDir, 'abc123.md');
            const oldSnapshot = {
                [filePath]: {
                    hash: 'old-hash',
                    pulledAt: '2025-01-01T00:00:00Z',
                    inode: 'inode-1',
                    identifier: 'id-abc123'
                }
            };
            fs.writeFileSync(
                path.join(oldSnapshotsDir, 'demo.json'),
                JSON.stringify(oldSnapshot),
                'utf-8'
            );

            migrateFromDotcli(tmpDir);

            const newSnapshot = loadSnapshot(contentDir);
            expect(newSnapshot['id-abc123'].source).toBe('');
        });

        it('should not overwrite existing co-located snapshots', () => {
            const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
            fs.mkdirSync(contentDir, { recursive: true });

            // Create existing co-located snapshot
            saveSnapshot(contentDir, {
                'id-existing': makeEntry({ file: 'existing.md', hash: 'keep-me' })
            });

            // Create old-style snapshot
            const oldSnapshotsDir = path.join(tmpDir, '.dotcli', 'snapshots');
            fs.mkdirSync(oldSnapshotsDir, { recursive: true });

            const filePath = path.join(contentDir, 'abc123.md');
            const oldSnapshot = {
                [filePath]: {
                    hash: 'old-hash',
                    pulledAt: '2025-01-01T00:00:00Z',
                    inode: 'inode-1',
                    identifier: 'id-old'
                }
            };
            fs.writeFileSync(
                path.join(oldSnapshotsDir, 'demo.json'),
                JSON.stringify(oldSnapshot),
                'utf-8'
            );

            migrateFromDotcli(tmpDir);

            // Existing snapshot should be preserved
            const snapshot = loadSnapshot(contentDir);
            expect(snapshot['id-existing'].hash).toBe('keep-me');
        });
    });
});
