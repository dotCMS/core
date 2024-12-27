import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { Observable, of, throwError } from 'rxjs';

import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { delay } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { RelationshipFieldService } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/services/relationship-field.service';

import { ExistingContentStore } from './existing-content.store';

import { Column } from '../../../models/column.model';
import { RelationshipFieldItem } from '../../../models/relationship.models';

describe('ExistingContentStore', () => {
    let store: InstanceType<typeof ExistingContentStore>;
    let service: SpyObject<RelationshipFieldService>;

    const mockColumns: Column[] = [
        { field: 'title', header: 'Title' },
        { field: 'modDate', header: 'Mod Date' }
    ];

    const mockData: RelationshipFieldItem[] = [
        { id: '1', title: 'Content 1', language: '1', modDate: new Date().toISOString() },
        { id: '2', title: 'Content 2', language: '1', modDate: new Date().toISOString() },
        { id: '3', title: 'Content 3', language: '1', modDate: new Date().toISOString() }
    ];

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [ExistingContentStore, mockProvider(RelationshipFieldService)]
        });

        store = TestBed.inject(ExistingContentStore);
        service = TestBed.inject(RelationshipFieldService) as SpyObject<RelationshipFieldService>;
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    describe('State Management', () => {
        it('should handle empty contentTypeId', fakeAsync(() => {
            store.initLoad({ contentTypeId: null, selectionMode: 'single' });
            tick();

            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.errorMessage()).toBe('dot.file.relationship.dialog.content.id.required');
            expect(service.getColumnsAndContent).not.toHaveBeenCalled();
        }));

        it('should load content successfully', fakeAsync(() => {
            service.getColumnsAndContent.mockReturnValue(of([mockColumns, mockData]));

            store.initLoad({ contentTypeId: '123', selectionMode: 'single' });
            tick();

            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.columns()).toEqual(mockColumns);
            expect(store.data()).toEqual(mockData);
            expect(service.getColumnsAndContent).toHaveBeenCalledWith('123');
        }));

        it('should handle error when loading content', fakeAsync(() => {
            service.getColumnsAndContent.mockReturnValue(
                throwError(() => new Error('Server Error'))
            );

            store.initLoad({ contentTypeId: '123', selectionMode: 'single' });
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
                rowsPerPage: 50
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
                rowsPerPage: 50
            });
        });

        it('should handle previous page', () => {
            store.nextPage();
            store.previousPage();

            expect(store.pagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 50
            });
        });

        it('should not go to previous page when on first page', () => {
            store.previousPage();

            expect(store.pagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 50
            });
        });
    });

    describe('Computed Properties', () => {
        it('should compute loading state correctly', fakeAsync(() => {
            const mockObservable = of([mockColumns, mockData]).pipe(delay(100)) as Observable<
                [Column[], RelationshipFieldItem[]]
            >;

            service.getColumnsAndContent.mockReturnValue(mockObservable);

            store.initLoad({ contentTypeId: '123', selectionMode: 'single' });
            expect(store.isLoading()).toBe(true);

            tick(100);
            expect(store.isLoading()).toBe(false);
        }));

        it('should compute total pages correctly', fakeAsync(() => {
            service.getColumnsAndContent.mockReturnValue(of([mockColumns, mockData]));

            store.initLoad({ contentTypeId: '123', selectionMode: 'single' });
            tick();

            expect(store.totalPages()).toBe(1);
        }));
    });
});
