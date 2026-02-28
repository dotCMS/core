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

jest.mock('../../../core/snapshot', () => ({
    loadSnapshot: jest.fn().mockReturnValue({}),
    scanContentFiles: jest.fn()
}));

import { scanContentFiles } from '../../../core/snapshot';
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
    });

    afterEach(() => {
        process.chdir(originalCwd);
        fs.rmSync(tmpDir, { recursive: true, force: true });
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

        await statusCommand.run!({} as never);

        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('1 unchanged'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('1 modified'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('1 new'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('1 deleted'));
    });

    it('should display no changes when everything is unchanged', async () => {
        (scanContentFiles as jest.Mock).mockReturnValue(
            new Map([['/project/default/content/Blog/abc123.md', 'unchanged']])
        );

        await statusCommand.run!({} as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('No changes'));
    });

    it('should display modified files with M prefix', async () => {
        const filePath = path.join(tmpDir, 'default', 'content', 'Blog', 'abc123.md');
        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[filePath, 'modified']]));

        await statusCommand.run!({} as never);

        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('M'));
    });

    it('should display new files with + prefix', async () => {
        const filePath = path.join(tmpDir, 'default', 'content', 'Blog', 'new123.md');
        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[filePath, 'new']]));

        await statusCommand.run!({} as never);

        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('+'));
    });

    it('should display deleted files with - prefix', async () => {
        const filePath = path.join(tmpDir, 'default', 'content', 'Blog', 'del123.md');
        (scanContentFiles as jest.Mock).mockReturnValue(new Map([[filePath, 'deleted']]));

        await statusCommand.run!({} as never);

        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('-'));
    });

    it('should handle empty scan results', async () => {
        (scanContentFiles as jest.Mock).mockReturnValue(new Map());

        await statusCommand.run!({} as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('No changes'));
    });
});
