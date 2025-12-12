import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    untracked
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { UVE_MODE } from '@dotcms/types';
import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../../../store/dot-uve.store';

@Component({
    selector: 'dot-editor-mode-selector',
    imports: [TooltipModule, MenuModule, ButtonModule, DotMessagePipe, NgClass],
    templateUrl: './dot-editor-mode-selector.component.html',
    styleUrl: './dot-editor-mode-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditorModeSelectorComponent {
    readonly #store = inject(UVEStore);

    /**
     * Determines whether to show the "Draft" mode option in the mode selector.
     *
     * With feature flag enabled: Always show draft mode (user can toggle lock)
     * Without feature flag: Only show if user can edit the page
     */
    readonly $shouldShowDraftMode = computed(() => {
        const isLockFeatureEnabled = this.#store.$isLockFeatureEnabled();

        // With new lock feature, draft mode is always available
        // Users can toggle lock to edit when ready
        if (isLockFeatureEnabled) {
            return true;
        }

        // Legacy behavior: only show if user can edit
        return this.#store.$hasAccessToEditMode();
    });

    readonly $menuItems = computed(() => {
        const menu = [];

        if (this.$shouldShowDraftMode()) {
            menu.push({
                label: 'uve.editor.mode.draft',
                description: 'uve.editor.mode.draft.description',
                id: UVE_MODE.EDIT
            });
        }

        menu.push({
            label: 'uve.editor.mode.preview',
            description: 'uve.editor.mode.preview.description',
            id: UVE_MODE.PREVIEW
        });

        menu.push({
            label: 'uve.editor.mode.published',
            description: 'uve.editor.mode.published.description',
            id: UVE_MODE.LIVE
        });

        return menu;
    });

    readonly $currentMode = computed(() => this.#store.pageParams().mode);

    readonly $currentModeLabel = computed(() => {
        return this.$menuItems().find((item) => item.id === this.$currentMode())?.label;
    });

    /**
     * Effect that guards against unauthorized edit mode access.
     *
     * If the lock feature is disabled (legacy behavior):
     * - Checks if user has edit permissions
     * - If user is in edit mode without permissions, automatically switches to preview mode
     *
     * If the lock feature is enabled:
     * - No guard is needed since edit access is controlled by the lock mechanism
     */
    readonly $modeGuardEffect = effect(() => {
        const currentMode = untracked(() => this.$currentMode());
        const hasAccessToEditMode = this.#store.$hasAccessToEditMode();
        const isToggleUnlockEnabled = this.#store.$isLockFeatureEnabled();

        if (isToggleUnlockEnabled) {
            return;
        }

        // If the user is in edit mode and does not have edit permission, change to preview mode
        if (currentMode === UVE_MODE.EDIT && !hasAccessToEditMode) {
            this.onModeChange(UVE_MODE.PREVIEW);
        }
    });

    onModeChange(mode: UVE_MODE) {
        if (mode === this.$currentMode()) return;

        if (mode === UVE_MODE.EDIT) {
            this.#store.clearDeviceAndSocialMedia();
        }

        this.#store.trackUVEModeChange({
            fromMode: this.$currentMode(),
            toMode: mode
        });

        /* More info here: https://github.com/dotCMS/core/issues/31719 */
        this.#store.loadPageAsset({ mode: mode, publishDate: undefined });
    }
}
