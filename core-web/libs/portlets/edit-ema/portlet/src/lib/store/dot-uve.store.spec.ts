import { describe, expect } from '@jest/globals';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute, ActivatedRouteSnapshot, ParamMap, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import {
    MockDotMessageService,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    getDraftExperimentMock,
    DotLanguagesServiceMock,
    CurrentUserDataMock,
    mockLanguageArray
} from '@dotcms/utils-testing';

import { UVEStore } from './dot-uve.store';

import { DotPageApiResponse, DotPageApiService } from '../services/dot-page-api.service';
import {
    ACTION_MOCK,
    ACTION_PAYLOAD_MOCK,
    DEFAULT_PERSONA,
    EMA_DRAG_ITEM_CONTENTLET_MOCK,
    getBoundsMock,
    getVanityUrl,
    MOCK_CONTENTLET_AREA,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL,
    PERMANENT_REDIRECT_VANITY_URL,
    TEMPORARY_REDIRECT_VANITY_URL
} from '../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../shared/enums';
import {
    getPersonalization,
    mapContainerStructureToArrayOfContainers,
    mapContainerStructureToDotContainerMap
} from '../utils';

const HEADLESS_BASE_QUERY_PARAMS = {
    url: 'test-url',
    language_id: '1',
    'com.dotmarketing.persona.id': DEFAULT_PERSONA.keyTag,
    variantName: DEFAULT_VARIANT_ID,
    clientHost: 'http://localhost:3000'
};

const VTL_BASE_QUERY_PARAMS = {
    url: 'test-url',
    language_id: '1',
    'com.dotmarketing.persona.id': DEFAULT_PERSONA.keyTag,
    variantName: DEFAULT_VARIANT_ID
};

const buildPageAPIResponseFromMock =
    (mock) =>
    ({ url }) =>
        of({
            ...mock,
            page: {
                ...mock.page,
                pageURI: url
            }
        });

describe('UVEStore', () => {
    let spectator: SpectatorService<InstanceType<typeof UVEStore>>;
    let store: InstanceType<typeof UVEStore>;
    let dotPageApiService: SpyObject<DotPageApiService>;
    let activatedRoute: SpyObject<ActivatedRoute>;
    let router: SpyObject<Router>;
    // let dotExperimentsService: DotExperimentsService;

    const createService = createServiceFactory({
        service: UVEStore,
        providers: [
            MessageService,
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of({});
                    },
                    save: jest.fn()
                }
            },
            {
                provide: DotLicenseService,
                useValue: {
                    isEnterprise: () => of(true)
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            {
                provide: DotExperimentsService,
                useValue: {
                    getById(experimentId: string) {
                        if (experimentId == 'i-have-a-running-experiment') {
                            return of(getRunningExperimentMock());
                        } else if (experimentId == 'i-have-a-scheduled-experiment') {
                            return of(getScheduleExperimentMock());
                        } else if (experimentId) return of(getDraftExperimentMock());

                        return of(undefined);
                    }
                }
            },
            {
                provide: LoginService,
                useValue: {
                    getCurrentUser: () => of(CurrentUserDataMock)
                }
            },
            {
                provide: DotLanguagesService,
                useValue: new DotLanguagesServiceMock()
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;

        dotPageApiService = spectator.inject(DotPageApiService);
        // dotExperimentsService = spectator.inject(DotExperimentsService);
        router = spectator.inject(Router);
        activatedRoute = spectator.inject(ActivatedRoute);
        jest.spyOn(dotPageApiService, 'get').mockImplementation(
            buildPageAPIResponseFromMock(MOCK_RESPONSE_HEADLESS)
        );

        store.load(HEADLESS_BASE_QUERY_PARAMS);
    });

    describe('withComputed', () => {
        describe('$shellProps', () => {
            it('should return the shell props for Headless Pages', () => {
                expect(store.$shellProps()).toEqual({
                    canRead: true,
                    error: null,
                    translateProps: {
                        page: MOCK_RESPONSE_HEADLESS.page,
                        languageId: 1,
                        languages: mockLanguageArray
                    },
                    seoParams: {
                        siteId: MOCK_RESPONSE_HEADLESS.site.identifier,
                        languageId: 1,
                        currentUrl: '/test-url',
                        requestHostName: 'http://localhost:3000'
                    },
                    uveErrorPageInfo: {
                        NOT_FOUND: {
                            icon: 'compass',
                            title: 'editema.infopage.notfound.title',
                            description: 'editema.infopage.notfound.description',
                            buttonPath: '/pages',
                            buttonText: 'editema.infopage.button.gotopages'
                        },
                        ACCESS_DENIED: {
                            icon: 'ban',
                            title: 'editema.infopage.accessdenied.title',
                            description: 'editema.infopage.accessdenied.description',
                            buttonPath: '/pages',
                            buttonText: 'editema.infopage.button.gotopages'
                        }
                    },
                    items: [
                        {
                            icon: 'pi-file',
                            label: 'editema.editor.navbar.content',
                            href: 'content',
                            id: 'content'
                        },
                        {
                            icon: 'pi-table',
                            label: 'editema.editor.navbar.layout',
                            href: 'layout',
                            id: 'layout',
                            isDisabled: false,
                            tooltip: null
                        },
                        {
                            icon: 'pi-sliders-h',
                            label: 'editema.editor.navbar.rules',
                            id: 'rules',
                            href: `rules/${MOCK_RESPONSE_HEADLESS.page.identifier}`,
                            isDisabled: false
                        },
                        {
                            iconURL: 'experiments',
                            label: 'editema.editor.navbar.experiments',
                            href: `experiments/${MOCK_RESPONSE_HEADLESS.page.identifier}`,
                            id: 'experiments',
                            isDisabled: false
                        },
                        {
                            icon: 'pi-th-large',
                            label: 'editema.editor.navbar.page-tools',
                            id: 'page-tools'
                        },
                        {
                            icon: 'pi-ellipsis-v',
                            label: 'editema.editor.navbar.properties',
                            id: 'properties'
                        }
                    ]
                });
            });

            it('should return the shell props for Legacy Pages', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(
                    buildPageAPIResponseFromMock(MOCK_RESPONSE_VTL)
                );

                store.load(VTL_BASE_QUERY_PARAMS);

                expect(store.$shellProps()).toEqual({
                    canRead: true,
                    error: null,
                    translateProps: {
                        page: MOCK_RESPONSE_VTL.page,
                        languageId: 1,
                        languages: mockLanguageArray
                    },
                    seoParams: {
                        siteId: MOCK_RESPONSE_VTL.site.identifier,
                        languageId: 1,
                        currentUrl: '/test-url',
                        requestHostName: 'http://localhost'
                    },
                    uveErrorPageInfo: {
                        NOT_FOUND: {
                            icon: 'compass',
                            title: 'editema.infopage.notfound.title',
                            description: 'editema.infopage.notfound.description',
                            buttonPath: '/pages',
                            buttonText: 'editema.infopage.button.gotopages'
                        },
                        ACCESS_DENIED: {
                            icon: 'ban',
                            title: 'editema.infopage.accessdenied.title',
                            description: 'editema.infopage.accessdenied.description',
                            buttonPath: '/pages',
                            buttonText: 'editema.infopage.button.gotopages'
                        }
                    },
                    items: [
                        {
                            icon: 'pi-file',
                            label: 'editema.editor.navbar.content',
                            href: 'content',
                            id: 'content'
                        },
                        {
                            icon: 'pi-table',
                            label: 'editema.editor.navbar.layout',
                            href: 'layout',
                            id: 'layout',
                            isDisabled: false,
                            tooltip: null
                        },
                        {
                            icon: 'pi-sliders-h',
                            label: 'editema.editor.navbar.rules',
                            id: 'rules',
                            href: `rules/${MOCK_RESPONSE_VTL.page.identifier}`,
                            isDisabled: false
                        },
                        {
                            iconURL: 'experiments',
                            label: 'editema.editor.navbar.experiments',
                            href: `experiments/${MOCK_RESPONSE_VTL.page.identifier}`,
                            id: 'experiments',
                            isDisabled: false
                        },
                        {
                            icon: 'pi-th-large',
                            label: 'editema.editor.navbar.page-tools',
                            id: 'page-tools'
                        },
                        {
                            icon: 'pi-ellipsis-v',
                            label: 'editema.editor.navbar.properties',
                            id: 'properties'
                        }
                    ]
                });
            });

            it('should return item for layout as disable', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(
                    buildPageAPIResponseFromMock({
                        ...MOCK_RESPONSE_VTL,
                        page: {
                            ...MOCK_RESPONSE_VTL.page,
                            canEdit: false
                        }
                    })
                );

                store.load(VTL_BASE_QUERY_PARAMS);

                const layoutItem = store.$shellProps().items.find((item) => item.id === 'layout');

                expect(layoutItem.isDisabled).toBe(true);
            });

            it('should return item for layout as disable and with a tooltip', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(
                    buildPageAPIResponseFromMock({
                        ...MOCK_RESPONSE_VTL,
                        template: {
                            ...MOCK_RESPONSE_VTL.template,
                            drawed: false
                        }
                    })
                );

                store.load(VTL_BASE_QUERY_PARAMS);

                const layoutItem = store.$shellProps().items.find((item) => item.id === 'layout');

                expect(layoutItem.isDisabled).toBe(true);
                expect(layoutItem.tooltip).toBe(
                    'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
                );
            });

            it('should return rules and experiments as disable when page cannot be edited', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(
                    buildPageAPIResponseFromMock({
                        ...MOCK_RESPONSE_VTL,
                        page: {
                            ...MOCK_RESPONSE_VTL.page,
                            canEdit: false
                        }
                    })
                );

                store.load(VTL_BASE_QUERY_PARAMS);

                const rules = store.$shellProps().items.find((item) => item.id === 'rules');
                const experiments = store
                    .$shellProps()
                    .items.find((item) => item.id === 'experiments');

                expect(rules.isDisabled).toBe(true);
                expect(experiments.isDisabled).toBe(true);
            });
        });
    });

    describe('withMethods', () => {
        describe('setUveStatus', () => {
            it('should set the status of the UVEStore', () => {
                expect(store.$status()).toBe(UVE_STATUS.LOADED);

                store.setUveStatus(UVE_STATUS.LOADING);

                expect(store.$status()).toBe(UVE_STATUS.LOADING);
            });
        });
    });

    describe('withLoad', () => {
        describe('withMethods', () => {
            it('should load the store with the base data', () => {
                expect(store.$pageAPIResponse()).toEqual(MOCK_RESPONSE_HEADLESS);
                expect(store.$isEnterprise()).toBe(true);
                expect(store.$currentUser()).toEqual(CurrentUserDataMock);
                expect(store.$experiment()).toBe(undefined);
                expect(store.$languages()).toBe(mockLanguageArray);
                expect(store.$params()).toEqual(HEADLESS_BASE_QUERY_PARAMS);
                expect(store.$canEditPage()).toBe(true);
                expect(store.$pageIsLocked()).toBe(false);
                expect(store.$status()).toBe(UVE_STATUS.LOADED);
                expect(store.$isTraditionalPage()).toBe(false);
            });

            it('should load the store with the base data for traditional page', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(
                    buildPageAPIResponseFromMock(MOCK_RESPONSE_VTL)
                );

                store.load(VTL_BASE_QUERY_PARAMS);

                expect(store.$pageAPIResponse()).toEqual(MOCK_RESPONSE_VTL);
                expect(store.$isEnterprise()).toBe(true);
                expect(store.$currentUser()).toEqual(CurrentUserDataMock);
                expect(store.$experiment()).toBe(undefined);
                expect(store.$languages()).toBe(mockLanguageArray);
                expect(store.$params()).toEqual(VTL_BASE_QUERY_PARAMS);
                expect(store.$canEditPage()).toBe(true);
                expect(store.$pageIsLocked()).toBe(false);
                expect(store.$status()).toBe(UVE_STATUS.LOADED);
                expect(store.$isTraditionalPage()).toBe(true);
            });

            it('should navigate when the page is a vanityUrl permanent redirect', () => {
                const permanentRedirect = getVanityUrl(
                    VTL_BASE_QUERY_PARAMS.url,
                    PERMANENT_REDIRECT_VANITY_URL
                ) as unknown as DotPageApiResponse;

                const forwardTo = PERMANENT_REDIRECT_VANITY_URL.forwardTo;

                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of(permanentRedirect)
                );

                store.load(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        ...VTL_BASE_QUERY_PARAMS,
                        url: forwardTo
                    },
                    queryParamsHandling: 'merge'
                });
            });

            it('should navigate when the page is a vanityUrl temporary redirect', () => {
                const temporaryRedirect = getVanityUrl(
                    VTL_BASE_QUERY_PARAMS.url,
                    TEMPORARY_REDIRECT_VANITY_URL
                ) as unknown as DotPageApiResponse;

                const forwardTo = TEMPORARY_REDIRECT_VANITY_URL.forwardTo;

                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of(temporaryRedirect)
                );

                store.load(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        ...VTL_BASE_QUERY_PARAMS,
                        url: forwardTo
                    },
                    queryParamsHandling: 'merge'
                });
            });

            it('should navigate to content when the layout is disable by page.canEdit and current route is layout', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of({
                        ...MOCK_RESPONSE_VTL,
                        page: {
                            ...MOCK_RESPONSE_VTL.page,
                            canEdit: false
                        }
                    })
                );

                jest.spyOn(activatedRoute, 'firstChild', 'get').mockReturnValue({
                    snapshot: {
                        url: [
                            {
                                path: 'layout',
                                parameters: {},
                                parameterMap: {} as unknown as ParamMap
                            }
                        ]
                    } as unknown as ActivatedRouteSnapshot
                } as unknown as ActivatedRoute);

                store.load(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
                    queryParamsHandling: 'merge'
                });
            });

            it('should navigate to content when the layout is disable by template.drawed and current route is layout', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of({
                        ...MOCK_RESPONSE_VTL,
                        template: {
                            ...MOCK_RESPONSE_VTL.template,
                            drawed: false
                        }
                    })
                );

                jest.spyOn(activatedRoute, 'firstChild', 'get').mockReturnValue({
                    snapshot: {
                        url: [
                            {
                                path: 'layout',
                                parameters: {},
                                parameterMap: {} as unknown as ParamMap
                            }
                        ]
                    } as unknown as ActivatedRouteSnapshot
                } as unknown as ActivatedRoute);

                store.load(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).toHaveBeenCalledWith(['edit-page/content'], {
                    queryParamsHandling: 'merge'
                });
            });

            it('should not navigate to content when the layout is disable by template.drawed and current route is not layout', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of({
                        ...MOCK_RESPONSE_VTL,
                        template: {
                            ...MOCK_RESPONSE_VTL.template,
                            drawed: false
                        }
                    })
                );

                jest.spyOn(activatedRoute, 'firstChild', 'get').mockReturnValue({
                    snapshot: {
                        url: [
                            {
                                path: 'rules',
                                parameters: {},
                                parameterMap: {} as unknown as ParamMap
                            }
                        ]
                    } as unknown as ActivatedRouteSnapshot
                } as unknown as ActivatedRoute);

                store.load(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).not.toHaveBeenCalled();
            });

            it('should not navigate to content when the layout is disable by page.canEdit and current route is not layout', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of({
                        ...MOCK_RESPONSE_VTL,
                        page: {
                            ...MOCK_RESPONSE_VTL.page,
                            canEdit: false
                        }
                    })
                );

                jest.spyOn(activatedRoute, 'firstChild', 'get').mockReturnValue({
                    snapshot: {
                        url: [
                            {
                                path: 'rules',
                                parameters: {},
                                parameterMap: {} as unknown as ParamMap
                            }
                        ]
                    } as unknown as ActivatedRouteSnapshot
                } as unknown as ActivatedRoute);

                store.load(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).not.toHaveBeenCalled();
            });

            it('should reload the store with the same queryParams', () => {
                const getPageSpy = jest.spyOn(dotPageApiService, 'get');

                store.reload();

                expect(getPageSpy).toHaveBeenCalledWith(store.$params());
            });
        });
    });

    describe('withLayout', () => {
        describe('withComputed', () => {
            describe('$layoutProps', () => {
                it('should return the layout props', () => {
                    expect(store.$layoutProps()).toEqual({
                        containersMap: mapContainerStructureToDotContainerMap(
                            MOCK_RESPONSE_HEADLESS.containers
                        ),
                        layout: MOCK_RESPONSE_HEADLESS.layout,
                        template: {
                            identifier: MOCK_RESPONSE_HEADLESS.template.identifier,
                            themeId: MOCK_RESPONSE_HEADLESS.template.theme
                        },
                        pageId: MOCK_RESPONSE_HEADLESS.page.identifier
                    });
                });
            });
        });

        describe('withMethods', () => {
            it('should update the layout', () => {
                const layout = {
                    ...MOCK_RESPONSE_HEADLESS.layout,
                    title: 'New layout'
                };

                store.updateLayout(layout);

                expect(store.$pageAPIResponse().layout).toEqual(layout);
            });
        });
    });

    describe('withEditor', () => {
        describe('withEditorToolbar', () => {
            describe('withComputed', () => {
                describe('$toolbarProps', () => {
                    //
                });

                describe('$infoDisplayOptions', () => {
                    //
                });
            });

            describe('withMethods', () => {
                it('should set the device with setDevice', () => {
                    const device = {
                        identifier: '123',
                        cssHeight: '120',
                        cssWidth: '120',
                        name: 'square',
                        inode: '1234',
                        icon: 'icon'
                    };

                    store.setDevice(device);

                    expect(store.$device()).toEqual(device);
                    expect(store.$isEditState()).toBe(false);
                });

                it('should set the socialMedia with setSocialMedia', () => {
                    const socialMedia = 'facebook';

                    store.setSocialMedia(socialMedia);

                    expect(store.$socialMedia()).toEqual(socialMedia);
                    expect(store.$isEditState()).toBe(false);
                });

                it('should reset the state with clearDeviceAndSocialMedia', () => {
                    store.clearDeviceAndSocialMedia();

                    expect(store.$device()).toBe(null);
                    expect(store.$socialMedia()).toBe(null);
                    expect(store.$isEditState()).toBe(true);
                });
            });
        });

        describe('withSave', () => {
            describe('withMethods', () => {
                describe('savePage', () => {
                    //
                });
            });
        });

        describe('withComputed', () => {
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
                    expect(store.$reloadEditorContent()).toEqual({
                        code: MOCK_RESPONSE_HEADLESS.page.rendered,
                        isTraditionalPage: false,
                        isEditState: true,
                        isEnterprise: true
                    });
                });
                it('should return the expected data for VTL', () => {
                    jest.spyOn(dotPageApiService, 'get').mockImplementation(
                        buildPageAPIResponseFromMock(MOCK_RESPONSE_VTL)
                    );

                    store.load(VTL_BASE_QUERY_PARAMS);

                    expect(store.$reloadEditorContent()).toEqual({
                        code: MOCK_RESPONSE_VTL.page.rendered,
                        isTraditionalPage: true,
                        isEditState: true,
                        isEnterprise: true
                    });
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

            describe('$editorProps', () => {
                //
            });
        });

        describe('withMethods', () => {
            describe('updateEditorScrollState', () => {
                it("should update the editor's scroll state when there is no drag item", () => {
                    store.updateEditorScrollState();

                    expect(store.$state()).toEqual(EDITOR_STATE.SCROLLING);
                });

                it("should update the editor's scroll state when there is drag item", () => {
                    store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                    store.updateEditorScrollState();

                    expect(store.$state()).toEqual(EDITOR_STATE.SCROLL_DRAG);
                });

                it("should not update the editor's scroll state when the state is OUT_OF_BOUNDS", () => {
                    store.setEditorState(EDITOR_STATE.OUT_OF_BOUNDS);

                    store.updateEditorScrollState();

                    expect(store.$state()).toEqual(EDITOR_STATE.OUT_OF_BOUNDS);
                });
            });

            describe('updateEditorOnScrollEnd', () => {
                it("should update the editor's drag state when there is no drag item", () => {
                    store.updateEditorOnScrollEnd();

                    expect(store.$state()).toEqual(EDITOR_STATE.IDLE);
                });

                it("should update the editor's drag state when there is drag item", () => {
                    store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                    store.updateEditorOnScrollEnd();

                    expect(store.$state()).toEqual(EDITOR_STATE.DRAGGING);
                });

                it("should not update the editor's drag state when the state is OUT_OF_BOUNDS", () => {
                    store.setEditorState(EDITOR_STATE.OUT_OF_BOUNDS);

                    store.updateEditorOnScrollEnd();

                    expect(store.$state()).toEqual(EDITOR_STATE.OUT_OF_BOUNDS);
                });
            });

            describe('updateEditorScrollDragState', () => {
                it('should update the store correctly', () => {
                    store.updateEditorScrollDragState();

                    expect(store.$state()).toEqual(EDITOR_STATE.SCROLL_DRAG);
                    expect(store.$bounds()).toEqual([]);
                });
            });

            describe('setEditorState', () => {
                it('should update the state correctly', () => {
                    store.setEditorState(EDITOR_STATE.SCROLLING);

                    expect(store.$state()).toEqual(EDITOR_STATE.SCROLLING);
                });
            });

            describe('setEditorDragItem', () => {
                it('should update the store correctly', () => {
                    store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);

                    expect(store.$dragItem()).toEqual(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                    expect(store.$state()).toEqual(EDITOR_STATE.DRAGGING);
                });
            });

            describe('setEditorContentletArea', () => {
                it("should update the store's contentlet area", () => {
                    store.setEditorContentletArea(MOCK_CONTENTLET_AREA);

                    expect(store.$contentletArea()).toEqual(MOCK_CONTENTLET_AREA);
                    expect(store.$state()).toEqual(EDITOR_STATE.IDLE);
                });

                it('should not update contentletArea if it is the same', () => {
                    store.setEditorContentletArea(MOCK_CONTENTLET_AREA);

                    // We can have contentletArea and state at the same time we are inline editing
                    store.setEditorState(EDITOR_STATE.INLINE_EDITING);

                    store.setEditorContentletArea(MOCK_CONTENTLET_AREA);

                    expect(store.$contentletArea()).toEqual(MOCK_CONTENTLET_AREA);
                    // State should not change
                    expect(store.$state()).toEqual(EDITOR_STATE.INLINE_EDITING);
                });
            });

            describe('setEditorBounds', () => {
                const bounds = getBoundsMock(ACTION_MOCK);

                it('should update the store correcly', () => {
                    store.setEditorBounds(bounds);

                    expect(store.$bounds()).toEqual(bounds);
                });
            });

            describe('resetEditorProperties', () => {
                it('should reset the editor props corretcly', () => {
                    store.setEditorDragItem(EMA_DRAG_ITEM_CONTENTLET_MOCK);
                    store.setEditorState(EDITOR_STATE.SCROLLING);
                    store.setEditorContentletArea(MOCK_CONTENTLET_AREA);
                    store.setEditorBounds(getBoundsMock(ACTION_MOCK));

                    store.resetEditorProperties();

                    expect(store.$dragItem()).toBe(null);
                    expect(store.$state()).toEqual(EDITOR_STATE.IDLE);
                    expect(store.$contentletArea()).toBe(null);
                    expect(store.$bounds()).toEqual([]);
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
                            uuid: 'uuid-123',
                            variantId: '123'
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
                it('should return the current TreeNode', () => {
                    const { container, contentlet } = ACTION_PAYLOAD_MOCK;

                    expect(store.getCurrentTreeNode(container, contentlet)).toEqual({
                        containerId: 'container-identifier-123',
                        contentId: 'contentlet-identifier-123',
                        pageId: '123',
                        personalization: 'dot:persona:dot:persona',
                        relationType: 'uuid-123',
                        treeOrder: '-1',
                        variantId: '123'
                    });
                });
            });
        });
    });
});
