import { describe, expect, it } from '@jest/globals';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { signalStore, withFeature, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

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
    CurrentUserDataMock,
    DotLanguagesServiceMock,
    getDraftExperimentMock,
    getRunningExperimentMock,
    getScheduleExperimentMock,
    MockDotMessageService,
    mockLanguageArray
} from '@dotcms/utils-testing';

import { withLoad } from './withLoad';

import { DotPageApiParams, DotPageApiService } from '../../../services/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import {
    dotPropertiesServiceMock,
    getNewVanityUrl,
    getVanityUrl,
    HEADLESS_BASE_QUERY_PARAMS,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL,
    NEW_PERMANENT_REDIRECT_VANITY_URL,
    NEW_PERMANENT_REDIRECT_VANITY_URL_WITH_ACTION,
    NEW_PERMANENT_REDIRECT_VANITY_URL_WITH_RESPONSE,
    NEW_TEMPORARY_REDIRECT_VANITY_URL,
    PERMANENT_REDIRECT_VANITY_URL,
    TEMPORARY_REDIRECT_VANITY_URL,
    VTL_BASE_QUERY_PARAMS
} from '../../../shared/mocks';
import { Orientation, PageType, UVEState } from '../../models';
import { withClient } from '../client/withClient';

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

const pageParams: DotPageApiParams = {
    url: 'new-url',
    language_id: '1',
    [PERSONA_KEY]: '2'
};

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    // Normalized page response properties
    page: null,
    site: null,
    template: null,
    containers: null,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams,
    status: UVE_STATUS.LOADING,
    pageType: PageType.TRADITIONAL,
    // Phase 3: Nested editor state
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
    // Phase 3: Nested view state
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
    withState<UVEState>(initialState),
    withClient(),
    withFeature((store) => withLoad({
        resetClientConfiguration: () => store.resetClientConfiguration(),
        getWorkflowActions: (inode) => {}, // Mock implementation
        graphqlRequest: () => store.graphqlRequest(),
        $graphqlWithParams: computed(() => store.$graphqlWithParams()),
        setGraphqlResponse: (response) => store.setGraphqlResponse(response),
        addHistory: (state) => store.addHistory(state)
    }))
);

describe('withLoad', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    let dotPageApiService: SpyObject<DotPageApiService>;
    let router: Router;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of([])
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
                    getGraphQLPage: () => of({}),
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

        router = spectator.inject(Router);
        dotPageApiService = spectator.inject(DotPageApiService);
        jest.spyOn(dotPageApiService, 'get').mockImplementation(
            buildPageAPIResponseFromMock(MOCK_RESPONSE_HEADLESS)
        );
    });

    describe('withMethods', () => {
        describe('load', () => {
            it('should load the store with the base data', () => {
                store.loadPageAsset(HEADLESS_BASE_QUERY_PARAMS);
                expect(store.page()).toEqual(MOCK_RESPONSE_HEADLESS.page);
                expect(store.site()).toEqual(MOCK_RESPONSE_HEADLESS.site);
                expect(store.template()).toEqual(MOCK_RESPONSE_HEADLESS.template);
                expect(store.layout()).toEqual(MOCK_RESPONSE_HEADLESS.layout);
                expect(store.containers()).toEqual(MOCK_RESPONSE_HEADLESS.containers);
                expect(store.isEnterprise()).toBe(true);
                expect(store.currentUser()).toEqual(CurrentUserDataMock);
                expect(store.experiment()).toBe(getDraftExperimentMock());
                expect(store.languages()).toBe(mockLanguageArray);
                expect(store.status()).toBe(UVE_STATUS.LOADED);
                expect(store.pageType()).toBe(PageType.HEADLESS);
                expect(store.isClientReady()).toBe(false);
            });

            it('should load the store with the base data for traditional page', () => {
                jest.spyOn(dotPageApiService, 'get').mockImplementation(
                    buildPageAPIResponseFromMock(MOCK_RESPONSE_VTL)
                );

                store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                expect(store.page()).toEqual(MOCK_RESPONSE_VTL.page);
                expect(store.site()).toEqual(MOCK_RESPONSE_VTL.site);
                expect(store.template()).toEqual(MOCK_RESPONSE_VTL.template);
                expect(store.layout()).toEqual(MOCK_RESPONSE_VTL.layout);
                expect(store.containers()).toEqual(MOCK_RESPONSE_VTL.containers);
                expect(store.isEnterprise()).toBe(true);
                expect(store.currentUser()).toEqual(CurrentUserDataMock);
                expect(store.experiment()).toBe(getDraftExperimentMock());
                expect(store.languages()).toBe(mockLanguageArray);
                expect(store.status()).toBe(UVE_STATUS.LOADED);
                expect(store.pageType()).toBe(PageType.TRADITIONAL);
                expect(store.isClientReady()).toBe(false);
            });

            it('should update the pageParams with the vanity URL on permanent redirect', () => {
                const permanentRedirect = getVanityUrl(
                    VTL_BASE_QUERY_PARAMS.url,
                    PERMANENT_REDIRECT_VANITY_URL
                );

                const forwardTo = PERMANENT_REDIRECT_VANITY_URL.forwardTo;

                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of(permanentRedirect)
                );

                store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { url: forwardTo },
                    queryParamsHandling: 'merge'
                });
            });

            it('should update the pageParams with the vanity URL on temporary redirect', () => {
                const temporaryRedirect = getVanityUrl(
                    VTL_BASE_QUERY_PARAMS.url,
                    TEMPORARY_REDIRECT_VANITY_URL
                );

                const forwardTo = TEMPORARY_REDIRECT_VANITY_URL.forwardTo;

                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of(temporaryRedirect)
                );

                store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { url: forwardTo },
                    queryParamsHandling: 'merge'
                });
            });

            it('should update the pageParams with the vanity URL on new temporary redirect', () => {
                const temporaryRedirect = getNewVanityUrl(
                    VTL_BASE_QUERY_PARAMS.url,
                    NEW_TEMPORARY_REDIRECT_VANITY_URL
                );

                const forwardTo = NEW_TEMPORARY_REDIRECT_VANITY_URL.forwardTo;

                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of(temporaryRedirect)
                );

                store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { url: forwardTo },
                    queryParamsHandling: 'merge'
                });
            });

            it('should update the pageParams with the vanity URL on new permanent redirect', () => {
                const permanentRedirect = getNewVanityUrl(
                    VTL_BASE_QUERY_PARAMS.url,
                    NEW_PERMANENT_REDIRECT_VANITY_URL
                );

                const forwardTo = NEW_PERMANENT_REDIRECT_VANITY_URL.forwardTo;

                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of(permanentRedirect)
                );

                store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { url: forwardTo },
                    queryParamsHandling: 'merge'
                });
            });

            it('should not navigate if the vanity URL has a response 200', () => {
                const permanentRedirect = getNewVanityUrl(
                    VTL_BASE_QUERY_PARAMS.url,
                    NEW_PERMANENT_REDIRECT_VANITY_URL_WITH_RESPONSE
                );

                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of(permanentRedirect)
                );

                store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).not.toHaveBeenCalled();
            });
            it('should not navigate if the vanity URL has a action 200', () => {
                const permanentRedirect = getNewVanityUrl(
                    VTL_BASE_QUERY_PARAMS.url,
                    NEW_PERMANENT_REDIRECT_VANITY_URL_WITH_ACTION
                );

                jest.spyOn(dotPageApiService, 'get').mockImplementation(() =>
                    of(permanentRedirect)
                );

                store.loadPageAsset(VTL_BASE_QUERY_PARAMS);

                expect(router.navigate).not.toHaveBeenCalled();
            });
        });

        describe('reloadCurrentPage', () => {
            it('should call page with same params', () => {
                const spy = jest
                    .spyOn(dotPageApiService, 'get')
                    .mockImplementation(() => of(MOCK_RESPONSE_HEADLESS));
                store.reloadCurrentPage();

                expect(spy).toHaveBeenCalledWith(pageParams);
            });

            it('should call getGraphQLPage if graphql is present', () => {
                jest.spyOn(dotPageApiService, 'getGraphQLPage').mockImplementation(() =>
                    of({
                        pageAsset: MOCK_RESPONSE_HEADLESS,
                        content: {}
                    })
                );

                store.setCustomGraphQL(
                    {
                        query: 'query',
                        variables: {
                            url: 'url',
                            mode: 'mode',
                            languageId: 'languageId'
                        }
                    },
                    true
                );
                store.reloadCurrentPage();
                expect(dotPageApiService.getGraphQLPage).toHaveBeenCalledWith(
                    store.$graphqlWithParams()
                );
            });
        });
    });
    afterEach(() => jest.clearAllMocks());
});
