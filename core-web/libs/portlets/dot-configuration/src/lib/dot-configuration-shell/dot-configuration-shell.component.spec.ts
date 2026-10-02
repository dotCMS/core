import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { MockComponent, ngMocks } from 'ng-mocks';
import { Subject } from 'rxjs';

import { signal, Type, WritableSignal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Confirmation, ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationShellComponent } from './dot-configuration-shell.component';

import { DotConfigurationActionBarComponent } from '../components/dot-configuration-action-bar/dot-configuration-action-bar.component';
import { DotConfigurationBackgroundDialogComponent } from '../dialogs/dot-configuration-background-dialog/dot-configuration-background-dialog.component';
import { DotConfigurationLicenseDialogComponent } from '../dialogs/dot-configuration-license-dialog/dot-configuration-license-dialog.component';
import { DotConfigurationLogoDialogComponent } from '../dialogs/dot-configuration-logo-dialog/dot-configuration-logo-dialog.component';
import { DotConfigurationRegenerateKeyDialogComponent } from '../dialogs/dot-configuration-regenerate-key-dialog/dot-configuration-regenerate-key-dialog.component';
import { DotConfigurationTestMailDialogComponent } from '../dialogs/dot-configuration-test-mail-dialog/dot-configuration-test-mail-dialog.component';
import { DOT_CONFIGURATION_CONFIRM_KEY } from '../guards/dot-configuration-unsaved-changes.guard';
import { DotConfigurationBrandingComponent } from '../sections/dot-configuration-branding/dot-configuration-branding.component';
import { DotConfigurationLocaleComponent } from '../sections/dot-configuration-locale/dot-configuration-locale.component';
import { DotConfigurationOutboundComponent } from '../sections/dot-configuration-outbound/dot-configuration-outbound.component';
import { DotConfigurationSecurityComponent } from '../sections/dot-configuration-security/dot-configuration-security.component';
import {
    DotConfigurationDraft,
    DotConfigurationSection,
    toDraft
} from '../store/dot-configuration.mappers';
import { DotConfigurationSaveState, DotConfigurationStore } from '../store/dot-configuration.store';
import { createFakeCompanyConfiguration } from '../testing/fake-company-configuration';

const SECTIONS: Type<unknown>[] = [
    DotConfigurationBrandingComponent,
    DotConfigurationLocaleComponent,
    DotConfigurationOutboundComponent,
    DotConfigurationSecurityComponent
];

interface StoreSignals {
    status: WritableSignal<ComponentStatus>;
    draft: WritableSignal<DotConfigurationDraft | null>;
    dirty: WritableSignal<boolean>;
    failedSection: WritableSignal<DotConfigurationSection | null>;
}

describe('DotConfigurationShellComponent', () => {
    let dialogClose: Subject<unknown>;
    let spectator: Spectator<DotConfigurationShellComponent>;

    const signals: StoreSignals = {
        status: signal<ComponentStatus>(ComponentStatus.LOADED),
        draft: signal<DotConfigurationDraft | null>(null),
        dirty: signal(false),
        failedSection: signal<DotConfigurationSection | null>(null)
    };

    const createComponent = createComponentFactory({
        component: DotConfigurationShellComponent,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            mockProvider(DotMessageDisplayService, { push: vi.fn() }),
            { provide: ActivatedRoute, useValue: { snapshot: { data: { isEnterprise: true } } } }
        ],
        overrideComponents: [
            [
                DotConfigurationShellComponent,
                {
                    remove: { imports: SECTIONS },
                    add: { imports: SECTIONS.map((section) => MockComponent(section)) }
                }
            ]
        ],
        componentProviders: [
            ConfirmationService,
            mockProvider(DialogService, {
                open: vi.fn(() => {
                    dialogClose = new Subject<unknown>();

                    return { onClose: dialogClose.asObservable() };
                })
            }),
            mockProvider(DotConfigurationStore, {
                ...signals,
                saveState: signal(DotConfigurationSaveState.SAVED),
                canSave: signal(false),
                load: vi.fn(),
                save: vi.fn(),
                discard: vi.fn(),
                restoreDefaults: vi.fn(),
                patchBranding: vi.fn(),
                regenerateKey: vi.fn()
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

        it('renders the four section cards in order, then the license link', () => {
            const sections = spectator.query(byTestId('configuration-sections'))?.children ?? [];

            expect(Array.from(sections).map((section) => section.tagName.toLowerCase())).toEqual([
                'dot-configuration-branding',
                'dot-configuration-locale',
                'dot-configuration-outbound',
                'dot-configuration-security',
                'p-button'
            ]);
        });

        it('tells the branding card whether the license is Enterprise', () => {
            const branding = ngMocks.find(
                spectator.debugElement,
                DotConfigurationBrandingComponent
            );

            expect(ngMocks.input(branding, 'isEnterprise')).toBe(true);
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

    describe('dialogs', () => {
        const dialogService = () => spectator.inject(DialogService, true);

        const closeDialogWith = (value: unknown) => {
            dialogClose.next(value);
            dialogClose.complete();
        };

        it('opens dialogs closable by the X and by Escape', () => {
            spectator.triggerEventHandler(
                DotConfigurationBrandingComponent,
                'changeBackground',
                undefined
            );

            expect(dialogService().open).toHaveBeenCalledWith(
                DotConfigurationBackgroundDialogComponent,
                expect.objectContaining({ closable: true, closeOnEscape: true, width: '700px' })
            );
        });

        it('puts the chosen background in the form', () => {
            spectator.triggerEventHandler(
                DotConfigurationBrandingComponent,
                'changeBackground',
                undefined
            );

            closeDialogWith('');

            expect(store().patchBranding).toHaveBeenCalledWith({ backgroundImage: '' });
        });

        it('leaves the background alone when the dialog is cancelled', () => {
            spectator.triggerEventHandler(
                DotConfigurationBrandingComponent,
                'changeBackground',
                undefined
            );

            closeDialogWith(undefined);

            expect(store().patchBranding).not.toHaveBeenCalled();
        });

        it('puts the chosen login logo in the form', () => {
            spectator.triggerEventHandler(
                DotConfigurationBrandingComponent,
                'changeLoginLogo',
                undefined
            );

            closeDialogWith('/dA/new-id/asset/logo.svg');

            expect(dialogService().open).toHaveBeenCalledWith(
                DotConfigurationLogoDialogComponent,
                expect.anything()
            );
            expect(store().patchBranding).toHaveBeenCalledWith({
                loginScreenLogo: '/dA/new-id/asset/logo.svg'
            });
        });

        it('puts the chosen navbar logo in the form', () => {
            spectator.triggerEventHandler(
                DotConfigurationBrandingComponent,
                'changeNavBarLogo',
                undefined
            );

            closeDialogWith('/dA/nav-id/asset/nav.png');

            expect(store().patchBranding).toHaveBeenCalledWith({
                navBarLogo: '/dA/nav-id/asset/nav.png'
            });
        });

        it('tells the user the test mail is on its way once queued', () => {
            spectator.triggerEventHandler(
                DotConfigurationOutboundComponent,
                'sendTestMail',
                'Acme <noreply@acme.com>'
            );

            closeDialogWith('Acme <noreply@acme.com>');

            expect(dialogService().open).toHaveBeenCalledWith(
                DotConfigurationTestMailDialogComponent,
                expect.objectContaining({
                    width: '500px',
                    data: { sender: 'Acme <noreply@acme.com>' }
                })
            );
            expect(spectator.inject(DotMessageDisplayService).push).toHaveBeenCalled();
        });

        it('regenerates the key only after the dialog confirms', () => {
            spectator.triggerEventHandler(
                DotConfigurationSecurityComponent,
                'regenerateKey',
                undefined
            );

            closeDialogWith(true);

            expect(dialogService().open).toHaveBeenCalledWith(
                DotConfigurationRegenerateKeyDialogComponent,
                expect.objectContaining({ width: '500px' })
            );
            expect(store().regenerateKey).toHaveBeenCalled();
        });

        it('keeps the key when the dialog is cancelled', () => {
            spectator.triggerEventHandler(
                DotConfigurationSecurityComponent,
                'regenerateKey',
                undefined
            );

            closeDialogWith(undefined);

            expect(store().regenerateKey).not.toHaveBeenCalled();
        });

        it('opens the license from the footer link', () => {
            spectator.click(
                spectator
                    .query(byTestId('configuration-license-link'))
                    ?.querySelector('button') as HTMLButtonElement
            );

            expect(dialogService().open).toHaveBeenCalledWith(
                DotConfigurationLicenseDialogComponent,
                expect.anything()
            );
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
