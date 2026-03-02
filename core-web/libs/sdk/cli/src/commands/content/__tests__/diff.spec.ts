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
    resolveToken: jest.fn()
}));

jest.mock('../../../core/http', () => ({
    createHttpClient: jest.fn(),
    graphql: jest.fn()
}));

jest.mock('../../../core/cache', () => ({
    getCachedContentType: jest.fn()
}));

jest.mock('../../../core/snapshot', () => ({
    loadSnapshot: jest.fn().mockReturnValue({}),
    getFileState: jest.fn().mockReturnValue('unchanged'),
    findEntryByFile: jest.fn().mockReturnValue(null)
}));

jest.mock('../../../core/languages', () => ({
    fetchLanguageMap: jest.fn().mockResolvedValue({ '1': 'en-US' })
}));

jest.mock('../../../handlers/content', () => ({
    buildGraphQLQuery: jest.fn().mockReturnValue('{ BlogCollection { identifier inode modDate } }'),
    parseContentFile: jest.fn(),
    serializeContentlet: jest.fn()
}));

import { resolveToken } from '../../../core/auth';
import { getCachedContentType } from '../../../core/cache';
import { loadConfig, resolveInstance } from '../../../core/config';
import { createHttpClient, graphql } from '../../../core/http';
import { getFileState } from '../../../core/snapshot';
import { parseContentFile, serializeContentlet } from '../../../handlers/content';
import { diffCommand } from '../diff';

import type { ContentTypeSchema } from '../../../core/types';

const consola = mockConsola;

describe('content diff command', () => {
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
            },
            {
                variable: 'body',
                name: 'Body',
                fieldType: 'WYSIWYG',
                dataType: 'TEXT',
                sortOrder: 2,
                required: false,
                fixed: false,
                readOnly: false,
                searchable: false,
                listed: false
            }
        ]
    };

    const localFileContent =
        '---\ncontentType: Blog\nidentifier: abc12345-6789\nlanguage: en-US\ninode: inode-1\nbodyField: body\ntitle: Test Post\n---\nHello world\n';

    beforeEach(() => {
        tmpDir = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-diff-')));
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
        (parseContentFile as jest.Mock).mockReturnValue({
            frontmatter: {
                contentType: 'Blog',
                identifier: 'abc12345-6789',
                language: 'en-US',
                inode: 'inode-1',
                bodyField: 'body',
                title: 'Test Post'
            },
            body: 'Hello world',
            filePath: path.join(tmpDir, 'test.md')
        });
    });

    afterEach(() => {
        process.chdir(originalCwd);
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it('should error when no file specified', async () => {
        await diffCommand.run!({ args: {} } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('specify a file'));
    });

    it('should error when file not found', async () => {
        await diffCommand.run!({ args: { files: 'nonexistent.md' } } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('File not found'));
    });

    it('should error when no project found', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, localFileContent);

        (loadConfig as jest.Mock).mockImplementation(() => {
            throw new Error('Config file not found');
        });

        await diffCommand.run!({ args: { files: 'test.md' } } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('No dotcli project'));
    });

    it('should show local state when --local flag is used', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, localFileContent);

        (getFileState as jest.Mock).mockReturnValue('modified');

        await diffCommand.run!({ args: { files: 'test.md', local: true } } as never);

        expect(getFileState).toHaveBeenCalled();
        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('modified'));
    });

    it('should error when file has no identifier', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, '---\ncontentType: Blog\n---\n');

        (parseContentFile as jest.Mock).mockReturnValue({
            frontmatter: { contentType: 'Blog' },
            body: '',
            filePath: testFile
        });

        await diffCommand.run!({ args: { files: 'test.md' } } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('no identifier'));
    });

    it('should error when no cached schema', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, localFileContent);

        (getCachedContentType as jest.Mock).mockReturnValue(null);

        await diffCommand.run!({ args: { files: 'test.md' } } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('No cached schema'));
    });

    it('should error when no auth token', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, localFileContent);

        (resolveToken as jest.Mock).mockReturnValue(null);

        await diffCommand.run!({ args: { files: 'test.md' } } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('No auth token'));
    });

    it('should show info when contentlet not found on server', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, localFileContent);

        (graphql as jest.Mock).mockResolvedValue({ BlogCollection: [] });

        await diffCommand.run!({ args: { files: 'test.md' } } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('not found on server'));
    });

    it('should report no differences when server and local are identical', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, localFileContent);

        (graphql as jest.Mock).mockResolvedValue({
            BlogCollection: [
                {
                    identifier: 'abc12345-6789',
                    inode: 'inode-1',
                    modDate: '2024-01-01',
                    title: 'Test Post',
                    body: 'Hello world'
                }
            ]
        });

        // serializeContentlet returns the same content as local file
        (serializeContentlet as jest.Mock).mockReturnValue({
            filename: 'abc123.md',
            content: localFileContent,
            binaries: []
        });

        await diffCommand.run!({ args: { files: 'test.md' } } as never);

        expect(consola.info).toHaveBeenCalledWith('No differences found.');
    });

    it('should show unified diff when server and local differ', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, localFileContent);

        (graphql as jest.Mock).mockResolvedValue({
            BlogCollection: [
                {
                    identifier: 'abc12345-6789',
                    inode: 'inode-1',
                    modDate: '2024-01-01',
                    title: 'Original Title',
                    body: 'Hello world'
                }
            ]
        });

        // Server version has different title
        const serverContent =
            '---\ncontentType: Blog\nidentifier: abc12345-6789\nlanguage: en-US\ninode: inode-1\nbodyField: body\ntitle: Original Title\n---\nHello world\n';

        (serializeContentlet as jest.Mock).mockReturnValue({
            filename: 'abc123.md',
            content: serverContent,
            binaries: []
        });

        // Spy on console.log to capture output
        const consoleSpy = jest.spyOn(console, 'log').mockImplementation();

        await diffCommand.run!({ args: { files: 'test.md' } } as never);

        // Should NOT print "No differences found."
        expect(consola.info).not.toHaveBeenCalledWith('No differences found.');

        // Should print diff output containing the changed line
        expect(consoleSpy).toHaveBeenCalled();
        const output = consoleSpy.mock.calls[0][0] as string;
        expect(output).toContain('--- server (demo)');
        expect(output).toContain('+++ local');
        expect(output).toContain('Original Title');
        expect(output).toContain('Test Post');

        consoleSpy.mockRestore();
    });

    it('should call serializeContentlet with correct arguments', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, localFileContent);

        const serverRecord = {
            identifier: 'abc12345-6789',
            inode: 'inode-1',
            modDate: '2024-01-01',
            title: 'Test Post',
            body: 'Hello world'
        };

        (graphql as jest.Mock).mockResolvedValue({
            BlogCollection: [serverRecord]
        });

        (serializeContentlet as jest.Mock).mockReturnValue({
            filename: 'abc123.md',
            content: localFileContent,
            binaries: []
        });

        await diffCommand.run!({ args: { files: 'test.md' } } as never);

        expect(serializeContentlet).toHaveBeenCalledWith(serverRecord, mockSchema, {
            '1': 'en-US'
        });
    });

    it('should handle graphql error gracefully', async () => {
        const testFile = path.join(tmpDir, 'test.md');
        fs.writeFileSync(testFile, localFileContent);

        (graphql as jest.Mock).mockRejectedValue(new Error('Network error'));

        await diffCommand.run!({ args: { files: 'test.md' } } as never);

        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('not found on server'));
    });
});
