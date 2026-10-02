import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotConfigurationActionBarComponent } from '../components/dot-configuration-action-bar/dot-configuration-action-bar.component';
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
    providers: [DotConfigurationStore, ConfirmationService],
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
