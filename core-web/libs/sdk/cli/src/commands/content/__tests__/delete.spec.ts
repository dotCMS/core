import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

const mockConsola = {
    info: jest.fn(),
    success: jest.fn(),
    error: jest.fn(),
    warn: jest.fn(),
    log: jest.fn()
};
jest.mock('consola', () => ({
    __esModule: true,
    default: mockConsola
}));

jest.mock('@clack/prompts', () => ({
    confirm: jest.fn().mockResolvedValue(true),
    isCancel: jest.fn().mockReturnValue(false)
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
    put: jest.fn()
}));

jest.mock('../../../core/snapshot', () => ({
    removeSnapshotEntry: jest.fn()
}));

jest.mock('../../../handlers/content', () => ({
    parseContentFile: jest.fn()
}));

import { resolveToken } from '../../../core/auth';
import { loadConfig, resolveInstance } from '../../../core/config';
import { createHttpClient, put } from '../../../core/http';
import { removeSnapshotEntry } from '../../../core/snapshot';
import { parseContentFile } from '../../../handlers/content';
import { deleteCommand } from '../delete';

const consola = mockConsola;

describe('content delete command', () => {
    let tmpDir: string;
    let originalCwd: string;

    beforeEach(() => {
        tmpDir = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-delete-')));
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
        (put as jest.Mock).mockResolvedValue({});
    });

    afterEach(() => {
        process.chdir(originalCwd);
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it('should not include inode in the archive payload', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(
            testFile,
            '---\ncontentType: Blog\nidentifier: id-1\ninode: stale-inode\n---\n'
        );

        (parseContentFile as jest.Mock).mockReturnValue({
            frontmatter: {
                contentType: 'Blog',
                identifier: 'id-1',
                inode: 'stale-inode',
                language: 'en-US',
                modDate: '2024-01-01'
            },
            body: '',
            filePath: testFile
        });

        await deleteCommand.run!({ args: { files: 'test.md' } } as never);

        expect(put).toHaveBeenCalledWith(
            expect.anything(),
            expect.stringContaining('ARCHIVE'),
            expect.objectContaining({
                contentlet: expect.not.objectContaining({ inode: expect.anything() })
            })
        );
        // Verify the payload has identifier and contentType but NOT inode
        const putCall = (put as jest.Mock).mock.calls[0];
        const payload = putCall[2];
        expect(payload.contentlet.identifier).toBe('id-1');
        expect(payload.contentlet.contentType).toBe('Blog');
        expect(payload.contentlet.inode).toBeUndefined();
    });

    it('should handle archive API errors gracefully', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, '---\ncontentType: Blog\nidentifier: id-1\n---\n');

        (parseContentFile as jest.Mock).mockReturnValue({
            frontmatter: {
                contentType: 'Blog',
                identifier: 'id-1',
                inode: 'stale-inode',
                language: 'en-US',
                modDate: '2024-01-01'
            },
            body: '',
            filePath: testFile
        });

        const apiError = new Error('Bad Request') as Error & { data?: unknown };
        apiError.data = { message: 'Invalid contentlet' };
        (put as jest.Mock).mockRejectedValue(apiError);

        await deleteCommand.run!({ args: { files: 'test.md' } } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('Archive failed'));
        // File should NOT be deleted when archive fails
        expect(fs.existsSync(testFile)).toBe(true);
        expect(removeSnapshotEntry).not.toHaveBeenCalled();
    });

    it('should remove file and snapshot on successful archive', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, '---\ncontentType: Blog\nidentifier: id-1\n---\n');

        (parseContentFile as jest.Mock).mockReturnValue({
            frontmatter: {
                contentType: 'Blog',
                identifier: 'id-1',
                inode: 'stale-inode',
                language: 'en-US',
                modDate: '2024-01-01'
            },
            body: '',
            filePath: testFile
        });

        await deleteCommand.run!({ args: { files: 'test.md' } } as never);

        expect(put).toHaveBeenCalled();
        expect(removeSnapshotEntry).toHaveBeenCalledWith(path.dirname(testFile), 'id-1');
        expect(consola.success).toHaveBeenCalledWith(expect.stringContaining('Archived'));
    });
});
