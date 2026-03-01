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

jest.mock('../../../core/config', () => ({
    loadConfig: jest.fn(),
    resolveInstance: jest.fn()
}));

jest.mock('../../../core/auth', () => ({
    resolveToken: jest.fn().mockReturnValue(null)
}));

jest.mock('../../../core/http', () => ({
    createHttpClient: jest.fn().mockReturnValue(jest.fn())
}));

jest.mock('../../../core/cache', () => ({
    getCachedContentType: jest.fn().mockReturnValue(null)
}));

jest.mock('../../../core/resolve', () => ({
    resolveIdentifiersOnServer: jest.fn().mockResolvedValue(new Map())
}));

jest.mock('../../../core/snapshot', () => ({
    scanContentFiles: jest.fn()
}));

jest.mock('../../../handlers/content', () => ({
    parseContentFile: jest.fn()
}));

import { resolveToken } from '../../../core/auth';
import { getCachedContentType } from '../../../core/cache';
import { loadConfig, resolveInstance } from '../../../core/config';
import { resolveIdentifiersOnServer } from '../../../core/resolve';
import { scanContentFiles } from '../../../core/snapshot';
import { parseContentFile } from '../../../handlers/content';
import { statusCommand } from '../status';

const consola = mockConsola;

describe('content status command', () => {
    let tmpDir: string;
    let originalCwd: string;

    beforeEach(() => {
        tmpDir = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-status-')));
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
    });

    afterEach(() => {
        process.chdir(originalCwd);
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it('should error when no project found', async () => {
        (loadConfig as jest.Mock).mockImplementation(() => {
            throw new Error('Config file not found');
        });

        await statusCommand.run!({ args: {} } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('No dotcli project'));
    });

    it('should scope scan to project root', async () => {
        (scanContentFiles as jest.Mock).mockReturnValue(new Map());

        await statusCommand.run!({ args: {} } as never);

        expect(scanContentFiles).toHaveBeenCalledWith(tmpDir);
    });

    it('should display instance name in status header', async () => {
        (scanContentFiles as jest.Mock).mockReturnValue(new Map());

        await statusCommand.run!({ args: {} } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('instance: demo'));
    });

    it('should use --from flag to target specific instance', async () => {
        (resolveInstance as jest.Mock).mockReturnValue({
            name: 'staging',
            url: 'https://staging.dotcms.com'
        });
        (scanContentFiles as jest.Mock).mockReturnValue(new Map());

        await statusCommand.run!({ args: { from: 'staging' } } as never);

        expect(resolveInstance).toHaveBeenCalledWith(expect.anything(), 'staging');
    });

    it('should display summary with all states', async () => {
        (scanContentFiles as jest.Mock).mockReturnValue(
            new Map([
                ['/project/default/content/Blog/abc123.md', 'unchanged'],
                ['/project/default/content/Blog/def456.md', 'modified'],
                ['/project/default/content/Blog/ghi789.md', 'new'],
                ['/project/default/content/Blog/jkl012.md', 'deleted']
            ])
        );

        await statusCommand.run!({ args: {} } as never);

        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('1 unchanged'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('1 modified'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('1 new'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('1 deleted'));
    });

    it('should display no changes when everything is unchanged', async () => {
        (scanContentFiles as jest.Mock).mockReturnValue(
            new Map([['/project/default/content/Blog/abc123.md', 'unchanged']])
        );

        await statusCommand.run!({ args: {} } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('No changes'));
    });

    it('should display modified files with M prefix', async () => {
        const filePath = path.join(tmpDir, 'default', 'content', 'Blog', 'abc123.md');
        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[filePath, 'modified']]));

        await statusCommand.run!({ args: {} } as never);

        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('M'));
    });

    it('should display new files with + prefix', async () => {
        const filePath = path.join(tmpDir, 'default', 'content', 'Blog', 'new123.md');
        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[filePath, 'new']]));

        await statusCommand.run!({ args: {} } as never);

        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('+'));
    });

    it('should display deleted files with - prefix', async () => {
        const filePath = path.join(tmpDir, 'default', 'content', 'Blog', 'del123.md');
        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[filePath, 'deleted']]));

        await statusCommand.run!({ args: {} } as never);

        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('-'));
    });

    it('should handle empty scan results', async () => {
        (scanContentFiles as jest.Mock).mockReturnValue(new Map());

        await statusCommand.run!({ args: {} } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('No changes'));
    });

    // ─── Cross-instance server resolution ──────────────────────────────────

    describe('server resolution for new files', () => {
        it('should reclassify new files that exist on server', async () => {
            const newFile = path.join(tmpDir, 'content', 'Blog', 'abc123.md');
            fs.mkdirSync(path.dirname(newFile), { recursive: true });
            fs.writeFileSync(
                newFile,
                '---\ncontentType: Blog\nidentifier: server-id\nlanguage: en-US\n---\n'
            );

            (scanContentFiles as jest.Mock).mockReturnValue(new Map([[newFile, 'new']]));
            (resolveToken as jest.Mock).mockReturnValue('test-token');
            (getCachedContentType as jest.Mock).mockReturnValue({
                variable: 'Blog',
                name: 'Blog',
                id: 'ct-1',
                fields: []
            });
            (parseContentFile as jest.Mock).mockReturnValue({
                frontmatter: {
                    contentType: 'Blog',
                    identifier: 'server-id',
                    language: 'en-US'
                },
                body: '',
                filePath: newFile
            });
            (resolveIdentifiersOnServer as jest.Mock).mockResolvedValue(
                new Map([['server-id', { identifier: 'server-id', inode: 'server-inode' }]])
            );

            await statusCommand.run!({ args: {} } as never);

            // Should show "update" line for server-existing content
            expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('update'));
            // Should show "0 new" since the file was reclassified
            expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('0 new'));
        });

        it('should not resolve when no auth token available', async () => {
            const newFile = path.join(tmpDir, 'content', 'Blog', 'abc123.md');
            fs.mkdirSync(path.dirname(newFile), { recursive: true });
            fs.writeFileSync(newFile, '---\ncontentType: Blog\nidentifier: id-1\n---\n');

            (scanContentFiles as jest.Mock).mockReturnValue(new Map([[newFile, 'new']]));
            (resolveToken as jest.Mock).mockReturnValue(null);

            await statusCommand.run!({ args: {} } as never);

            // Should still show as new since we can't resolve
            expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('1 new'));
            expect(resolveIdentifiersOnServer).not.toHaveBeenCalled();
        });
    });
});
