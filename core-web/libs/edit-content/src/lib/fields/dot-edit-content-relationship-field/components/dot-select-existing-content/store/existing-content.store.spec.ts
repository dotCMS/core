import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { Observable, of, throwError } from 'rxjs';

import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { ToggleSwitchChangeEvent } from 'primeng/toggleswitch';

import { delay } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { createFakeContentlet, mockLocales } from '@dotcms/utils-testing';

import {
    ExistingContentService,
    RelationshipFieldSearchResponse
} from './existing-content.service';
import { ExistingContentStore } from './existing-content.store';

import { Column } from '../../../models/column.model';

describe('ExistingContentStore', () => {
    let store: InstanceType<typeof ExistingContentStore>;
    let service: SpyObject<ExistingContentService>;

    const mockColumns: Column[] = [
        { field: 'title', header: 'Title' },
        { field: 'modDate', header: 'Mod Date' }
    ];

    const mockData = {
        contentlets: [
            createFakeContentlet({
                id: '1',
                inode: '1',
                identifier: 'id-1',
                languageId: mockLocales[0].id
            }),
            createFakeContentlet({
                id: '2',
                inode: '2',
                identifier: 'id-2',
                languageId: mockLocales[1].id
            })
        ],
        totalResults: 2
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [ExistingContentStore, mockProvider(ExistingContentService)]
        });

        store = TestBed.inject(ExistingContentStore);
        service = TestBed.inject(ExistingContentService) as SpyObject<ExistingContentService>;
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    describe('State Management', () => {
        it('should handle empty contentTypeId', fakeAsync(() => {
            store.initLoad({ contentTypeId: null, selectionMode: 'single', selectedItemsIds: [] });
            tick();

            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.errorMessage()).toBe('dot.file.relationship.dialog.content.id.required');
            expect(service.getColumnsAndContent).not.toHaveBeenCalled();
        }));

        it('should load content successfully', fakeAsync(() => {
            service.getColumnsAndContent.mockReturnValue(of([mockColumns, mockData]));

            store.initLoad({ contentTypeId: '123', selectionMode: 'single', selectedItemsIds: [] });
            tick();

            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.columns()).toEqual(mockColumns);
            expect(store.data()).toEqual(mockData.contentlets);
            expect(service.getColumnsAndContent).toHaveBeenCalledWith('123');
        }));

        it('should handle error when loading content', fakeAsync(() => {
            service.getColumnsAndContent.mockReturnValue(
                throwError(() => new Error('Server Error'))
            );

            store.initLoad({ contentTypeId: '123', selectionMode: 'single', selectedItemsIds: [] });
            tick();

            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.errorMessage()).toBe(
                'dot.file.relationship.dialog.content.request.failed'
            );
            expect(service.getColumnsAndContent).toHaveBeenCalledWith('123');
        }));
    });

    describe('Initial State', () => {
        it('should have correct initial state', () => {
            expect(store.data()).toEqual([]);
            expect(store.columns()).toEqual([]);
            expect(store.status()).toBe(ComponentStatus.INIT);
            expect(store.errorMessage()).toBeNull();
            expect(store.pagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 50,
                totalResults: 0
            });
            expect(store.selectionMode()).toBe(null);
        });
    });

    describe('Pagination', () => {
        it('should handle next page', () => {
            store.nextPage();

            expect(store.pagination()).toEqual({
                offset: 50,
                currentPage: 2,
                rowsPerPage: 50,
                totalResults: 0
            });
        });

        it('should handle previous page', () => {
            store.nextPage();
            store.previousPage();

            expect(store.pagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 50,
                totalResults: 0
            });
        });

        it('should not go to previous page when on first page', () => {
            store.previousPage();

            expect(store.pagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 50,
                totalResults: 0
            });
        });
    });

    describe('Computed Properties', () => {
        it('should compute loading state correctly', fakeAsync(() => {
            const mockObservable = of([mockColumns, mockData]).pipe(delay(100)) as Observable<
                [Column[], RelationshipFieldSearchResponse]
            >;

            service.getColumnsAndContent.mockReturnValue(mockObservable);

            store.initLoad({ contentTypeId: '123', selectionMode: 'single', selectedItemsIds: [] });
            expect(store.isLoading()).toBe(true);

            tick(100);
            expect(store.isLoading()).toBe(false);
        }));

        it('should compute total pages correctly', fakeAsync(() => {
            service.getColumnsAndContent.mockReturnValue(of([mockColumns, mockData]));

            store.initLoad({ contentTypeId: '123', selectionMode: 'single', selectedItemsIds: [] });
            tick();

            expect(store.totalPages()).toBe(1);
        }));
    });

    describe('Show Selected Items Toggle', () => {
        beforeEach(fakeAsync(() => {
            service.getColumnsAndContent.mockReturnValue(of([mockColumns, mockData]));
            store.initLoad({
                contentTypeId: '123',
                selectionMode: 'multiple',
                selectedItemsIds: []
            });
            tick();
        }));

        it('should initialize with showOnlySelected set to false', () => {
            expect(store.isSelectedView()).toBe(false);
            expect(store.viewMode()).toBe('all');
            expect(store.previousPagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 50,
                totalResults: 0
            });
        });

        it('should change the viewMode state and update previousPagination', () => {
            // Move to page 2
            store.nextPage();
            expect(store.pagination().currentPage).toBe(2);

            // Toggle to selected view
            const event = { checked: true } as ToggleSwitchChangeEvent;
            store.changeViewMode(event);
            expect(store.isSelectedView()).toBe(true);
            expect(store.viewMode()).toBe('selected');
            // previousPagination should be page 2
            expect(store.previousPagination()).toEqual({
                offset: 50,
                currentPage: 2,
                rowsPerPage: 50,
                totalResults: 2
            });
            // pagination should reset to initial
            expect(store.pagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 50,
                totalResults: 0
            });
        });

        it('should restore previousPagination when toggling back to all view', () => {
            // Move to page 3
            store.setOffset({ first: 100 } as import('primeng/table').TablePageEvent);
            expect(store.pagination().currentPage).toBe(3);

            // Toggle to selected view
            store.changeViewMode({ checked: true } as ToggleSwitchChangeEvent);
            expect(store.isSelectedView()).toBe(true);
            expect(store.pagination().currentPage).toBe(1); // reset

            // Toggle back to all view
            store.changeViewMode({ checked: false } as ToggleSwitchChangeEvent);
            expect(store.isSelectedView()).toBe(false);
            // pagination should be restored to previousPagination (page 3)
            expect(store.pagination().currentPage).toBe(3);
            expect(store.pagination().offset).toBe(100);
        });

        it('should allow toggling multiple times and always restore previousPagination', () => {
            // Move to page 2
            store.nextPage();
            // Toggle to selected view
            store.changeViewMode({ checked: true } as ToggleSwitchChangeEvent);
            // Move to page 1 in selected view (already at 1)
            expect(store.pagination().currentPage).toBe(1);
            // Toggle back to all view
            store.changeViewMode({ checked: false } as ToggleSwitchChangeEvent);
            expect(store.pagination().currentPage).toBe(2);
            // Move to page 3
            store.setOffset({ first: 100 } as import('primeng/table').TablePageEvent);
            // Toggle to selected view again
            store.changeViewMode({ checked: true } as ToggleSwitchChangeEvent);
            expect(store.pagination().currentPage).toBe(1);
            // Toggle back to all view
            store.changeViewMode({ checked: false } as ToggleSwitchChangeEvent);
            expect(store.pagination().currentPage).toBe(3);
        });

        it('should return all data when showOnlySelected is false', () => {
            expect(store.filteredData()).toEqual(mockData.contentlets);
        });

        it('should return only selected items when showOnlySelected is true', () => {
            // Select one of the items
            const selectedItem = mockData.contentlets[0];
            store.setSelectionItems([selectedItem]);

            // Toggle to show only selected items
            const event = { checked: true } as ToggleSwitchChangeEvent;
            store.changeViewMode(event);

            // Should filter to show only the selected item
            expect(store.filteredData()).toEqual([selectedItem]);
        });

        it('should return empty data when showOnlySelected is true but no items are selected', () => {
            store.initLoad({
                contentTypeId: '123',
                selectionMode: 'multiple',
                selectedItemsIds: []
            });
            const event = { checked: true } as ToggleSwitchChangeEvent;
            store.changeViewMode(event);
            expect(store.filteredData()).toEqual([]);
        });

        it('should return only the selected item when showOnlySelected is true', () => {
            store.initLoad({
                contentTypeId: '123',
                selectionMode: 'multiple',
                selectedItemsIds: [mockData.contentlets[0].inode]
            });
            const event = { checked: true } as ToggleSwitchChangeEvent;
            store.changeViewMode(event);
            expect(store.filteredData()).toEqual([mockData.contentlets[0]]);
        });
    });

    describe('setOffset', () => {
        it('should set the correct offset and current page in pagination', () => {
            // Set initial pagination
            store.nextPage(); // move to page 2
            expect(store.pagination().currentPage).toBe(2);

            // Set offset to 0 (should be page 1)
            store.setOffset({ first: 0 } as import('primeng/table').TablePageEvent);
            expect(store.pagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 50,
                totalResults: 0
            });

            // Set offset to 100 (should be page 3)
            store.setOffset({ first: 100 } as import('primeng/table').TablePageEvent);
            expect(store.pagination().currentPage).toBe(3);
            expect(store.pagination().offset).toBe(100);
        });
    });
});
