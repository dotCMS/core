import { describe, expect, it } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { UVE_MODE } from '@dotcms/types';

import { withPage } from './withPage';

import { DotPageApiParams, DotPageApiService } from '../../../services/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';
import { withFlags } from '../flags/withFlags';

const initialState = createInitialUVEState();

/** patchState type assertion - Spectator store type doesn't satisfy WritableStateSource but runtime works */
const patchStoreState = (store: unknown, state: Partial<UVEState>) => {
    patchState(store as Parameters<typeof patchState>[0], state);
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withFlags([]),
    withPage()
);

describe('withPage', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of(false))
            }),
            {
                provide: DotPageApiService,
                useValue: {
                    get: () => of({}),
                    getClientPage: () => of({}),
                    getGraphQLPage: () => of({}),
                    save: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should have initial state', () => {
        expect(store.isClientReady()).toBe(false);
        expect(store.requestMetadata()).toEqual(null);
        expect(store.pageAssetResponse()).toEqual(null);
    });

    describe('client / request metadata', () => {
        it('should set the client ready status', () => {
            store.setIsClientReady(true);
            expect(store.isClientReady()).toBe(true);
        });

        it('should set request metadata via setCustomClient', () => {
            const graphql = {
                query: 'test',
                variables: { depth: '1' }
            };
            store.setCustomClient(graphql);
            expect(store.requestMetadata()).toEqual(graphql);
        });

        /**
         * resetClientConfiguration clears page asset and ready state but intentionally
         * preserves requestMetadata so headless/GraphQL clients keep their query across
         * pageLoad cycles (see withPageApi / shell behavior).
         */
        it('should reset page asset and ready state but preserve requestMetadata', () => {
            const graphql = {
                query: 'test',
                variables: {}
            };
            store.setCustomClient(graphql);
            store.setIsClientReady(true);
            patchStoreState(store, {
                pageParams: {
                    url: '/x',
                    language_id: '1',
                    [PERSONA_KEY]: 'p',
                    mode: UVE_MODE.EDIT
                }
            });
            store.setPageAsset({
                pageAsset: { page: { identifier: 'p1' } } as Parameters<
                    typeof store.setPageAsset
                >[0]['pageAsset']
            });

            store.resetClientConfiguration();

            expect(store.requestMetadata()).toEqual(graphql);
            expect(store.isClientReady()).toBe(false);
            expect(store.pageAssetResponse()).toBeNull();
        });
    });

    describe('$requestWithParams', () => {
        it('should return null when requestMetadata is null', () => {
            expect(store.$requestWithParams()).toBeNull();
        });

        it('should return null after reset when graphql was never set', () => {
            store.resetClientConfiguration();
            expect(store.$requestWithParams()).toBeNull();
        });

        it('should merge graphql variables with page params', () => {
            const graphql = {
                query: 'test query',
                variables: { depth: '1' }
            };
            const pageParams: DotPageApiParams = {
                url: '/test-url',
                mode: UVE_MODE.EDIT,
                language_id: '1',
                variantName: 'DEFAULT',
                [PERSONA_KEY]: 'persona-id-123'
            };
            patchStoreState(store, { pageParams });
            store.setCustomClient(graphql);

            expect(store.$requestWithParams()).toEqual({
                query: 'test query',
                variables: {
                    depth: '1',
                    url: '/test-url',
                    mode: UVE_MODE.EDIT,
                    languageId: '1',
                    variantName: 'DEFAULT',
                    personaId: 'persona-id-123'
                }
            });
        });

        it('should preserve existing graphql variables when merging with page params', () => {
            const graphql = {
                query: 'test query',
                variables: { depth: '2', customVar: 'custom-value' }
            };
            const pageParams: DotPageApiParams = {
                url: '/another-url',
                mode: UVE_MODE.PREVIEW,
                language_id: '2',
                variantName: 'VARIANT_A',
                [PERSONA_KEY]: 'persona-id-456'
            };
            patchStoreState(store, { pageParams });
            store.setCustomClient(graphql);

            expect(store.$requestWithParams()).toEqual({
                query: 'test query',
                variables: {
                    depth: '2',
                    customVar: 'custom-value',
                    url: '/another-url',
                    mode: UVE_MODE.PREVIEW,
                    languageId: '2',
                    variantName: 'VARIANT_A',
                    personaId: 'persona-id-456'
                }
            });
        });

        it('should handle missing optional page params', () => {
            const graphql = {
                query: 'test query',
                variables: { depth: '1' }
            };
            const pageParams: DotPageApiParams = {
                url: '/test-url',
                language_id: '1',
                [PERSONA_KEY]: 'persona-id-123'
            };
            patchStoreState(store, { pageParams });
            store.setCustomClient(graphql);

            expect(store.$requestWithParams()).toEqual({
                query: 'test query',
                variables: {
                    depth: '1',
                    url: '/test-url',
                    mode: undefined,
                    languageId: '1',
                    variantName: undefined,
                    personaId: 'persona-id-123'
                }
            });
        });
    });
});
