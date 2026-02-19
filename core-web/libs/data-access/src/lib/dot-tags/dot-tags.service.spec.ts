import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotCMSAPIResponse, DotTag } from '@dotcms/dotcms-models';

import { DotTagsService } from './dot-tags.service';

describe('DotTagsService', () => {
    let spectator: SpectatorHttp<DotTagsService>;

    const createFakeTag = (overrides: Partial<DotTag> = {}): DotTag => ({
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
});
