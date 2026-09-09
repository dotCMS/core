import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';

import {
    DotAiCompletionsStreamService,
    DotAiConfigService,
    DotAiContentService,
    DotAiEmbeddingsService,
    DotAiSearchService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { DotAiResolvedConfig } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotAiStore } from './dot-ai.store';

const resolved = (overrides: Partial<DotAiResolvedConfig> = {}): DotAiResolvedConfig => ({
    configHost: 'demo.dotcms.com',
    settings: {},
    providerConfig: { chat: { model: 'a,b' } },
    chatModels: ['a', 'b'],
    isConfigured: true,
    redactionFailed: false,
    ...overrides
});

/**
 * The composed store, exercised only for what the slices cannot cover on their own: the
 * wiring in `onInit`. Everything else is tested against its own feature.
 */
describe('DotAiStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotAiStore>>;
    let currentSiteId: ReturnType<typeof signal<string | null>>;

    // Held outside the factory: a factory's stub object is built once, so a `jest.fn()`
    // written inline is shared by every test in the file and its call counts accumulate.
    const getResolvedConfig = jest.fn();
    const getIndexes = jest.fn();

    const createService = createServiceFactory({
        service: DotAiStore,
        providers: [
            mockProvider(DotAiConfigService, { getResolvedConfig }),
            mockProvider(DotAiEmbeddingsService, { getIndexes }),
            mockProvider(DotAiSearchService),
            mockProvider(DotAiContentService),
            mockProvider(DotAiCompletionsStreamService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(GlobalStore, {
                // A getter, so the store reads the live signal rather than a snapshot taken
                // when the provider was built. Same shape as dot-tags-list.store.spec.
                get currentSiteId() {
                    return currentSiteId;
                }
            })
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        getResolvedConfig.mockReturnValue(of(resolved()));
        getIndexes.mockReturnValue(of([]));
        currentSiteId = signal<string | null>('site-1');
        spectator = createService();
        // onInit's effect is what loads the config; nothing loads without this.
        spectator.flushEffects();
    });

    describe('the header site selector (per-site config)', () => {
        it('should load the config for the current site on init', () => {
            expect(getResolvedConfig).toHaveBeenCalledWith('site-1');
        });

        it('should reload the config when the site changes', () => {
            currentSiteId.set('site-2');
            spectator.flushEffects();

            expect(getResolvedConfig).toHaveBeenCalledWith('site-2');
            expect(getResolvedConfig).toHaveBeenCalledTimes(2);
        });

        it('should pick up a provider that only the new site has', () => {
            // The point of reloading: the app is configured per site, so the model list and
            // whether dotAI is configured at all both travel with the site.
            currentSiteId.set('site-2');
            getResolvedConfig.mockReturnValue(
                of(resolved({ chatModels: ['claude'], isConfigured: true }))
            );
            spectator.flushEffects();

            expect(spectator.service.chatModels()).toEqual(['claude']);
            expect(spectator.service.settingsModel()).toBe('claude');
        });

        it('should send no site when the current one is unknown', () => {
            // `currentSiteId` is null until GlobalStore resolves it. Omitting the parameter
            // is what makes the endpoint fall back to the session's own site, so that load
            // is correct rather than skipped.
            currentSiteId.set(null);
            spectator.flushEffects();

            expect(getResolvedConfig).toHaveBeenLastCalledWith(undefined);
        });

        it('should not re-fetch when the site is set to the same value', () => {
            // A computed only notifies on a real change, so a no-op switch must not cost a
            // request.
            currentSiteId.set('site-1');
            spectator.flushEffects();

            expect(getResolvedConfig).toHaveBeenCalledTimes(1);
        });
    });

    describe('the index list', () => {
        it('should load once and not follow the site', () => {
            // `indexCount` takes no site and the embeddings live in one global table.
            currentSiteId.set('site-2');
            spectator.flushEffects();

            expect(getIndexes).toHaveBeenCalledTimes(1);
        });
    });
});
