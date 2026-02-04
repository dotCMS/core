import { describe, expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';

// UVE_PALETTE_TABS removed - now managed locally in DotUvePaletteComponent
import { withEditor } from './withEditor';


import { DotPageApiParams, DotPageApiService } from '../../../services/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import {
    ACTION_MOCK,
    ACTION_PAYLOAD_MOCK,
    EMA_DRAG_ITEM_CONTENTLET_MOCK,
    getBoundsMock,
    MOCK_CONTENTLET_AREA,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL
} from '../../../shared/mocks';
import { ActionPayload } from '../../../shared/models';
import { getPersonalization, mapContainerStructureToArrayOfContainers } from '../../../utils';
import { Orientation, PageType, UVEState } from '../../models';
import { withFlags } from '../flags/withFlags';
import { withPageContext } from '../withPageContext';

const emptyParams = {} as DotPageApiParams;

const initialState: UVEState = {
    isEnterprise: true,
    languages: [],
    flags: {},
    // Normalized page response
    page: MOCK_RESPONSE_HEADLESS.page,
    site: MOCK_RESPONSE_HEADLESS.site,
    template: MOCK_RESPONSE_HEADLESS.template,
    containers: MOCK_RESPONSE_HEADLESS.containers,
    viewAs: MOCK_RESPONSE_HEADLESS.viewAs,
    vanityUrl: MOCK_RESPONSE_HEADLESS.vanityUrl,
    urlContentMap: MOCK_RESPONSE_HEADLESS.urlContentMap,
    numberContents: MOCK_RESPONSE_HEADLESS.numberContents,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: {
        ...emptyParams,
        url: 'test-url',
        language_id: '1',
        [PERSONA_KEY]: 'dot:persona',
        variantName: 'DEFAULT',
        clientHost: 'http://localhost:3000',
        mode: UVE_MODE.EDIT
    },
    status: UVE_STATUS.LOADED,
    pageType: PageType.HEADLESS,
    // Phase 3.2: Nested editor state
    editor: {
        dragItem: null,
        bounds: [],
        state: EDITOR_STATE.IDLE,
        activeContentlet: null,
        contentArea: null,
        panels: {
            palette: { open: true },
            rightSidebar: { open: false }
        },
        ogTags: null,
        styleSchemas: []
    },
    // Phase 3.2: Nested view state
    view: {
        device: null,
        orientation: Orientation.LANDSCAPE,
        socialMedia: null,
        viewParams: null,
        isEditState: true,
        isPreviewModeActive: false,
        ogTagsResults: null
    }
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withFlags([]),               // Provides flags state (empty array for tests)
    withPageContext(),           // Provides all PageContextComputed properties including $enableInlineEdit
    withEditor()
);

describe('withEditor', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of(false))
            }),
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of({});
                    },
                    getClientPage() {
                        return of({});
                    },
                    save: jest.fn()
                }
            },
            {
                provide: WINDOW,
                useValue: window
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        patchState(store, initialState);
    });

    // Toolbar tests removed - toolbar functionality should be tested in withToolbar.spec.ts

    describe('withComputed', () => {
        describe('$areaContentType', () => {
            it('should return empty string when contentArea is null', () => {
                patchState(store, {
                    editor: {
                        ...store.editor(),
                        contentArea: null
                    }
                });

                expect(store.$areaContentType()).toBe('');
            });

            it('should return the content type of the current contentArea', () => {
                patchState(store, {
                    editor: {
                        ...store.editor(),
                        contentArea: MOCK_CONTENTLET_AREA
                    }
                });

                expect(store.$areaContentType()).toBe(
                    MOCK_CONTENTLET_AREA.payload.contentlet.contentType
                );
            });
        });

        describe('$pageData', () => {
            it('should return the expected data', () => {
                expect(store.$pageData()).toEqual({
                    containers: mapContainerStructureToArrayOfContainers(
                        MOCK_RESPONSE_HEADLESS.containers
                    ),
                    id: MOCK_RESPONSE_HEADLESS.page.identifier,
                    personalization: getPersonalization(MOCK_RESPONSE_HEADLESS.viewAs.persona),
                    languageId: MOCK_RESPONSE_HEADLESS.viewAs.language.id,
                    personaTag: MOCK_RESPONSE_HEADLESS.viewAs.persona.keyTag
                });
            });
        });

        describe('$reloadEditorContent', () => {
            it('should return the expected data for Headless', () => {
                patchState(store, {
                    page: MOCK_RESPONSE_HEADLESS.page,
                    pageType: PageType.HEADLESS
                });

                expect(store.$reloadEditorContent()).toEqual({
                    code: MOCK_RESPONSE_HEADLESS.page.rendered,
                    pageType: PageType.HEADLESS,
                    enableInlineEdit: true
                });
            });
            it('should return the expected data for VTL', () => {
                patchState(store, {
                    page: MOCK_RESPONSE_VTL.page,
                    pageType: PageType.TRADITIONAL
                });
                expect(store.$reloadEditorContent()).toEqual({
                    code: MOCK_RESPONSE_VTL.page.rendered,
                    pageType: PageType.TRADITIONAL,
                    enableInlineEdit: true
                });
            });
        });

        // $editorProps tests removed (Phase 3): this computed was moved/removed during refactoring

        describe('$showContentletControls', () => {
            it('should return false when contentArea is null', () => {
                patchState(store, {
                    editor: {
                        ...store.editor(),
                        contentArea: null,
                        state: EDITOR_STATE.IDLE
                    }
                });

                expect(store.$showContentletControls()).toBe(false);
            });

            it('should return false when canEditPage is false', () => {
                patchState(store, {
                    page: {
                        ...store.page(),
                        canEdit: false  // Set canEdit to false to make $canEditPageContent false
                    },
                    editor: {
                        ...store.editor(),
                        contentArea: MOCK_CONTENTLET_AREA,
                        state: EDITOR_STATE.IDLE
                    }
                });

                expect(store.$showContentletControls()).toBe(false);
            });

            it('should return false when state is not IDLE', () => {
                patchState(store, {
                    editor: {
                        ...store.editor(),
                        contentArea: MOCK_CONTENTLET_AREA,
                        state: EDITOR_STATE.DRAGGING
                    }
                });

                expect(store.$showContentletControls()).toBe(false);
            });

            it('should return true when contentArea exists, canEditPage is true, and state is IDLE', () => {
                patchState(store, {
                    editor: {
                        ...store.editor(),
                        contentArea: MOCK_CONTENTLET_AREA,
                        state: EDITOR_STATE.IDLE
                    }
                });

                expect(store.$showContentletControls()).toBe(true);
            });

            it('should return false when scrolling', () => {
                patchState(store, {
                    editor: {
                        ...store.editor(),
                        contentArea: MOCK_CONTENTLET_AREA,
                        state: EDITOR_STATE.SCROLLING
                    }
                });

                expect(store.$showContentletControls()).toBe(false);
            });
        });

        describe('$styleSchema', () => {
            it('should return undefined when no activeContentlet', () => {
                patchState(store, {
                    editor: {
                        ...store.editor(),
                        activeContentlet: null,
                        styleSchemas: []
                    }
                });

                expect(store.$styleSchema()).toBeUndefined();
            });

            it('should return undefined when styleSchemas is empty', () => {
                patchState(store, {
                    editor: {
                        ...store.editor(),
                        activeContentlet: {
                            language_id: '1',
                            pageContainers: [],
                            pageId: '123',
                            container: {
                                identifier: 'test-container-id',
                                uuid: 'test-container-uuid',
                                acceptTypes: 'test',
                                maxContentlets: 1
                            },
                            contentlet: {
                                identifier: 'test-contentlet-id',
                                inode: 'test-inode',
                                title: 'Test Contentlet',
                                contentType: 'testType'
                            }
                        },
                        styleSchemas: []
                    }
                });

                expect(store.$styleSchema()).toBeUndefined();
            });

            it('should return matching schema when contentType matches', () => {
                const mockSchema = {
                    contentType: 'testContentType',
                    sections: []
                };

                patchState(store, {
                    editor: {
                        ...store.editor(),
                        activeContentlet: {
                            language_id: '1',
                            pageContainers: [],
                            pageId: '123',
                            container: {
                                identifier: 'test-container-id',
                                uuid: 'test-container-uuid',
                                acceptTypes: 'test',
                                maxContentlets: 1
                            },
                            contentlet: {
                                identifier: 'test-contentlet-id',
                                inode: 'test-inode',
                                title: 'Test Contentlet',
                                contentType: 'testContentType'
                            }
                        },
                        styleSchemas: [mockSchema]
                    }
                });

                expect(store.$styleSchema()).toEqual(mockSchema);
            });

            it('should return correct schema when multiple schemas exist', () => {
                const schema1 = { contentType: 'type1', sections: [] };
                const schema2 = { contentType: 'type2', sections: [] };
                const schema3 = { contentType: 'type3', sections: [] };

                patchState(store, {
                    editor: {
                        ...store.editor(),
                        activeContentlet: {
                            language_id: '1',
                            pageContainers: [],
                            pageId: '123',
                            container: {
                                identifier: 'test-container-id',
                                uuid: 'test-container-uuid',
                                acceptTypes: 'test',
                                maxContentlets: 1
                            },
                            contentlet: {
                                identifier: 'test-contentlet-id',
                                inode: 'test-inode',
                                title: 'Test Contentlet',
                                contentType: 'type2'
                            }
                        },
                        styleSchemas: [schema1, schema2, schema3]
                    }
                });

                expect(store.$styleSchema()).toEqual(schema2);
            });

            it('should return undefined when contentType does not match any schema', () => {
                const mockSchema = {
                    contentType: 'differentType',
                    sections: []
                };

                patchState(store, {
                    editor: {
                        ...store.editor(),
                        activeContentlet: {
                            language_id: '1',
                            pageContainers: [],
                            pageId: '123',
                            container: {
                                identifier: 'test-container-id',
                                uuid: 'test-container-uuid',
                                acceptTypes: 'test',
                                maxContentlets: 1
                            },
                            contentlet: {
                                identifier: 'test-contentlet-id',
                                inode: 'test-inode',
                                title: 'Test Contentlet',
                                contentType: 'testType'
                            }
                        },
                        styleSchemas: [mockSchema]
                    }

                });

                expect(store.$styleSchema()).toBeUndefined();
            });
        });

        describe('$editorIsInDraggingState', () => {
            it("should return the editor's dragging state", () => {
                expect(store.$editorIsInDraggingState()).toBe(false);
            });

            it("should return the editor's dragging state after a change", () => {
                // This will trigger a change in the dragging state
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                expect(store.$editorIsInDraggingState()).toBe(true);
            });
        });

        describe('$iframeURL', () => {
            it("should return the iframe's URL", () => {
                expect(store.$iframeURL()).toBe(
                    'http://localhost:3000/test-url?language_id=1&variantName=DEFAULT&mode=EDIT_MODE&personaId=dot%3Apersona&dotCMSHost=http://localhost'
                );
            });

            // There is an issue with Signal Store when you try to spy on a signal called from a computed property
            // Unskip this when this discussion is resolved: https://github.com/ngrx/platform/discussions/4627
            describe.skip('page dependency', () => {
                it('should call page when it is a headless page', () => {
                    const spy = jest.spyOn(store, 'page');
                    patchState(store, { pageType: PageType.HEADLESS });
                    store.$iframeURL();

                    expect(spy).toHaveBeenCalled();
                });

                it('should call page when it is a traditional page', () => {
                    const spy = jest.spyOn(store, 'page');

                    patchState(store, { pageType: PageType.TRADITIONAL });

                    store.$iframeURL();

                    expect(spy).toHaveBeenCalled();
                });
            });

            it('should be an instance of String in src when the page is traditional', () => {
                patchState(store, {
                    page: MOCK_RESPONSE_VTL.page,
                    pageType: PageType.TRADITIONAL
                });

                expect(store.$iframeURL()).toBeInstanceOf(String);
            });

            it('should be an empty string in src when the page is traditional', () => {
                patchState(store, {
                    page: MOCK_RESPONSE_VTL.page,
                    pageType: PageType.TRADITIONAL
                });

                expect(store.$iframeURL().toString()).toBe('');
            });

            it('should contain the right url when the page is a vanity url  ', () => {
                patchState(store, {
                    vanityUrl: {
                        ...MOCK_RESPONSE_HEADLESS.vanityUrl,
                        url: 'first'
                    },
                    pageParams: {
                        language_id: '1',
                        variantName: 'DEFAULT',
                        url: 'first',
                        [PERSONA_KEY]: 'dot:persona'
                    }
                });

                expect(store.$iframeURL()).toBe(
                    'http://localhost/first?language_id=1&variantName=DEFAULT&personaId=dot%3Apersona&dotCMSHost=http://localhost'
                );
            });

            it('should set the right iframe url when the clientHost is present', () => {
                patchState(store, {
                    pageParams: {
                        ...emptyParams,
                        url: 'test-url',
                        clientHost: 'http://localhost:3000'
                    }
                });

                expect(store.$iframeURL()).toBe(
                    'http://localhost:3000/test-url&dotCMSHost=http://localhost'
                );
            });

            it('should set the right iframe url when the clientHost is present with a aditional path', () => {
                patchState(store, {
                    pageParams: {
                        ...emptyParams,
                        url: 'test-url',
                        clientHost: 'http://localhost:3000/test'
                    }
                });

                expect(store.$iframeURL()).toBe(
                    'http://localhost:3000/test/test-url&dotCMSHost=http://localhost'
                );
            });
        });

        // $editorProps tests removed (Phase 3): this computed was moved/removed during refactoring
    });

    describe('withMethods', () => {
        describe('updateEditorScrollState', () => {
            it("should update the editor's scroll state and remove bounds when there is no drag item", () => {
                store.updateEditorScrollState();

                expect(store.editor().state).toEqual(EDITOR_STATE.SCROLLING);
                expect(store.editor().bounds).toEqual([]);
            });

            it("should update the editor's scroll drag state and remove bounds when there is drag item", () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                store.setEditorBounds(getBoundsMock(ACTION_MOCK));

                store.updateEditorScrollState();

                expect(store.editor().state).toEqual(EDITOR_STATE.SCROLL_DRAG);
                expect(store.editor().bounds).toEqual([]);
            });

            it('should set the contentArea to null when we are scrolling', () => {
                store.setEditorState(EDITOR_STATE.SCROLLING);

                store.updateEditorScrollState();

                expect(store.editor().contentArea).toBe(null);
            });
        });

        describe('setPaletteOpen', () => {
            it('should toggle the palette', () => {
                store.setPaletteOpen(true);

                expect(store.editor().panels.palette.open).toBe(true);
            });

            it('should toggle the palette', () => {
                store.setPaletteOpen(false);

                expect(store.editor().panels.palette.open).toBe(false);
            });
        });

        describe('updateEditorOnScrollEnd', () => {
            it("should update the editor's drag state when there is no drag item", () => {
                store.updateEditorOnScrollEnd();

                expect(store.editor().state).toEqual(EDITOR_STATE.IDLE);
            });

            it("should update the editor's drag state when there is drag item", () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                store.updateEditorOnScrollEnd();

                expect(store.editor().state).toEqual(EDITOR_STATE.DRAGGING);
            });
        });

        describe('updateEditorScrollDragState', () => {
            it('should update the store correctly', () => {
                store.updateEditorScrollDragState();

                expect(store.editor().state).toEqual(EDITOR_STATE.SCROLL_DRAG);
                expect(store.editor().bounds).toEqual([]);
            });
        });

        describe('setEditorState', () => {
            it('should update the state correctly', () => {
                store.setEditorState(EDITOR_STATE.SCROLLING);

                expect(store.editor().state).toEqual(EDITOR_STATE.SCROLLING);
            });
        });

        describe('setEditorDragItem', () => {
            it('should update the store correctly', () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                expect(store.editor().dragItem).toEqual(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                expect(store.editor().state).toEqual(EDITOR_STATE.DRAGGING);
            });
        });

        describe('setContentletArea', () => {
            it("should update the store's contentlet area", () => {
                store.setContentletArea(MOCK_CONTENTLET_AREA);

                expect(store.editor().contentArea).toEqual(MOCK_CONTENTLET_AREA);
                expect(store.editor().state).toEqual(EDITOR_STATE.IDLE);
            });

            it('should not update contentArea if it is the same', () => {
                store.setContentletArea(MOCK_CONTENTLET_AREA);

                // We can have contentArea and state at the same time we are inline editing
                store.setEditorState(EDITOR_STATE.INLINE_EDITING);

                store.setContentletArea(MOCK_CONTENTLET_AREA);

                expect(store.editor().contentArea).toEqual(MOCK_CONTENTLET_AREA);
                // State should not change
                expect(store.editor().state).toEqual(EDITOR_STATE.INLINE_EDITING);
            });
        });

        describe('setActiveContentlet', () => {
            it('should set the active contentlet', () => {
                const mockContentlet: ActionPayload = {
                    language_id: '1',
                    pageContainers: [],
                    pageId: '123',
                    container: {
                        identifier: 'test-container-id',
                        uuid: 'test-container-uuid',
                        acceptTypes: 'test',
                        maxContentlets: 1
                    },
                    contentlet: {
                        identifier: 'test-contentlet-id',
                        inode: 'test-inode',
                        title: 'Test Contentlet',
                        contentType: 'testType'
                    }
                };

                store.setActiveContentlet(mockContentlet);

                expect(store.editor.activeContentlet()).toEqual(mockContentlet);
            });

            it('should open palette and set current tab to STYLE_EDITOR', () => {
                const mockContentlet: ActionPayload = {
                    language_id: '1',
                    pageContainers: [],
                    pageId: '123',
                    container: {
                        identifier: 'test-container-id',
                        uuid: 'test-container-uuid',
                        acceptTypes: 'test',
                        maxContentlets: 1
                    },
                    contentlet: {
                        identifier: 'test-contentlet-id',
                        inode: 'test-inode',
                        title: 'Test Contentlet',
                        contentType: 'testType'
                    }
                };

                store.setActiveContentlet(mockContentlet);

                expect(store.editor().panels.palette).toEqual({
                    open: true
                    // currentTab removed - now managed locally in DotUvePaletteComponent
                });
            });

            it('should switch to STYLE_EDITOR tab even if palette was on different tab', () => {
                const mockContentlet: ActionPayload = {
                    language_id: '1',
                    pageContainers: [],
                    pageId: '123',
                    container: {
                        identifier: 'test-container-id',
                        uuid: 'test-container-uuid',
                        acceptTypes: 'test',
                        maxContentlets: 1
                    },
                    contentlet: {
                        identifier: 'test-contentlet-id',
                        inode: 'test-inode',
                        title: 'Test Contentlet',
                        contentType: 'testType'
                    }
                };

                store.setActiveContentlet(mockContentlet);

                expect(store.editor.activeContentlet()).toEqual(mockContentlet);
                expect(store.editor().panels.palette.open).toBe(true);
                // Tab switching to STYLE_EDITOR now handled by DotUvePaletteComponent via effect
            });
        });

        describe('setEditorBounds', () => {
            const bounds = getBoundsMock(ACTION_MOCK);

            it('should update the store correcly', () => {
                store.setEditorBounds(bounds);

                expect(store.editor().bounds).toEqual(bounds);
            });
        });

        describe('resetEditorProperties', () => {
            it('should reset the editor props correctly', () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                store.setEditorState(EDITOR_STATE.SCROLLING);
                store.setContentletArea(MOCK_CONTENTLET_AREA);
                store.setEditorBounds(getBoundsMock(ACTION_MOCK));

                store.resetEditorProperties();

                expect(store.editor().dragItem).toBe(null);
                expect(store.editor().state).toEqual(EDITOR_STATE.IDLE);
                expect(store.editor().contentArea).toBe(null);
                expect(store.editor().bounds).toEqual([]);
            });
        });
        describe('getPageSavePayload', () => {
            it("should return the page's save payload", () => {
                expect(store.getPageSavePayload(ACTION_PAYLOAD_MOCK)).toEqual({
                    container: {
                        acceptTypes: 'test',
                        contentletsId: [],
                        identifier: 'container-identifier-123',
                        maxContentlets: 1,
                        uuid: 'uuid-123'
                    },
                    contentlet: {
                        contentType: 'test',
                        identifier: 'contentlet-identifier-123',
                        inode: 'contentlet-inode-123',
                        onNumberOfPages: 1,
                        title: 'Hello World'
                    },
                    language_id: '1',
                    pageContainers: [
                        {
                            contentletsId: ['123', '456'],
                            identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
                            uuid: '123'
                        },
                        {
                            contentletsId: ['123'],
                            identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
                            uuid: '456'
                        },
                        {
                            contentletsId: ['123', '456'],
                            identifier: '/container/path',
                            uuid: '123'
                        },
                        {
                            contentletsId: ['123'],
                            identifier: '/container/path',
                            uuid: '456'
                        }
                    ],
                    pageId: '123',
                    personaTag: 'dot:persona',
                    position: 'after'
                });
            });
        });

        describe('getCurrentTreeNode', () => {
            it('should return the current TreeNode with variantId from store.$variantId()', () => {
                const { container, contentlet } = ACTION_PAYLOAD_MOCK;

                // When variantId is not set in pageParams, $variantId() returns DEFAULT_VARIANT_ID
                expect(store.getCurrentTreeNode(container, contentlet)).toEqual({
                    containerId: 'container-identifier-123',
                    contentId: 'contentlet-identifier-123',
                    pageId: '123',
                    personalization: 'dot:persona:dot:persona',
                    relationType: 'uuid-123',
                    treeOrder: '-1',
                    variantId: DEFAULT_VARIANT_ID
                });
            });

            it('should use variantId from store.$variantId() when variantId is set in pageParams', () => {
                const { container, contentlet } = ACTION_PAYLOAD_MOCK;
                const testVariantId = 'test-variant-id-123';

                // Set variantId in pageParams
                patchState(store, {
                    pageParams: {
                        ...store.pageParams(),
                        variantId: testVariantId
                    }
                });

                const result = store.getCurrentTreeNode(container, contentlet);

                expect(result.variantId).toBe(testVariantId);
                expect(result).toEqual({
                    containerId: 'container-identifier-123',
                    contentId: 'contentlet-identifier-123',
                    pageId: '123',
                    personalization: 'dot:persona:dot:persona',
                    relationType: 'uuid-123',
                    treeOrder: '-1',
                    variantId: testVariantId
                });
            });
        });

        describe('setOgTags', () => {
            it('should set the ogTags correctly', () => {
                const ogTags = {
                    title: 'Title',
                    description: 'Description',
                    image: 'Image',
                    type: 'Type',
                    url: 'URL'
                };

                store.setOgTags(ogTags);

                expect(store.editor().ogTags).toEqual(ogTags);
            });
        });
    });
});
