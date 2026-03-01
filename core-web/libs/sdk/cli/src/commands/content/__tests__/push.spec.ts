import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

const mockConsola = {
    info: jest.fn(),
    success: jest.fn(),
    error: jest.fn(),
    warn: jest.fn(),
    log: jest.fn(),
    start: jest.fn()
};
jest.mock('consola', () => ({
    __esModule: true,
    default: mockConsola
}));

jest.mock('../../../core/config', () => ({
    loadConfig: jest.fn(),
    resolveInstance: jest.fn()
}));

jest.mock('../../../core/auth', () => ({
    resolveToken: jest.fn()
}));

jest.mock('../../../core/http', () => ({
    createHttpClient: jest.fn(),
    graphql: jest.fn(),
    put: jest.fn()
}));

jest.mock('../../../core/cache', () => ({
    getCachedContentType: jest.fn()
}));

jest.mock('../../../core/snapshot', () => ({
    loadSnapshot: jest.fn().mockReturnValue({}),
    saveSnapshot: jest.fn(),
    scanContentFiles: jest.fn().mockReturnValue(new Map()),
    computeContentHash: jest.fn().mockReturnValue('hash123'),
    updateSnapshotEntry: jest.fn(),
    checkConflict: jest.fn().mockReturnValue({ hasConflict: false }),
    findEntryByFile: jest.fn().mockReturnValue(null)
}));

jest.mock('../../../handlers/content', () => ({
    parseContentFile: jest.fn(),
    validateContentFile: jest.fn().mockReturnValue({ valid: true, errors: [] }),
    buildPushPayload: jest.fn().mockReturnValue({
        contentlet: { contentType: 'Blog', identifier: 'id-1', title: 'Test' },
        binaries: []
    })
}));

jest.mock('../../../core/resolve', () => ({
    resolveIdentifiersOnServer: jest.fn().mockResolvedValue(new Map())
}));

jest.mock('../../../handlers/binary', () => ({
    buildMultipartPayload: jest.fn()
}));

import { resolveToken } from '../../../core/auth';
import { getCachedContentType } from '../../../core/cache';
import { loadConfig, resolveInstance } from '../../../core/config';
import { createHttpClient, put } from '../../../core/http';
import { resolveIdentifiersOnServer } from '../../../core/resolve';
import { loadSnapshot, scanContentFiles, updateSnapshotEntry } from '../../../core/snapshot';
import { buildPushPayload, parseContentFile, validateContentFile } from '../../../handlers/content';
import { pushCommand } from '../push';

import type { ContentTypeSchema } from '../../../core/types';

const consola = mockConsola;

describe('content push command', () => {
    let tmpDir: string;
    let originalCwd: string;

    const mockSchema: ContentTypeSchema = {
        variable: 'Blog',
        name: 'Blog',
        id: 'ct-id-1',
        fields: [
            {
                variable: 'title',
                name: 'Title',
                fieldType: 'Text',
                dataType: 'TEXT',
                sortOrder: 1,
                required: true,
                fixed: false,
                readOnly: false,
                searchable: true,
                listed: true
            }
        ]
    };

    beforeEach(() => {
        tmpDir = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-push-')));
        originalCwd = process.cwd();
        process.chdir(tmpDir);
        jest.clearAllMocks();

        (loadConfig as jest.Mock).mockReturnValue({
            default: 'demo',
            instances: { demo: { url: 'https://demo.dotcms.com' } }
        });
        (resolveInstance as jest.Mock).mockReturnValue({
            name: 'demo',
            url: 'https://demo.dotcms.com'
        });
        (resolveToken as jest.Mock).mockReturnValue('test-token');
        (createHttpClient as jest.Mock).mockReturnValue(jest.fn());
        (getCachedContentType as jest.Mock).mockReturnValue(mockSchema);
        (put as jest.Mock).mockResolvedValue({
            entity: { identifier: 'id-1', inode: 'new-inode', modDate: '2024-01-02' }
        });
        (validateContentFile as jest.Mock).mockReturnValue({ valid: true, errors: [] });
        (buildPushPayload as jest.Mock).mockReturnValue({
            contentlet: { contentType: 'Blog', identifier: 'id-1', title: 'Test' },
            binaries: []
        });
        (parseContentFile as jest.Mock).mockReturnValue({
            frontmatter: {
                contentType: 'Blog',
                identifier: 'id-1',
                language: 'en-US',
                inode: 'old-inode',
                modDate: '2024-01-01',
                title: 'Test'
            },
            body: 'Hello world',
            filePath: ''
        });
    });

    afterEach(() => {
        process.chdir(originalCwd);
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it('should error when no project found', async () => {
        (loadConfig as jest.Mock).mockImplementation(() => {
            throw new Error('Config file not found');
        });

        await pushCommand.run!({ args: {} } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('No dotcli project'));
    });

    it('should error when no auth token available', async () => {
        (resolveToken as jest.Mock).mockReturnValue(null);

        await pushCommand.run!({ args: {} } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('No auth token'));
    });

    it('should report no changes when nothing modified', async () => {
        (scanContentFiles as jest.Mock).mockReturnValue(new Map([['/some/file.md', 'unchanged']]));

        await pushCommand.run!({ args: {} } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('No changes'));
    });

    it('should scope scan to project root when no files specified', async () => {
        (scanContentFiles as jest.Mock).mockReturnValue(new Map());

        await pushCommand.run!({ args: {} } as never);

        expect(scanContentFiles).toHaveBeenCalledWith(tmpDir);
    });

    it('should push modified files', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(
            testFile,
            '---\ncontentType: Blog\nidentifier: id-1\nlanguage: en-US\ninode: old-inode\nmodDate: 2024-01-01\ntitle: Test\n---\nHello world\n'
        );

        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[testFile, 'modified']]));

        await pushCommand.run!({ args: {} } as never);

        expect(put).toHaveBeenCalled();
        expect(updateSnapshotEntry).toHaveBeenCalledWith(
            path.dirname(testFile),
            expect.any(String),
            expect.objectContaining({ hash: 'hash123', source: 'demo' })
        );
        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('Push complete'));
    });

    it('should show dry-run output without pushing', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, '---\ncontentType: Blog\n---\n');

        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[testFile, 'modified']]));

        await pushCommand.run!({ args: { 'dry-run': true } } as never);

        expect(put).not.toHaveBeenCalled();
        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('Would push'));
    });

    it('should push specific file', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(
            testFile,
            '---\ncontentType: Blog\nidentifier: id-1\nlanguage: en-US\ninode: old-inode\nmodDate: 2024-01-01\ntitle: Test\n---\nHello\n'
        );

        await pushCommand.run!({ args: { files: 'test.md' } } as never);

        expect(put).toHaveBeenCalled();
    });

    it('should handle validation errors', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, '---\ncontentType: Blog\n---\n');

        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[testFile, 'modified']]));
        (validateContentFile as jest.Mock).mockReturnValue({
            valid: false,
            errors: ['contentType is required']
        });

        await pushCommand.run!({ args: {} } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('error'));
    });

    it('should handle retry mode with no previous errors', async () => {
        await pushCommand.run!({ args: { retry: true } } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('No previously failed'));
    });

    it('should handle retry mode with previous errors', async () => {
        const errPath = path.join(tmpDir, '.dotcli', 'last-push-errors.json');
        fs.mkdirSync(path.join(tmpDir, '.dotcli'), { recursive: true });

        const testFile = path.join(tmpDir, 'retry-test.md');
        fs.writeFileSync(
            testFile,
            '---\ncontentType: Blog\nidentifier: id-1\nlanguage: en-US\ninode: old-inode\nmodDate: 2024-01-01\ntitle: Retry\n---\nRetry content\n'
        );

        fs.writeFileSync(
            errPath,
            JSON.stringify({
                timestamp: '2024-01-01',
                failed: [{ file: testFile, error: 'timeout' }]
            })
        );

        await pushCommand.run!({ args: { retry: true } } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('Retrying'));
        // Verify the push completed (either put was called or the summary was logged)
        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('Push complete'));
    });

    it('should bail on first error when --bail is set', async () => {
        const file1 = path.join(tmpDir, 'file1.md');
        const file2 = path.join(tmpDir, 'file2.md');
        fs.writeFileSync(file1, '---\ncontentType: Blog\n---\n');
        fs.writeFileSync(file2, '---\ncontentType: Blog\n---\n');

        (scanContentFiles as jest.Mock).mockReturnValue(
            new Map([
                [file1, 'modified'],
                [file2, 'modified']
            ])
        );

        (getCachedContentType as jest.Mock).mockReturnValue(null);

        await pushCommand.run!({ args: { bail: true } } as never);

        // Should stop after first error (no schema cache)
        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('Failed'));
    });

    // ─── Cross-instance push tests ──────────────────────────────────────────

    describe('cross-instance push', () => {
        it('should strip identifier when destination snapshot has no entry (new content)', async () => {
            const testFile = path.join(tmpDir, 'test.md');
            fs.writeFileSync(
                testFile,
                '---\ncontentType: Blog\nidentifier: source-id\nlanguage: en-US\ninode: old-inode\nmodDate: 2024-01-01\ntitle: Test\n---\nHello\n'
            );

            // File has identifier from source instance, but destination snapshot is empty
            (loadSnapshot as jest.Mock).mockReturnValue({});
            (scanContentFiles as jest.Mock).mockReturnValue(new Map([[testFile, 'new']]));
            const mockContentlet = {
                contentType: 'Blog',
                identifier: 'source-id',
                title: 'Test'
            };
            (buildPushPayload as jest.Mock).mockReturnValue({
                contentlet: mockContentlet,
                binaries: []
            });
            (put as jest.Mock).mockResolvedValue({
                entity: {
                    identifier: 'new-dest-id',
                    inode: 'new-inode',
                    modDate: '2024-01-02'
                }
            });

            await pushCommand.run!({ args: {} } as never);

            expect(put).toHaveBeenCalled();
            // The contentlet should have had identifier deleted before being sent
            expect(mockContentlet.identifier).toBeUndefined();
        });

        it('should use resolved server identifier when content exists on target (cross-instance)', async () => {
            const testFile = path.join(tmpDir, 'test.md');
            fs.writeFileSync(
                testFile,
                '---\ncontentType: Blog\nidentifier: source-id\nlanguage: en-US\ninode: old-inode\nmodDate: 2024-01-01\ntitle: Test\n---\nHello\n'
            );

            // No snapshot entry for this file (new to this instance)
            (loadSnapshot as jest.Mock).mockReturnValue({});
            (scanContentFiles as jest.Mock).mockReturnValue(new Map([[testFile, 'new']]));

            // parseContentFile must return source-id so pre-resolution can find it
            (parseContentFile as jest.Mock).mockReturnValue({
                frontmatter: {
                    contentType: 'Blog',
                    identifier: 'source-id',
                    language: 'en-US',
                    inode: 'old-inode',
                    modDate: '2024-01-01',
                    title: 'Test'
                },
                body: 'Hello',
                filePath: testFile
            });

            // But the content exists on the target server (resolved via batch query)
            (resolveIdentifiersOnServer as jest.Mock).mockResolvedValue(
                new Map([['source-id', { identifier: 'source-id', inode: 'server-inode' }]])
            );

            const mockContentlet = {
                contentType: 'Blog',
                identifier: 'source-id',
                title: 'Test'
            };
            (buildPushPayload as jest.Mock).mockReturnValue({
                contentlet: mockContentlet,
                binaries: []
            });
            (put as jest.Mock).mockResolvedValue({
                entity: {
                    identifier: 'source-id',
                    inode: 'new-server-inode',
                    modDate: '2024-01-02'
                }
            });

            await pushCommand.run!({ args: {} } as never);

            expect(put).toHaveBeenCalled();
            // Should use the resolved identifier (update, not create)
            expect(mockContentlet.identifier).toBe('source-id');
        });

        it('should use destination identifier when destination snapshot has entry', async () => {
            const testFile = path.join(tmpDir, 'test.md');
            fs.writeFileSync(
                testFile,
                '---\ncontentType: Blog\nidentifier: source-id\nlanguage: en-US\ninode: old-inode\nmodDate: 2024-01-01\ntitle: Test\n---\nHello\n'
            );

            // parseContentFile must return source-id to match snapshot key
            (parseContentFile as jest.Mock).mockReturnValue({
                frontmatter: {
                    contentType: 'Blog',
                    identifier: 'source-id',
                    language: 'en-US',
                    inode: 'old-inode',
                    modDate: '2024-01-01',
                    title: 'Test'
                },
                body: 'Hello',
                filePath: testFile
            });

            // Destination snapshot has its own entry keyed by identifier
            (loadSnapshot as jest.Mock).mockReturnValue({
                'source-id': {
                    file: 'test.md',
                    title: 'Test',
                    hash: 'old-hash',
                    pulledAt: '2024-01-01',
                    inode: 'dest-inode',
                    source: 'demo'
                }
            });
            (scanContentFiles as jest.Mock).mockReturnValue(new Map([[testFile, 'modified']]));
            const mockContentlet = {
                contentType: 'Blog',
                identifier: 'source-id',
                title: 'Test'
            };
            (buildPushPayload as jest.Mock).mockReturnValue({
                contentlet: mockContentlet,
                binaries: []
            });
            (put as jest.Mock).mockResolvedValue({
                entity: {
                    identifier: 'source-id',
                    inode: 'updated-inode',
                    modDate: '2024-01-02'
                }
            });

            await pushCommand.run!({ args: {} } as never);

            expect(put).toHaveBeenCalled();
            // The contentlet should keep the identifier since it's in snapshot
            expect(mockContentlet.identifier).toBe('source-id');
        });
    });
});
