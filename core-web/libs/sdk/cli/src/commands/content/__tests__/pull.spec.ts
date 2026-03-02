import * as prompts from '@clack/prompts';

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
    get: jest.fn(),
    graphql: jest.fn()
}));

jest.mock('../../../core/cache', () => ({
    getCachedContentType: jest.fn(),
    cacheContentType: jest.fn()
}));

jest.mock('../../../core/snapshot', () => ({
    computeContentHash: jest.fn().mockReturnValue('abc123hash'),
    computeContentHashFromString: jest.fn().mockReturnValue('abc123hash'),
    buildSnapshotEntry: jest.fn().mockReturnValue({
        file: 'abc123.md',
        title: '',
        hash: 'abc123hash',
        pulledAt: '2024-01-01T00:00:00.000Z',
        inode: 'inode-1',
        source: 'demo',
        modDate: '2024-01-01'
    }),
    loadSnapshot: jest.fn().mockReturnValue({}),
    findEntryByFile: jest.fn().mockReturnValue(null),
    saveSnapshot: jest.fn()
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
import { findEntryByFile, loadSnapshot, saveSnapshot } from '../../../core/snapshot';
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
        expect(saveSnapshot).toHaveBeenCalled();
        expect(consola.success).toHaveBeenCalledWith(expect.stringContaining('Pulled'));
    });

    it('should include source in snapshot entry', async () => {
        await pullCommand.run!({ args: {} } as never);

        expect(saveSnapshot).toHaveBeenCalledWith(
            expect.any(String),
            expect.objectContaining({
                'abc12345-6789': expect.objectContaining({ source: 'demo' })
            })
        );
    });

    it('should prompt for confirmation when overwriting locally modified files', async () => {
        const entry = {
            file: 'abc123.md',
            title: 'Test',
            hash: 'old-hash-before-local-edit',
            pulledAt: '2025-01-01',
            inode: 'inode-1',
            source: 'demo',
            modDate: '2024-01-01'
        };

        // findEntryByFile looks up by filename — return a match with stale hash
        (findEntryByFile as jest.Mock).mockReturnValue(['abc12345-6789', entry]);
        // loadSnapshot returns the snapshot (needed for stale cleanup)
        (loadSnapshot as jest.Mock).mockReturnValue({
            'abc12345-6789': entry
        });
        // computeContentHash returns 'abc123hash' by default which differs from 'old-hash-before-local-edit'

        // Create the file on disk so fs.existsSync returns true
        const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
        fs.mkdirSync(contentDir, { recursive: true });
        fs.writeFileSync(path.join(contentDir, 'abc123.md'), 'existing content', 'utf-8');

        (prompts.confirm as jest.Mock).mockResolvedValue(true);

        await pullCommand.run!({ args: {} } as never);

        expect(consola.warn).toHaveBeenCalledWith(
            expect.stringContaining('Local changes detected')
        );
        expect(prompts.confirm).toHaveBeenCalledWith(
            expect.objectContaining({ message: expect.stringContaining('Overwrite') })
        );
        // Should proceed with pull after confirmation
        expect(saveSnapshot).toHaveBeenCalled();
    });

    it('should abort pull when user declines overwrite confirmation', async () => {
        const entry = {
            file: 'abc123.md',
            title: 'Test',
            hash: 'old-hash-before-local-edit',
            pulledAt: '2025-01-01',
            inode: 'inode-1',
            source: 'demo',
            modDate: '2024-01-01'
        };

        (findEntryByFile as jest.Mock).mockReturnValue(['abc12345-6789', entry]);
        (loadSnapshot as jest.Mock).mockReturnValue({
            'abc12345-6789': entry
        });

        const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
        fs.mkdirSync(contentDir, { recursive: true });
        fs.writeFileSync(path.join(contentDir, 'abc123.md'), 'existing content', 'utf-8');

        (prompts.confirm as jest.Mock).mockResolvedValue(false);

        await pullCommand.run!({ args: {} } as never);

        expect(consola.info).toHaveBeenCalledWith('Pull aborted.');
        // Should NOT write any files
        expect(saveSnapshot).not.toHaveBeenCalled();
    });

    it('should skip confirmation with --force flag', async () => {
        const entry = {
            file: 'abc123.md',
            title: 'Test',
            hash: 'old-hash-before-local-edit',
            pulledAt: '2025-01-01',
            inode: 'inode-1',
            source: 'demo',
            modDate: '2024-01-01'
        };

        (findEntryByFile as jest.Mock).mockReturnValue(['abc12345-6789', entry]);
        (loadSnapshot as jest.Mock).mockReturnValue({
            'abc12345-6789': entry
        });

        const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
        fs.mkdirSync(contentDir, { recursive: true });
        fs.writeFileSync(path.join(contentDir, 'abc123.md'), 'existing content', 'utf-8');

        await pullCommand.run!({ args: { force: true } } as never);

        // Should NOT prompt
        expect(prompts.confirm).not.toHaveBeenCalled();
        // Should proceed with pull
        expect(saveSnapshot).toHaveBeenCalled();
    });

    it('should remove stale entries not in current pull', async () => {
        const staleEntry = {
            file: 'stale1.md',
            title: 'Stale',
            hash: 'stale-hash',
            pulledAt: '2025-01-01',
            inode: 'stale-inode',
            source: 'other-instance',
            modDate: '2024-01-01'
        };

        // loadSnapshot returns the stale entry (identifier differs from pulled one)
        (loadSnapshot as jest.Mock).mockReturnValue({
            'stale-id-999': staleEntry
        });

        // Create the stale file on disk
        const contentDir = path.join(tmpDir, 'default', 'content', 'Blog');
        fs.mkdirSync(contentDir, { recursive: true });
        fs.writeFileSync(path.join(contentDir, 'stale1.md'), 'old content', 'utf-8');

        await pullCommand.run!({ args: {} } as never);

        // Stale file should be removed
        expect(fs.existsSync(path.join(contentDir, 'stale1.md'))).toBe(false);
        // saveSnapshot should be called to persist the cleanup
        expect(saveSnapshot).toHaveBeenCalled();
        expect(consola.info).toHaveBeenCalledWith(expect.stringContaining('Removed stale'));
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

    it('should write content files under host directory (flat working copy)', async () => {
        await pullCommand.run!({ args: {} } as never);

        // Files should be written under {hostName}/content/{type} (no instance prefix)
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
