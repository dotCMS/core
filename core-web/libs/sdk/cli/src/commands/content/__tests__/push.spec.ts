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
    checkConflict: jest.fn().mockReturnValue({ hasConflict: false })
}));

jest.mock('../../../handlers/content', () => ({
    parseContentFile: jest.fn(),
    validateContentFile: jest.fn().mockReturnValue({ valid: true, errors: [] }),
    buildPushPayload: jest.fn().mockReturnValue({
        contentlet: { contentType: 'Blog', identifier: 'id-1', title: 'Test' },
        binaries: []
    })
}));

jest.mock('../../../handlers/binary', () => ({
    buildMultipartPayload: jest.fn()
}));

import { resolveToken } from '../../../core/auth';
import { getCachedContentType } from '../../../core/cache';
import { loadConfig, resolveInstance } from '../../../core/config';
import { createHttpClient, put } from '../../../core/http';
import { scanContentFiles, updateSnapshotEntry } from '../../../core/snapshot';
import { parseContentFile, validateContentFile } from '../../../handlers/content';
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

    it('should push modified files', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(
            testFile,
            '---\ncontentType: Blog\nidentifier: id-1\nlanguage: en-US\ninode: old-inode\nmodDate: 2024-01-01\ntitle: Test\n---\nHello world\n'
        );

        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[testFile, 'modified']]));

        await pushCommand.run!({ args: {} } as never);

        expect(put).toHaveBeenCalled();
        expect(updateSnapshotEntry).toHaveBeenCalled();
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
        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('Bailing'));
    });
});
