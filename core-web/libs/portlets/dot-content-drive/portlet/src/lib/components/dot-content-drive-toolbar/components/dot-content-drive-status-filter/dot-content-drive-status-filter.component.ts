import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    linkedSignal,
    untracked
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
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
    PANEL_SCROLL_HEIGHT,
    STATUS_FILTER_KEY,
    STATUS_FILTER_OPTIONS
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

/**
 * Status filter for the Content Drive toolbar: Archived, Unpublished and Locked.
 *
 * Selections combine with **OR** — checking more boxes returns more content, the same way the
 * content-type and locale filters behave. That is the whole reason this is a multiselect rather
 * than a dropdown, and it is why nothing here needs to reason about combinations: the server ORs
 * whatever it is given.
 *
 * Deliberately much smaller than the workflow filter it is modelled on. That one carries a service,
 * two caches, a request-id guard and a reconcile pass because its options are fetched and can
 * disappear between loads. These three options are fixed, so none of that machinery applies.
 *
 * The selection lives in the shared `filters` bag rather than its own query param, which is what
 * gives it deep-link, reload, folder-navigation, Back/Forward and legacy-editor round-trip
 * behaviour for free — identical to every other filter.
 */
@Component({
    selector: 'dot-content-drive-status-filter',
    imports: [
        FormsModule,
        CheckboxModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-content-drive-status-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveStatusFilterComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotMessageService = inject(DotMessageService);

    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly LISTBOX_SCROLL_HEIGHT = PANEL_SCROLL_HEIGHT;
    protected readonly options = STATUS_FILTER_OPTIONS;

    /**
     * Current selection, read from the filter bag.
     *
     * `getFilterValue` can return `string | string[]`; only an array is valid here. A bare string
     * would mean the URL decoder lost the array shape — see the explicit `status` entry in
     * `decodeByFilterKey`.
     */
    readonly $selection = linkedSignal<string[]>(
        () => {
            const raw = this.#store.getFilterValue(STATUS_FILTER_KEY);

            return Array.isArray(raw) ? raw : [];
        },
        {
            // Compare by VALUE, not reference. This is what makes the reactive sync below safe
            // rather than merely lucky: the effect writes to the store, the store feeds this
            // signal, and without value equality every write that minted a new array — a
            // `[...selection]` spread, or the fresh `[]` produced when the key is absent — would
            // notify, re-run the effect and write again. With it, a write that does not change
            // the selection is inert and the cycle cannot start.
            equal: (a, b) => a.length === b.length && a.every((value, i) => value === b[i])
        }
    );

    /** Resolved labels for the chip, in the control's display order rather than click order. */
    protected readonly $chipSelections = computed(() => {
        const selected = this.$selection();

        return this.options
            .filter((option) => selected.includes(option.value))
            .map((option) => this.#dotMessageService.get(option.labelKey));
    });

    protected isSelected(value: string): boolean {
        return this.$selection().includes(value);
    }

    constructor() {
        // The selection is the single source of truth: whenever it changes, the filter bag follows.
        // Handlers just set the signal, so there is no way to update the selection and forget to
        // persist it — the failure mode two explicit #syncStore() calls invited.
        //
        // Safe against the obvious circularity (effect writes store → store feeds $selection →
        // effect) because $selection compares by value: a write that does not change the selection
        // produces no notification. untracked keeps the store writes out of the dependency set.
        effect(() => {
            const selection = this.$selection();

            untracked(() => this.#syncStore(selection));
        });
    }

    /**
     * Selection changed in the listbox. In multiple mode it hands back the full array of selected
     * values, so this replaces rather than toggles — which also means clicking anywhere on a row,
     * label included, works without a second code path.
     */
    protected onSelectionChange(selection: string[]): void {
        this.$selection.set(selection ?? []);
    }

    protected onClearAll(): void {
        this.$selection.set([]);
    }

    #syncStore(selection: string[]): void {
        if (selection.length) {
            this.#store.patchFilters({ [STATUS_FILTER_KEY]: selection });

            return;
        }

        // removeFilter rather than patching an empty array: an empty array would linger in the URL
        // and keep folders suppressed for a filter that is no longer doing anything.
        this.#store.removeFilter(STATUS_FILTER_KEY);
    }
}
