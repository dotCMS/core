import { describe, expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withComputed, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { computed, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { DotDeviceListItem } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';
import { mockDotDevices, seoOGTagsMock } from '@dotcms/utils-testing';

import { UVE_PALETTE_TABS } from './models';
import { withEditor } from './withEditor';

import { DotPageApiParams, DotPageApiService } from '../../../services/dot-page-api.service';
import { BASE_IFRAME_MEASURE_UNIT, PERSONA_KEY } from '../../../shared/consts';
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
import { UVEState } from '../../models';
import { withPageContext } from '../withPageContext';

const emptyParams = {} as DotPageApiParams;

const initialState: UVEState = {
    isEnterprise: true,
    languages: [],
    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
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
    isTraditionalPage: false,
    isClientReady: false,
    viewParams: {
        orientation: undefined,
        seo: undefined,
        device: undefined
    }
};

const mockCanEditPage = signal(true);

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withPageContext(),
    withComputed(() => {
        return {
            $canEditPage: computed(() => mockCanEditPage())
        };
    }),
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
        mockCanEditPage.set(true);
    });

    describe('withUVEToolbar', () => {
        describe('withComputed', () => {
            describe('$toolbarProps', () => {
                it('should return the base info', () => {
                    expect(store.$uveToolbar()).toEqual({
                        editor: {
                            apiUrl: '/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=dot%3Apersona&variantName=DEFAULT&mode=EDIT_MODE',
                            bookmarksUrl: '/test-url?host_id=123-xyz-567-xxl&language_id=1'
                        },
                        preview: null,
                        currentLanguage: MOCK_RESPONSE_HEADLESS.viewAs.language,
                        urlContentMap: null,
                        runningExperiment: null,
                        unlockButton: null
                    });
                });
            });
        });
    });

    describe('withComputed', () => {
        describe('$areaContentType', () => {
            it('should return empty string when contentArea is null', () => {
                patchState(store, {
                    contentArea: null
                });

                expect(store.$areaContentType()).toBe('');
            });

            it('should return the content type of the current contentArea', () => {
                patchState(store, {
                    contentArea: MOCK_CONTENTLET_AREA
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
                    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
                    isTraditionalPage: false
                });

                expect(store.$reloadEditorContent()).toEqual({
                    code: MOCK_RESPONSE_HEADLESS.page.rendered,
                    isTraditionalPage: false,
                    enableInlineEdit: true
                });
            });
            it('should return the expected data for VTL', () => {
                patchState(store, {
                    pageAPIResponse: MOCK_RESPONSE_VTL,
                    isTraditionalPage: true
                });
                expect(store.$reloadEditorContent()).toEqual({
                    code: MOCK_RESPONSE_VTL.page.rendered,
                    isTraditionalPage: true,
                    enableInlineEdit: true
                });
            });
        });

        describe('$showContentletControls', () => {
            it('should return false when contentArea is null', () => {
                patchState(store, {
                    contentArea: null,
                    state: EDITOR_STATE.IDLE
                });

                expect(store.$showContentletControls()).toBe(false);
            });

            it('should return false when canEditPage is false', () => {
                mockCanEditPage.set(false);
                patchState(store, {
                    contentArea: MOCK_CONTENTLET_AREA,
                    state: EDITOR_STATE.IDLE
                });

                expect(store.$showContentletControls()).toBe(false);
            });

            it('should return false when state is not IDLE', () => {
                mockCanEditPage.set(true);
                patchState(store, {
                    contentArea: MOCK_CONTENTLET_AREA,
                    state: EDITOR_STATE.DRAGGING
                });

                expect(store.$showContentletControls()).toBe(false);
            });

            it('should return true when contentArea exists, canEditPage is true, and state is IDLE', () => {
                mockCanEditPage.set(true);
                patchState(store, {
                    contentArea: MOCK_CONTENTLET_AREA,
                    state: EDITOR_STATE.IDLE
                });

                expect(store.$showContentletControls()).toBe(true);
            });

            it('should return false when scrolling', () => {
                mockCanEditPage.set(true);
                patchState(store, {
                    contentArea: MOCK_CONTENTLET_AREA,
                    state: EDITOR_STATE.SCROLLING
                });

                expect(store.$showContentletControls()).toBe(false);
            });
        });

        describe('$styleSchema', () => {
            it('should return undefined when no activeContentlet', () => {
                patchState(store, {
                    activeContentlet: null,
                    styleSchemas: []
                });

                expect(store.$styleSchema()).toBeUndefined();
            });

            it('should return undefined when styleSchemas is empty', () => {
                patchState(store, {
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
                });

                expect(store.$styleSchema()).toBeUndefined();
            });

            it('should return matching schema when contentType matches', () => {
                const mockSchema = {
                    contentType: 'testContentType',
                    sections: []
                };

                patchState(store, {
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
                });

                expect(store.$styleSchema()).toEqual(mockSchema);
            });

            it('should return correct schema when multiple schemas exist', () => {
                const schema1 = { contentType: 'type1', sections: [] };
                const schema2 = { contentType: 'type2', sections: [] };
                const schema3 = { contentType: 'type3', sections: [] };

                patchState(store, {
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
                });

                expect(store.$styleSchema()).toEqual(schema2);
            });

            it('should return undefined when contentType does not match any schema', () => {
                const mockSchema = {
                    contentType: 'differentType',
                    sections: []
                };

                patchState(store, {
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
            describe.skip('pageAPIResponse dependency', () => {
                it('should call pageAPIResponse when it is a headless page', () => {
                    const spy = jest.spyOn(store, 'pageAPIResponse');
                    patchState(store, { isTraditionalPage: false });
                    store.$iframeURL();

                    expect(spy).toHaveBeenCalled();
                });

                it('should call pageAPIResponse when it is a traditional page', () => {
                    const spy = jest.spyOn(store, 'pageAPIResponse');

                    patchState(store, { isTraditionalPage: true });

                    store.$iframeURL();

                    expect(spy).toHaveBeenCalled();
                });
            });

            it('should be an instance of String in src when the page is traditional', () => {
                patchState(store, {
                    pageAPIResponse: MOCK_RESPONSE_VTL,
                    isTraditionalPage: true
                });

                expect(store.$iframeURL()).toBeInstanceOf(String);
            });

            it('should be an empty string in src when the page is traditional', () => {
                patchState(store, {
                    pageAPIResponse: MOCK_RESPONSE_VTL,
                    isTraditionalPage: true
                });

                expect(store.$iframeURL().toString()).toBe('');
            });

            it('should contain the right url when the page is a vanity url  ', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        vanityUrl: {
                            ...MOCK_RESPONSE_HEADLESS.vanityUrl,
                            url: 'first'
                        }
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
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS
                    },
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
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS
                    },
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

        describe('$editorProps', () => {
            it('should return the expected data on init', () => {
                expect(store.$editorProps()).toEqual({
                    showDialogs: true,
                    showBlockEditorSidebar: true,
                    iframe: {
                        opacity: '0.5',
                        pointerEvents: 'auto',
                        wrapper: {
                            width: '100%',
                            height: '100%'
                        }
                    },
                    progressBar: true,
                    dropzone: null,
                    seoResults: null
                });
            });

            it('should set iframe opacity to 1 when client is Ready', () => {
                store.setIsClientReady(true);

                expect(store.$editorProps().iframe.opacity).toBe('1');
            });

            it('should not have opacity or progressBar in preview mode', () => {
                patchState(store, {
                    pageParams: { ...emptyParams, mode: UVE_MODE.PREVIEW }
                });

                expect(store.$editorProps().iframe.opacity).toBe('1');
                expect(store.$editorProps().progressBar).toBe(false);
            });

            describe('showDialogs', () => {
                it('should have the value of false when we cannot edit the page', () => {
                    mockCanEditPage.set(false);

                    expect(store.$editorProps().showDialogs).toBe(false);
                });

                it('should have the value of false when we are not on edit state', () => {
                    patchState(store, { isEditState: false });

                    expect(store.$editorProps().showDialogs).toBe(false);
                });
            });

            describe('editorContentStyles', () => {
                it('should have display block when there is not social media', () => {
                    expect(store.$editorContentStyles()).toEqual({
                        display: 'block'
                    });
                });

                it('should have display none when there is social media', () => {
                    patchState(store, { socialMedia: 'facebook' });

                    expect(store.$editorContentStyles()).toEqual({
                        display: 'none'
                    });
                });
            });

            describe('iframe', () => {
                it('should have an opacity of 0.5 when loading', () => {
                    patchState(store, { status: UVE_STATUS.LOADING });

                    expect(store.$editorProps().iframe.opacity).toBe('0.5');
                });

                it('should have pointerEvents as none when dragging', () => {
                    patchState(store, { state: EDITOR_STATE.DRAGGING });

                    expect(store.$editorProps().iframe.pointerEvents).toBe('none');
                });

                it('should have pointerEvents as none when scroll-drag', () => {
                    patchState(store, { state: EDITOR_STATE.SCROLL_DRAG });

                    expect(store.$editorProps().iframe.pointerEvents).toBe('none');
                });

                it('should have a wrapper when a device is present', () => {
                    const device = mockDotDevices[0] as DotDeviceListItem;

                    patchState(store, { device });

                    expect(store.$editorProps().iframe.wrapper).toEqual({
                        width: device.cssWidth + BASE_IFRAME_MEASURE_UNIT,
                        height: device.cssHeight + BASE_IFRAME_MEASURE_UNIT
                    });
                });
            });

            describe('progressBar', () => {
                it('should have progressBar as true when the status is loading', () => {
                    patchState(store, { status: UVE_STATUS.LOADING });

                    expect(store.$editorProps().progressBar).toBe(true);
                });

                it('should have progressBar as true when the status is loaded but client is not ready', () => {
                    patchState(store, { status: UVE_STATUS.LOADED, isClientReady: false });

                    expect(store.$editorProps().progressBar).toBe(true);
                });

                it('should have progressBar as false when the status is loaded and client is ready', () => {
                    patchState(store, { status: UVE_STATUS.LOADED, isClientReady: true });

                    expect(store.$editorProps().progressBar).toBe(false);
                });
            });

            describe('dropzone', () => {
                const bounds = getBoundsMock(ACTION_MOCK);

                it('should have dropzone when the state is dragging and the page can be edited', () => {
                    mockCanEditPage.set(true);
                    patchState(store, {
                        state: EDITOR_STATE.DRAGGING,
                        dragItem: EMA_DRAG_ITEM_CONTENTLET_MOCK,
                        bounds
                    });

                    expect(store.$editorProps().dropzone).toEqual({
                        dragItem: EMA_DRAG_ITEM_CONTENTLET_MOCK,
                        bounds
                    });
                });

                it("should not have dropzone when the page can't be edited", () => {
                    mockCanEditPage.set(false);
                    patchState(store, {
                        state: EDITOR_STATE.DRAGGING,
                        dragItem: EMA_DRAG_ITEM_CONTENTLET_MOCK,
                        bounds
                    });

                    expect(store.$editorProps().dropzone).toBe(null);
                });
            });

            describe('seoResults', () => {
                it('should have the expected data when ogTags and socialMedia is present', () => {
                    patchState(store, {
                        ogTags: seoOGTagsMock,
                        socialMedia: 'facebook'
                    });

                    expect(store.$editorProps().seoResults).toEqual({
                        ogTags: seoOGTagsMock,
                        socialMedia: 'facebook'
                    });
                });

                it('should be null when ogTags is not present', () => {
                    patchState(store, {
                        socialMedia: 'facebook'
                    });

                    expect(store.$editorProps().seoResults).toBe(null);
                });

                it('should be null when socialMedia is not present', () => {
                    patchState(store, {
                        ogTags: seoOGTagsMock
                    });

                    expect(store.$editorProps().seoResults).toBe(null);
                });
            });
        });
    });

    describe('withMethods', () => {
        describe('updateEditorScrollState', () => {
            it("should update the editor's scroll state and remove bounds when there is no drag item", () => {
                store.updateEditorScrollState();

                expect(store.state()).toEqual(EDITOR_STATE.SCROLLING);
                expect(store.bounds()).toEqual([]);
            });

            it("should update the editor's scroll drag state and remove bounds when there is drag item", () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                store.setEditorBounds(getBoundsMock(ACTION_MOCK));

                store.updateEditorScrollState();

                expect(store.state()).toEqual(EDITOR_STATE.SCROLL_DRAG);
                expect(store.bounds()).toEqual([]);
            });

            it('should set the contentArea to null when we are scrolling', () => {
                store.setEditorState(EDITOR_STATE.SCROLLING);

                store.updateEditorScrollState();

                expect(store.contentArea()).toBe(null);
            });
        });

        describe('setPaletteOpen', () => {
            it('should toggle the palette', () => {
                store.setPaletteOpen(true);

                expect(store.palette().open).toBe(true);
            });

            it('should toggle the palette', () => {
                store.setPaletteOpen(false);

                expect(store.palette().open).toBe(false);
            });
        });

        describe('updateEditorOnScrollEnd', () => {
            it("should update the editor's drag state when there is no drag item", () => {
                store.updateEditorOnScrollEnd();

                expect(store.state()).toEqual(EDITOR_STATE.IDLE);
            });

            it("should update the editor's drag state when there is drag item", () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                store.updateEditorOnScrollEnd();

                expect(store.state()).toEqual(EDITOR_STATE.DRAGGING);
            });
        });

        describe('updateEditorScrollDragState', () => {
            it('should update the store correctly', () => {
                store.updateEditorScrollDragState();

                expect(store.state()).toEqual(EDITOR_STATE.SCROLL_DRAG);
                expect(store.bounds()).toEqual([]);
            });
        });

        describe('setEditorState', () => {
            it('should update the state correctly', () => {
                store.setEditorState(EDITOR_STATE.SCROLLING);

                expect(store.state()).toEqual(EDITOR_STATE.SCROLLING);
            });
        });

        describe('setEditorDragItem', () => {
            it('should update the store correctly', () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                expect(store.dragItem()).toEqual(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                expect(store.state()).toEqual(EDITOR_STATE.DRAGGING);
            });
        });

        describe('setContentletArea', () => {
            it("should update the store's contentlet area", () => {
                store.setContentletArea(MOCK_CONTENTLET_AREA);

                expect(store.contentArea()).toEqual(MOCK_CONTENTLET_AREA);
                expect(store.state()).toEqual(EDITOR_STATE.IDLE);
            });

            it('should not update contentArea if it is the same', () => {
                store.setContentletArea(MOCK_CONTENTLET_AREA);

                // We can have contentArea and state at the same time we are inline editing
                store.setEditorState(EDITOR_STATE.INLINE_EDITING);

                store.setContentletArea(MOCK_CONTENTLET_AREA);

                expect(store.contentArea()).toEqual(MOCK_CONTENTLET_AREA);
                // State should not change
                expect(store.state()).toEqual(EDITOR_STATE.INLINE_EDITING);
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

                expect(store.activeContentlet()).toEqual(mockContentlet);
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

                expect(store.palette()).toEqual({
                    open: true,
                    currentTab: UVE_PALETTE_TABS.STYLE_EDITOR
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

                // Set palette to a different tab first
                store.setPaletteTab(UVE_PALETTE_TABS.CONTENT_TYPES);
                expect(store.palette().currentTab).toBe(UVE_PALETTE_TABS.CONTENT_TYPES);

                // Now set active contentlet
                store.setActiveContentlet(mockContentlet);

                // Should switch to STYLE_EDITOR
                expect(store.palette().currentTab).toBe(UVE_PALETTE_TABS.STYLE_EDITOR);
                expect(store.palette().open).toBe(true);
            });
        });

        describe('setEditorBounds', () => {
            const bounds = getBoundsMock(ACTION_MOCK);

            it('should update the store correcly', () => {
                store.setEditorBounds(bounds);

                expect(store.bounds()).toEqual(bounds);
            });
        });

        describe('resetEditorProperties', () => {
            it('should reset the editor props correctly', () => {
                store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                store.setEditorState(EDITOR_STATE.SCROLLING);
                store.setContentletArea(MOCK_CONTENTLET_AREA);
                store.setEditorBounds(getBoundsMock(ACTION_MOCK));

                store.resetEditorProperties();

                expect(store.dragItem()).toBe(null);
                expect(store.state()).toEqual(EDITOR_STATE.IDLE);
                expect(store.contentArea()).toBe(null);
                expect(store.bounds()).toEqual([]);
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

                // When variantId is not set in pageParams, $variantId() returns empty string
                expect(store.getCurrentTreeNode(container, contentlet)).toEqual({
                    containerId: 'container-identifier-123',
                    contentId: 'contentlet-identifier-123',
                    pageId: '123',
                    personalization: 'dot:persona:dot:persona',
                    relationType: 'uuid-123',
                    treeOrder: '-1',
                    variantId: '' // Uses store.$variantId() which comes from pageParams()?.variantId ?? ''
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

                expect(store.ogTags()).toEqual(ogTags);
            });
        });
    });
});
