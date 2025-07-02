import { SiteService } from './site';

// Mock the AgnosticClient
const mockFetch = jest.fn();
jest.mock('./client', () => {
    return {
        AgnosticClient: class MockAgnosticClient {
            fetch = mockFetch;
        }
    };
});

// Mock Logger
jest.mock('../utils/logger', () => {
    return {
        Logger: jest.fn().mockImplementation(() => ({
            log: jest.fn(),
            error: jest.fn()
        }))
    };
});

describe('SiteService', () => {
    let service: SiteService;

    beforeEach(() => {
        service = new SiteService();
        mockFetch.mockClear();
    });

    describe('getCurrentSite', () => {
        it('should fetch current site successfully', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        aliases: 'localhost',
                        archived: false,
                        categoryId: 'cat123',
                        contentTypeId: 'ct123',
                        default: true,
                        dotAsset: false,
                        fileAsset: false,
                        folder: 'folder123',
                        form: false,
                        host: 'host123',
                        hostThumbnail: null,
                        hostname: 'localhost',
                        htmlpage: false,
                        identifier: 'id123',
                        indexPolicyDependencies: 'deps',
                        inode: 'inode123',
                        keyValue: false,
                        languageId: 1,
                        languageVariable: false,
                        live: true,
                        locked: false,
                        lowIndexPriority: false,
                        modDate: 123456789,
                        modUser: 'admin',
                        name: 'Default Site',
                        new: false,
                        owner: 'admin',
                        parent: false,
                        permissionId: 'perm123',
                        permissionType: 'host',
                        persona: false,
                        sortOrder: 1,
                        structureInode: 'struct123',
                        systemHost: false,
                        tagStorage: 'tag123',
                        title: 'Default Site',
                        titleImage: null,
                        type: 'host',
                        vanityUrl: false,
                        variantId: 'var123',
                        versionId: 'ver123',
                        working: true
                    },
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    pagination: null,
                    permissions: []
                })
            };

            mockFetch.mockResolvedValue(mockResponse);

            const result = await service.getCurrentSite();

            expect(mockFetch).toHaveBeenCalledWith('/api/v1/site/currentSite', { method: 'GET' });
            expect(result.name).toBe('Default Site');
            expect(result.hostname).toBe('localhost');
            expect(result.default).toBe(true);
        });

        it('should handle invalid response format', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        // Missing required fields
                        name: 'Test Site'
                    }
                })
            };

            mockFetch.mockResolvedValue(mockResponse);

            await expect(service.getCurrentSite()).rejects.toThrow('Invalid site response');
        });

        it('should handle fetch errors', async () => {
            const error = new Error('Network error');
            mockFetch.mockRejectedValue(error);

            await expect(service.getCurrentSite()).rejects.toThrow('Network error');
        });
    });
});