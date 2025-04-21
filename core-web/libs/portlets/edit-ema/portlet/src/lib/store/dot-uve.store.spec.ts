import { describe, expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotAnalyticsTrackerService,
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
    DotLanguagesServiceMock,
    CurrentUserDataMock,
    mockLanguageArray
} from '@dotcms/utils-testing';
import { UVE_MODE } from '@dotcms/uve/types';

import { UVEStore } from './dot-uve.store';
import { Orientation } from './models';

import { DotPageApiService } from '../services/dot-page-api.service';
import { UVE_STATUS } from '../shared/enums';
import {
    BASE_SHELL_PROPS_RESPONSE,
    dotPropertiesServiceMock,
    MOCK_RESPONSE_HEADLESS
} from '../shared/mocks';

// const mockPageAPIResponse = (mock: DotPageApiResponse) => {

//     return ({ url }) => {
//         const pageAPIResponse = {
//             ...mock,
//             page: {
//                 ...mock.page,
//                 pageURI: url
//             }
//         };

//         return of(pageAPIResponse);
//     };
// };

const BASIC_OPTIONS = {
    allowedDevURLs: ['http://localhost:3000']
};

const UVE_CONFIG_MOCK = (options) => {
    return {
        uveConfig: {
            options
        }
    };
};

describe('UVEStore', () => {
    let spectator: SpectatorService<InstanceType<typeof UVEStore>>;
    let store: InstanceType<typeof UVEStore>;

    const createService = createServiceFactory({
        service: UVEStore,
        providers: [
            MessageService,
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotExperimentsService),
            mockProvider(DotPageApiService),
            mockProvider(DotWorkflowsActionsService),
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        queryParams: {
                            clientHost: 'http://localhost:3000'
                        },
                        data: UVE_CONFIG_MOCK(BASIC_OPTIONS)
                    }
                }
            },
            {
                provide: DotPropertiesService,
                useValue: dotPropertiesServiceMock
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
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('withComputed', () => {
        const updateMode = (mode: UVE_MODE) => {
            patchState(store, {
                pageParams: {
                    ...store.pageParams(),
                    mode
                }
            });
        };

        describe('$isPreviewMode', () => {
            it('should return true when the editor is in Preview Mode', () => {
                updateMode(UVE_MODE.PREVIEW);
                expect(store.$isPreviewMode()).toBe(true);
            });

            it('should return false when the editor is not in Preview Mode', () => {
                updateMode(UVE_MODE.LIVE);
                expect(store.$isPreviewMode()).toBe(false);
            });
        });

        describe('$isLiveMode', () => {
            it("should return true when the live is 'true'", () => {
                updateMode(UVE_MODE.LIVE);
                expect(store.$isLiveMode()).toBe(true);
            });

            it("should return false when the live is not 'true'", () => {
                updateMode(UVE_MODE.PREVIEW);
                expect(store.$isLiveMode()).toBe(false);
            });
        });

        describe('$isLiveMode', () => {
            it("should return true when the live is 'true'", () => {
                updateMode(UVE_MODE.LIVE);

                expect(store.$isLiveMode()).toBe(true);
            });

            it("should return false when the live is not 'true'", () => {
                updateMode(UVE_MODE.PREVIEW);

                expect(store.$isLiveMode()).toBe(false);
            });
        });

        describe('$translateProps', () => {
            it('should return the page and the currentLanguage', () => {
                patchState(store, {
                    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
                    languages: mockLanguageArray
                });

                expect(store.$translateProps()).toEqual({
                    page: MOCK_RESPONSE_HEADLESS.page,
                    currentLanguage: mockLanguageArray[0]
                });
            });
        });

        describe('$languageId', () => {
            it('should return the languageId', () => {
                patchState(store, {
                    pageAPIResponse: MOCK_RESPONSE_HEADLESS
                });

                const expectedLanguageId = MOCK_RESPONSE_HEADLESS.viewAs.language.id;
                expect(store.$languageId()).toBe(expectedLanguageId);
            });
        });

        describe('$shellProps', () => {
            beforeEach(() => {
                patchState(store, {
                    status: UVE_STATUS.LOADED,
                    pageAPIResponse: MOCK_RESPONSE_HEADLESS
                });
            });

            it('should return the shell props for Headless Pages', () => {
                expect(store.$shellProps()).toEqual(BASE_SHELL_PROPS_RESPONSE);
            });

            it('should disable layout, rule and experiments when user is not enterprise', () => {
                patchState(store, {
                    isEnterprise: false
                });

                const shellProps = store.$shellProps();
                const layoutItem = shellProps.items.find((item) => item.id === 'layout');
                const rulesItem = shellProps.items.find((item) => item.id === 'rules');
                const experimentsItem = shellProps.items.find((item) => item.id === 'experiments');

                expect(layoutItem.isDisabled).toBe(true);
                expect(rulesItem.isDisabled).toBe(true);
                expect(experimentsItem.isDisabled).toBe(true);
            });

            it('should disable layout, rule and experiments when page cannot be edited', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            canEdit: false
                        }
                    }
                });

                const shellProps = store.$shellProps();
                const layoutItem = shellProps.items.find((item) => item.id === 'layout');
                const rulesItem = shellProps.items.find((item) => item.id === 'rules');
                const experimentsItem = shellProps.items.find((item) => item.id === 'experiments');

                expect(layoutItem.isDisabled).toBe(true);
                expect(rulesItem.isDisabled).toBe(true);
                expect(experimentsItem.isDisabled).toBe(true);
            });

            it('should disable properties when loading', () => {
                patchState(store, {
                    status: UVE_STATUS.LOADING
                });

                const shellProps = store.$shellProps();
                const propertiesItem = shellProps.items.find((item) => item.id === 'properties');

                expect(propertiesItem.isDisabled).toBe(true);
            });

            it('should disable layout when template is not drawed', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        template: {
                            ...MOCK_RESPONSE_HEADLESS.template,
                            drawed: false
                        }
                    }
                });

                const shellProps = store.$shellProps();
                const layoutItem = shellProps.items.find((item) => item.id === 'layout');

                expect(layoutItem.isDisabled).toBe(true);
                expect(layoutItem.tooltip).toBe(
                    'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
                );
            });
        });

        // Remember me this test
        // describe('$friendlyParams', () => {
        //     it('should return a readable user params', () => {
        //         const pageParams = {
        //             url: '/index',
        //             language_id: '1',
        //             [PERSONA_KEY]: 'someCoolDude'
        //         };

        //         const viewParams = {
        //             orientation: Orientation.LANDSCAPE,
        //             device: '',
        //             seo: ''
        //         };

        //         const expected = normalizeQueryParams({ ...pageParams, ...viewParams });

        //         patchState(store, { pageParams, viewParams });
        //         expect(store.$friendlyParams()).toEqual(expected);
        //     });
        // });
    });

    describe('withMethods', () => {
        describe('setUveStatus', () => {
            it('should set the status of the UVEStore', () => {
                store.setUveStatus(UVE_STATUS.LOADED);
                expect(store.status()).toBe(UVE_STATUS.LOADED);
            });
        });

        describe('patchViewParams', () => {
            it('should patch the view params', () => {
                store.patchViewParams({ orientation: Orientation.LANDSCAPE });
                expect(store.viewParams()).toEqual({ orientation: Orientation.LANDSCAPE });
            });
        });
    });

    describe('withHooks', () => {
        it('should set Traditional Page to false is headless configuration is set', () => {
            expect(store.isTraditionalPage()).toBe(false);
        });
    });
});
