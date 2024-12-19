import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { TestBed, fakeAsync, tick } from '@angular/core/testing';

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

    describe('Method: loadContent', () => {
        it('should set error state when contentId is empty', fakeAsync(() => {
            store.loadContent('');
            tick();

            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.errorMessage()).toBe('dot.file.relationship.dialog.content.id.required');
            expect(service.getColumnsAndContent).not.toHaveBeenCalled();
        }));

        it('should load content successfully', fakeAsync(() => {
            service.getColumnsAndContent.mockReturnValue(of([mockColumns, mockData]));

            store.loadContent('123');
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

            store.loadContent('123');
            tick();

            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.errorMessage()).toBe(
                'dot.file.relationship.dialog.content.request.failed'
            );
            expect(service.getColumnsAndContent).toHaveBeenCalledWith('123');
        }));
    });

    describe('Method: applyInitialState', () => {
        it('should reset store to initial state', () => {
            // First set some data
            service.getColumnsAndContent.mockReturnValue(of([mockColumns, mockData]));
            store.loadContent('123');

            // Then reset
            store.applyInitialState();

            expect(store.data()).toEqual([]);
            expect(store.columns()).toEqual([]);
            expect(store.status()).toBe(ComponentStatus.INIT);
            expect(store.errorMessage()).toBeNull();
            expect(store.pagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 50
            });
        });
    });

    describe('Pagination Methods', () => {
        describe('Method: nextPage', () => {
            it('should update pagination state for next page', () => {
                store.nextPage();

                expect(store.pagination()).toEqual({
                    offset: 50,
                    currentPage: 2,
                    rowsPerPage: 50
                });
            });
        });

        describe('Method: previousPage', () => {
            it('should update pagination state for previous page', () => {
                // First go to next page
                store.nextPage();
                // Then go back
                store.previousPage();

                expect(store.pagination()).toEqual({
                    offset: 0,
                    currentPage: 1,
                    rowsPerPage: 50
                });
            });
        });
    });

    describe('Computed Properties', () => {
        it('should compute isLoading correctly', () => {
            store.loadContent('123');
            expect(store.isLoading()).toBe(true);
        });

        it('should compute totalPages correctly', fakeAsync(() => {
            service.getColumnsAndContent.mockReturnValue(of([mockColumns, mockData]));

            store.loadContent('123');
            tick();

            // With 3 items and 50 items per page, should be 1 page
            expect(store.totalPages()).toBe(1);
        }));
    });
});
