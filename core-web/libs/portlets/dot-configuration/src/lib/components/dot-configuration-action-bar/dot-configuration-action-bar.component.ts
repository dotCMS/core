import { Component, computed, inject, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotConfigurationSaveState } from '../../store/dot-configuration.store';

const STATUS_KEYS: Record<DotConfigurationSaveState, string> = {
    saved: 'configuration.action-bar.status.saved',
    unsaved: 'configuration.action-bar.status.unsaved',
    saving: 'configuration.action-bar.status.saving',
    failed: 'configuration.action-bar.status.failed'
};

/**
 * Sticky bar at the bottom of the Configuration page: what state the form is in on the left,
 * and Restore Defaults, Cancel and Save Changes on the right.
 *
 * Presentational only. The page shell owns the store and asks for any confirmation before acting
 * on these outputs.
 */
@Component({
    selector: 'dot-configuration-action-bar',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-configuration-action-bar.component.html',
    styleUrls: ['./dot-configuration-action-bar.component.scss'],
    host: {
        class: 'flex flex-none items-center justify-between gap-4 border-t border-surface-200 bg-surface-50 px-8 py-3'
    }
})
export class DotConfigurationActionBarComponent {
    readonly #dotMessageService = inject(DotMessageService);

    readonly saveState = input.required<DotConfigurationSaveState>();
    /** i18n key naming the section whose save failed, shown when `saveState` is `failed`. */
    readonly failedSectionKey = input<string | null>(null);
    readonly canSave = input(false);
    readonly dirty = input(false);

    readonly restoreDefaults = output<void>();
    /** Cancel was clicked: throw away the edits. */
    readonly discard = output<void>();
    readonly save = output<void>();

    protected readonly $statusText = computed(() => {
        const failedSectionKey = this.failedSectionKey();
        const sectionName = failedSectionKey ? this.#dotMessageService.get(failedSectionKey) : '';

        return this.#dotMessageService.get(STATUS_KEYS[this.saveState()], sectionName);
    });
    protected readonly $isFailed = computed(
        () => this.saveState() === DotConfigurationSaveState.FAILED
    );
    protected readonly $isSaving = computed(
        () => this.saveState() === DotConfigurationSaveState.SAVING
    );
}
