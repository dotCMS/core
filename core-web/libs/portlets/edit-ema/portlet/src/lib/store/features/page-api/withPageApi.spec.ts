import { describe, expect, it } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withFeature, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import {
    DotExperimentsService,
    DotLanguagesService,
    DotPageLayoutService,
    DotPropertiesService
} from '@dotcms/data-access';
import { UVE_MODE } from '@dotcms/types';

import { withPageApi } from './withPageApi';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { UveIframeMessengerService } from '../../../services/iframe-messenger/uve-iframe-messenger.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { UVE_STATUS } from '../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';
import { withFlags } from '../flags/withFlags';
import { withPage } from '../page/withPage';

const pageParamsBase = {
    url: 'test-url',
    language_id: '1',
    [PERSONA_KEY]: 'dot:persona',
    mode: UVE_MODE.EDIT
};

const graphqlRequest = {
    query: '{ page { url } }',
    variables: { depth: '1', language_id: '1' }
};

function buildTestStore() {
    return signalStore(
        { protectedState: false },
        withState<UVEState>(createInitialUVEState({ pageParams: pageParamsBase })),
        withFlags([]),
        withPage(),
        withFeature((store) =>
            withPageApi({
                resetClientConfiguration: () => store.resetClientConfiguration(),
                requestMetadata: () => store.requestMetadata(),
                $requestWithParams: store.$requestWithParams,
                setPageAsset: (payload) => store.setPageAsset(payload),
                rollbackPageAssetResponse: () => store.rollbackPageAssetResponse(),
                addHistory: (response) => store.addToHistory(response),
                resetHistoryToCurrent: () => store.resetHistoryToCurrent(),
                pageAsset: () => store.pageAsset()
            })
        )
    );
}

describe('withPageApi', () => {
    let spectator: SpectatorService<InstanceType<ReturnType<typeof buildTestStore>>>;
    let store: InstanceType<ReturnType<typeof buildTestStore>>;

    const getSpy = jest.fn(() => of(MOCK_RESPONSE_HEADLESS));
    const getGraphQLPageSpy = jest.fn(() =>
        of({
            pageAsset: MOCK_RESPONSE_HEADLESS,
            content: { source: 'graphql' }
        })
    );

    const createService = createServiceFactory({
        service: buildTestStore(),
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotExperimentsService, {
                getById: jest.fn().mockReturnValue(of(null))
            }),
            mockProvider(DotLanguagesService, {
                getLanguagesUsedPage: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotPageLayoutService, {
                save: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(UveIframeMessengerService, {
                sendPageData: jest.fn()
            }),
            {
                provide: DotPageApiService,
                useValue: {
                    get: getSpy,
                    getGraphQLPage: getGraphQLPageSpy,
                    save: jest.fn().mockReturnValue(of({})),
                    saveStyleProperties: jest.fn().mockReturnValue(of({}))
                }
            }
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createService();
        store = spectator.service;
        spectator.flushEffects();
    });

    describe('pageLoad – fetch vs GraphQL', () => {
        it('should use regular get(pageParams) when requestMetadata is null', () => {
            expect(store.requestMetadata()).toBeNull();

            store.pageLoad({ language_id: '1' });
            spectator.flushEffects();

            expect(getSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    ...pageParamsBase,
                    language_id: '1'
                })
            );
            expect(getGraphQLPageSpy).not.toHaveBeenCalled();
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should use getGraphQLPage when requestMetadata is set', () => {
            store.setCustomClient(graphqlRequest);
            expect(store.requestMetadata()).toEqual(graphqlRequest);

            store.pageLoad({ language_id: '2' });
            spectator.flushEffects();

            expect(getGraphQLPageSpy).toHaveBeenCalled();
            expect(getSpy).not.toHaveBeenCalled();
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should pass merged params to getGraphQLPage via $requestWithParams', () => {
            store.setCustomClient({ query: 'query', variables: { depth: '1' } });
            store.pageUpdateParams({ language_id: '3' });

            store.pageLoad({});
            spectator.flushEffects();

            expect(getGraphQLPageSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    query: 'query',
                    variables: expect.objectContaining({
                        depth: '1',
                        // $requestWithParams merges pageParams into variables (friendly keys)
                        languageId: '3',
                        url: 'test-url'
                    })
                })
            );
        });

        it('should set page asset with content when GraphQL response includes content', () => {
            store.setCustomClient(graphqlRequest);

            store.pageLoad({});
            spectator.flushEffects();

            const response = store.pageAssetResponse();
            expect(response?.pageAsset).toEqual(MOCK_RESPONSE_HEADLESS);
            expect(response?.content).toEqual({ source: 'graphql' });
        });
    });

    describe('pageReload – fetch vs GraphQL', () => {
        it('should call get when reloading without GraphQL metadata', () => {
            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });
            jest.clearAllMocks();

            store.pageReload();
            spectator.flushEffects();

            expect(getSpy).toHaveBeenCalled();
            expect(getGraphQLPageSpy).not.toHaveBeenCalled();
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should call getGraphQLPage when reloading with GraphQL metadata', () => {
            store.setCustomClient(graphqlRequest);
            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });
            jest.clearAllMocks();

            store.pageReload();
            spectator.flushEffects();

            expect(getGraphQLPageSpy).toHaveBeenCalled();
            expect(getSpy).not.toHaveBeenCalled();
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });
    });
});
