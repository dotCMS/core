import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { signal } from '@angular/core';

jest.mock('@dotcms/utils', () => ({
    ...jest.requireActual('@dotcms/utils'),
    getDownloadLink: jest.fn().mockReturnValue({ click: jest.fn() })
}));

import { DotHttpErrorManagerService, DotTagsService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { getDownloadLink } from '@dotcms/utils';

import { DotTagsListStore } from './dot-tags-list.store';

const MOCK_TAGS: DotTag[] = [
    { id: '1', label: 'tag1', siteId: 'site1', siteName: 'Site 1', persona: false },
    { id: '2', label: 'tag2', siteId: 'site2', siteName: 'Site 2', persona: false }
];

const MOCK_API_RESPONSE_BASE = {
    errors: [],
    messages: [],
    permissions: [],
    i18nMessagesMap: {}
};

const MOCK_PAGINATED_RESPONSE = {
    ...MOCK_API_RESPONSE_BASE,
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
                createTag: jest
                    .fn()
                    .mockReturnValue(of({ ...MOCK_API_RESPONSE_BASE, entity: MOCK_TAGS })),
                updateTag: jest
                    .fn()
                    .mockReturnValue(of({ ...MOCK_API_RESPONSE_BASE, entity: MOCK_TAGS[0] })),
                deleteTags: jest.fn().mockReturnValue(
                    of({
                        ...MOCK_API_RESPONSE_BASE,
                        entity: { successCount: 2, fails: [] }
                    })
                )
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(GlobalStore, {
                get currentSiteId() {
                    return currentSiteIdSignal;
                }
            })
        ]
    });

    let currentSiteIdSignal: ReturnType<typeof signal<string>>;

    beforeEach(() => {
        currentSiteIdSignal = signal('site-1');
        spectator = createService();
        store = spectator.service;
        tagsService = spectator.inject(DotTagsService) as jest.Mocked<DotTagsService>;
        // The effect in onInit triggers loadTags automatically
        spectator.flushEffects();
    });

    function readBlob(blob: Blob): Promise<string> {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result as string);
            reader.onerror = reject;
            reader.readAsText(blob);
        });
    }

    describe('Initial State', () => {
        it('should have default initial state values after effect triggers loadTags', () => {
            expect(store.tags()).toEqual(MOCK_TAGS);
            expect(store.selectedTags()).toEqual([]);
            expect(store.totalRecords()).toBe(100);
            expect(store.page()).toBe(1);
            expect(store.rows()).toBe(25);
            expect(store.filter()).toBe('');
            expect(store.showGlobal()).toBe(false);
            expect(store.sortField()).toBe('tagname');
            expect(store.sortOrder()).toBe('ASC');
            expect(store.status()).toBe('loaded');
        });
    });

    describe('loadTags', () => {
        it('should call getTagsPaginated with correct params including site', () => {
            expect(tagsService.getTagsPaginated).toHaveBeenCalledWith({
                filter: undefined,
                site: 'site-1',
                global: undefined,
                page: 1,
                per_page: 25,
                orderBy: 'tagname',
                direction: 'ASC'
            });
        });

        it('should send global=true when showGlobal is enabled', () => {
            tagsService.getTagsPaginated.mockClear();
            store.setShowGlobal(true);
            spectator.flushEffects();

            expect(tagsService.getTagsPaginated).toHaveBeenCalledWith(
                expect.objectContaining({ global: true })
            );
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

        it('should preserve selection across filter changes', () => {
            store.setSelectedTags(MOCK_TAGS);
            store.setFilter('test-filter');
            expect(store.selectedTags()).toEqual(MOCK_TAGS);
        });
    });

    describe('setShowGlobal', () => {
        it('should update showGlobal and reset page to 1', () => {
            store.setPagination(3, 25);
            store.setShowGlobal(true);

            expect(store.showGlobal()).toBe(true);
            expect(store.page()).toBe(1);
        });

        it('should preserve selection when toggling Show Global', () => {
            store.setSelectedTags(MOCK_TAGS);
            store.setShowGlobal(true);
            expect(store.selectedTags()).toEqual(MOCK_TAGS);
        });
    });

    describe('setPagination', () => {
        it('should update page and rows', () => {
            store.setPagination(5, 50);

            expect(store.page()).toBe(5);
            expect(store.rows()).toBe(50);
        });

        it('should preserve selection across page navigation', () => {
            store.setSelectedTags(MOCK_TAGS);
            store.setPagination(2, 25);
            expect(store.selectedTags()).toEqual(MOCK_TAGS);
        });
    });

    describe('setSort', () => {
        it('should update sortField and sortOrder', () => {
            store.setSort('label', 'DESC');

            expect(store.sortField()).toBe('label');
            expect(store.sortOrder()).toBe('DESC');
        });

        it('should preserve selection when sort changes', () => {
            store.setSelectedTags(MOCK_TAGS);
            store.setSort('label', 'DESC');
            expect(store.selectedTags()).toEqual(MOCK_TAGS);
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

    describe('exportSelected', () => {
        it('should dump the selection to CSV without calling the backend', async () => {
            const mockGetDownloadLink = getDownloadLink as jest.Mock;
            mockGetDownloadLink.mockClear();
            tagsService.getTagsPaginated.mockReturnValue(of(MOCK_PAGINATED_RESPONSE));
            store.loadTags();
            tagsService.getTagsPaginated.mockClear();

            store.setSelectedTags([store.tags()[0]]);
            store.exportSelected();

            expect(tagsService.getTagsPaginated).not.toHaveBeenCalled();
            const blob: Blob = mockGetDownloadLink.mock.calls[0][0];
            const text = await readBlob(blob);
            expect(text).toContain('"Tag Name","Host ID"');
            expect(text).toContain('"tag1","site1"');
            expect(text).not.toContain('"tag2"');
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
            store.exportSelected();

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
            store.exportSelected();

            const blob: Blob = mockGetDownloadLink.mock.calls[0][0];
            const text = await readBlob(blob);
            expect(text).toContain('"\'=SUM(A1)"');
            expect(text).toContain('"\'+cmd|test"');
        });

        it('should be a no-op when nothing is selected', () => {
            const mockGetDownloadLink = getDownloadLink as jest.Mock;
            mockGetDownloadLink.mockClear();
            tagsService.getTagsPaginated.mockClear();

            store.setSelectedTags([]);
            store.exportSelected();

            expect(mockGetDownloadLink).not.toHaveBeenCalled();
            expect(tagsService.getTagsPaginated).not.toHaveBeenCalled();
        });
    });

    describe('exportAll', () => {
        it('should fetch the entire filtered set in one request sized by totalRecords', async () => {
            const mockGetDownloadLink = getDownloadLink as jest.Mock;
            mockGetDownloadLink.mockClear();
            tagsService.getTagsPaginated.mockReturnValue(of(MOCK_PAGINATED_RESPONSE));
            store.loadTags();
            tagsService.getTagsPaginated.mockClear();

            store.exportAll();

            expect(tagsService.getTagsPaginated).toHaveBeenCalledTimes(1);
            expect(tagsService.getTagsPaginated).toHaveBeenCalledWith(
                expect.objectContaining({ page: 1, per_page: 100 })
            );

            const blob: Blob = mockGetDownloadLink.mock.calls[0][0];
            const text = await readBlob(blob);
            expect(text).toContain('"tag1","site1"');
            expect(text).toContain('"tag2","site2"');
        });

        it('should be a no-op when totalRecords is 0', () => {
            const mockGetDownloadLink = getDownloadLink as jest.Mock;
            mockGetDownloadLink.mockClear();
            tagsService.getTagsPaginated.mockReturnValue(
                of({
                    ...MOCK_API_RESPONSE_BASE,
                    entity: [],
                    pagination: { currentPage: 1, perPage: 25, totalEntries: 0 }
                })
            );
            store.loadTags();
            tagsService.getTagsPaginated.mockClear();

            store.exportAll();

            expect(tagsService.getTagsPaginated).not.toHaveBeenCalled();
            expect(mockGetDownloadLink).not.toHaveBeenCalled();
        });

        it('should propagate the current filter and showGlobal state to the fetch', () => {
            tagsService.getTagsPaginated.mockReturnValue(of(MOCK_PAGINATED_RESPONSE));
            store.loadTags();
            store.setFilter('marketing');
            store.setShowGlobal(true);
            tagsService.getTagsPaginated.mockClear();

            store.exportAll();

            expect(tagsService.getTagsPaginated).toHaveBeenCalledWith(
                expect.objectContaining({ filter: 'marketing', global: true })
            );
        });

        it('should handle backend errors', () => {
            tagsService.getTagsPaginated.mockReturnValue(of(MOCK_PAGINATED_RESPONSE));
            store.loadTags();
            tagsService.getTagsPaginated.mockReturnValue(
                throwError(() => new Error('export fail'))
            );

            store.exportAll();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('showExportAll (computed)', () => {
        it('should be false when nothing is selected', () => {
            tagsService.getTagsPaginated.mockReturnValue(of(MOCK_PAGINATED_RESPONSE));
            store.loadTags();
            store.setSelectedTags([]);
            spectator.flushEffects();
            expect(store.showExportAll()).toBe(false);
        });

        it('should be false on partial selection', () => {
            tagsService.getTagsPaginated.mockReturnValue(of(MOCK_PAGINATED_RESPONSE));
            store.loadTags();
            store.setSelectedTags([store.tags()[0]]);
            spectator.flushEffects();
            expect(store.showExportAll()).toBe(false);
        });

        it('should be false when all visible selected but only one page exists', () => {
            tagsService.getTagsPaginated.mockReturnValue(
                of({
                    ...MOCK_API_RESPONSE_BASE,
                    entity: MOCK_TAGS,
                    pagination: { currentPage: 1, perPage: 25, totalEntries: 2 }
                })
            );
            store.loadTags();
            store.setSelectedTags(store.tags());
            spectator.flushEffects();
            expect(store.showExportAll()).toBe(false);
        });

        it('should be true when all visible selected and there are more pages', () => {
            tagsService.getTagsPaginated.mockReturnValue(of(MOCK_PAGINATED_RESPONSE));
            store.loadTags();
            store.setSelectedTags(store.tags());
            spectator.flushEffects();
            expect(store.showExportAll()).toBe(true);
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

        it('should reset page to 1, set status to loading, and clear selection when currentSiteId changes', () => {
            tagsService.getTagsPaginated.mockReturnValue(of(MOCK_PAGINATED_RESPONSE));
            store.setPagination(3, 25);
            store.setSelectedTags([store.tags()[0]]);
            spectator.flushEffects();
            expect(store.page()).toBe(3);
            expect(store.selectedTags().length).toBe(1);

            currentSiteIdSignal.set('site-2');
            spectator.flushEffects();

            expect(store.page()).toBe(1);
            expect(store.selectedTags()).toEqual([]);
            // Status is set to loading by the site-change effect; loadTags() then runs and sets loaded when the mock completes
            expect(store.status()).toBe('loaded');
        });

        it('should pass the updated site to getTagsPaginated when currentSiteId changes', () => {
            tagsService.getTagsPaginated.mockClear();
            currentSiteIdSignal.set('site-2');
            spectator.flushEffects();

            expect(tagsService.getTagsPaginated).toHaveBeenCalledWith(
                expect.objectContaining({ site: 'site-2' })
            );
        });
    });
});
