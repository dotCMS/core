import { describe, expect } from '@jest/globals';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, ActivatedRouteSnapshot, ParamMap, Router } from '@angular/router';

import {
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    CurrentUserDataMock,
    DotLanguagesServiceMock,
    getDraftExperimentMock,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    MockDotMessageService,
    mockLanguageArray
} from '@dotcms/utils-testing';

import { withLoad } from './withLoad';

import {
    DotPageApiParams,
    DotPageApiResponse,
    DotPageApiService
} from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import {
    getVanityUrl,
    HEADLESS_BASE_QUERY_PARAMS,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL,
    PERMANENT_REDIRECT_VANITY_URL,
    TEMPORARY_REDIRECT_VANITY_URL,
    VTL_BASE_QUERY_PARAMS
} from '../../../shared/mocks';
import { UVEState } from '../../models';

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
const emptyParams = {} as DotPageApiParams;

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: null,
    currentUser: null,
    experiment: null,
    errorCode: null,
    params: emptyParams,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true,
    canEditPage: false,
    pageIsLocked: true
};

export const uveStoreMock = signalStore(withState<UVEState>(initialState), withLoad());

describe('withLoad', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    let dotPageApiService: SpyObject<DotPageApiService>;
    let activatedRoute: SpyObject<ActivatedRoute>;
    let router: SpyObject<Router>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
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
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;

        dotPageApiService = spectator.inject(DotPageApiService);
        router = spectator.inject(Router);
        activatedRoute = spectator.inject(ActivatedRoute);
        jest.spyOn(dotPageApiService, 'get').mockImplementation(
            buildPageAPIResponseFromMock(MOCK_RESPONSE_HEADLESS)
        );

        store.init(HEADLESS_BASE_QUERY_PARAMS);
    });

    describe('withMethods', () => {
        it('should load the store with the base data', () => {
            expect(store.pageAPIResponse()).toEqual(MOCK_RESPONSE_HEADLESS);
            expect(store.isEnterprise()).toBe(true);
            expect(store.currentUser()).toEqual(CurrentUserDataMock);
            expect(store.experiment()).toBe(undefined);
            expect(store.languages()).toBe(mockLanguageArray);
            expect(store.params()).toEqual(HEADLESS_BASE_QUERY_PARAMS);
            expect(store.canEditPage()).toBe(true);
            expect(store.pageIsLocked()).toBe(false);
            expect(store.status()).toBe(UVE_STATUS.LOADED);
            expect(store.isTraditionalPage()).toBe(false);
            expect(store.isClientReady()).toBe(false);
        });

        it('should load the store with the base data for traditional page', () => {
            jest.spyOn(dotPageApiService, 'get').mockImplementation(
                buildPageAPIResponseFromMock(MOCK_RESPONSE_VTL)
            );

            store.init(VTL_BASE_QUERY_PARAMS);

            expect(store.pageAPIResponse()).toEqual(MOCK_RESPONSE_VTL);
            expect(store.isEnterprise()).toBe(true);
            expect(store.currentUser()).toEqual(CurrentUserDataMock);
            expect(store.experiment()).toBe(undefined);
            expect(store.languages()).toBe(mockLanguageArray);
            expect(store.params()).toEqual(VTL_BASE_QUERY_PARAMS);
            expect(store.canEditPage()).toBe(true);
            expect(store.pageIsLocked()).toBe(false);
            expect(store.status()).toBe(UVE_STATUS.LOADED);
            expect(store.isTraditionalPage()).toBe(true);
            expect(store.isClientReady()).toBe(true);
        });

        it('should navigate when the page is a vanityUrl permanent redirect', () => {
            const permanentRedirect = getVanityUrl(
                VTL_BASE_QUERY_PARAMS.url,
                PERMANENT_REDIRECT_VANITY_URL
            ) as unknown as DotPageApiResponse;

            const forwardTo = PERMANENT_REDIRECT_VANITY_URL.forwardTo;

            jest.spyOn(dotPageApiService, 'get').mockImplementation(() => of(permanentRedirect));

            store.init(VTL_BASE_QUERY_PARAMS);

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

            jest.spyOn(dotPageApiService, 'get').mockImplementation(() => of(temporaryRedirect));

            store.init(VTL_BASE_QUERY_PARAMS);

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

            store.init(VTL_BASE_QUERY_PARAMS);

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

            store.init(VTL_BASE_QUERY_PARAMS);

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

            store.init(VTL_BASE_QUERY_PARAMS);

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

            store.init(VTL_BASE_QUERY_PARAMS);

            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('should reload the store with the same queryParams', () => {
            const getPageSpy = jest.spyOn(dotPageApiService, 'get');

            store.reload();

            expect(getPageSpy).toHaveBeenCalledWith(store.params());
        });
    });
});
