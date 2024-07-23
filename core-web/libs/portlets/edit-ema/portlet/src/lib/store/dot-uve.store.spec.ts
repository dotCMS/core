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
    DEFAULT_PERSONA,
    getVanityUrl,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL,
    PERMANENT_REDIRECT_VANITY_URL,
    TEMPORARY_REDIRECT_VANITY_URL
} from '../shared/consts';
import { UVE_STATUS } from '../shared/enums';

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

    describe('withLoad', () => {
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

            jest.spyOn(dotPageApiService, 'get').mockImplementation(() => of(permanentRedirect));

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

            jest.spyOn(dotPageApiService, 'get').mockImplementation(() => of(temporaryRedirect));

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
                        { path: 'layout', parameters: {}, parameterMap: {} as unknown as ParamMap }
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
                        { path: 'layout', parameters: {}, parameterMap: {} as unknown as ParamMap }
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
                        { path: 'rules', parameters: {}, parameterMap: {} as unknown as ParamMap }
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
                        { path: 'rules', parameters: {}, parameterMap: {} as unknown as ParamMap }
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
