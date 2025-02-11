import { describe, expect } from '@jest/globals';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPropertiesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    MockDotMessageService,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    getDraftExperimentMock,
    DotLanguagesServiceMock,
    CurrentUserDataMock,
    mockLanguageArray
} from '@dotcms/utils-testing';
import { UVE_MODE } from '@dotcms/uve/types';

import { UVEStore } from './dot-uve.store';
import { Orientation } from './models';

import { DotPageApiService } from '../services/dot-page-api.service';
import { COMMON_ERRORS, PERSONA_KEY } from '../shared/consts';
import { UVE_STATUS } from '../shared/enums';
import {
    BASE_SHELL_ITEMS,
    BASE_SHELL_PROPS_RESPONSE,
    dotPropertiesServiceMock,
    HEADLESS_BASE_QUERY_PARAMS,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL,
    VTL_BASE_QUERY_PARAMS
} from '../shared/mocks';
import { normalizeQueryParams } from '../utils';

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

    const createService = createServiceFactory({
        service: UVEStore,
        providers: [
            MessageService,
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of({})
                }
            },
            {
                provide: DotPropertiesService,
                useValue: dotPropertiesServiceMock
            },
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
        jest.spyOn(dotPageApiService, 'get').mockImplementation(
            buildPageAPIResponseFromMock(MOCK_RESPONSE_HEADLESS)
        );
    });

    describe('withComputed', () => {
        describe('$translateProps', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));
            it('should return the page and the currentLanguage', () => {
                expect(store.$translateProps()).toEqual({
                    page: MOCK_RESPONSE_HEADLESS.page,
                    currentLanguage: mockLanguageArray[0]
                });
            });
        });

        describe('$languageId', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));
            it('should return the languageId', () => {
                expect(store.$languageId()).toBe(MOCK_RESPONSE_HEADLESS.viewAs.language.id);
            });
        });

        describe('$shellProps', () => {
            describe('Headless Page', () => {
                beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));
                it('should return the shell props for Headless Pages', () => {
                    expect(store.$shellProps()).toEqual(BASE_SHELL_PROPS_RESPONSE);
                });
                it('should return the shell props with property item disable when loading', () => {
                    store.setUveStatus(UVE_STATUS.LOADING);
                    const baseItems = BASE_SHELL_ITEMS.slice(0, BASE_SHELL_ITEMS.length - 1);

                    expect(store.$shellProps()).toEqual({
                        ...BASE_SHELL_PROPS_RESPONSE,
                        items: [
                            ...baseItems,
                            {
                                icon: 'pi-ellipsis-v',
                                label: 'editema.editor.navbar.properties',
                                id: 'properties',
                                isDisabled: true
                            }
                        ]
                    });
                });
                it('should return the error for 404', () => {
                    patchState(store, { errorCode: 404 });

                    expect(store.$shellProps()).toEqual({
                        ...BASE_SHELL_PROPS_RESPONSE,
                        error: {
                            code: 404,
                            pageInfo: COMMON_ERRORS['404']
                        }
                    });
                });
                it('should return the error for 403', () => {
                    patchState(store, { errorCode: 403 });

                    expect(store.$shellProps()).toEqual({
                        ...BASE_SHELL_PROPS_RESPONSE,
                        error: {
                            code: 403,
                            pageInfo: COMMON_ERRORS['403']
                        }
                    });
                });
                it('should return the error for 401', () => {
                    patchState(store, { errorCode: 401 });

                    expect(store.$shellProps()).toEqual({
                        ...BASE_SHELL_PROPS_RESPONSE,
                        error: {
                            code: 401,
                            pageInfo: null
                        }
                    });
                });

                it('should return layout, rules and experiments as disabled when isEnterprise is false', () => {
                    jest.spyOn(dotPageApiService, 'get').mockImplementation(
                        buildPageAPIResponseFromMock(MOCK_RESPONSE_VTL)
                    );

                    patchState(store, { isEnterprise: false });

                    const shellProps = store.$shellProps();
                    const layoutItem = shellProps.items.find((item) => item.id === 'layout');
                    const rulesItem = shellProps.items.find((item) => item.id === 'rules');
                    const experimentsItem = shellProps.items.find(
                        (item) => item.id === 'experiments'
                    );

                    expect(layoutItem.isDisabled).toBe(true);
                    expect(rulesItem.isDisabled).toBe(true);
                    expect(experimentsItem.isDisabled).toBe(true);
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

                    store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                    const rules = store.$shellProps().items.find((item) => item.id === 'rules');
                    const experiments = store
                        .$shellProps()
                        .items.find((item) => item.id === 'experiments');

                    expect(rules.isDisabled).toBe(true);
                    expect(experiments.isDisabled).toBe(true);
                });
            });

            describe('VTL Page', () => {
                it('should return the shell props for Legacy Pages', () => {
                    jest.spyOn(dotPageApiService, 'get').mockImplementation(
                        buildPageAPIResponseFromMock(MOCK_RESPONSE_VTL)
                    );

                    store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                    expect(store.$shellProps()).toEqual({
                        canRead: true,
                        error: null,
                        seoParams: {
                            siteId: MOCK_RESPONSE_VTL.site.identifier,
                            languageId: 1,
                            currentUrl: '/test-url',
                            requestHostName: 'http://localhost'
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
                                id: 'properties',
                                isDisabled: false
                            }
                        ]
                    });
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

                    store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                    const layoutItem = store
                        .$shellProps()
                        .items.find((item) => item.id === 'layout');

                    expect(layoutItem.isDisabled).toBe(true);
                    expect(layoutItem.tooltip).toBe(
                        'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
                    );
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

                    store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                    const layoutItem = store
                        .$shellProps()
                        .items.find((item) => item.id === 'layout');

                    expect(layoutItem.isDisabled).toBe(true);
                });
            });
        });

        describe('$isPreviewMode', () => {
            it("should return true when the preview is 'true'", () => {
                store.loadPageAsset({ mode: UVE_MODE.PREVIEW });

                expect(store.$isPreviewMode()).toBe(true);
            });

            it("should return false when the preview is not 'true'", () => {
                store.loadPageAsset({ mode: null });

                expect(store.$isPreviewMode()).toBe(false);
            });
        });

        describe('$isLiveMode', () => {
            it("should return true when the live is 'true'", () => {
                store.loadPageAsset({ mode: UVE_MODE.LIVE });

                expect(store.$isLiveMode()).toBe(true);
            });

            it("should return false when the live is not 'true'", () => {
                store.loadPageAsset({ mode: null });

                expect(store.$isLiveMode()).toBe(false);
            });
        });

        describe('$friendlyParams', () => {
            it('should return a readable user params', () => {
                const pageParams = {
                    url: '/index',
                    language_id: '1',
                    [PERSONA_KEY]: 'someCoolDude'
                };

                const viewParams = {
                    orientation: Orientation.LANDSCAPE,
                    device: '',
                    seo: ''
                };

                const expected = normalizeQueryParams({ ...pageParams, ...viewParams });

                patchState(store, { pageParams, viewParams });
                expect(store.$friendlyParams()).toEqual(expected);
            });
        });
    });

    describe('withMethods', () => {
        describe('setUveStatus', () => {
            it('should set the status of the UVEStore', () => {
                store.setUveStatus(UVE_STATUS.LOADED);

                expect(store.status()).toBe(UVE_STATUS.LOADED);
            });
        });

        describe('updatePageResponse', () => {
            it('should update the page response', () => {
                const pageAPIResponse = {
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        title: 'New title'
                    }
                };

                store.updatePageResponse(pageAPIResponse);

                expect(store.pageAPIResponse()).toEqual(pageAPIResponse);
                expect(store.status()).toBe(UVE_STATUS.LOADED);
            });
        });
    });
});
