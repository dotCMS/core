import { expect, describe } from '@jest/globals';
import { SpectatorService, createServiceFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import {
    DotContentTypeService,
    DotFieldService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { ComponentStatus, FeaturedFlags } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeRelationshipField } from '@dotcms/utils-testing';

import { RelationshipFieldService } from './relationship-field.service';
import { RelationshipFieldStore } from './relationship-field.store';

describe('RelationshipFieldStore', () => {
    let spectator: SpectatorService<InstanceType<typeof RelationshipFieldStore>>;
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

    const mockContentType = {
        id: 'test-content-type',
        name: 'Test Content Type',
        metadata: {
            [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
        }
    };

    const createStoreService = createServiceFactory({
        service: RelationshipFieldStore,
        providers: [
            RelationshipFieldService,
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn().mockReturnValue(of(mockContentType))
            }),
            mockProvider(DotFieldService),
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn()
            })
        ]
    });

    beforeEach(() => {
        spectator = createStoreService();
        store = spectator.inject(RelationshipFieldStore);
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
                const mockField = createFakeRelationshipField({
                    variable: 'relationship_field',
                    relationships: { cardinality: 2, isParentField: true, velocityVar: 'AllTypes' }
                });
                store.initialize({
                    field: mockField,
                    contentlet: mockContentlet
                });

                expect(store.selectionMode()).toBe('single');
            });

            it('should set multiple selection mode for other relationship types', () => {
                const mockField = createFakeRelationshipField({
                    variable: 'relationship_field',
                    relationships: { cardinality: 0, isParentField: true, velocityVar: 'AllTypes' }
                });
                store.initialize({
                    field: mockField,
                    contentlet: mockContentlet
                });

                expect(store.selectionMode()).toBe('multiple');
            });

            it('should initialize data from contentlet', () => {
                const mockField = createFakeRelationshipField({
                    variable: 'relationship_field',
                    relationships: { cardinality: 0, isParentField: true, velocityVar: 'AllTypes' }
                });
                store.initialize({
                    field: mockField,
                    contentlet: mockContentlet
                });

                expect(store.data()).toBeDefined();
            });

            it('should load content type when initialized', () => {
                const dotContentTypeService = spectator.inject(DotContentTypeService);

                const mockField = createFakeRelationshipField({
                    variable: 'relationship_field',
                    relationships: {
                        cardinality: 0,
                        isParentField: true,
                        velocityVar: 'test-content-type'
                    }
                });

                store.initialize({
                    field: mockField,
                    contentlet: mockContentlet
                });

                expect(dotContentTypeService.getContentType).toHaveBeenCalledWith(
                    'test-content-type'
                );
                expect(store.status()).toBe(ComponentStatus.LOADED);
                expect(store.contentType()).toEqual(mockContentType);
                expect(store.isNewEditorEnabled()).toBe(true);
            });
        });

        describe('setData', () => {
            it('should set data correctly', () => {
                store.setData(mockData);

                expect(store.data()).toEqual(mockData);
            });

            it('should reset pagination to page 1 when data is replaced', () => {
                const eightItems = Array.from({ length: 8 }, (_, i) =>
                    createFakeContentlet({
                        inode: `set-inode-${i + 1}`,
                        identifier: `set-identifier-${i + 1}`,
                        id: `${i + 1}`
                    })
                );
                store.setData(eightItems);

                // Navigate to page 2
                store.nextPage();
                expect(store.pagination().currentPage).toBe(2);
                expect(store.pagination().offset).toBe(6);

                // Replace with fewer items
                store.setData(mockData);

                expect(store.pagination().currentPage).toBe(1);
                expect(store.pagination().offset).toBe(0);
                expect(store.data()).toEqual(mockData);
            });
        });

        describe('reorderData', () => {
            it('should update data without resetting pagination', () => {
                const eightItems = Array.from({ length: 8 }, (_, i) =>
                    createFakeContentlet({
                        inode: `reorder-inode-${i + 1}`,
                        identifier: `reorder-identifier-${i + 1}`,
                        id: `${i + 1}`
                    })
                );
                store.setData(eightItems);

                // Navigate to page 2
                store.nextPage();
                expect(store.pagination().currentPage).toBe(2);
                expect(store.pagination().offset).toBe(6);

                // Reorder items (swap first two)
                const reordered = [...eightItems];
                [reordered[0], reordered[1]] = [reordered[1], reordered[0]];
                store.reorderData(reordered);

                // Pagination should be preserved
                expect(store.pagination().currentPage).toBe(2);
                expect(store.pagination().offset).toBe(6);
                expect(store.data()[0].inode).toBe('reorder-inode-2');
                expect(store.data()[1].inode).toBe('reorder-inode-1');
            });

            it('should update data on page 1 without changing pagination', () => {
                const items = Array.from({ length: 8 }, (_, i) =>
                    createFakeContentlet({
                        inode: `p1-inode-${i + 1}`,
                        identifier: `p1-identifier-${i + 1}`,
                        id: `${i + 1}`
                    })
                );
                store.setData(items);

                expect(store.pagination().currentPage).toBe(1);
                expect(store.pagination().offset).toBe(0);

                // Reorder items
                const reordered = [...items];
                [reordered[0], reordered[1]] = [reordered[1], reordered[0]];
                store.reorderData(reordered);

                // Should stay on page 1
                expect(store.pagination().currentPage).toBe(1);
                expect(store.pagination().offset).toBe(0);
                expect(store.data()[0].inode).toBe('p1-inode-2');
            });
        });

        describe('deleteItem', () => {
            it('should delete item by inode', () => {
                store.setData(mockData);
                store.deleteItem('inode1');

                expect(store.data().length).toBe(2);
                expect(store.data().find((item) => item.inode === 'inode1')).toBeUndefined();
            });

            it('should reset pagination when current page exceeds total after delete', () => {
                const sevenItems = Array.from({ length: 7 }, (_, i) =>
                    createFakeContentlet({
                        inode: `del-inode-${i + 1}`,
                        identifier: `del-identifier-${i + 1}`,
                        id: `${i + 1}`
                    })
                );
                store.setData(sevenItems);

                // Navigate to page 2 (offset 6, only item index 6)
                store.nextPage();
                expect(store.pagination().currentPage).toBe(2);
                expect(store.pagination().offset).toBe(6);

                // Delete the single item on page 2
                store.deleteItem('del-inode-7');

                // Should reset to page 1
                expect(store.pagination().currentPage).toBe(1);
                expect(store.pagination().offset).toBe(0);
                expect(store.data().length).toBe(6);
            });

            it('should stay on current page when delete does not invalidate it', () => {
                const nineItems = Array.from({ length: 9 }, (_, i) =>
                    createFakeContentlet({
                        inode: `stay-inode-${i + 1}`,
                        identifier: `stay-identifier-${i + 1}`,
                        id: `${i + 1}`
                    })
                );
                store.setData(nineItems);

                // Navigate to page 2 (offset 6, items 7-9)
                store.nextPage();
                expect(store.pagination().currentPage).toBe(2);
                expect(store.pagination().offset).toBe(6);

                // Delete one of the 3 items on page 2
                store.deleteItem('stay-inode-8');

                // Should stay on page 2
                expect(store.pagination().currentPage).toBe(2);
                expect(store.pagination().offset).toBe(6);
                expect(store.data().length).toBe(8);
            });

            it('should reset pagination to page 1 when all items are deleted', () => {
                const sevenItems = Array.from({ length: 7 }, (_, i) =>
                    createFakeContentlet({
                        inode: `all-inode-${i + 1}`,
                        identifier: `all-identifier-${i + 1}`,
                        id: `${i + 1}`
                    })
                );
                store.setData(sevenItems);

                // Navigate to page 2
                store.nextPage();

                // Delete all items on page 2, then all on page 1
                store.deleteItem('all-inode-7');
                // Now back on page 1 (auto-reset), delete remaining
                for (let i = 1; i <= 6; i++) {
                    store.deleteItem(`all-inode-${i}`);
                }

                expect(store.pagination().currentPage).toBe(1);
                expect(store.pagination().offset).toBe(0);
                expect(store.data().length).toBe(0);
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
                const mockField = createFakeRelationshipField({
                    variable: 'relationship_field',
                    relationships: {
                        cardinality: 2,
                        isParentField: true,
                        velocityVar: 'test-content-type'
                    }
                });
                store.initialize({
                    field: mockField,
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
                const mockField = createFakeRelationshipField({
                    variable: 'relationship_field',
                    relationships: {
                        cardinality: 0,
                        isParentField: true,
                        velocityVar: 'test-content-type'
                    }
                });
                store.initialize({
                    field: mockField,
                    contentlet: mockContentlet
                });
                store.setData(mockData);

                expect(store.isDisabledCreateNewContent()).toBe(false);
            });
        });

        describe('paginatedData', () => {
            const paginatedMockData = Array.from({ length: 8 }, (_, i) =>
                createFakeContentlet({
                    inode: `page-inode-${i + 1}`,
                    identifier: `page-identifier-${i + 1}`,
                    id: `${i + 1}`
                })
            );

            it('should return first page slice by default', () => {
                store.setData(paginatedMockData);

                const result = store.paginatedData();
                expect(result.length).toBe(6);
                expect(result[0].inode).toBe('page-inode-1');
                expect(result[5].inode).toBe('page-inode-6');
            });

            it('should return second page after nextPage()', () => {
                store.setData(paginatedMockData);
                store.nextPage();

                const result = store.paginatedData();
                expect(result.length).toBe(2);
                expect(result[0].inode).toBe('page-inode-7');
                expect(result[1].inode).toBe('page-inode-8');
            });

            it('should return empty array when data is empty', () => {
                expect(store.paginatedData()).toEqual([]);
            });

            it('should update when data changes', () => {
                store.setData(paginatedMockData);
                expect(store.paginatedData().length).toBe(6);

                store.setData(paginatedMockData.slice(0, 3));
                expect(store.paginatedData().length).toBe(3);
            });
        });

        describe('showThumbnail', () => {
            it('should return false when no items have title images', () => {
                store.setData([
                    createFakeContentlet({ inode: '1', hasTitleImage: false }),
                    createFakeContentlet({ inode: '2', hasTitleImage: false })
                ]);

                expect(store.showThumbnail()).toBe(false);
            });

            it('should return true when at least one item has a title image', () => {
                store.setData([
                    createFakeContentlet({ inode: '1', hasTitleImage: false }),
                    createFakeContentlet({ inode: '2', hasTitleImage: true })
                ]);

                expect(store.showThumbnail()).toBe(true);
            });

            it('should return false when data is empty', () => {
                expect(store.showThumbnail()).toBe(false);
            });

            it('should return true when hasTitleImage is string "true"', () => {
                store.setData([
                    createFakeContentlet({
                        inode: '1',
                        hasTitleImage: 'true' as unknown as boolean
                    })
                ]);

                expect(store.showThumbnail()).toBe(true);
            });

            it('should return false when hasTitleImage is string "false"', () => {
                store.setData([
                    createFakeContentlet({
                        inode: '1',
                        hasTitleImage: 'false' as unknown as boolean
                    })
                ]);

                expect(store.showThumbnail()).toBe(false);
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
                const mockField = createFakeRelationshipField({
                    variable: 'relationship_field',
                    relationships: {
                        cardinality: 0,
                        isParentField: true,
                        velocityVar: 'test-content-type'
                    }
                });
                store.initialize({
                    field: mockField,
                    contentlet: emptyContentlet
                });

                expect(store.data()).toBeDefined();
                expect(store.data().length).toBe(0);
            });

            it('should handle extreme cardinality values', () => {
                const mockField = createFakeRelationshipField({
                    variable: 'relationship_field',
                    relationships: {
                        cardinality: 9999,
                        isParentField: true,
                        velocityVar: 'test-content-type'
                    }
                });
                store.initialize({
                    field: mockField,
                    contentlet: mockContentlet
                });

                expect(store.status()).toBe(ComponentStatus.ERROR);
            });
        });
    });
});

describe('RelationshipFieldStore - Instance Isolation', () => {
    afterEach(() => TestBed.resetTestingModule());

    const mockContentType = {
        id: 'test-content-type',
        name: 'Test Content Type',
        metadata: {
            [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
        }
    };

    const storeProviders = [
        RelationshipFieldStore,
        RelationshipFieldService,
        mockProvider(DotContentTypeService, {
            getContentType: jest.fn().mockReturnValue(of(mockContentType))
        }),
        mockProvider(DotFieldService),
        mockProvider(DotHttpErrorManagerService, {
            handle: jest.fn()
        })
    ];

    /**
     * These tests use TestBed directly because Spectator's createServiceFactory
     * shares the same TestBed context — calling it twice returns the same singleton.
     * TestBed.resetTestingModule() is needed to create truly independent injectors.
     */

    it('should create independent instances that do not share state', () => {
        const injector1 = TestBed.configureTestingModule({ providers: [...storeProviders] });
        const store1 = injector1.inject(RelationshipFieldStore);

        TestBed.resetTestingModule();

        const injector2 = TestBed.configureTestingModule({ providers: [...storeProviders] });
        const store2 = injector2.inject(RelationshipFieldStore);

        expect(store1).not.toBe(store2);

        const dataA = [
            createFakeContentlet({ inode: 'a1', identifier: 'id-a1', id: 'a1' }),
            createFakeContentlet({ inode: 'a2', identifier: 'id-a2', id: 'a2' })
        ];
        const dataB = [createFakeContentlet({ inode: 'b1', identifier: 'id-b1', id: 'b1' })];

        store1.setData(dataA);
        store2.setData(dataB);

        expect(store1.data().length).toBe(2);
        expect(store2.data().length).toBe(1);
        expect(store1.formattedRelationship()).toBe('id-a1,id-a2');
        expect(store2.formattedRelationship()).toBe('id-b1');
    });

    it('should not reset one instance when another initializes', () => {
        const injector1 = TestBed.configureTestingModule({ providers: [...storeProviders] });
        const store1 = injector1.inject(RelationshipFieldStore);

        TestBed.resetTestingModule();

        const injector2 = TestBed.configureTestingModule({ providers: [...storeProviders] });
        const store2 = injector2.inject(RelationshipFieldStore);

        const data = [createFakeContentlet({ inode: 'x1', identifier: 'id-x1', id: 'x1' })];
        store1.setData(data);

        const mockField = createFakeRelationshipField({
            variable: 'other_field',
            relationships: { cardinality: 0, isParentField: true, velocityVar: 'test-content-type' }
        });
        const mockContentlet = createFakeContentlet({ id: '999', inode: '999' });

        store2.initialize({ field: mockField, contentlet: mockContentlet });

        expect(store1.data().length).toBe(1);
        expect(store1.formattedRelationship()).toBe('id-x1');
    });

    it('should not affect other instance when deleting items', () => {
        const injector1 = TestBed.configureTestingModule({ providers: [...storeProviders] });
        const store1 = injector1.inject(RelationshipFieldStore);

        TestBed.resetTestingModule();

        const injector2 = TestBed.configureTestingModule({ providers: [...storeProviders] });
        const store2 = injector2.inject(RelationshipFieldStore);

        const sharedItem = createFakeContentlet({
            inode: 'shared-inode',
            identifier: 'shared-id',
            id: 'shared'
        });

        store1.setData([sharedItem]);
        store2.setData([sharedItem]);

        store1.deleteItem('shared-inode');

        expect(store1.data().length).toBe(0);
        expect(store2.data().length).toBe(1);
    });
});
