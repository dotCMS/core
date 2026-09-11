import { expect, describe } from '@jest/globals';
import { SpectatorService, createServiceFactory, mockProvider } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import {
    DotContentTypeService,
    DotFieldService,
    DotHttpErrorManagerService,
    DotPropertiesService
} from '@dotcms/data-access';
import { ComponentStatus, DotLanguage, FeaturedFlags } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeRelationshipField } from '@dotcms/utils-testing';

import { RelationshipFieldService } from './relationship-field.service';
import { RELATED_PAGE_SIZE, RelationshipFieldStore } from './relationship-field.store';

import { DotEditContentService } from '../../../services/dot-edit-content.service';

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
            }),
            mockProvider(DotEditContentService, {
                getContentById: jest.fn().mockReturnValue(of({}))
            }),
            // `withFlags` batch-fetches the side-panel flag on init.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest
                    .fn()
                    .mockReturnValue(
                        of({ [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: false })
                    )
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
            expect(store.visibleCount()).toBe(RELATED_PAGE_SIZE);
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
        });

        describe('reorderData', () => {
            it('should apply the new order and mark the change as a user edit', () => {
                const items = Array.from({ length: 8 }, (_, i) =>
                    createFakeContentlet({
                        inode: `reorder-inode-${i + 1}`,
                        identifier: `reorder-identifier-${i + 1}`,
                        id: `${i + 1}`
                    })
                );
                store.setData(items);

                const reordered = [...items];
                [reordered[0], reordered[1]] = [reordered[1], reordered[0]];
                store.reorderData(reordered);

                expect(store.data()[0].inode).toBe('reorder-inode-2');
                expect(store.data()[1].inode).toBe('reorder-inode-1');
                expect(store.lastChangeSource()).toBe('user');
            });
        });

        describe('refreshItem', () => {
            it('should replace the matching item by identifier, keeping revealed rows revealed', () => {
                const eightItems = Array.from({ length: 8 }, (_, i) =>
                    createFakeContentlet({
                        inode: `refresh-inode-${i + 1}`,
                        identifier: `refresh-identifier-${i + 1}`,
                        id: `${i + 1}`
                    })
                );
                store.setData(eightItems);

                // The editor has revealed a second page of rows and is looking at one they just
                // edited elsewhere.
                store.loadMore();
                expect(store.visibleCount()).toBe(RELATED_PAGE_SIZE * 2);

                // A save mints a new inode; the identifier is what stays stable.
                store.refreshItem(
                    createFakeContentlet({
                        inode: 'inode-after-save',
                        identifier: 'refresh-identifier-7',
                        title: 'Edited elsewhere'
                    })
                );

                // Collapsing the revealed rows would lose the editor's place — this is why the
                // method exists instead of reusing setData.
                expect(store.visibleCount()).toBe(RELATED_PAGE_SIZE * 2);
                expect(store.data()[6].inode).toBe('inode-after-save');
                expect(store.data()[6].title).toBe('Edited elsewhere');
                expect(store.data().length).toBe(8);
            });

            it('should mark the change as a load so it never dirties the form', () => {
                store.setData(mockData);
                expect(store.lastChangeSource()).toBe('user');

                store.refreshItem(
                    createFakeContentlet({ inode: 'new-inode', identifier: 'identifier1' })
                );

                // The relationship itself did not change — only the version of one entry.
                expect(store.lastChangeSource()).toBe('load');
            });

            it('should be a no-op when the identifier is not in the list', () => {
                store.setData(mockData);
                const before = store.data();

                store.refreshItem(
                    createFakeContentlet({ inode: 'x', identifier: 'not-related-here' })
                );

                expect(store.data()).toBe(before);
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
    });

    describe('Computed Properties', () => {
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

        describe('locale resolution (targetLanguageId)', () => {
            const mockLanguage: DotLanguage = {
                id: 42,
                language: 'Spanish',
                languageCode: 'es',
                isoCode: 'es-ES'
            };

            let dotEditContentService: InstanceType<typeof DotEditContentService>;
            let relationshipFieldService: RelationshipFieldService;
            let mockField: ReturnType<typeof createFakeRelationshipField>;

            beforeEach(() => {
                dotEditContentService = spectator.inject(DotEditContentService);
                relationshipFieldService = spectator.inject(RelationshipFieldService);
                mockField = createFakeRelationshipField({
                    variable: 'relationship_field',
                    relationships: {
                        cardinality: 0,
                        isParentField: true,
                        velocityVar: 'test-content-type'
                    }
                });
            });

            it('should skip locale resolution when targetLanguageId is not provided', () => {
                const items = [createFakeContentlet({ inode: 'inode1', identifier: 'id1' })];
                store.setData(items);

                jest.spyOn(relationshipFieldService, 'prepareField').mockReturnValue(
                    of({
                        data: [],
                        contentType: mockContentType,
                        columns: [],
                        selectionMode: 'multiple',
                        isNewEditorEnabled: false
                    } as never)
                );

                store.initialize({ field: mockField, contentlet: null });

                expect(dotEditContentService.getContentById).not.toHaveBeenCalled();
            });

            it('should skip locale resolution when dataToProcess is empty', () => {
                jest.spyOn(relationshipFieldService, 'prepareField').mockReturnValue(
                    of({
                        data: [],
                        contentType: mockContentType,
                        columns: [],
                        selectionMode: 'multiple',
                        isNewEditorEnabled: false
                    } as never)
                );

                store.initialize({
                    field: mockField,
                    contentlet: null,
                    targetLanguageId: 42,
                    targetLanguage: mockLanguage
                });

                expect(dotEditContentService.getContentById).not.toHaveBeenCalled();
            });

            it('should call getContentById for each item when targetLanguageId is provided', () => {
                const items = [
                    createFakeContentlet({ inode: 'inode1', identifier: 'id1' }),
                    createFakeContentlet({ inode: 'inode2', identifier: 'id2' })
                ];
                store.setData(items);

                jest.spyOn(relationshipFieldService, 'prepareField').mockReturnValue(
                    of({
                        data: [],
                        contentType: mockContentType,
                        columns: [],
                        selectionMode: 'multiple',
                        isNewEditorEnabled: false
                    } as never)
                );

                store.initialize({
                    field: mockField,
                    contentlet: null,
                    targetLanguageId: 42,
                    targetLanguage: mockLanguage
                });

                expect(dotEditContentService.getContentById).toHaveBeenCalledWith({
                    id: 'id1',
                    languageId: 42
                });
                expect(dotEditContentService.getContentById).toHaveBeenCalledWith({
                    id: 'id2',
                    languageId: 42
                });
            });

            it('should patch language to the full DotLanguage object on each resolved item', () => {
                const items = [createFakeContentlet({ inode: 'inode1', identifier: 'id1' })];
                store.setData(items);

                const fetched = createFakeContentlet({ inode: 'resolved', identifier: 'id1' });
                jest.spyOn(dotEditContentService, 'getContentById').mockReturnValue(of(fetched));
                jest.spyOn(relationshipFieldService, 'prepareField').mockReturnValue(
                    of({
                        data: [],
                        contentType: mockContentType,
                        columns: [],
                        selectionMode: 'multiple',
                        isNewEditorEnabled: false
                    } as never)
                );

                store.initialize({
                    field: mockField,
                    contentlet: null,
                    targetLanguageId: 42,
                    targetLanguage: mockLanguage
                });

                expect((store.data()[0] as { language: DotLanguage }).language).toEqual(
                    mockLanguage
                );
            });

            it('should fall back to the original item when getContentById fails', () => {
                const original = createFakeContentlet({ inode: 'inode1', identifier: 'id1' });
                store.setData([original]);

                jest.spyOn(dotEditContentService, 'getContentById').mockReturnValue(
                    throwError(() => new Error('Not found'))
                );
                jest.spyOn(relationshipFieldService, 'prepareField').mockReturnValue(
                    of({
                        data: [],
                        contentType: mockContentType,
                        columns: [],
                        selectionMode: 'multiple',
                        isNewEditorEnabled: false
                    } as never)
                );

                store.initialize({
                    field: mockField,
                    contentlet: null,
                    targetLanguageId: 42,
                    targetLanguage: mockLanguage
                });

                expect(store.data()[0].inode).toBe('inode1');
            });

            it('should use existingData (captured before reset) when contentlet is null', () => {
                const existingItems = [
                    createFakeContentlet({ inode: 'existing1', identifier: 'exist-id' })
                ];
                store.setData(existingItems);

                jest.spyOn(relationshipFieldService, 'prepareField').mockReturnValue(
                    of({
                        data: [],
                        contentType: mockContentType,
                        columns: [],
                        selectionMode: 'multiple',
                        isNewEditorEnabled: false
                    } as never)
                );

                store.initialize({
                    field: mockField,
                    contentlet: null,
                    targetLanguageId: 42,
                    targetLanguage: mockLanguage
                });

                expect(dotEditContentService.getContentById).toHaveBeenCalledWith({
                    id: 'exist-id',
                    languageId: 42
                });
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

    describe('Load more (US4)', () => {
        const many = (n: number) =>
            Array.from({ length: n }, (_, i) =>
                createFakeContentlet({
                    inode: `inode${i}`,
                    identifier: `identifier${i}`,
                    id: `${i}`
                })
            );

        it('starts by rendering the first page and no more', () => {
            store.setData(many(95));

            expect(store.visibleCount()).toBe(RELATED_PAGE_SIZE);
            expect(store.$visibleItems()).toHaveLength(RELATED_PAGE_SIZE);
        });

        it('renders everything and reports nothing remaining for a short list', () => {
            store.setData(many(12));

            expect(store.$visibleItems()).toHaveLength(12);
            expect(store.$remaining()).toBe(0);
        });

        it('reveals one page at a time and stops once everything is on screen', () => {
            store.setData(many(95));

            store.loadMore();
            expect(store.$visibleItems()).toHaveLength(80);
            expect(store.$remaining()).toBe(15);

            store.loadMore();
            expect(store.$visibleItems()).toHaveLength(95);
            expect(store.$remaining()).toBe(0);
        });

        /**
         * FR-023. The bug `DotKeyValueComponent` hit and documented: with the count derived from
         * the data, a single removal after revealing rows snapped the table back to 40.
         */
        it('keeps revealed rows revealed when an item is removed', () => {
            store.setData(many(95));
            store.loadMore();
            store.deleteItem('inode0');

            expect(store.visibleCount()).toBe(80);
        });

        it('keeps revealed rows revealed when the list is reordered', () => {
            store.setData(many(95));
            store.loadMore();
            store.reorderData(many(95).reverse());

            expect(store.visibleCount()).toBe(80);
        });

        /**
         * FR-022. Withholding is a rendering limit, never a change to the value: everything the
         * field holds is emitted regardless of how much of it is on screen.
         */
        it('emits every item, including the withheld ones', () => {
            store.setData(many(95));

            expect(store.data()).toHaveLength(95);
            expect(store.formattedRelationship().split(',')).toHaveLength(95);
        });

        it('does not lose withheld items when a visible row is removed', () => {
            store.setData(many(95));
            store.deleteItem('inode0');

            expect(store.data()).toHaveLength(94);
        });

        it('does not reorder withheld items as a side effect of a drag', () => {
            const items = many(95);
            store.setData(items);
            const reordered = [items[1], items[0], ...items.slice(2)];
            store.reorderData(reordered);

            expect(
                store
                    .data()
                    .map((i) => i.inode)
                    .slice(2)
            ).toEqual(items.slice(2).map((i) => i.inode));
        });
    });

    /**
     * US4 / T060 — the regression guard.
     *
     * Passes today and must never stop passing. A programmatic load that marks the form dirty makes
     * the unsaved-changes guard fire on content the editor never touched, and this state lives
     * right beside the paging state being deleted.
     */
    describe('lastChangeSource is untouched by the paging removal (US4)', () => {
        it('stays "user" for an explicit edit', () => {
            store.setData([createFakeContentlet({ inode: 'a', identifier: 'a' })]);
            expect(store.lastChangeSource()).toBe('user');
        });

        it('stays "user" for a removal and for a reorder', () => {
            store.setData([
                createFakeContentlet({ inode: 'a', identifier: 'a' }),
                createFakeContentlet({ inode: 'b', identifier: 'b' })
            ]);
            store.deleteItem('a');
            expect(store.lastChangeSource()).toBe('user');

            store.reorderData([createFakeContentlet({ inode: 'b', identifier: 'b' })]);
            expect(store.lastChangeSource()).toBe('user');
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
        }),
        mockProvider(DotEditContentService, {
            getContentById: jest.fn().mockReturnValue(of({}))
        }),
        // `withFlags` batch-fetches the side-panel flag on init.
        mockProvider(DotPropertiesService, {
            getFeatureFlags: jest
                .fn()
                .mockReturnValue(
                    of({ [FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]: false })
                )
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

    /**
     * US4 — the related list drops paging and reveals rows in pages of 40 instead.
     *
     * Mirrors `DotKeyValueComponent`, whose comments already record the trap: deriving the visible
     * count from the data collapses the table to the first page on every edit.
     */
});
