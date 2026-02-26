import { describe } from '@jest/globals';
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

describe('UVEStore', () => {
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
        expect(store.isClientReady()).toBeFalsy();
        expect(store.requestMetadata()).toEqual(null);
        expect(store.pageAssetResponse()).toEqual(null);
        expect(store.isClientReady()).toBe(false);
        expect(store.legacyResponseFormat()).toBe(false);
    });

    describe('withMethods', () => {
        it('should set the client ready status', () => {
            store.setIsClientReady(true);

            expect(store.isClientReady()).toBe(true);
        });

        describe('setClientConfiguration', () => {
            it('should set the client configuration', () => {
                const graphql = {
                    query: 'test',
                    variables: {
                        depth: '1'
                    }
                };

                store.setCustomClient(graphql, true);

                expect(store.requestMetadata()).toEqual(graphql);
            });
        });

        it('should reset the client configuration', () => {
            const graphql = {
                query: 'test',
                variables: null
            };

            store.setCustomClient(graphql, true);
            store.resetClientConfiguration();

            expect(store.requestMetadata()).toEqual(null);
        });
    });

    describe('$requestWithParams', () => {
        it('should return null when graphql is null', () => {
            expect(store.$requestWithParams()).toBeNull();
        });

        it('should return null when graphql is not set', () => {
            store.resetClientConfiguration();
            expect(store.$requestWithParams()).toBeNull();
        });

        it('should merge graphql variables with page params', () => {
            const graphql = {
                query: 'test query',
                variables: {
                    depth: '1'
                }
            };

            const pageParams: DotPageApiParams = {
                url: '/test-url',
                mode: UVE_MODE.EDIT,
                language_id: '1',
                variantName: 'DEFAULT',
                [PERSONA_KEY]: 'persona-id-123'
            };

            patchStoreState(store, { pageParams });
            store.setCustomClient(graphql, false);

            const result = store.$requestWithParams();

            expect(result).toEqual({
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
                variables: {
                    depth: '2',
                    customVar: 'custom-value'
                }
            };

            const pageParams: DotPageApiParams = {
                url: '/another-url',
                mode: UVE_MODE.PREVIEW,
                language_id: '2',
                variantName: 'VARIANT_A',
                [PERSONA_KEY]: 'persona-id-456'
            };

            patchStoreState(store, { pageParams });
            store.setCustomClient(graphql, false);

            const result = store.$requestWithParams();

            expect(result).toEqual({
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
                variables: {
                    depth: '1'
                }
            };

            const pageParams: DotPageApiParams = {
                url: '/test-url',
                language_id: '1',
                [PERSONA_KEY]: 'persona-id-123'
                // mode and variantName are missing
            };

            patchStoreState(store, { pageParams });
            store.setCustomClient(graphql, false);

            const result = store.$requestWithParams();

            expect(result).toEqual({
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
