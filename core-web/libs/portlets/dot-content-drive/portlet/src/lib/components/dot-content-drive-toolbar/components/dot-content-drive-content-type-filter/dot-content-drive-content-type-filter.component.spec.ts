import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotContentTypeFilterComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveContentTypeFilterComponent } from './dot-content-drive-content-type-filter.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveContentTypeFilterComponent', () => {
    let spectator: Spectator<DotContentDriveContentTypeFilterComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    const createComponent = createComponentFactory({
        component: DotContentDriveContentTypeFilterComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                getFilterValue: jest.fn().mockReturnValue(undefined),
                patchFilters: jest.fn(),
                removeFilter: jest.fn()
            }),
            mockProvider(DotContentTypeService, {
                getAllContentTypes: jest.fn().mockReturnValue(of([])),
                getContentTypesWithPagination: jest.fn().mockReturnValue(
                    of({
                        contentTypes: [],
                        pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
                    })
                )
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.type-filter.title': 'Content Types',
                    'content-drive.type-filter.all-content-types': 'All Content Types',
                    search: 'Search'
                })
            },
            provideHttpClient()
        ],
        detectChanges: false
    });

    const contentTypeFilter = () =>
        spectator.fixture.debugElement.query(By.directive(DotContentTypeFilterComponent));

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        store.getFilterValue.mockReset().mockReturnValue(undefined);
    });

    afterEach(() => jest.clearAllMocks());

    it('should render the shared content-type filter', () => {
        spectator.detectChanges();

        expect(contentTypeFilter()).toBeTruthy();
    });

    describe('store → filter (numeric keys to base-type names)', () => {
        it('should decode the baseType filter into base-type names', () => {
            store.getFilterValue.mockImplementation(((key: string) =>
                key === 'baseType' ? ['1', '4'] : undefined) as never);
            spectator.detectChanges();

            expect(contentTypeFilter().componentInstance.$baseTypes()).toEqual([
                'CONTENT',
                'FILEASSET'
            ]);
        });

        it('should pass the contentType variables straight through', () => {
            store.getFilterValue.mockImplementation(((key: string) =>
                key === 'contentType' ? ['blog', 'videoFile'] : undefined) as never);
            spectator.detectChanges();

            expect(contentTypeFilter().componentInstance.$contentTypes()).toEqual([
                'blog',
                'videoFile'
            ]);
        });

        it('should bind empty selections when no filters are set', () => {
            spectator.detectChanges();

            expect(contentTypeFilter().componentInstance.$baseTypes()).toEqual([]);
            expect(contentTypeFilter().componentInstance.$contentTypes()).toEqual([]);
        });
    });

    describe('filter → store (base-type names to numeric keys)', () => {
        beforeEach(() => spectator.detectChanges());

        it('should encode base-type names as numeric keys', () => {
            spectator.triggerEventHandler(contentTypeFilter(), 'selectionChange', {
                baseTypes: ['CONTENT', 'FILEASSET'],
                contentTypes: []
            });

            expect(store.patchFilters).toHaveBeenCalledWith({ baseType: ['1', '4'] });
        });

        it('should patch the content-type variables as-is', () => {
            spectator.triggerEventHandler(contentTypeFilter(), 'selectionChange', {
                baseTypes: ['CONTENT'],
                contentTypes: ['blog']
            });

            expect(store.patchFilters).toHaveBeenCalledWith({ contentType: ['blog'] });
        });

        it('should remove both filters when the selection is empty', () => {
            spectator.triggerEventHandler(contentTypeFilter(), 'selectionChange', {
                baseTypes: [],
                contentTypes: []
            });

            expect(store.removeFilter).toHaveBeenCalledWith('baseType');
            expect(store.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('should remove only the content-type filter when base types remain selected', () => {
            spectator.triggerEventHandler(contentTypeFilter(), 'selectionChange', {
                baseTypes: ['CONTENT'],
                contentTypes: []
            });

            expect(store.patchFilters).toHaveBeenCalledWith({ baseType: ['1'] });
            expect(store.removeFilter).toHaveBeenCalledWith('contentType');
            expect(store.removeFilter).not.toHaveBeenCalledWith('baseType');
        });

        it('should drop base types that have no numeric key', () => {
            spectator.triggerEventHandler(contentTypeFilter(), 'selectionChange', {
                baseTypes: ['CONTENT', 'NOT_A_BASE_TYPE'],
                contentTypes: []
            });

            expect(store.patchFilters).toHaveBeenCalledWith({ baseType: ['1'] });
        });
    });
});
