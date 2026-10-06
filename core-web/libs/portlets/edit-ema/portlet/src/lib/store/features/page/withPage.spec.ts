import { patchState, signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { ActivatedRoute, Router } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { withFlags } from '@dotcms/store';
import { UVE_MODE } from '@dotcms/types';

import { withPage } from './withPage';

import {
    DotPageApiParams,
    DotPageApiService
} from '../../../services/dot-page-api/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';

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
                getFeatureFlags: vi.fn().mockReturnValue(of(false))
            }),
            {
                provide: DotPageApiService,
                useValue: {
                    get: () => of({}),
                    getClientPage: () => of({}),
                    getGraphQLPage: () => of({}),
                    save: vi.fn()
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
         * pageLoad cycles. Cross-page staleness is handled in withPageApi's pageLoad,
         * which drops the stored request when it belongs to another page.
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

        it('should drop only the stored client request via resetRequestMetadata', () => {
            const graphql = {
                query: 'test',
                variables: {}
            };
            store.setCustomClient(graphql);
            store.setIsClientReady(true);
            store.setPageAsset({
                pageAsset: { page: { identifier: 'p1' } } as Parameters<
                    typeof store.setPageAsset
                >[0]['pageAsset']
            });

            store.resetRequestMetadata();

            expect(store.requestMetadata()).toBeNull();
            // Everything else stays untouched
            expect(store.isClientReady()).toBe(true);
            expect(store.pageAssetResponse()).not.toBeNull();
        });
    });

    describe('pageAssetResponse.source', () => {
        const mockPageAsset = { page: { identifier: 'p1' } } as Parameters<
            typeof store.setPageAsset
        >[0]['pageAsset'];

        it('should set source to rest when the payload specifies it', () => {
            store.setPageAsset({ pageAsset: mockPageAsset, source: 'rest' });

            expect(store.pageAssetResponse()?.source).toBe('rest');
        });

        it('should set source to graphql when the payload specifies it', () => {
            store.setPageAsset({ pageAsset: mockPageAsset, source: 'graphql' });

            expect(store.pageAssetResponse()?.source).toBe('graphql');
        });

        it('should inherit the current source when the payload omits it', () => {
            store.setPageAsset({ pageAsset: mockPageAsset, source: 'graphql' });

            // A mutate-in-place update (e.g. an optimistic edit or layout change) that
            // does not pass `source` must not silently reclassify GraphQL-sourced data as REST.
            store.setPageAsset({
                pageAsset: { ...mockPageAsset, layout: {} } as Parameters<
                    typeof store.setPageAsset
                >[0]['pageAsset']
            });

            expect(store.pageAssetResponse()?.source).toBe('graphql');
        });

        it('should leave source undefined (not invent a value) when the payload omits it and there is no current response to inherit from', () => {
            store.setPageAsset({ pageAsset: mockPageAsset });

            expect(store.pageAssetResponse()?.source).toBeUndefined();
        });

        it('should clear source along with the rest of pageAssetResponse on resetClientConfiguration', () => {
            store.setPageAsset({ pageAsset: mockPageAsset, source: 'graphql' });

            store.resetClientConfiguration();

            expect(store.pageAssetResponse()).toBeNull();
        });
    });

    describe('pageReloadPending', () => {
        // Bounds the headless reload effect to one UVE_RELOAD_PAGE request per non-resolved
        // streak — see dotCMS/core#37097's post-merge QA regression (endless client reload
        // loop). This suite covers the store-level state transitions the effect relies on;
        // edit-ema-editor.component.spec.ts covers the effect's own bounded-send behavior.
        const mockPageAsset = { page: { identifier: 'p1' } } as Parameters<
            typeof store.setPageAsset
        >[0]['pageAsset'];

        it('should default to false', () => {
            expect(store.pageReloadPending()).toBe(false);
        });

        it('should be settable via setPageReloadPending', () => {
            store.setPageReloadPending(true);

            expect(store.pageReloadPending()).toBe(true);
        });

        it('should clear when setPageAsset resolves the asset to graphql-sourced', () => {
            store.setPageReloadPending(true);

            store.setPageAsset({ pageAsset: mockPageAsset, source: 'graphql' });

            expect(store.pageReloadPending()).toBe(false);
        });

        it('should NOT clear when setPageAsset sets a rest-sourced asset', () => {
            store.setPageReloadPending(true);

            store.setPageAsset({ pageAsset: mockPageAsset, source: 'rest' });

            expect(store.pageReloadPending()).toBe(true);
        });

        it('should NOT clear when setPageAsset omits source (mutate-in-place)', () => {
            store.setPageReloadPending(true);

            store.setPageAsset({ pageAsset: mockPageAsset });

            expect(store.pageReloadPending()).toBe(true);
        });

        it('should clear on markPageLoading, so a new navigation gets its own fresh reload request', () => {
            store.setPageReloadPending(true);

            store.markPageLoading();

            expect(store.pageReloadPending()).toBe(false);
        });

        it('should clear on resetClientConfiguration', () => {
            store.setPageReloadPending(true);

            store.resetClientConfiguration();

            expect(store.pageReloadPending()).toBe(false);
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

    describe('pageTranslateProps', () => {
        it('should return undefined currentLanguage when pageAsset is not set', () => {
            expect(store.pageTranslateProps().currentLanguage).toBeUndefined();
        });

        it('should reflect translated status from pageLanguages for the current language', () => {
            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });
            patchStoreState(store, {
                pageLanguages: [
                    { id: 1, language: 'English', languageCode: 'en', translated: true }
                ] as DotLanguage[]
            });

            expect(store.pageTranslateProps().currentLanguage?.translated).toBe(true);
        });

        it('should recompute when pageLanguages changes independently of pageAsset', () => {
            // Regression for #35647: pageTranslateProps was previously wrapped with
            // untracked(() => store.pageLanguages()), making it blind to language changes.
            // Removing untracked() makes the computed reactive on both signals so any
            // call site that updates pageLanguages is automatically reflected here.
            store.setPageAsset({ pageAsset: MOCK_RESPONSE_HEADLESS });
            patchStoreState(store, {
                pageLanguages: [
                    { id: 1, language: 'English', languageCode: 'en', translated: false }
                ] as DotLanguage[]
            });
            expect(store.pageTranslateProps().currentLanguage?.translated).toBe(false);

            patchStoreState(store, {
                pageLanguages: [
                    { id: 1, language: 'English', languageCode: 'en', translated: true }
                ] as DotLanguage[]
            });
            expect(store.pageTranslateProps().currentLanguage?.translated).toBe(true);
        });
    });
});
