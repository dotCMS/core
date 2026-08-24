import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import {
    DotAiService,
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

    const createComponent = createComponentFactory({
        component: DotAiConfigDetailComponent,
        providers: [
            mockProvider(DotAiService),
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

    describe('when the initial load fails', () => {
        beforeEach(() => {
            spectator = createComponent();
            spectator.inject(DotAiService).getProviders.mockReturnValue(of(providers));
            spectator
                .inject(DotAiService)
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

            expect(spectator.inject(DotAiService).saveConfig).not.toHaveBeenCalled();
        });
    });

    describe('when the initial load succeeds', () => {
        beforeEach(() => {
            spectator = createComponent();
            spectator.inject(DotAiService).getProviders.mockReturnValue(of(providers));
            spectator.inject(DotAiService).getConfig.mockReturnValue(
                of({
                    providerConfig: JSON.stringify({ settings: { textPrompt: 'hi' } })
                } as never)
            );

            spectator.component.ngOnInit();
        });

        it('does not mark the load as failed', () => {
            expect(spectator.component.loadFailed()).toBe(false);
            expect(spectator.component.loading()).toBe(false);
        });
    });
});
