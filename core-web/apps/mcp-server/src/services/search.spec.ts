import { ContentSearchService } from './search';

import { mockFetch } from '../test-setup';

describe('ContentSearchService', () => {
    let service: ContentSearchService;

    beforeEach(() => {
        service = new ContentSearchService();
    });

    describe('search', () => {
        it('should search content successfully with published content', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        contentTook: 1,
                        resultsSize: 1,
                        jsonObjectView: {
                            contentlets: [
                                {
                                    title: 'Test Result',
                                    hostName: 'localhost',
                                    modDate: '1764284834965',
                                    publishDate: '1764284834994',
                                    baseType: 'CONTENT',
                                    inode: 'inode1',
                                    archived: false,
                                    host: 'host1',
                                    ownerUserName: 'admin',
                                    working: true,
                                    locked: false,
                                    stInode: 'stinode1',
                                    contentType: 'Blog',
                                    live: true,
                                    owner: 'admin',
                                    identifier: 'id1',
                                    publishUserName: 'admin',
                                    publishUser: 'admin',
                                    languageId: 1,
                                    creationDate: '1764284749322',
                                    shortyId: 'shorty1',
                                    url: '/test',
                                    titleImage: 'TITLE_IMAGE_NOT_FOUND',
                                    modUserName: 'admin',
                                    hasLiveVersion: true,
                                    folder: '/test',
                                    hasTitleImage: false,
                                    sortOrder: 1,
                                    modUser: 'admin',
                                    __icon__: 'icon',
                                    contentTypeIcon: 'icon',
                                    variant: 'default'
                                }
                            ]
                        },
                        queryTook: 1
                    },
                    errors: [],
                    messages: [],
                    i18nMessagesMap: {},
                    permissions: []
                })
            };
            mockFetch.mockResolvedValue(mockResponse);

            const params = {
                query: '+title:Test',
                limit: 1,
                languageId: 1,
                depth: 1,
                allCategoriesInfo: false
            };
            const result = await service.search(params);

            expect(mockFetch).toHaveBeenCalledWith(
                '/api/content/_search?rememberQuery=false',
                expect.objectContaining({
                    method: 'POST',
                    body: expect.stringContaining('+title:Test')
                })
            );
            expect(result.entity?.jsonObjectView?.contentlets).toHaveLength(1);
            expect(result.entity?.jsonObjectView?.contentlets?.[0].title).toBe('Test Result');
            // Verify dates are converted to numbers
            expect(typeof result.entity?.jsonObjectView?.contentlets?.[0].modDate).toBe('number');
            expect(typeof result.entity?.jsonObjectView?.contentlets?.[0].creationDate).toBe(
                'number'
            );
            expect(typeof result.entity?.jsonObjectView?.contentlets?.[0].publishDate).toBe(
                'number'
            );
        });

        it('should search content successfully with unpublished content', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        contentTook: 1,
                        resultsSize: 1,
                        jsonObjectView: {
                            contentlets: [
                                {
                                    title: 'Unpublished Result',
                                    hostName: 'localhost',
                                    modDate: '1764284834965',
                                    baseType: 'CONTENT',
                                    inode: 'inode2',
                                    archived: false,
                                    host: 'host1',
                                    ownerUserName: 'admin',
                                    working: true,
                                    locked: false,
                                    stInode: 'stinode1',
                                    contentType: 'Blog',
                                    live: false,
                                    owner: 'admin',
                                    identifier: 'id2',
                                    languageId: 1,
                                    creationDate: '1764284749322',
                                    shortyId: 'shorty2',
                                    url: '/test2',
                                    titleImage: 'TITLE_IMAGE_NOT_FOUND',
                                    modUserName: 'admin',
                                    hasLiveVersion: false,
                                    folder: '/test',
                                    hasTitleImage: false,
                                    sortOrder: 0,
                                    modUser: 'admin',
                                    __icon__: 'icon',
                                    contentTypeIcon: 'icon',
                                    variant: 'default'
                                }
                            ]
                        },
                        queryTook: 1
                    },
                    errors: [],
                    messages: [],
                    i18nMessagesMap: {},
                    permissions: []
                })
            };
            mockFetch.mockResolvedValue(mockResponse);

            const params = {
                query: '+title:Unpublished',
                limit: 1,
                languageId: 1,
                depth: 1,
                allCategoriesInfo: false
            };
            const result = await service.search(params);

            expect(result.entity?.jsonObjectView?.contentlets).toHaveLength(1);
            expect(result.entity?.jsonObjectView?.contentlets?.[0].title).toBe(
                'Unpublished Result'
            );
            // Verify publishDate is null/undefined for unpublished content
            expect(result.entity?.jsonObjectView?.contentlets?.[0].publishDate).toBeUndefined();
            // Verify dates are converted to numbers
            expect(typeof result.entity?.jsonObjectView?.contentlets?.[0].modDate).toBe('number');
            expect(typeof result.entity?.jsonObjectView?.contentlets?.[0].creationDate).toBe(
                'number'
            );
        });

        it('should handle numeric date values', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        contentTook: 1,
                        resultsSize: 1,
                        jsonObjectView: {
                            contentlets: [
                                {
                                    title: 'Numeric Dates',
                                    hostName: 'localhost',
                                    modDate: 1764284834965,
                                    publishDate: 1764284834994,
                                    baseType: 'CONTENT',
                                    inode: 'inode3',
                                    archived: false,
                                    host: 'host1',
                                    ownerUserName: 'admin',
                                    working: true,
                                    locked: false,
                                    stInode: 'stinode1',
                                    contentType: 'Blog',
                                    live: true,
                                    owner: 'admin',
                                    identifier: 'id3',
                                    publishUserName: 'admin',
                                    publishUser: 'admin',
                                    languageId: 1,
                                    creationDate: 1764284749322,
                                    shortyId: 'shorty3',
                                    url: '/test3',
                                    titleImage: 'TITLE_IMAGE_NOT_FOUND',
                                    modUserName: 'admin',
                                    hasLiveVersion: true,
                                    folder: '/test',
                                    hasTitleImage: false,
                                    sortOrder: 0,
                                    modUser: 'admin',
                                    __icon__: 'icon',
                                    contentTypeIcon: 'icon',
                                    variant: 'default'
                                }
                            ]
                        },
                        queryTook: 1
                    },
                    errors: [],
                    messages: [],
                    i18nMessagesMap: {},
                    permissions: []
                })
            };
            mockFetch.mockResolvedValue(mockResponse);

            const params = {
                query: '+title:Numeric',
                limit: 1,
                languageId: 1,
                depth: 1,
                allCategoriesInfo: false
            };
            const result = await service.search(params);

            expect(result.entity?.jsonObjectView?.contentlets).toHaveLength(1);
            expect(result.entity?.jsonObjectView?.contentlets?.[0].modDate).toBe(1764284834965);
            expect(result.entity?.jsonObjectView?.contentlets?.[0].creationDate).toBe(
                1764284749322
            );
            expect(result.entity?.jsonObjectView?.contentlets?.[0].publishDate).toBe(1764284834994);
        });

        it('should handle null publishDate', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        contentTook: 1,
                        resultsSize: 1,
                        jsonObjectView: {
                            contentlets: [
                                {
                                    title: 'Null Publish Date',
                                    hostName: 'localhost',
                                    modDate: '1764284834965',
                                    publishDate: null,
                                    baseType: 'CONTENT',
                                    inode: 'inode4',
                                    archived: false,
                                    host: 'host1',
                                    ownerUserName: 'admin',
                                    working: true,
                                    locked: false,
                                    stInode: 'stinode1',
                                    contentType: 'Blog',
                                    live: false,
                                    owner: 'admin',
                                    identifier: 'id4',
                                    languageId: 1,
                                    creationDate: '1764284749322',
                                    shortyId: 'shorty4',
                                    url: '/test4',
                                    titleImage: 'TITLE_IMAGE_NOT_FOUND',
                                    modUserName: 'admin',
                                    hasLiveVersion: false,
                                    folder: '/test',
                                    hasTitleImage: false,
                                    sortOrder: 0,
                                    modUser: 'admin',
                                    __icon__: 'icon',
                                    contentTypeIcon: 'icon',
                                    variant: 'default'
                                }
                            ]
                        },
                        queryTook: 1
                    },
                    errors: [],
                    messages: [],
                    i18nMessagesMap: {},
                    permissions: []
                })
            };
            mockFetch.mockResolvedValue(mockResponse);

            const params = {
                query: '+title:Null',
                limit: 1,
                languageId: 1,
                depth: 1,
                allCategoriesInfo: false
            };
            const result = await service.search(params);

            expect(result.entity?.jsonObjectView?.contentlets).toHaveLength(1);
            expect(result.entity?.jsonObjectView?.contentlets?.[0].publishDate).toBeNull();
        });

        it('should handle invalid search parameters', async () => {
            const invalidParams = { query: 123 } as unknown as import('./search').SearchForm;
            await expect(service.search(invalidParams)).rejects.toThrow(
                'Invalid search parameters'
            );
        });

        it('should handle invalid search response format', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({ entity: 'not-an-array' })
            };
            mockFetch.mockResolvedValue(mockResponse);
            const params = {
                query: '+title:Test',
                limit: 1,
                languageId: 1,
                depth: 1,
                allCategoriesInfo: false
            };
            await expect(service.search(params)).rejects.toThrow('Invalid search response');
        });

        it('should handle fetch errors', async () => {
            const error = new Error('Network error');
            mockFetch.mockRejectedValue(error);
            const params = {
                query: '+title:Test',
                limit: 1,
                languageId: 1,
                depth: 1,
                allCategoriesInfo: false
            };
            await expect(service.search(params)).rejects.toThrow('Network error');
        });
    });
});
