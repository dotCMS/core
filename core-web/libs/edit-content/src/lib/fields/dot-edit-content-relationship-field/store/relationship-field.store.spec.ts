import { TestBed } from '@angular/core/testing';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { RelationshipFieldStore } from './relationship-field.store';

describe('RelationshipFieldStore', () => {
    let store: InstanceType<typeof RelationshipFieldStore>;

    const mockData = [
        createFakeContentlet({
            inode: 'inode1',
            id: '1'
        }),
        createFakeContentlet({
            inode: 'inode2',
            id: '2'
        }),
        createFakeContentlet({
            inode: 'inode3',
            id: '3'
        })
    ];

    const mockContentlet = createFakeContentlet({
        id: '123',
        inode: '123',
        variable: 'relationship_field'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [RelationshipFieldStore]
        });

        store = TestBed.inject(RelationshipFieldStore);
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    describe('Initial State', () => {
        it('should have correct initial state', () => {
            expect(store.data()).toEqual([]);
            expect(store.status()).toBe(ComponentStatus.INIT);
            expect(store.selectionMode()).toBeNull();
            expect(store.pagination()).toEqual({
                offset: 0,
                currentPage: 1,
                rowsPerPage: 6
            });
        });
    });

    describe('State Management', () => {
        describe('initialize', () => {
            it('should set single selection mode for ONE_TO_ONE relationship', () => {
                store.initialize({
                    cardinality: 2,
                    contentlet: mockContentlet,
                    variable: 'relationship_field'
                });
                expect(store.selectionMode()).toBe('single');
            });

            it('should set multiple selection mode for other relationship types', () => {
                store.initialize({
                    cardinality: 0,
                    contentlet: mockContentlet,
                    variable: 'relationship_field'
                });
                expect(store.selectionMode()).toBe('multiple');
            });

            it('should initialize data from contentlet', () => {
                store.initialize({
                    cardinality: 0,
                    contentlet: mockContentlet,
                    variable: 'relationship_field'
                });
                expect(store.data()).toBeDefined();
            });
        });

        describe('setData', () => {
            it('should set data correctly', () => {
                store.setData(mockData);
                expect(store.data()).toEqual(mockData);
            });
        });

        describe('addData', () => {
            it('should add new data', () => {
                store.addData(mockData);
                expect(store.data()).toEqual(mockData);
            });
        });

        describe('deleteItem', () => {
            it('should delete item by inode', () => {
                store.setData(mockData);
                store.deleteItem('inode1');
                expect(store.data().length).toBe(2);
                expect(store.data().find((item) => item.inode === 'inode1')).toBeUndefined();
            });
        });

        describe('pagination', () => {
            it('should handle next page correctly', () => {
                store.nextPage();
                expect(store.pagination()).toEqual({
                    offset: 6,
                    currentPage: 2,
                    rowsPerPage: 6
                });
            });

            it('should handle previous page correctly', () => {
                store.nextPage();
                store.previousPage();
                expect(store.pagination()).toEqual({
                    offset: 0,
                    currentPage: 1,
                    rowsPerPage: 6
                });
            });
        });
    });

    describe('Computed Properties', () => {
        describe('totalPages', () => {
            it('should compute total pages correctly', () => {
                store.setData(mockData);
                expect(store.totalPages()).toBe(1);
            });

            it('should handle empty data', () => {
                expect(store.totalPages()).toBe(0);
            });
        });

        describe('isDisabledCreateNewContent', () => {
            beforeEach(() => {
                store.initialize({
                    cardinality: 2,
                    contentlet: mockContentlet,
                    variable: 'relationship_field'
                });
            });

            it('should disable for single mode with one item', () => {
                store.setData([mockData[0]]);
                expect(store.isDisabledCreateNewContent()).toBe(true);
            });

            it('should not disable for single mode with no items', () => {
                store.setData([]);
                expect(store.isDisabledCreateNewContent()).toBe(false);
            });

            it('should not disable for multiple mode regardless of items', () => {
                store.initialize({
                    cardinality: 0,
                    contentlet: mockContentlet,
                    variable: 'relationship_field'
                });
                store.setData(mockData);
                expect(store.isDisabledCreateNewContent()).toBe(false);
            });
        });

        describe('formattedRelationship', () => {
            it('should format relationship IDs correctly', () => {
                store.setData(mockData);
                expect(store.formattedRelationship()).toBe('1,2,3');
            });

            it('should handle empty data', () => {
                expect(store.formattedRelationship()).toBe('');
            });
        });
    });
});
