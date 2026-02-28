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
    get: jest.fn(),
    graphql: jest.fn()
}));

jest.mock('../../../core/cache', () => ({
    getCachedContentType: jest.fn(),
    cacheContentType: jest.fn()
}));

jest.mock('../../../core/snapshot', () => ({
    computeContentHash: jest.fn().mockReturnValue('abc123hash'),
    updateSnapshotEntry: jest.fn()
}));

jest.mock('../../../handlers/content', () => ({
    buildGraphQLQuery: jest.fn().mockReturnValue('{ BlogCollection { identifier inode modDate } }'),
    serializeContentlet: jest.fn().mockReturnValue({
        filename: 'abc123.md',
        content: '---\ncontentType: Blog\nidentifier: abc12345-...\n---\nHello world\n',
        binaries: []
    })
}));

jest.mock('../../../handlers/binary', () => ({
    downloadContentBinaries: jest.fn().mockResolvedValue([])
}));

import { resolveToken } from '../../../core/auth';
import { getCachedContentType } from '../../../core/cache';
import { loadConfig, resolveInstance } from '../../../core/config';
import { createHttpClient, get, graphql } from '../../../core/http';
import { updateSnapshotEntry } from '../../../core/snapshot';
import { serializeContentlet } from '../../../handlers/content';
import { pullCommand } from '../pull';

import type { ContentTypeSchema } from '../../../core/types';

const consola = mockConsola;

describe('content pull command', () => {
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

    beforeEach(() => {
        tmpDir = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'dotcli-pull-')));
        originalCwd = process.cwd();
        process.chdir(tmpDir);
        jest.clearAllMocks();

        // Default mock setup
        (loadConfig as jest.Mock).mockReturnValue({
            default: 'demo',
            instances: { demo: { url: 'https://demo.dotcms.com' } },
            pull: [{ type: 'Blog' }]
        });
        (resolveInstance as jest.Mock).mockReturnValue({
            name: 'demo',
            url: 'https://demo.dotcms.com'
        });
        (resolveToken as jest.Mock).mockReturnValue('test-token');
        (createHttpClient as jest.Mock).mockReturnValue(jest.fn());
        (get as jest.Mock).mockImplementation((_client, url) => {
            if (url === '/api/v2/languages') {
                return Promise.resolve({
                    entity: [
                        {
                            id: 1,
                            languageCode: 'en',
                            countryCode: 'US',
                            language: 'English',
                            country: 'US',
                            defaultLanguage: true
                        }
                    ]
                });
            }
            if (url.startsWith('/api/v1/contenttype/id/')) {
                return Promise.resolve({ entity: mockSchema });
            }
            return Promise.resolve({ entity: {} });
        });
        (graphql as jest.Mock).mockResolvedValue({
            BlogCollection: [
                {
                    identifier: 'abc12345-6789',
                    inode: 'inode-1',
                    modDate: '2024-01-01',
                    languageId: 1,
                    hostName: 'default',
                    title: 'Test',
                    body: 'Hello'
                }
            ]
        });
        (getCachedContentType as jest.Mock).mockReturnValue(null);
    });

    afterEach(() => {
        process.chdir(originalCwd);
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it('should error when no project found', async () => {
        (loadConfig as jest.Mock).mockImplementation(() => {
            throw new Error('Config file not found');
        });

        await pullCommand.run!({ args: {} } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('No dotcli project'));
    });

    it('should error when no auth token available', async () => {
        (resolveToken as jest.Mock).mockReturnValue(null);

        await pullCommand.run!({ args: {} } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('No auth token'));
    });

    it('should pull content types from config', async () => {
        await pullCommand.run!({ args: {} } as never);

        expect(graphql).toHaveBeenCalled();
        expect(serializeContentlet).toHaveBeenCalled();
        expect(updateSnapshotEntry).toHaveBeenCalled();
        expect(consola.success).toHaveBeenCalledWith(expect.stringContaining('Pulled'));
    });

    it('should pull by --type flag', async () => {
        (loadConfig as jest.Mock).mockReturnValue({
            default: 'demo',
            instances: { demo: { url: 'https://demo.dotcms.com' } }
        });

        await pullCommand.run!({ args: { type: 'Blog' } } as never);

        expect(graphql).toHaveBeenCalled();
    });

    it('should error when no content types specified', async () => {
        (loadConfig as jest.Mock).mockReturnValue({
            default: 'demo',
            instances: { demo: { url: 'https://demo.dotcms.com' } }
        });

        await pullCommand.run!({ args: {} } as never);

        expect(consola.error).toHaveBeenCalledWith(expect.stringContaining('No content types'));
    });

    it('should write content files to disk', async () => {
        await pullCommand.run!({ args: {} } as never);

        // The serialized content should have been written
        // Since we mock serializeContentlet to return a filename, verify the directory was created
        const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
        expect(fs.existsSync(contentDir)).toBe(true);
    });

    it('should handle pagination when results fill a page', async () => {
        // First call returns full page, second returns empty (no more)
        (graphql as jest.Mock)
            .mockResolvedValueOnce({
                BlogCollection: Array(25).fill({
                    identifier: 'abc12345-6789',
                    inode: 'inode-1',
                    modDate: '2024-01-01',
                    languageId: 1,
                    hostName: 'default',
                    title: 'Test',
                    body: 'Hello'
                })
            })
            .mockResolvedValueOnce({ BlogCollection: [] });

        await pullCommand.run!({ args: {} } as never);

        expect(graphql).toHaveBeenCalledTimes(2);
    });
});
