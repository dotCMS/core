import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    model,
    untracked
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';

import { DotMessageService } from '@dotcms/data-access';
import { UVE_MODE } from '@dotcms/types';

import { UVEStore } from '../../../../../store/dot-uve.store';

interface EditorModeOption {
    label: string;
    description: string;
    id: UVE_MODE;
}

@Component({
    selector: 'dot-editor-mode-selector',
    imports: [SelectModule, FormsModule],
    templateUrl: './dot-editor-mode-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditorModeSelectorComponent {
    readonly #store = inject(UVEStore);
    readonly #dotMessageService = inject(DotMessageService);
    /**
     * Determines whether to show the "Draft" mode option in the mode selector.
     *
     * Never shown when the page has no version in the selected language —
     * there is nothing to edit.
     * With feature flag enabled: shown otherwise (user can toggle lock)
     * Without feature flag: only shown if user can edit the page
     */
    readonly $shouldShowDraftMode = computed(() => {
        // Missing translation means there is nothing to draft-edit.
        // Always hide, regardless of lock feature flag.
        if (this.#store.$isMissingTranslation()) {
            return false;
        }

        const isLockFeatureEnabled = this.#store.$lockFeatureEnabled();

        // With new lock feature, draft mode is always available
        // Users can toggle lock to edit when ready
        if (isLockFeatureEnabled) {
            return true;
        }

        // Legacy behavior: only show if user can edit
        return this.#store.editorHasAccessToEditMode();
    });

    readonly $menuItems = computed(() => {
        const menu: EditorModeOption[] = [];

        if (this.$shouldShowDraftMode()) {
            menu.push({
                label: this.#dotMessageService.get('uve.editor.mode.draft'),
                description: this.#dotMessageService.get('uve.editor.mode.draft.description'),
                id: UVE_MODE.EDIT
            });
        }

        menu.push({
            label: this.#dotMessageService.get('uve.editor.mode.preview'),
            description: this.#dotMessageService.get('uve.editor.mode.preview.description'),
            id: UVE_MODE.PREVIEW
        });

        // Published shows the live version of the page. Not relevant when the page
        // has no version in the selected language — the fallback content shown is
        // not the published state of this language.
        if (!this.#store.$isMissingTranslation()) {
            menu.push({
                label: this.#dotMessageService.get('uve.editor.mode.published'),
                description: this.#dotMessageService.get('uve.editor.mode.published.description'),
                id: UVE_MODE.LIVE
            });
        }

        return menu;
    });

    readonly $currentMode = computed(() => this.#store.pageParams().mode);
    readonly selectedModeModel = model<EditorModeOption | null>(null);

    /**
     * Keeps the PrimeNG select model in sync with the store's current mode.
     * Runs on init and whenever pageParams.mode or the available menu items change
     * (e.g. when isMissingTranslation flips and Draft/Published are removed).
     */
    readonly $syncSelectedMode = effect(() => {
        const currentMode = this.$currentMode();
        const menuItems = this.$menuItems();
        const match = menuItems.find((item) => item.id === currentMode);
        untracked(() => this.selectedModeModel.set(match ?? null));
    });

    /**
     * TODO: This should be in the shell or in the store
     * A main effect should not be hidden in a component
     *
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
        const hasAccessToEditMode = this.#store.editorHasAccessToEditMode();
        const isToggleUnlockEnabled = this.#store.$lockFeatureEnabled();

        if (isToggleUnlockEnabled) {
            return;
        }

        // If the user is in edit mode and does not have edit permission, change to preview mode
        if (currentMode === UVE_MODE.EDIT && !hasAccessToEditMode) {
            this.onModeChange(UVE_MODE.PREVIEW);
        }
    });

    /**
     * Style for the dropdown icon
     * This is used to style the dropdown icon to match the primary color
     */
    protected readonly dropdownIconStyle = {
        dropdownIcon: {
            class: 'text-primary-500'
        }
    };

    onModeChange(mode: UVE_MODE) {
        if (mode === this.$currentMode()) return;

        if (mode === UVE_MODE.EDIT) {
            this.#store.viewClearDeviceAndSocialMedia();
        }

        this.#store.trackUVEModeChange({
            fromMode: this.$currentMode(),
            toMode: mode
        });

        /* More info here: https://github.com/dotCMS/core/issues/31719 */
        this.#store.pageLoad({ mode: mode, publishDate: undefined });
    }

    onModeOptionChange(option: EditorModeOption | null) {
        if (!option) return;
        this.onModeChange(option.id);
    }
}
