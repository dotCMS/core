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

import { listCommand } from '../list';

const consola = mockConsola;

describe('content list command', () => {
    let tmpDir: string;
    let originalCwd: string;

    beforeEach(() => {
        tmpDir = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-list-')));
        originalCwd = process.cwd();
        process.chdir(tmpDir);
        jest.clearAllMocks();
    });

    afterEach(() => {
        process.chdir(originalCwd);
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it('should show info when no snapshots found', async () => {
        await listCommand.run!({ args: {} } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('No content found'));
    });

    it('should list all content types', async () => {
        // Create Blog snapshot
        const blogDir = path.join(tmpDir, 'default', 'content', 'Blog');
        fs.mkdirSync(blogDir, { recursive: true });
        fs.writeFileSync(
            path.join(blogDir, '.snapshot.json'),
            JSON.stringify({
                'id-1': {
                    file: 'e5e92e.md',
                    title: 'My First Post',
                    hash: 'h1',
                    pulledAt: '2025-01-01',
                    inode: 'i1',
                    source: 'demo'
                },
                'id-2': {
                    file: '2b100a.md',
                    title: 'Getting Started',
                    hash: 'h2',
                    pulledAt: '2025-01-01',
                    inode: 'i2',
                    source: 'demo'
                }
            }),
            'utf-8'
        );

        // Create Author snapshot
        const authorDir = path.join(tmpDir, 'default', 'content', 'Author');
        fs.mkdirSync(authorDir, { recursive: true });
        fs.writeFileSync(
            path.join(authorDir, '.snapshot.json'),
            JSON.stringify({
                'id-3': {
                    file: 'a1b2c3.md',
                    title: 'John Doe',
                    hash: 'h3',
                    pulledAt: '2025-01-01',
                    inode: 'i3',
                    source: 'staging'
                }
            }),
            'utf-8'
        );

        await listCommand.run!({ args: {} } as never);

        // Should list both content types with source
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('Blog'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('from demo'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('Author'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('from staging'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('My First Post'));
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('John Doe'));
    });

    it('should filter by --type', async () => {
        const blogDir = path.join(tmpDir, 'default', 'content', 'Blog');
        fs.mkdirSync(blogDir, { recursive: true });
        fs.writeFileSync(
            path.join(blogDir, '.snapshot.json'),
            JSON.stringify({
                'id-1': {
                    file: 'e5e92e.md',
                    title: 'Blog Post',
                    hash: 'h1',
                    pulledAt: '2025-01-01',
                    inode: 'i1',
                    source: 'demo'
                }
            }),
            'utf-8'
        );

        const authorDir = path.join(tmpDir, 'default', 'content', 'Author');
        fs.mkdirSync(authorDir, { recursive: true });
        fs.writeFileSync(
            path.join(authorDir, '.snapshot.json'),
            JSON.stringify({
                'id-2': {
                    file: 'a1b2c3.md',
                    title: 'Author Name',
                    hash: 'h2',
                    pulledAt: '2025-01-01',
                    inode: 'i2',
                    source: 'demo'
                }
            }),
            'utf-8'
        );

        await listCommand.run!({ args: { type: 'Blog' } } as never);

        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('Blog'));
        // Author should not appear
        const allLogs = consola.log.mock.calls.map((c: unknown[]) => String(c[0]));
        expect(allLogs.some((l) => l.includes('Author Name'))).toBe(false);
    });

    it('should show short identifier in output', async () => {
        const blogDir = path.join(tmpDir, 'default', 'content', 'Blog');
        fs.mkdirSync(blogDir, { recursive: true });
        fs.writeFileSync(
            path.join(blogDir, '.snapshot.json'),
            JSON.stringify({
                'abc12345-6789-abcd': {
                    file: 'abc123.md',
                    title: 'Test',
                    hash: 'h1',
                    pulledAt: '2025-01-01',
                    inode: 'i1',
                    source: 'demo'
                }
            }),
            'utf-8'
        );

        await listCommand.run!({ args: {} } as never);

        // Should show first 6 chars of identifier
        expect(consola.log).toHaveBeenCalledWith(expect.stringContaining('abc123'));
    });

    it('should show info for non-matching type filter', async () => {
        const blogDir = path.join(tmpDir, 'default', 'content', 'Blog');
        fs.mkdirSync(blogDir, { recursive: true });
        fs.writeFileSync(
            path.join(blogDir, '.snapshot.json'),
            JSON.stringify({
                'id-1': {
                    file: 'abc123.md',
                    title: 'Test',
                    hash: 'h1',
                    pulledAt: '2025-01-01',
                    inode: 'i1',
                    source: 'demo'
                }
            }),
            'utf-8'
        );

        await listCommand.run!({ args: { type: 'NonExistent' } } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('No content found'));
    });
});
