import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

jest.mock('@dotcms/utils', () => ({
    ...jest.requireActual('@dotcms/utils'),
    getDownloadLink: jest.fn().mockReturnValue({ click: jest.fn() })
}));

import { DotHttpErrorManagerService, DotTagsService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';
import { getDownloadLink } from '@dotcms/utils';

import { DotTagsListStore } from './dot-tags-list.store';

const MOCK_TAGS: DotTag[] = [
    { id: '1', label: 'tag1', siteId: 'site1', siteName: 'Site 1', persona: false },
    { id: '2', label: 'tag2', siteId: 'site2', siteName: 'Site 2', persona: false }
];

const MOCK_PAGINATED_RESPONSE = {
    entity: MOCK_TAGS,
    pagination: { currentPage: 1, perPage: 25, totalEntries: 100 }
};

describe('DotTagsListStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotTagsListStore>>;
    let store: InstanceType<typeof DotTagsListStore>;
    let tagsService: jest.Mocked<DotTagsService>;

    const createService = createServiceFactory({
        service: DotTagsListStore,
        providers: [
            mockProvider(DotTagsService, {
                getTagsPaginated: jest.fn().mockReturnValue(of(MOCK_PAGINATED_RESPONSE)),
                createTag: jest.fn().mockReturnValue(of({ entity: MOCK_TAGS })),
                updateTag: jest.fn().mockReturnValue(of({ entity: MOCK_TAGS[0] })),
                deleteTags: jest
                    .fn()
                    .mockReturnValue(of({ entity: { successCount: 2, fails: [] } }))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        tagsService = spectator.inject(DotTagsService) as jest.Mocked<DotTagsService>;
        // The effect in onInit triggers loadTags automatically
        spectator.flushEffects();
    });

    describe('Initial State', () => {
        it('should have default initial state values after effect triggers loadTags', () => {
            expect(store.tags()).toEqual(MOCK_TAGS);
            expect(store.selectedTags()).toEqual([]);
            expect(store.totalRecords()).toBe(100);
            expect(store.page()).toBe(1);
            expect(store.rows()).toBe(25);
            expect(store.filter()).toBe('');
            expect(store.sortField()).toBe('tagname');
            expect(store.sortOrder()).toBe('ASC');
            expect(store.status()).toBe('loaded');
        });
    });

    describe('loadTags', () => {
        it('should call getTagsPaginated with correct params', () => {
            expect(tagsService.getTagsPaginated).toHaveBeenCalledWith({
                filter: undefined,
                page: 1,
                per_page: 25,
                orderBy: 'tagname',
                direction: 'ASC'
            });
        });

        it('should omit filter when empty', () => {
            const callArgs = tagsService.getTagsPaginated.mock.calls[0][0];
            expect(callArgs.filter).toBeUndefined();
        });

        it('should set tags and totalRecords on success', () => {
            expect(store.tags()).toEqual(MOCK_TAGS);
            expect(store.totalRecords()).toBe(100);
            expect(store.status()).toBe('loaded');
        });

        it('should handle error and set status to error', () => {
            tagsService.getTagsPaginated.mockReturnValue(throwError(() => new Error('fail')));
            store.loadTags();

            expect(store.status()).toBe('error');
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
        });
    });

    describe('setFilter', () => {
        it('should update filter and reset page to 1', () => {
            store.setPagination(3, 25);
            store.setFilter('test-filter');

            expect(store.filter()).toBe('test-filter');
            expect(store.page()).toBe(1);
        });
    });

    describe('setPagination', () => {
        it('should update page and rows', () => {
            store.setPagination(5, 50);

            expect(store.page()).toBe(5);
            expect(store.rows()).toBe(50);
        });
    });

    describe('setSort', () => {
        it('should update sortField and sortOrder', () => {
            store.setSort('label', 'DESC');

            expect(store.sortField()).toBe('label');
            expect(store.sortOrder()).toBe('DESC');
        });
    });

    describe('setSelectedTags', () => {
        it('should update selectedTags', () => {
            store.setSelectedTags(MOCK_TAGS);

            expect(store.selectedTags()).toEqual(MOCK_TAGS);
        });
    });

    describe('createTag', () => {
        it('should call tagsService.createTag and reload', () => {
            tagsService.getTagsPaginated.mockClear();

            store.createTag({ name: 'new-tag', siteId: 'site1' });

            expect(tagsService.createTag).toHaveBeenCalledWith([
                { name: 'new-tag', siteId: 'site1' }
            ]);
            expect(tagsService.getTagsPaginated).toHaveBeenCalled();
        });

        it('should omit siteId when empty', () => {
            store.createTag({ name: 'new-tag', siteId: '' });

            expect(tagsService.createTag).toHaveBeenCalledWith([
                { name: 'new-tag', siteId: undefined }
            ]);
        });

        it('should handle create error', () => {
            tagsService.createTag.mockReturnValue(throwError(() => new Error('create fail')));

            store.createTag({ name: 'new-tag', siteId: 'site1' });

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('updateTag', () => {
        it('should call tagsService.updateTag with correct args and reload', () => {
            tagsService.getTagsPaginated.mockClear();

            const tag = MOCK_TAGS[0];
            store.updateTag(tag, { name: 'updated-tag', siteId: 'site1' });

            expect(tagsService.updateTag).toHaveBeenCalledWith('1', {
                tagName: 'updated-tag',
                siteId: 'site1'
            });
            expect(tagsService.getTagsPaginated).toHaveBeenCalled();
        });

        it('should preserve original siteId when form siteId is empty', () => {
            const tag = MOCK_TAGS[0];
            store.updateTag(tag, { name: 'updated-tag', siteId: '' });

            expect(tagsService.updateTag).toHaveBeenCalledWith('1', {
                tagName: 'updated-tag',
                siteId: 'site1'
            });
        });

        it('should handle update error', () => {
            tagsService.updateTag.mockReturnValue(throwError(() => new Error('update fail')));

            store.updateTag(MOCK_TAGS[0], { name: 'updated-tag', siteId: 'site1' });

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('deleteTags', () => {
        it('should call tagsService.deleteTags with selected IDs, clear selection, and reload', () => {
            tagsService.getTagsPaginated.mockClear();
            store.setSelectedTags(MOCK_TAGS);

            store.deleteTags();

            expect(tagsService.deleteTags).toHaveBeenCalledWith(['1', '2']);
            expect(store.selectedTags()).toEqual([]);
            expect(tagsService.getTagsPaginated).toHaveBeenCalled();
        });

        it('should handle delete error', () => {
            tagsService.deleteTags.mockReturnValue(throwError(() => new Error('delete fail')));
            store.setSelectedTags(MOCK_TAGS);

            store.deleteTags();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('exportSelectedTags', () => {
        function readBlob(blob: Blob): Promise<string> {
            return new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(reader.result as string);
                reader.onerror = reject;
                reader.readAsText(blob);
            });
        }

        it('should generate CSV with correct content', async () => {
            const mockGetDownloadLink = getDownloadLink as jest.Mock;
            mockGetDownloadLink.mockClear();

            store.setSelectedTags(MOCK_TAGS);
            store.exportSelectedTags();

            expect(mockGetDownloadLink).toHaveBeenCalled();

            const blob: Blob = mockGetDownloadLink.mock.calls[0][0];
            const text = await readBlob(blob);
            expect(text).toContain('"Tag Name","Host ID"');
            expect(text).toContain('"tag1","site1"');
            expect(text).toContain('"tag2","site2"');
        });

        it('should escape quotes in tag names', async () => {
            const mockGetDownloadLink = getDownloadLink as jest.Mock;
            mockGetDownloadLink.mockClear();

            const tagsWithQuotes: DotTag[] = [
                {
                    id: '3',
                    label: 'tag "with" quotes',
                    siteId: 'site3',
                    siteName: 'Site 3',
                    persona: false
                }
            ];

            store.setSelectedTags(tagsWithQuotes);
            store.exportSelectedTags();

            const blob: Blob = mockGetDownloadLink.mock.calls[0][0];
            const text = await readBlob(blob);
            expect(text).toContain('"tag ""with"" quotes","site3"');
        });

        it('should sanitize formula injection characters in tag names', async () => {
            const mockGetDownloadLink = getDownloadLink as jest.Mock;
            mockGetDownloadLink.mockClear();

            const dangerousTags: DotTag[] = [
                {
                    id: '4',
                    label: '=SUM(A1)',
                    siteId: 'site4',
                    siteName: 'Site 4',
                    persona: false
                },
                {
                    id: '5',
                    label: '+cmd|test',
                    siteId: 'site5',
                    siteName: 'Site 5',
                    persona: false
                }
            ];

            store.setSelectedTags(dangerousTags);
            store.exportSelectedTags();

            const blob: Blob = mockGetDownloadLink.mock.calls[0][0];
            const text = await readBlob(blob);
            expect(text).toContain('"\'=SUM(A1)"');
            expect(text).toContain('"\'+cmd|test"');
        });

        it('should not export when no tags selected', () => {
            const mockGetDownloadLink = getDownloadLink as jest.Mock;
            mockGetDownloadLink.mockClear();

            store.setSelectedTags([]);
            store.exportSelectedTags();

            expect(mockGetDownloadLink).not.toHaveBeenCalled();
        });
    });

    describe('Effect (auto-reload)', () => {
        it('should trigger loadTags when filter changes', () => {
            tagsService.getTagsPaginated.mockClear();
            store.setFilter('new-filter');
            spectator.flushEffects();

            expect(tagsService.getTagsPaginated).toHaveBeenCalledWith(
                expect.objectContaining({ filter: 'new-filter' })
            );
        });

        it('should trigger loadTags when pagination changes', () => {
            tagsService.getTagsPaginated.mockClear();
            store.setPagination(2, 50);
            spectator.flushEffects();

            expect(tagsService.getTagsPaginated).toHaveBeenCalledWith(
                expect.objectContaining({ page: 2, per_page: 50 })
            );
        });
    });
});
