import { ContentSearchService } from './search';

import { mockFetch } from '../test-setup';

describe('ContentSearchService', () => {
    let service: ContentSearchService;

    beforeEach(() => {
        service = new ContentSearchService();
    });

    describe('search', () => {
        it('should search content successfully', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        contentTook: 1,
                        resultsSize: 1,
                        jsonObjectView: {
                            contentlets: [
                                {
                                    id: '1',
                                    title: 'Test Result',
                                    hostName: 'localhost',
                                    modDate: '20240101',
                                    publishDate: '20240101',
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
                                    creationDate: '20240101',
                                    shortyId: 'shorty1',
                                    url: '/test',
                                    titleImage: '',
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
