import { TestBed } from '@angular/core/testing';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { RelationshipFieldStore } from './relationship-field.store';

describe('RelationshipFieldStore', () => {
    let store: InstanceType<typeof RelationshipFieldStore>;

    const mockData = [
        createFakeContentlet({
            inode: 'inode1',
            identifier: 'identifier1',
            id: '1'
        }),
        createFakeContentlet({
            inode: 'inode2',
            identifier: 'identifier2',
            id: '2'
        }),
        createFakeContentlet({
            inode: 'inode3',
            identifier: 'identifier3',
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
                const field = {
                    ...mockField,
                    variable: 'relationship_field',
                    relationships: { cardinality: 2 }
                };
                store.initialize({
                    field,
                    contentlet: mockContentlet
                });
                expect(store.selectionMode()).toBe('single');
            });

            it('should set multiple selection mode for other relationship types', () => {
                const field = {
                    ...mockField,
                    variable: 'relationship_field',
                    relationships: { cardinality: 0 }
                };
                store.initialize({
                    field,
                    contentlet: mockContentlet
                });
                expect(store.selectionMode()).toBe('multiple');
            });

            it('should initialize data from contentlet', () => {
                const field = {
                    ...mockField,
                    variable: 'relationship_field',
                    relationships: { cardinality: 0 }
                };
                store.initialize({
                    field,
                    contentlet: mockContentlet
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
                const field = {
                    ...mockField,
                    variable: 'relationship_field',
                    relationships: { cardinality: 2 }
                };
                store.initialize({
                    field,
                    contentlet: mockContentlet
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
                const field = {
                    ...mockField,
                    variable: 'relationship_field',
                    relationships: { cardinality: 0 }
                };
                store.initialize({
                    field,
                    contentlet: mockContentlet
                });
                store.setData(mockData);
                expect(store.isDisabledCreateNewContent()).toBe(false);
            });
        });

        describe('formattedRelationship', () => {
            it('should format relationship IDs correctly', () => {
                store.setData(mockData);
                expect(store.formattedRelationship()).toBe('identifier1,identifier2,identifier3');
            });

            it('should handle empty data', () => {
                expect(store.formattedRelationship()).toBe('');
            });

            it('should handle single item', () => {
                store.setData([mockData[0]]);
                expect(store.formattedRelationship()).toBe('identifier1');
            });

            it('should handle data with different identifiers', () => {
                const customData = [
                    createFakeContentlet({ identifier: 'abc123', id: 'abc123' }),
                    createFakeContentlet({ identifier: 'def456', id: 'def456' })
                ];
                store.setData(customData);
                expect(store.formattedRelationship()).toBe('abc123,def456');
            });

            it('should handle data with special characters in identifiers', () => {
                const specialData = [
                    createFakeContentlet({ identifier: 'test-123', id: 'test-123' }),
                    createFakeContentlet({ identifier: 'test_456', id: 'test_456' })
                ];
                store.setData(specialData);
                expect(store.formattedRelationship()).toBe('test-123,test_456');
            });
        });
    });

    describe('Edge Cases', () => {
        describe('pagination handling', () => {
            it('should handle multiple page transitions correctly', () => {
                // Move forward two pages
                store.nextPage();
                store.nextPage();
                expect(store.pagination()).toEqual({
                    offset: 12,
                    currentPage: 3,
                    rowsPerPage: 6
                });

                // Move back one page
                store.previousPage();
                expect(store.pagination()).toEqual({
                    offset: 6,
                    currentPage: 2,
                    rowsPerPage: 6
                });
            });
        });

        describe('data manipulation', () => {
            it('should handle deletion of non-existent item gracefully', () => {
                store.setData(mockData);
                const initialLength = store.data().length;
                store.deleteItem('non-existent-inode');
                expect(store.data().length).toBe(initialLength);
            });

            it('should handle multiple deletions correctly', () => {
                store.setData(mockData);
                store.deleteItem('inode1');
                store.deleteItem('inode2');
                expect(store.data().length).toBe(1);
                expect(store.data()[0].inode).toBe('inode3');
            });
        });

        describe('initialization edge cases', () => {
            it('should handle initialization with empty contentlet', () => {
                const emptyContentlet = createFakeContentlet({
                    id: 'empty',
                    inode: 'empty',
                    variable: 'relationship_field'
                });
                const field = {
                    ...mockField,
                    variable: 'relationship_field',
                    relationships: { cardinality: 0 }
                };
                store.initialize({
                    field,
                    contentlet: emptyContentlet
                });
                expect(store.data()).toBeDefined();
                expect(store.data().length).toBe(0);
            });

            it('should handle extreme cardinality values', () => {
                expect(() => {
                    const field = {
                        ...mockField,
                        variable: 'relationship_field',
                        relationships: { cardinality: 999 }
                    };
                    store.initialize({
                        field,
                        contentlet: mockContentlet
                    });
                }).toThrowError();
            });
        });
    });
});
