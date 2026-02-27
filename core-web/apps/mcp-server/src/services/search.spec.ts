import { ContentSearchService, type SearchForm } from './search';

import { mockFetch } from '../test-setup';

describe('ContentSearchService', () => {
    let service: ContentSearchService;

    beforeEach(() => {
        service = new ContentSearchService();
    });

    describe('search', () => {
        it('should post to /api/v1/drive/search with the provided payload and return parsed results', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        contentCount: 1,
                        contentTotalCount: 1,
                        folderCount: 0,
                        list: [
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
                    errors: [],
                    messages: [],
                    i18nMessagesMap: {},
                    permissions: []
                })
            };
            mockFetch.mockResolvedValue(mockResponse);

            const params: SearchForm = {
                assetPath: '//SiteName/',
                includeSystemHost: true,
                filters: { text: 'footer', filterFolders: true },
                contentTypes: [],
                offset: 0,
                maxResults: 20,
                sortBy: 'modDate:desc',
                archived: false,
                showFolders: false
            };
            const result = await service.search(params);

            expect(mockFetch).toHaveBeenCalledTimes(1);
            const [url, options] = mockFetch.mock.calls[0];
            expect(url).toBe('/api/v1/drive/search');
            expect(options.method).toBe('POST');
            expect(JSON.parse(options.body)).toEqual(params);

            expect(result.entity.contentCount).toBe(1);
            expect(result.entity.list).toHaveLength(1);
            expect(result.entity.list[0].title).toBe('Test Result');
        });

        it('should handle invalid drive search parameters', async () => {
            const invalidParams = { assetPath: 123 } as unknown as SearchForm;
            await expect(service.search(invalidParams)).rejects.toThrow(
                'Invalid drive search parameters'
            );
        });

        it('should handle invalid drive search response format', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({ entity: 'not-a-valid-entity' })
            };
            mockFetch.mockResolvedValue(mockResponse);

            const params: SearchForm = {
                assetPath: '//SiteName/',
                includeSystemHost: true,
                filters: { text: 'footer', filterFolders: true },
                contentTypes: [],
                offset: 0,
                maxResults: 20,
                sortBy: 'modDate:desc',
                archived: false,
                showFolders: false
            };

            await expect(service.search(params)).rejects.toThrow('Invalid drive search response');
        });

        it('should propagate fetch errors', async () => {
            const error = new Error('Network error');
            mockFetch.mockRejectedValue(error);

            const params: SearchForm = {
                assetPath: '//SiteName/',
                includeSystemHost: true,
                filters: { text: 'footer', filterFolders: true },
                contentTypes: [],
                offset: 0,
                maxResults: 20,
                sortBy: 'modDate:desc',
                archived: false,
                showFolders: false
            };

            await expect(service.search(params)).rejects.toThrow('Network error');
        });
    });
});
