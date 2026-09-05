import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DotAiConfigService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAiResolvedConfig } from '@dotcms/dotcms-models';

import { withAiConfig } from './with-ai-config.feature';

import { DOT_AI_INITIAL_STATE, DotAiPortletState } from '../../models/dot-ai-portlet.models';

const resolved = (overrides: Partial<DotAiResolvedConfig> = {}): DotAiResolvedConfig => ({
    configHost: 'demo.dotcms.com (falls back to system host)',
    settings: { embeddingsSearchThreshold: '0.4' },
    providerConfig: { chat: { model: 'a,b' } },
    chatModels: ['a', 'b'],
    isConfigured: true,
    redactionFailed: false,
    ...overrides
});

const TestStore = signalStore(
    { providedIn: 'root' },
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withAiConfig()
);

describe('withAiConfig', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;

    const createService = createServiceFactory({
        service: TestStore,
        providers: [mockProvider(DotAiConfigService), mockProvider(DotHttpErrorManagerService)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should seed the threshold default from the resolved settings', () => {
        spectator.inject(DotAiConfigService).getResolvedConfig = jest
            .fn()
            .mockReturnValue(of(resolved()));

        store.loadConfig();

        expect(store.settingsThreshold()).toBe(0.4);
    });

    it('should fall back to the documented default when the setting is missing', () => {
        spectator.inject(DotAiConfigService).getResolvedConfig = jest
            .fn()
            .mockReturnValue(of(resolved({ settings: {} })));

        store.loadConfig();

        expect(store.settingsThreshold()).toBe(0.25);
    });

    it('should expose the chat models and default to the first', () => {
        spectator.inject(DotAiConfigService).getResolvedConfig = jest
            .fn()
            .mockReturnValue(of(resolved({ chatModels: ['first', 'second'] })));

        store.loadConfig();

        expect(store.chatModels()).toEqual(['first', 'second']);
        expect(store.settingsModel()).toBe('first');
    });

    it('should be unconfigured when providerConfig is absent (FR-047)', () => {
        spectator.inject(DotAiConfigService).getResolvedConfig = jest.fn().mockReturnValue(
            of(
                resolved({
                    providerConfig: null,
                    chatModels: [],
                    isConfigured: false
                })
            )
        );

        store.loadConfig();

        expect(store.isConfigured()).toBe(false);
    });

    it('should route a load failure through the error manager', () => {
        const error = new HttpErrorResponse({ status: 500 });
        spectator.inject(DotAiConfigService).getResolvedConfig = jest
            .fn()
            .mockReturnValue(throwError(() => error));

        store.loadConfig();

        expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
    });
});
