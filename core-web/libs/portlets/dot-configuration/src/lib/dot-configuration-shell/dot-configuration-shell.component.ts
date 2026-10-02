import { Observable } from 'rxjs';

import { Component, Type, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';

import { take } from 'rxjs/operators';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotConfigurationActionBarComponent } from '../components/dot-configuration-action-bar/dot-configuration-action-bar.component';
import { DotConfigurationBackgroundDialogComponent } from '../dialogs/dot-configuration-background-dialog/dot-configuration-background-dialog.component';
import { DotConfigurationLicenseDialogComponent } from '../dialogs/dot-configuration-license-dialog/dot-configuration-license-dialog.component';
import { DotConfigurationLogoDialogComponent } from '../dialogs/dot-configuration-logo-dialog/dot-configuration-logo-dialog.component';
import { DotConfigurationRegenerateKeyDialogComponent } from '../dialogs/dot-configuration-regenerate-key-dialog/dot-configuration-regenerate-key-dialog.component';
import { DotConfigurationTestMailDialogComponent } from '../dialogs/dot-configuration-test-mail-dialog/dot-configuration-test-mail-dialog.component';
import {
    DOT_CONFIGURATION_CONFIRM_KEY,
    DotConfigurationUnsavedWork
} from '../guards/dot-configuration-unsaved-changes.guard';
import { DotConfigurationBrandingComponent } from '../sections/dot-configuration-branding/dot-configuration-branding.component';
import { DotConfigurationLocaleComponent } from '../sections/dot-configuration-locale/dot-configuration-locale.component';
import { DotConfigurationOutboundComponent } from '../sections/dot-configuration-outbound/dot-configuration-outbound.component';
import { DotConfigurationSecurityComponent } from '../sections/dot-configuration-security/dot-configuration-security.component';
import { DotConfigurationSection } from '../store/dot-configuration.mappers';
import { DotConfigurationStore } from '../store/dot-configuration.store';

const SECTION_NAME_KEYS: Record<DotConfigurationSection, string> = {
    branding: 'configuration.section.branding',
    authentication: 'configuration.section.security',
    locale: 'configuration.section.locale'
};

/**
 * Page shell of the Configuration (Beta) portlet. Provides the page store, renders the section
 * cards and the sticky action bar, and asks for confirmation before destructive actions or
 * before leaving with unsaved edits.
 */
@Component({
    selector: 'dot-configuration-shell',
    imports: [
        ButtonModule,
        ConfirmDialogModule,
        MessageModule,
        SkeletonModule,
        DotMessagePipe,
        DotConfigurationActionBarComponent,
        DotConfigurationBrandingComponent,
        DotConfigurationLocaleComponent,
        DotConfigurationOutboundComponent,
        DotConfigurationSecurityComponent
    ],
    providers: [DotConfigurationStore, ConfirmationService, DialogService],
    templateUrl: './dot-configuration-shell.component.html',
    styleUrls: ['./dot-configuration-shell.component.scss'],
    host: {
        class: 'flex h-full min-h-0 flex-col',
        '(window:beforeunload)': 'onBeforeUnload($event)'
    }
})
export class DotConfigurationShellComponent implements DotConfigurationUnsavedWork {
    readonly #dotMessageService = inject(DotMessageService);
    readonly #route = inject(ActivatedRoute);
    readonly #dialogService = inject(DialogService);
    readonly #messageDisplayService = inject(DotMessageDisplayService);

    protected readonly store = inject(DotConfigurationStore);
    readonly confirmationService = inject(ConfirmationService);

    readonly $hasUnsavedChanges = this.store.dirty;

    protected readonly confirmKey = DOT_CONFIGURATION_CONFIRM_KEY;
    protected readonly ComponentStatus = ComponentStatus;
    /** Resolved by the route; the navbar logo override is Enterprise-only. */
    protected readonly isEnterprise = !!this.#route.snapshot.data['isEnterprise'];
    /** One placeholder per section card while the configuration loads. */
    protected readonly skeletonSections = [1, 2, 3, 4];

    protected readonly $failedSectionKey = computed(() => {
        const section = this.store.failedSection();

        return section ? SECTION_NAME_KEYS[section] : null;
    });

    /** Asks before putting the default colors and login background back in the form. */
    onRestoreDefaults(): void {
        this.confirmationService.confirm({
            key: this.confirmKey,
            header: this.#dotMessageService.get('configuration.restore-defaults.header'),
            message: this.#dotMessageService.get('configuration.restore-defaults.message'),
            acceptLabel: this.#dotMessageService.get('configuration.restore-defaults.accept'),
            rejectLabel: this.#dotMessageService.get('configuration.action-bar.cancel'),
            acceptIcon: 'hidden',
            rejectIcon: 'hidden',
            rejectButtonStyleClass: 'p-button-outlined',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.restoreDefaults()
        });
    }

    /** Picks the login background; the choice goes into the form, not straight to the server. */
    onChangeBackground(): void {
        this.#openDialog<string>(
            DotConfigurationBackgroundDialogComponent,
            'configuration.background-dialog.header',
            '700px',
            { current: this.store.draft()?.branding.backgroundImage ?? '' }
        ).subscribe((backgroundImage) => {
            if (backgroundImage !== undefined) {
                this.store.patchBranding({ backgroundImage });
            }
        });
    }

    onChangeLoginLogo(): void {
        this.#openDialog<string>(
            DotConfigurationLogoDialogComponent,
            'configuration.logo-dialog.login-header',
            '700px',
            { current: this.store.draft()?.branding.loginScreenLogo ?? '' }
        ).subscribe((loginScreenLogo) => {
            if (loginScreenLogo) {
                this.store.patchBranding({ loginScreenLogo });
            }
        });
    }

    onChangeNavBarLogo(): void {
        this.#openDialog<string>(
            DotConfigurationLogoDialogComponent,
            'configuration.logo-dialog.navbar-header',
            '700px',
            { current: this.store.draft()?.branding.navBarLogo ?? '' }
        ).subscribe((navBarLogo) => {
            if (navBarLogo) {
                this.store.patchBranding({ navBarLogo });
            }
        });
    }

    /** The dialog sends the email itself; once queued, the result arrives as a notification. */
    onSendTestMail(sender: string): void {
        this.#openDialog<string>(
            DotConfigurationTestMailDialogComponent,
            'configuration.test-mail.header',
            '500px',
            { sender }
        ).subscribe((sentFrom) => {
            if (sentFrom) {
                this.#messageDisplayService.push({
                    life: 5000,
                    severity: DotMessageSeverity.INFO,
                    message: this.#dotMessageService.get(
                        'configuration.test-mail.queued',
                        sentFrom
                    ),
                    type: DotMessageType.SIMPLE_MESSAGE
                });
            }
        });
    }

    onRegenerateKey(): void {
        this.#openDialog<boolean>(
            DotConfigurationRegenerateKeyDialogComponent,
            'configuration.regenerate-key.header',
            '500px'
        ).subscribe((confirmed) => {
            if (confirmed) {
                this.store.regenerateKey();
            }
        });
    }

    onOpenLicense(): void {
        this.#openDialog<void>(
            DotConfigurationLicenseDialogComponent,
            'configuration.license.header',
            '700px'
        ).subscribe();
    }

    /** Reverts every edit to the values last saved. */
    onCancel(): void {
        this.store.discard();
    }

    onSave(): void {
        this.store.save();
    }

    onRetry(): void {
        this.store.load();
    }

    #openDialog<T>(
        component: Type<unknown>,
        headerKey: string,
        width: string,
        data?: DynamicDialogConfig['data']
    ) {
        const ref = this.#dialogService.open(component, {
            header: this.#dotMessageService.get(headerKey),
            width,
            data,
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        return ref.onClose.pipe(take(1)) as Observable<T | undefined>;
    }

    /**
     * Makes the browser ask before closing or reloading the tab with unsaved edits. In-app
     * navigation is covered by the route guard.
     */
    onBeforeUnload(event: BeforeUnloadEvent): void {
        if (this.$hasUnsavedChanges()) {
            event.preventDefault();
        }
    }
}
