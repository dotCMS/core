import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { NEVER, of, throwError } from 'rxjs';

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import {
    DotAiConfigService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import { DotAiProviderMetadata } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAiConfigDetailComponent } from './dot-ai-config-detail.component';

describe('DotAiConfigDetailComponent', () => {
    let spectator: Spectator<DotAiConfigDetailComponent>;

    const providers: DotAiProviderMetadata[] = [];

    // Held here rather than inline in `mockProvider`, because a factory's stub object is
    // built once: the failure suite's `getConfig` override would otherwise leak into every
    // suite that ran after it.
    const getProviders = jest.fn();
    const getConfig = jest.fn();

    const createComponent = createComponentFactory({
        component: DotAiConfigDetailComponent,
        providers: [
            // Happy-path stubs live on the factory; only the failure suite overrides
            // getConfig, which is the reason the two suites exist.
            mockProvider(DotAiConfigService, { getProviders, getConfig, saveConfig: jest.fn() }),
            mockProvider(DotRouterService),
            mockProvider(DotMessageDisplayService),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: { paramMap: { get: () => 'site-identifier' } },
                    data: of({ data: null })
                }
            }
        ],
        schemas: [NO_ERRORS_SCHEMA],
        detectChanges: false
    });

    beforeEach(() => {
        jest.clearAllMocks();
        // Happy path by default; only the failure suite overrides getConfig, which is the
        // reason the two suites exist.
        getProviders.mockReturnValue(of(providers));
        getConfig.mockReturnValue(
            of({ providerConfig: JSON.stringify({ settings: { textPrompt: 'hi' } }) } as never)
        );
    });

    describe('when the initial load fails', () => {
        beforeEach(() => {
            spectator = createComponent();
            spectator
                .inject(DotAiConfigService)
                .getConfig.mockReturnValue(throwError(() => new Error('network error')));

            spectator.component.ngOnInit();
        });

        it('sets loadFailed instead of leaving the form open on empty/default data', () => {
            expect(spectator.component.loadFailed()).toBe(true);
            expect(spectator.component.loading()).toBe(false);
        });

        it('surfaces the load error to the user', () => {
            expect(spectator.inject(DotMessageDisplayService).push).toHaveBeenCalled();
        });

        it('refuses to save over a config that never actually loaded', () => {
            spectator.component.save();

            expect(spectator.inject(DotAiConfigService).saveConfig).not.toHaveBeenCalled();
        });
    });

    describe('when the initial load succeeds', () => {
        beforeEach(() => {
            spectator = createComponent();
            spectator.component.ngOnInit();
        });

        it('does not mark the load as failed', () => {
            expect(spectator.component.loadFailed()).toBe(false);
            expect(spectator.component.loading()).toBe(false);
        });
    });

    // The specs above assert signal values against an unrendered component, so they stay green
    // through any template change. These render the two states instead, so the loading and error
    // branches are checked against the markup that actually ships.
    describe('load-state rendering', () => {
        it('renders the spinner and no error while the config is still loading', () => {
            spectator = createComponent();
            spectator.inject(DotAiConfigService).getProviders.mockReturnValue(NEVER);
            spectator.inject(DotAiConfigService).getConfig.mockReturnValue(NEVER);

            spectator.detectChanges();

            expect(spectator.query(byTestId('loading-indicator'))).toExist();
            expect(spectator.query(byTestId('error-message'))).not.toExist();
        });

        it('renders the error message and no spinner when the load fails', () => {
            spectator = createComponent();
            spectator.inject(DotAiConfigService).getProviders.mockReturnValue(of(providers));
            spectator
                .inject(DotAiConfigService)
                .getConfig.mockReturnValue(throwError(() => new Error('network error')));

            spectator.detectChanges();

            expect(spectator.query(byTestId('error-message'))).toExist();
            expect(spectator.query(byTestId('loading-indicator'))).not.toExist();
        });
    });
});
