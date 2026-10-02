import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';

import { signal, WritableSignal } from '@angular/core';

import { Confirmation, ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationShellComponent } from './dot-configuration-shell.component';

import { DotConfigurationActionBarComponent } from '../components/dot-configuration-action-bar/dot-configuration-action-bar.component';
import { DOT_CONFIGURATION_CONFIRM_KEY } from '../guards/dot-configuration-unsaved-changes.guard';
import {
    DotConfigurationDraft,
    DotConfigurationSection,
    toDraft
} from '../store/dot-configuration.mappers';
import { DotConfigurationSaveState, DotConfigurationStore } from '../store/dot-configuration.store';
import { createFakeCompanyConfiguration } from '../testing/fake-company-configuration';

interface StoreSignals {
    status: WritableSignal<ComponentStatus>;
    draft: WritableSignal<DotConfigurationDraft | null>;
    dirty: WritableSignal<boolean>;
    failedSection: WritableSignal<DotConfigurationSection | null>;
}

describe('DotConfigurationShellComponent', () => {
    let spectator: Spectator<DotConfigurationShellComponent>;

    const signals: StoreSignals = {
        status: signal<ComponentStatus>(ComponentStatus.LOADED),
        draft: signal<DotConfigurationDraft | null>(null),
        dirty: signal(false),
        failedSection: signal<DotConfigurationSection | null>(null)
    };

    const createComponent = createComponentFactory({
        component: DotConfigurationShellComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService({}) }],
        componentProviders: [
            ConfirmationService,
            mockProvider(DotConfigurationStore, {
                ...signals,
                saveState: signal(DotConfigurationSaveState.SAVED),
                canSave: signal(false),
                load: vi.fn(),
                save: vi.fn(),
                discard: vi.fn(),
                restoreDefaults: vi.fn()
            })
        ],
        detectChanges: false
    });

    const store = () => spectator.inject(DotConfigurationStore, true);

    beforeEach(() => {
        signals.status.set(ComponentStatus.LOADED);
        signals.draft.set(toDraft(createFakeCompanyConfiguration()));
        signals.dirty.set(false);
        signals.failedSection.set(null);
        spectator = createComponent();
        spectator.detectChanges();
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('states', () => {
        it('shows placeholders while the configuration loads', () => {
            signals.status.set(ComponentStatus.LOADING);
            signals.draft.set(null);
            spectator.detectChanges();

            expect(spectator.query(byTestId('configuration-loading'))).toExist();
            expect(spectator.query(byTestId('configuration-page'))).not.toExist();
        });

        it('shows an error with a retry button when loading fails', () => {
            signals.status.set(ComponentStatus.ERROR);
            spectator.detectChanges();

            expect(spectator.query(byTestId('configuration-error'))).toExist();
            spectator.click(
                spectator
                    .query(byTestId('configuration-retry-btn'))
                    ?.querySelector('button') as HTMLButtonElement
            );

            expect(store().load).toHaveBeenCalled();
        });

        it('shows the page and the action bar once loaded', () => {
            expect(spectator.query(byTestId('configuration-page'))).toExist();
            expect(spectator.query(DotConfigurationActionBarComponent)).toBeTruthy();
        });
    });

    describe('action bar', () => {
        it('saves through the store', () => {
            spectator.triggerEventHandler(DotConfigurationActionBarComponent, 'save', undefined);

            expect(store().save).toHaveBeenCalled();
        });

        it('reverts the edits on cancel', () => {
            spectator.triggerEventHandler(DotConfigurationActionBarComponent, 'discard', undefined);

            expect(store().discard).toHaveBeenCalled();
        });

        it('passes the failed section name to the action bar', () => {
            signals.failedSection.set('locale');
            spectator.detectChanges();

            expect(spectator.query(DotConfigurationActionBarComponent)?.failedSectionKey()).toBe(
                'configuration.section.locale'
            );
        });

        it('restores the defaults only after the user confirms', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            const confirm = vi
                .spyOn(confirmationService, 'confirm')
                .mockImplementation((confirmation: Confirmation) => {
                    confirmation.accept?.();

                    return confirmationService;
                });

            spectator.triggerEventHandler(
                DotConfigurationActionBarComponent,
                'restoreDefaults',
                undefined
            );

            expect(confirm).toHaveBeenCalledWith(
                expect.objectContaining({ key: DOT_CONFIGURATION_CONFIRM_KEY })
            );
            expect(store().restoreDefaults).toHaveBeenCalled();
        });

        it('does not restore the defaults when the user does not confirm', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            vi.spyOn(confirmationService, 'confirm').mockReturnValue(confirmationService);

            spectator.triggerEventHandler(
                DotConfigurationActionBarComponent,
                'restoreDefaults',
                undefined
            );

            expect(store().restoreDefaults).not.toHaveBeenCalled();
        });
    });

    describe('leaving the page', () => {
        it('asks the browser to confirm closing the tab with unsaved edits', () => {
            signals.dirty.set(true);
            const event = new Event('beforeunload', { cancelable: true });

            window.dispatchEvent(event);

            expect(event.defaultPrevented).toBe(true);
        });

        it('lets the tab close when everything is saved', () => {
            const event = new Event('beforeunload', { cancelable: true });

            window.dispatchEvent(event);

            expect(event.defaultPrevented).toBe(false);
        });
    });
});
