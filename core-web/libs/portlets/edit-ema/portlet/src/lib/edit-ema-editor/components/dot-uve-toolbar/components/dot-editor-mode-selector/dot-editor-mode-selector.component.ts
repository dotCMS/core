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

import { DotMessagePipe } from '@dotcms/ui';
import { UVE_MODE } from '@dotcms/uve/types';

import { UVEStore } from '../../../../../store/dot-uve.store';

@Component({
    selector: 'dot-editor-mode-selector',
    standalone: true,
    imports: [TooltipModule, MenuModule, ButtonModule, DotMessagePipe, NgClass],
    templateUrl: './dot-editor-mode-selector.component.html',
    styleUrl: './dot-editor-mode-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditorModeSelectorComponent {
    readonly #store = inject(UVEStore);

    readonly $menuItems = computed(() => {
        const canEditPage = this.#store.canEditPage();
        const hasLiveVersion = this.#store.$hasLiveVersion();
        const menu = [];

        if (canEditPage) {
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

        if (hasLiveVersion) {
            menu.push({
                label: 'uve.editor.mode.published',
                description: 'uve.editor.mode.published.description',
                id: UVE_MODE.LIVE
            });
        }

        return menu;
    });

    readonly $currentMode = computed(() => this.#store.pageParams().mode);

    readonly $currentModeLabel = computed(() => {
        return this.$menuItems().find((item) => item.id === this.$currentMode())?.label;
    });

    readonly $modeGuardEffect = effect(
        () => {
            const currentMode = untracked(() => this.$currentMode());
            const canEditPage = this.#store.canEditPage();
            const hasLiveVersion = this.#store.$hasLiveVersion();

            // If the user is in edit mode and does not have edit permission, change to preview mode
            if (currentMode === UVE_MODE.EDIT && !canEditPage) {
                this.onModeChange(UVE_MODE.PREVIEW);
            }

            // If the user is in live mode and does not have a live version, change to preview mode
            if (currentMode === UVE_MODE.LIVE && !hasLiveVersion) {
                this.onModeChange(UVE_MODE.PREVIEW);
            }
        },
        {
            allowSignalWrites: true
        }
    );

    onModeChange(mode: UVE_MODE) {
        if (mode === this.$currentMode()) return;

        if (mode === UVE_MODE.EDIT) {
            this.#store.clearDeviceAndSocialMedia();
        }

        this.#store.loadPageAsset({
            mode: mode,
            publishDate: mode === UVE_MODE.LIVE ? new Date().toISOString() : undefined
        });
    }
}
