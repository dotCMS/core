import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    model,
    OnInit,
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
    styleUrl: './dot-editor-mode-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditorModeSelectorComponent implements OnInit {
    readonly #store = inject(UVEStore);
    readonly #dotMessageService = inject(DotMessageService);
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

        menu.push({
            label: this.#dotMessageService.get('uve.editor.mode.published'),
            description: this.#dotMessageService.get('uve.editor.mode.published.description'),
            id: UVE_MODE.LIVE
        });

        return menu;
    });

    readonly $currentMode = computed(() => this.#store.pageParams().mode);
    readonly selectedModeModel = model<EditorModeOption | null>(null);

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

    /**
     * Style for the dropdown icon
     * This is used to style the dropdown icon to match the primary color
     */
    protected readonly dropdownIconStyle = {
        dropdownIcon: {
            class: 'text-[var(--color-palette-primary-500)]'
        }
    };

    ngOnInit() {
        const currentMode = this.$currentMode();
        const match = this.$menuItems().find((item) => item.id === currentMode);
        this.selectedModeModel.set(match ?? null);
    }

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

    onModeOptionChange(option: EditorModeOption | null) {
        if (!option) return;
        this.onModeChange(option.id);
    }
}
