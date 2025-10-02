import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { UVE_MODE } from '@dotcms/types';
import { DotMessagePipe } from '@dotcms/ui';

import { MENU_ITEMS_MAP, MODE_SELECTOR_ITEM } from './utils';

import { UVEStore } from '../../../store/dot-uve.store';

@Component({
    selector: 'dot-uve-mode-selector',
    imports: [ButtonModule, MenuModule, DotMessagePipe, NgClass],
    templateUrl: './dot-uve-mode-selector.component.html',
    styleUrl: './dot-uve-mode-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUVEModeSelectorComponent implements OnInit {
    readonly #store = inject(UVEStore);

    readonly $currentMode = this.#store.configuration.mode;
    readonly $menuItems = computed<MODE_SELECTOR_ITEM[]>(() => {
        const canEditPage = this.#store.$canEdit();
        const menu = [];

        if (canEditPage) {
            menu.push(MENU_ITEMS_MAP[UVE_MODE.EDIT]);
        }

        menu.push(MENU_ITEMS_MAP[UVE_MODE.PREVIEW], MENU_ITEMS_MAP[UVE_MODE.LIVE]);

        return menu;
    });

    readonly $currentModeLabel = computed(() => {
        return MENU_ITEMS_MAP[this.$currentMode()].label;
    });

    ngOnInit() {
        // TODO: This should be not happening here, maybe in the init of the store or shell component
        const currentMode = this.#store.configuration.mode();
        const canEdit = this.#store.$canEdit();

        if (currentMode === UVE_MODE.EDIT && !canEdit) {
            this.onModeChange(UVE_MODE.PREVIEW);
        }
    }

    onModeChange(_mode: UVE_MODE) {
        /* */
    }
}
