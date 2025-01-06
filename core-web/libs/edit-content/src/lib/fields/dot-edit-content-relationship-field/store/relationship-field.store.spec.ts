import { TestBed } from '@angular/core/testing';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { RelationshipFieldStore } from './relationship-field.store';

import { DynamicRelationshipFieldItem } from '../models/relationship.models';

describe('RelationshipFieldStore', () => {
    let store: InstanceType<typeof RelationshipFieldStore>;

    const mockData: DynamicRelationshipFieldItem[] = [
        { id: '1', title: 'Content 1', language: '1', modDate: new Date().toISOString() },
        { id: '2', title: 'Content 2', language: '1', modDate: new Date().toISOString() },
        { id: '3', title: 'Content 3', language: '1', modDate: new Date().toISOString() }
    ];

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
        describe('setData', () => {
            it('should set data correctly', () => {
                store.setData(mockData);
                expect(store.data()).toEqual(mockData);
            });
        });

        describe('setCardinality', () => {
            it('should set single selection mode for ONE_TO_ONE relationship', () => {
                store.setCardinality(2); // ONE_TO_ONE cardinality
                expect(store.selectionMode()).toBe('single');
            });

            it('should set multiple selection mode for other relationship types', () => {
                store.setCardinality(0); // ONE_TO_MANY cardinality
                expect(store.selectionMode()).toBe('multiple');
            });

            it('should throw error for invalid cardinality', () => {
                expect(() => store.setCardinality(999)).toThrow('Invalid relationship type');
            });
        });

        describe('addData', () => {
            it('should add new unique data to existing data', () => {
                const initialData = [mockData[0]];
                const newData = [mockData[1], mockData[2]];

                store.setData(initialData);
                store.addData(newData);

                expect(store.data()).toEqual([...initialData, ...newData]);
            });

            it('should not add duplicate data', () => {
                const initialData = [mockData[0]];
                const newData = [mockData[0], mockData[1]];

                store.setData(initialData);
                store.addData(newData);

                expect(store.data()).toEqual([mockData[0], mockData[1]]);
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
            it('should disable for single mode with one item', () => {
                store.setCardinality(2); // ONE_TO_ONE
                store.setData([mockData[0]]);
                expect(store.isDisabledCreateNewContent()).toBe(true);
            });

            it('should not disable for single mode with no items', () => {
                store.setCardinality(2); // ONE_TO_ONE
                expect(store.isDisabledCreateNewContent()).toBe(false);
            });

            it('should not disable for multiple mode regardless of items', () => {
                store.setCardinality(0); // ONE_TO_MANY
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
