import { expect, describe } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator';
import { of, throwError } from 'rxjs';

import { DotContentTypeService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus, FeaturedFlags } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

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
            {
                provide: DotContentTypeService,
                useValue: {
                    getContentType: jest.fn()
                }
            },
            {
                provide: DotHttpErrorManagerService,
                useValue: {
                    handle: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createStoreService();
        store = spectator.inject(RelationshipFieldStore);

        // Set up default mock behavior
        const dotContentTypeService = spectator.inject(DotContentTypeService);
        jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(of(mockContentType));
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
                    variable: 'relationship_field',
                    contentTypeId: 'test-content-type'
                });

                expect(store.selectionMode()).toBe('single');
            });

            it('should set multiple selection mode for other relationship types', () => {
                store.initialize({
                    cardinality: 0,
                    contentlet: mockContentlet,
                    variable: 'relationship_field',
                    contentTypeId: 'test-content-type'
                });

                expect(store.selectionMode()).toBe('multiple');
            });

            it('should initialize data from contentlet', () => {
                store.initialize({
                    cardinality: 0,
                    contentlet: mockContentlet,
                    variable: 'relationship_field',
                    contentTypeId: 'test-content-type'
                });

                expect(store.data()).toBeDefined();
            });

            it('should load content type when initialized', () => {
                const dotContentTypeService = spectator.inject(DotContentTypeService);
                const getContentTypeSpy = jest.spyOn(dotContentTypeService, 'getContentType');

                store.initialize({
                    cardinality: 0,
                    contentlet: mockContentlet,
                    variable: 'relationship_field',
                    contentTypeId: 'test-content-type'
                });

                expect(getContentTypeSpy).toHaveBeenCalledWith('test-content-type');
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
                    variable: 'relationship_field',
                    contentTypeId: 'test-content-type'
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
                    variable: 'relationship_field',
                    contentTypeId: 'test-content-type'
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

                store.initialize({
                    cardinality: 0,
                    contentlet: emptyContentlet,
                    variable: 'relationship_field',
                    contentTypeId: 'test-content-type'
                });

                expect(store.data()).toBeDefined();
                expect(store.data().length).toBe(0);
            });

            it('should handle extreme cardinality values', async () => {
                const dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
                const handleErrorSpy = jest.spyOn(dotHttpErrorManagerService, 'handle');

                store.initialize({
                    cardinality: 999,
                    contentlet: mockContentlet,
                    variable: 'relationship_field',
                    contentTypeId: 'test-content-type'
                });

                // Wait for async operation to complete
                await new Promise((resolve) => setTimeout(resolve, 10));

                expect(handleErrorSpy).toHaveBeenCalled();
                expect(store.status()).toBe(ComponentStatus.ERROR);
            });
        });

        describe('content type loading', () => {
            it('should handle content type loading error', async () => {
                const dotContentTypeService = spectator.inject(DotContentTypeService);
                const dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);

                jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                    throwError(() => new Error('Content type not found'))
                );

                const handleErrorSpy = jest.spyOn(dotHttpErrorManagerService, 'handle');

                store.initialize({
                    cardinality: 0,
                    contentlet: mockContentlet,
                    variable: 'relationship_field',
                    contentTypeId: 'test-content-type'
                });

                // Wait for async operation to complete
                await new Promise((resolve) => setTimeout(resolve, 10));

                expect(handleErrorSpy).toHaveBeenCalled();
                expect(store.status()).toBe(ComponentStatus.ERROR);
            });
        });
    });
});
