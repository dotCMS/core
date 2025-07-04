import { ContentTypeService } from './contentype';

import { mockFetch } from '../test-setup';

describe('ContentTypeService', () => {
    let service: ContentTypeService;

    beforeEach(() => {
        service = new ContentTypeService();
    });

    describe('list', () => {
        it('should fetch content types successfully', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: [{
                        id: '1',
                        name: 'Test Content Type',
                        baseType: 'CONTENT',
                        clazz: 'test.class',
                        defaultType: false,
                        fixed: false,
                        folder: 'folder',
                        folderPath: '/folder',
                        host: 'host123',
                        iDate: 123456789,
                        layout: [],
                        metadata: {},
                        modDate: 123456789,
                        multilingualable: false,
                        sortOrder: 1,
                        system: false,
                        variable: 'testContentType',
                        versionable: true
                    }]
                })
            };

            mockFetch.mockResolvedValue(mockResponse);

            const result = await service.list();

            expect(mockFetch).toHaveBeenCalledWith('/api/v1/contenttype', { method: 'GET' });
            expect(result).toHaveLength(1);
            expect(result[0].name).toBe('Test Content Type');
        });

        it('should handle invalid parameters', async () => {
            await expect(service.list({ page: -1 })).rejects.toThrow('Invalid parameters');
        });
    });

    describe('create', () => {
        it('should create content type successfully', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: [{
                        id: '1',
                        name: 'New Content Type',
                        baseType: 'CONTENT',
                        clazz: 'test.class',
                        defaultType: false,
                        fixed: false,
                        folder: 'folder',
                        folderPath: '/folder',
                        host: 'host123',
                        iDate: 123456789,
                        layout: [],
                        metadata: {},
                        modDate: 123456789,
                        multilingualable: false,
                        sortOrder: 1,
                        system: false,
                        variable: 'newContentType',
                        versionable: true
                    }]
                })
            };

            mockFetch.mockResolvedValue(mockResponse);

            const params = {
                name: 'New Content Type',
                description: 'Test description',
                host: 'host123',
                workflow: ['workflow1'],
                fields: [{
                    name: 'Title',
                    fieldType: 'Text' as const
                }]
            };

            const result = await service.create(params);

            expect(mockFetch).toHaveBeenCalledWith('/api/v1/contenttype', {
                method: 'POST',
                body: expect.stringContaining('New Content Type')
            });
            expect(result).toHaveLength(1);
            expect(result[0].name).toBe('New Content Type');
        });

        it('should handle invalid creation parameters', async () => {
            const invalidParams = {
                name: '',
                description: 'Test',
                host: 'host123',
                workflow: [],
                fields: []
            };

            await expect(service.create(invalidParams)).rejects.toThrow('Invalid parameters');
        });
    });

    describe('getContentTypesSchema', () => {
        it('should fetch content types schema', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: [{
                        id: '1',
                        name: 'Schema Content Type',
                        baseType: 'CONTENT',
                        clazz: 'test.class',
                        defaultType: false,
                        fixed: false,
                        folder: 'folder',
                        folderPath: '/folder', 
                        host: 'host123',
                        iDate: 123456789,
                        layout: [],
                        metadata: {},
                        modDate: 123456789,
                        multilingualable: false,
                        sortOrder: 1,
                        system: false,
                        variable: 'schemaContentType',
                        versionable: true
                    }]
                })
            };

            mockFetch.mockResolvedValue(mockResponse);

            const result = await service.getContentTypesSchema();

            expect(mockFetch).toHaveBeenCalledWith('/api/v1/contenttype?page=1&per_page=100&orderby=name&direction=ASC', { method: 'GET' });
            expect(result).toHaveLength(1);
            expect(result[0].name).toBe('Schema Content Type');
        });
    });
});