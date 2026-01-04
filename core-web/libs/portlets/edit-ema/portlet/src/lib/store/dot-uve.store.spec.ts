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
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        viewAs: undefined
                    }
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
                        userId: 'current-user'
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

        describe('$isEditMode', () => {
            it('should return true when mode is EDIT', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                expect(store.$isEditMode()).toBe(true);
            });

            it('should return false when mode is PREVIEW', () => {
                store.updatePageParams({ mode: UVE_MODE.PREVIEW });
                expect(store.$isEditMode()).toBe(false);
            });

            it('should return false when mode is LIVE', () => {
                store.updatePageParams({ mode: UVE_MODE.LIVE });
                expect(store.$isEditMode()).toBe(false);
            });
        });

        describe('$isPreviewMode', () => {
            it('should return true when mode is PREVIEW', () => {
                store.updatePageParams({ mode: UVE_MODE.PREVIEW });
                expect(store.$isPreviewMode()).toBe(true);
            });

            it('should return false when mode is EDIT', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                expect(store.$isPreviewMode()).toBe(false);
            });
        });

        describe('$isLiveMode', () => {
            it('should return true when mode is LIVE', () => {
                store.updatePageParams({ mode: UVE_MODE.LIVE });
                expect(store.$isLiveMode()).toBe(true);
            });

            it('should return false when mode is EDIT', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                expect(store.$isLiveMode()).toBe(false);
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

        describe('$hasPermissionToEditLayout', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return false when page is locked', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: true,
                        lockedBy: 'other-user'
                    },
                    currentUser: {
                        ...CurrentUserDataMock,
                        userId: 'current-user'
                    }
                });
                expect(store.$hasPermissionToEditLayout()).toBe(false);
            });

            it('should return false when experiment is running', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getRunningExperimentMock()
                });
                expect(store.$hasPermissionToEditLayout()).toBe(false);
            });

            it('should return false when experiment is scheduled', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getScheduleExperimentMock()
                });
                expect(store.$hasPermissionToEditLayout()).toBe(false);
            });

            it('should return true when all conditions met (canEdit)', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getDraftExperimentMock()
                });
                expect(store.$hasPermissionToEditLayout()).toBe(true);
            });

            it('should return true when template is drawed', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: false,
                        locked: false
                    },
                    template: {
                        ...MOCK_RESPONSE_HEADLESS.template,
                        drawed: true
                    },
                    experiment: getDraftExperimentMock()
                });
                expect(store.$hasPermissionToEditLayout()).toBe(true);
            });
        });

        describe('$hasPermissionToEditStyles', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return false when page is locked', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: true,
                        lockedBy: 'other-user'
                    },
                    currentUser: {
                        ...CurrentUserDataMock,
                        userId: 'current-user'
                    }
                });
                expect(store.$hasPermissionToEditStyles()).toBe(false);
            });

            it('should return false when experiment is running', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getRunningExperimentMock()
                });
                expect(store.$hasPermissionToEditStyles()).toBe(false);
            });

            it('should return true when all conditions met', () => {
                patchState(store, {
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getDraftExperimentMock()
                });
                expect(store.$hasPermissionToEditStyles()).toBe(true);
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
                    pageType: 1, // HEADLESS
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: false
                    },
                    experiment: getDraftExperimentMock()
                });
                expect(store.$canEditStyles()).toBe(true);
            });

            it('should return false when not in EDIT mode', () => {
                store.updatePageParams({ mode: UVE_MODE.PREVIEW });
                patchState(store, {
                    pageType: 1, // HEADLESS
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
                    pageType: 1, // HEADLESS
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        canEdit: true,
                        locked: true,
                        lockedBy: 'other-user'
                    },
                    currentUser: {
                        ...CurrentUserDataMock,
                        userId: 'current-user'
                    }
                });
                expect(store.$canEditStyles()).toBe(false);
            });

            it('should return false when experiment is running', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageType: 1, // HEADLESS
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
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: null
                    }
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

        describe('$isPageLocked', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when page is locked by another user', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            locked: true,
                            lockedBy: 'another-user-id'
                        }
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
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            locked: true,
                            lockedBy: 'current-user-id'
                        }
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
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            locked: false
                        }
                    }
                });

                expect(store.$isPageLocked()).toBe(false);
            });
        });

        describe('$canEditPage', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when user has access and is in EDIT mode', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: true,
                            locked: false
                        }
                    }
                });

                expect(store.$canEditPage()).toBe(true);
            });

            it('should return false when page is locked by another user', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: true,
                            locked: true,
                            lockedBy: 'another-user'
                        }
                    },
                    currentUser: {
                        ...CurrentUserDataMock,
                        userId: 'current-user',
                        loginAs: false
                    }
                });

                expect(store.$canEditPage()).toBe(false);
            });

            it('should return false when not in EDIT mode', () => {
                store.updatePageParams({ mode: UVE_MODE.PREVIEW });
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: true,
                            locked: false
                        }
                    }
                });

                expect(store.$canEditPage()).toBe(false);
            });

            it('should return false when user cannot edit page', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: false,
                            locked: false
                        }
                    }
                });

                expect(store.$canEditPage()).toBe(false);
            });
        });

        describe('$isLockFeatureEnabled', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when page can be locked', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canLock: true
                        }
                    }
                });

                expect(store.$isLockFeatureEnabled()).toBe(true);
            });

            it('should return false when page cannot be locked', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canLock: false
                        }
                    }
                });

                expect(store.$isLockFeatureEnabled()).toBe(false);
            });
        });

        describe('$isStyleEditorEnabled', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when all conditions are met', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: true,
                            locked: false
                        }
                    },
                    isEnterprise: true
                });

                expect(store.$isStyleEditorEnabled()).toBe(true);
            });

            it('should return false when not enterprise', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: true,
                            locked: false
                        }
                    },
                    isEnterprise: false
                });

                expect(store.$isStyleEditorEnabled()).toBe(false);
            });

            it('should return false when cannot edit page', () => {
                store.updatePageParams({ mode: UVE_MODE.EDIT });
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: false,
                            locked: false
                        }
                    },
                    isEnterprise: true
                });

                expect(store.$isStyleEditorEnabled()).toBe(false);
            });
        });

        describe('$hasAccessToEditMode', () => {
            beforeEach(() => store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS));

            it('should return true when page can be edited and is not locked by another user', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: true,
                            locked: false
                        }
                    }
                });

                expect(store.$hasAccessToEditMode()).toBe(true);
            });

            it('should return false when page cannot be edited', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: false,
                            locked: false
                        }
                    }
                });

                expect(store.$hasAccessToEditMode()).toBe(false);
            });

            it('should return false when page is locked by another user', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: true,
                            locked: true,
                            lockedBy: 'another-user'
                        }
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
        describe.skip('$shellProps (DEPRECATED - moved to component)', () => {
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

                it('should return rules as disabled when page does not have the canSeeRules property and cannot edit and is not enterprise', () => {
                    jest.spyOn(dotPageApiService, 'get').mockImplementation(
                        buildPageAPIResponseFromMock({
                            ...MOCK_RESPONSE_VTL,
                            page: {
                                ...MOCK_RESPONSE_VTL.page,
                                canSeeRules: undefined,
                                canEdit: false
                            }
                        })
                    );

                    store.loadPageAsset(VTL_BASE_QUERY_PARAMS);
                    patchState(store, { isEnterprise: false });

                    const rules = store.$shellProps().items.find((item) => item.id === 'rules');
                    expect(rules.isDisabled).toBe(true);
                });

                it('should return rules as not disabled when page does not have the canSeeRules property and can edit and is enterprise', () => {
                    const pageWithoutCanSeeRules = MOCK_RESPONSE_VTL.page;
                    // delete the canSeeRules property
                    delete pageWithoutCanSeeRules.canSeeRules;

                    jest.spyOn(dotPageApiService, 'get').mockImplementation(
                        buildPageAPIResponseFromMock({
                            ...MOCK_RESPONSE_VTL,
                            page: {
                                ...pageWithoutCanSeeRules,
                                canEdit: true
                            }
                        })
                    );

                    store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                    const rules = store.$shellProps().items.find((item) => item.id === 'rules');
                    expect(rules.isDisabled).toBe(false);
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

            describe('currentUrl', () => {
                it('should not add a initial slash if the url has one', () => {
                    jest.spyOn(dotPageApiService, 'get').mockImplementation(
                        buildPageAPIResponseFromMock({
                            ...MOCK_RESPONSE_VTL,
                            page: {
                                ...MOCK_RESPONSE_VTL.page,
                                pageURI: '/test-url'
                            }
                        })
                    );

                    store.loadPageAsset(VTL_BASE_QUERY_PARAMS);
                    const seoParams = store.$shellProps().seoParams;

                    expect(seoParams.currentUrl).toEqual('/test-url');
                });

                it('should add a initial slash if the url does not have one', () => {
                    jest.spyOn(dotPageApiService, 'get').mockImplementation(
                        buildPageAPIResponseFromMock({
                            ...MOCK_RESPONSE_VTL,
                            page: {
                                ...MOCK_RESPONSE_VTL.page,
                                pageURI: 'test-url'
                            }
                        })
                    );

                    store.loadPageAsset(VTL_BASE_QUERY_PARAMS);
                    const seoParams = store.$shellProps().seoParams;

                    expect(seoParams.currentUrl).toEqual('/test-url');
                });
            });
        });

        // Duplicate tests - already tested above in Phase 5 additions (lines 267-289)
        describe.skip('$isPreviewMode (duplicate)', () => {
            it("should return true when the preview is 'true'", () => {
                store.loadPageAsset({ mode: UVE_MODE.PREVIEW });

                expect(store.$isPreviewMode()).toBe(true);
            });

            it("should return false when the preview is not 'true'", () => {
                store.loadPageAsset({ mode: null });

                expect(store.$isPreviewMode()).toBe(false);
            });
        });

        describe.skip('$isLiveMode (duplicate)', () => {
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
