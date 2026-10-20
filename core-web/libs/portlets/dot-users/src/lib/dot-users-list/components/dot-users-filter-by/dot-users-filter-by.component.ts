import { Component, computed, inject, linkedSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotChipFilterComponent,
    DotFilterListItemComponent,
    DotMessagePipe
} from '@dotcms/ui';

import {
    DotUsersListStore,
    USERS_FILTER_ALL,
    USERS_FILTER_BACKEND,
    USERS_FILTER_CMS_ADMIN,
    USERS_FILTER_FRONTEND
} from '../../store/dot-users-list.store';

interface FilterOption {
    value: string;
    label: string;
}

@Component({
    selector: 'dot-users-filter-by',
    imports: [
        FormsModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-users-filter-by.component.html'
})
export class DotUsersFilterByComponent {
    readonly #store = inject(DotUsersListStore);
    readonly #dotMessageService = inject(DotMessageService);

    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;

    readonly #accessOptions: FilterOption[] = [
        {
            value: USERS_FILTER_ALL,
            label: this.#dotMessageService.get('users.filter.all-access')
        },
        {
            value: USERS_FILTER_BACKEND,
            label: this.#dotMessageService.get('users.access.backend')
        },
        {
            value: USERS_FILTER_FRONTEND,
            label: this.#dotMessageService.get('users.access.frontend')
        }
    ];

    readonly #cmsAdminOption: FilterOption = {
        value: USERS_FILTER_CMS_ADMIN,
        label: this.#dotMessageService.get('users.access.cms-admin')
    };

    /**
     * Listbox options. CMS Administrator is offered only while the store
     * says the viewer may filter by role membership; the backend answers
     * 403 for anyone else.
     */
    protected readonly $options = computed<FilterOption[]>(() =>
        this.#store.canFilterByRole()
            ? [...this.#accessOptions, this.#cmsAdminOption]
            : this.#accessOptions
    );

    /** Two-way bound to the listbox. Kept in sync with the store's roleFilter. */
    protected readonly $selectedValue = linkedSignal<string>(() => this.#store.roleFilter());

    /**
     * Chip label list — empty when All access is selected so the chip stays in
     * its inactive state, populated with the label otherwise so the chip shows
     * the selected filter and the remove button.
     */
    protected readonly $selections = computed(() => {
        const value = this.$selectedValue();
        if (!value) return [];
        const option = this.$options().find((opt) => opt.value === value);

        return option ? [option.label] : [];
    });

    protected onChange(): void {
        this.#store.setRoleFilter(this.$selectedValue() ?? USERS_FILTER_ALL);
    }

    protected onRemove(): void {
        this.$selectedValue.set(USERS_FILTER_ALL);
        this.onChange();
    }
}
