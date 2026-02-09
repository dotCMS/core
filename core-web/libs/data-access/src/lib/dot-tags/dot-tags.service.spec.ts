import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotCMSAPIResponse, DotTag } from '@dotcms/dotcms-models';

import { DotTagsService } from './dot-tags.service';

describe('DotTagsService', () => {
    let spectator: SpectatorHttp<DotTagsService>;

    const createFakeTag = (overrides: Partial<DotTag> = {}): DotTag => ({
        id: 'test-id',
        label: 'test',
        siteId: '1',
        siteName: 'Site',
        persona: false,
        ...overrides
    });

    const createHttp = createHttpFactory({
        service: DotTagsService
    });

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should get tags suggestions without name filter', () => {
        const mockTag1 = createFakeTag({ label: 'test' });
        const mockTag2 = createFakeTag({ label: 'united' });
        const mockResponse: Record<string, DotTag> = {
            test: mockTag1,
            united: mockTag2
        };

        spectator.service.getSuggestions().subscribe((res) => {
            expect(res).toEqual([mockTag1, mockTag2]);
        });

        const req = spectator.expectOne('/api/v1/tags', HttpMethod.GET);
        req.flush(mockResponse);
    });

    it('should get tags suggestions filtered by name', () => {
        const mockTag1 = createFakeTag({ label: 'test' });
        const mockTag2 = createFakeTag({ label: 'testing' });
        const mockResponse: Record<string, DotTag> = {
            test: mockTag1,
            testing: mockTag2
        };

        spectator.service.getSuggestions('test').subscribe((res) => {
            expect(res).toEqual([mockTag1, mockTag2]);
        });

        const req = spectator.expectOne('/api/v1/tags?name=test', HttpMethod.GET);
        req.flush(mockResponse);
    });

    it('should get tags by name', () => {
        const mockTag1 = createFakeTag({ label: 'angular' });
        const mockTag2 = createFakeTag({ label: 'typescript' });
        const mockResponse: DotCMSAPIResponse<DotTag[]> = {
            entity: [mockTag1, mockTag2],
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        };

        spectator.service.getTags('angular').subscribe((res) => {
            expect(res).toEqual([mockTag1, mockTag2]);
        });

        const req = spectator.expectOne('/api/v2/tags?name=angular', HttpMethod.GET);
        req.flush(mockResponse);
    });

    it('should get paginated tags with all params', () => {
        const mockTag1 = createFakeTag({ label: 'tag1' });
        const mockTag2 = createFakeTag({ label: 'tag2' });
        const mockResponse: DotCMSAPIResponse<DotTag[]> = {
            entity: [mockTag1, mockTag2],
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {},
            pagination: { currentPage: 2, perPage: 10, totalEntries: 100 }
        };

        spectator.service
            .getTagsPaginated({
                filter: 'test',
                page: 2,
                per_page: 10,
                orderBy: 'tagname',
                direction: 'ASC'
            })
            .subscribe((res) => {
                expect(res).toEqual(mockResponse);
            });

        const req = spectator.expectOne(
            '/api/v2/tags?filter=test&page=2&per_page=10&orderBy=tagname&direction=ASC',
            HttpMethod.GET
        );
        req.flush(mockResponse);
    });

    it('should get paginated tags with partial params', () => {
        const mockTag1 = createFakeTag({ label: 'tag1' });
        const mockResponse: DotCMSAPIResponse<DotTag[]> = {
            entity: [mockTag1],
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {},
            pagination: { currentPage: 1, perPage: 25, totalEntries: 1 }
        };

        spectator.service.getTagsPaginated({ filter: 'test', page: 1 }).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne('/api/v2/tags?filter=test&page=1', HttpMethod.GET);
        req.flush(mockResponse);
    });

    it('should get paginated tags with no params', () => {
        const mockResponse: DotCMSAPIResponse<DotTag[]> = {
            entity: [],
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {},
            pagination: { currentPage: 1, perPage: 25, totalEntries: 0 }
        };

        spectator.service.getTagsPaginated({}).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne('/api/v2/tags', HttpMethod.GET);
        req.flush(mockResponse);
    });

    it('should create a single tag', () => {
        const mockTag = createFakeTag({ label: 'new-tag' });
        const mockResponse: DotCMSAPIResponse<DotTag[]> = {
            entity: [mockTag],
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        };

        spectator.service.createTag([{ name: 'new-tag' }]).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne('/api/v2/tags', HttpMethod.POST);
        expect(req.request.body).toEqual([{ name: 'new-tag' }]);
        req.flush(mockResponse);
    });

    it('should create multiple tags with siteId', () => {
        const mockTag1 = createFakeTag({ label: 'tag1', siteId: '123' });
        const mockTag2 = createFakeTag({ label: 'tag2', siteId: '456' });
        const mockResponse: DotCMSAPIResponse<DotTag[]> = {
            entity: [mockTag1, mockTag2],
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        };

        const tags = [
            { name: 'tag1', siteId: '123' },
            { name: 'tag2', siteId: '456' }
        ];

        spectator.service.createTag(tags).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne('/api/v2/tags', HttpMethod.POST);
        expect(req.request.body).toEqual(tags);
        req.flush(mockResponse);
    });

    it('should update a tag', () => {
        const mockTag = createFakeTag({ id: 'tag-123', label: 'updated-name', siteId: 'site-1' });
        const mockResponse: DotCMSAPIResponse<DotTag> = {
            entity: mockTag,
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        };

        spectator.service
            .updateTag('tag-123', { tagName: 'updated-name', siteId: 'site-1' })
            .subscribe((res) => {
                expect(res).toEqual(mockResponse);
            });

        const req = spectator.expectOne('/api/v2/tags/tag-123', HttpMethod.PUT);
        expect(req.request.body).toEqual({ tagName: 'updated-name', siteId: 'site-1' });
        req.flush(mockResponse);
    });

    it('should delete tags by ids', () => {
        const mockResponse: DotCMSAPIResponse<{ successCount: number; fails: unknown[] }> = {
            entity: { successCount: 2, fails: [] },
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        };

        spectator.service.deleteTags(['id-1', 'id-2']).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne('/api/v2/tags', HttpMethod.DELETE);
        expect(req.request.body).toEqual(['id-1', 'id-2']);
        req.flush(mockResponse);
    });

    it('should import tags from a CSV file', () => {
        const mockFile = new File(['content'], 'test.csv', { type: 'text/csv' });
        const mockResponse: DotCMSAPIResponse<{
            totalRows: number;
            successCount: number;
            failureCount: number;
            success: boolean;
        }> = {
            entity: { totalRows: 10, successCount: 8, failureCount: 2, success: true },
            errors: [],
            messages: [],
            permissions: [],
            i18nMessagesMap: {}
        };

        spectator.service.importTags(mockFile).subscribe((res) => {
            expect(res).toEqual(mockResponse);
        });

        const req = spectator.expectOne('/api/v2/tags/import', HttpMethod.POST);
        expect(req.request.body).toBeInstanceOf(FormData);
        req.flush(mockResponse);
    });
});
