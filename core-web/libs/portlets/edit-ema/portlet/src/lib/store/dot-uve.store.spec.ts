import { describe, expect, it } from '@jest/globals';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotAnalyticsTrackerService,
    DotContentletLockerService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPageLayoutService,
    DotPropertiesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { UVE_MODE } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';
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
import { Orientation, PageType } from './models';

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
            ConfirmationService,
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
                provide: DotContentletLockerService,
                useValue: {
                    lock: jest.fn().mockReturnValue(of({})),
                    unlock: jest.fn().mockReturnValue(of({}))
                }
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
            },
            {
                provide: DotAnalyticsTrackerService,
                useValue: {
                    track: jest.fn()
                }
            },
            {
                provide: DotPageLayoutService,
                useValue: {
                    save: jest.fn().mockReturnValue(of({})),
                    updateFromRowToContainers: jest.fn().mockReturnValue([])
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

        describe('$currentLanguage', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));
            it('should return the current language object', () => {
                expect(store.$currentLanguage()).toEqual(MOCK_RESPONSE_HEADLESS.viewAs.language);
            });

            it('should return undefined when viewAs is not available', () => {
                patchState(store, {
                    viewAs: undefined
                });
                expect(store.$currentLanguage()).toBeUndefined();
            });
        });

        describe('$canEditLayout', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when has permission and in EDIT mode', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getDraftExperimentMock()
                });
                expect(store.$canEditLayout()).toBe(true);
            });

            it('should return false when not in EDIT mode even with permission', () => {
                store.updatePageParams({ mode: UVE_MODE.PREVIEW });
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getDraftExperimentMock()
                });
                expect(store.$canEditLayout()).toBe(false);
            });

            it('should return false when page is locked', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: true,
                        lockedBy: 'other-user'
                    },
                    currentUser: {
                        ...CurrentUserDataMock,
                        userId: 'current-user',
                        loginAs: false
                    }
                });
                expect(store.$canEditLayout()).toBe(false);
            });

            it('should return false when experiment is running', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getRunningExperimentMock()
                });
                expect(store.$canEditLayout()).toBe(false);
            });

            it('should return false when no permission', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: false,
                        locked: false
                    },
                    template: {
                        ...MOCK_RESPONSE_HEADLESS.template,
                        drawed: false
                    }
                });
                expect(store.$canEditLayout()).toBe(false);
            });
        });


        describe('$mode', () => {
            it('should return EDIT when mode is EDIT', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                expect(store.$mode()).toBe(UVE_MODE.EDIT);
            });

            it('should return PREVIEW when mode is PREVIEW', () => {
                store.updatePageParams({ mode: UVE_MODE.PREVIEW });
                expect(store.$mode()).toBe(UVE_MODE.PREVIEW);
            });

            it('should return LIVE when mode is LIVE', () => {
                store.updatePageParams({ mode: UVE_MODE.LIVE });
                expect(store.$mode()).toBe(UVE_MODE.LIVE);
            });

            it('should return UNKNOWN when mode is undefined', () => {
                store.updatePageParams({ mode: undefined });
                expect(store.$mode()).toBe(UVE_MODE.UNKNOWN);
            });
        });



        describe('$canEditPageContent', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return false when not in EDIT mode even with permission', () => {
                store.updatePageParams({ mode: UVE_MODE.PREVIEW });
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    }
                });
                expect(store.$canEditPageContent()).toBe(false);
            });

            it('should return false when in EDIT mode but no permission', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: false
                    }
                });
                expect(store.$canEditPageContent()).toBe(false);
            });

            it('should return true when has permission and in EDIT mode', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getDraftExperimentMock()
                });
                expect(store.$canEditPageContent()).toBe(true);
            });
        });

        describe('$canEditStyles', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when feature enabled, has permission, and in EDIT mode', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageType: PageType.HEADLESS, // PageType.HEADLESS
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getDraftExperimentMock(),
                    flags: {
                        ...store.flags(),
                        FEATURE_FLAG_UVE_STYLE_EDITOR: true
                    }
                });
                expect(store.$canEditStyles()).toBe(true);
            });

            it('should return false when not in EDIT mode', () => {
                store.updatePageParams({ mode: UVE_MODE.PREVIEW });
                patchState(store, {
                    pageType: PageType.HEADLESS, // HEADLESS
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    }
                });
                expect(store.$canEditStyles()).toBe(false);
            });

            it('should return false when page is locked', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageType: PageType.HEADLESS, // HEADLESS
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: true,
                        lockedBy: 'other-user'
                    },
                    currentUser: {
                        ...CurrentUserDataMock,
                        userId: 'current-user',
                        loginAs: false
                    }
                });
                expect(store.$canEditStyles()).toBe(false);
            });

            it('should return false when experiment is running', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageType: PageType.HEADLESS, // HEADLESS
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getRunningExperimentMock()
                });
                expect(store.$canEditStyles()).toBe(false);
            });
        });

        describe('$pageURI', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return the page URI from page response', () => {
                expect(store.$pageURI()).toBe(MOCK_RESPONSE_HEADLESS.page.pageURI);
            });

            it('should return empty string when page is not available', () => {
                patchState(store, {
                    page: null
                });
                expect(store.$pageURI()).toBe('');
            });
        });

        describe('$variantId', () => {
            it('should return variant ID from page params', () => {
                store.updatePageParams({ variantId: 'test-variant-123' });
                expect(store.$variantId()).toBe('test-variant-123');
            });

            it('should return empty string when no variant ID', () => {
                store.updatePageParams({ variantId: undefined });
                expect(store.$variantId()).toBe('');
            });
        });

        describe('$enableInlineEdit', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when in edit state and enterprise is enabled', () => {
                patchState(store, {
                    isEnterprise: true,
                    view: {
                        ...store.view(),
                        isEditState: true
                    }
                });
                expect(store.$enableInlineEdit()).toBe(true);
            });

            it('should return false when not in edit state', () => {
                patchState(store, {
                    isEnterprise: true,
                    view: {
                        ...store.view(),
                        isEditState: false
                    }
                });
                expect(store.$enableInlineEdit()).toBe(false);
            });

            it('should return false when enterprise is not enabled', () => {
                patchState(store, {
                    isEnterprise: false,
                    view: {
                        ...store.view(),
                        isEditState: true
                    }
                });
                expect(store.$enableInlineEdit()).toBe(false);
            });

            it('should return false when neither condition is met', () => {
                patchState(store, {
                    isEnterprise: false,
                    view: {
                        ...store.view(),
                        isEditState: false
                    }
                });
                expect(store.$enableInlineEdit()).toBe(false);
            });
        });

        describe('$isPageLocked', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when page is locked by another user', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: true,
                        lockedBy: 'another-user-id'
                    },
                    currentUser: {
                        ...CurrentUserDataMock,
                        userId: 'current-user-id',
                        loginAs: false
                    }
                });

                expect(store.$isPageLocked()).toBe(true);
            });

            it('should return false when page is locked by current user', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: true,
                        lockedBy: 'current-user-id'
                    },
                    currentUser: {
                        ...CurrentUserDataMock,
                        userId: 'current-user-id',
                        loginAs: false
                    }
                });

                expect(store.$isPageLocked()).toBe(false);
            });

            it('should return false when page is not locked', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: false
                    }
                });

                expect(store.$isPageLocked()).toBe(false);
            });
        });


        describe('$isLockFeatureEnabled', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when feature flag is enabled', () => {
                patchState(store, {
                    flags: {
                        ...store.flags(),
                        FEATURE_FLAG_UVE_TOGGLE_LOCK: true
                    }
                });

                expect(store.$isLockFeatureEnabled()).toBe(true);
            });

            it('should return false when page cannot be locked', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canLock: false
                    }
                });

                expect(store.$isLockFeatureEnabled()).toBe(false);
            });
        });


        describe('$hasAccessToEditMode', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when page can be edited and is not locked by another user', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    }
                });

                expect(store.$hasAccessToEditMode()).toBe(true);
            });

            it('should return false when page cannot be edited', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: false,
                        locked: false
                    }
                });

                expect(store.$hasAccessToEditMode()).toBe(false);
            });

            it('should return false when page is locked by another user', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: true,
                        lockedBy: 'another-user'
                    },
                    currentUser: {
                        ...CurrentUserDataMock,
                        userId: 'current-user',
                        loginAs: false
                    }
                });

                expect(store.$hasAccessToEditMode()).toBe(false);
            });
        });

        // Phase 2: $shellProps moved to DotEmaShellComponent - these tests are deprecated

        // Duplicate tests - already tested above in Phase 5 additions (lines 267-289)


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

                patchState(store, {
                    pageParams,
                    view: {
                        ...store.view(),
                        viewParams
                    }
                });
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

                expect(store.page()).toEqual(pageAPIResponse.page);
                expect(store.site()).toEqual(pageAPIResponse.site);
                expect(store.viewAs()).toEqual(pageAPIResponse.viewAs);
                expect(store.template()).toEqual(pageAPIResponse.template);
                expect(store.layout()).toEqual(pageAPIResponse.layout);
                expect(store.containers()).toEqual(pageAPIResponse.containers);
                expect(store.status()).toBe(UVE_STATUS.LOADED);
            });
        });

        describe('setActiveContentlet and resetActiveContentlet', () => {
            it('should set activeContentlet in editor state', () => {
                const payload = {
                    container: {
                        identifier: 'container-123',
                        uuid: 'uuid-123',
                        acceptTypes: 'Blog,News',
                        maxContentlets: 10,
                        variantId: 'DEFAULT'
                    },
                    contentlet: {
                        identifier: 'contentlet-456',
                        inode: 'inode-456',
                        title: 'Test Blog Post',
                        contentType: 'Blog'
                    },
                    language_id: '1',
                    pageId: 'page-1',
                    pageContainers: []
                };

                store.setActiveContentlet(payload);

                expect(store.editor().activeContentlet).toEqual(payload);
            });

            it('should clear activeContentlet when resetActiveContentlet is called', () => {
                const payload = {
                    container: {
                        identifier: 'container-123',
                        uuid: 'uuid-123',
                        acceptTypes: 'Blog,News',
                        maxContentlets: 10,
                        variantId: 'DEFAULT'
                    },
                    contentlet: {
                        identifier: 'contentlet-456',
                        inode: 'inode-456',
                        title: 'Test',
                        contentType: 'Blog'
                    },
                    language_id: '1',
                    pageId: 'page-1',
                    pageContainers: []
                };
                store.setActiveContentlet(payload);
                expect(store.editor().activeContentlet).not.toBeNull();

                store.resetActiveContentlet();
                expect(store.editor().activeContentlet).toBeNull();
            });
        });
    });
});
