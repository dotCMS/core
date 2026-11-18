import { Spectator, createComponentFactory, mockProvider } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { SHARED_ASSETS_DISABLED_VALUE, SHARED_ASSETS_FILTER_KEY } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveScopeBarComponent } from './dot-content-drive-scope-bar.component';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

describe('DotContentDriveScopeBarComponent', () => {
    let spectator: Spectator<DotContentDriveScopeBarComponent>;

    const filtersSignal = signal<Record<string, string>>({});

    const createComponent = createComponentFactory({
        component: DotContentDriveScopeBarComponent,
        componentProviders: [
            mockProvider(DotContentDriveStore, {
                filters: filtersSignal,
                getFilterValue: vi.fn((key: string) => filtersSignal()[key]),
                patchFilters: vi.fn()
            })
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.scope-bar.all-site-content.excluded':
                        'All Files in site (System Host shared files excluded)',
                    'content-drive.scope-bar.all-site-content.included':
                        'All Files in site (System Host shared files included)',
                    'content-drive.scope-bar.include-system-host': 'Include System Host:',
                    'content-drive.scope-bar.off': 'Off'
                })
            }
        ]
    });

    beforeEach(() => {
        filtersSignal.set({});
        spectator = createComponent();
    });

    const summary = () => spectator.query('[data-testid="scope-bar-summary"]')?.textContent ?? '';

    it('should say shared files are included while the toggle is on', () => {
        // On is the default everywhere: the endpoint's form defaults the flag to true and an
        // absent key reads as on, so the sentence has to agree with that rather than with silence.
        expect(summary()).toContain('included');
    });

    it('should say shared files are excluded once the toggle is off', () => {
        filtersSignal.set({ [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE });
        spectator.detectChanges();

        expect(summary()).toContain('excluded');
    });

    it('should write the filter when the toggle is flipped', () => {
        // The control the toolbar chip used to be. It writes the state either way rather than
        // clearing the key, so the applied filter is spelled out in the URL instead of implied by
        // an absence that reads as on.
        const store = spectator.inject(DotContentDriveStore, true);

        spectator.click('[data-testid="scope-bar-toggle"] input');

        expect(store.patchFilters).toHaveBeenCalledWith({
            [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE
        });
    });
});
