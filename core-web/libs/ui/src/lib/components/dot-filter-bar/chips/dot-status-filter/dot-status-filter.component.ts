import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    linkedSignal,
    untracked
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';

import { DotContentStatus, STATUS_FILTER_KEY, STATUS_FILTER_OPTIONS } from './constants';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';
import { CHIP_FILTER_LISTBOX_PT, CHIP_FILTER_POPOVER_PT } from '../../../dot-chip-filter/constants';
import { DotChipFilterComponent } from '../../../dot-chip-filter/dot-chip-filter.component';
import { DotFilterListItemComponent } from '../../../dot-filter-list-item/dot-filter-list-item.component';
import { DOT_FILTER_FACADE } from '../../filter-facade.token';

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
    selector: 'dot-status-filter',
    imports: [
        FormsModule,
        CheckboxModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-status-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { 'data-filter-chip': 'status' }
})
export class DotStatusFilterComponent {
    readonly #filters = inject(DOT_FILTER_FACADE);
    readonly #dotMessageService = inject(DotMessageService);

    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly LISTBOX_SCROLL_HEIGHT = '25rem';
    /**
     * Conditions this surface may offer, or `null` for no bound (FR-014d).
     *
     * A caller restriction, not a filter: it arrives as an input rather than through the facade,
     * and the editor cannot change it. A picker pinned to published content passes `['LOCKED']` —
     * neither Archived nor Unpublished has a published version, so offering them would force the
     * whole query onto the working version and describe content by a version nobody asked for.
     */
    readonly $allowedOptions = input<DotContentStatus[] | null>(null, { alias: 'allowedOptions' });

    /** The options actually rendered, after the bound. */
    protected readonly $options = computed(() => {
        const allowed = this.$allowedOptions();

        return allowed
            ? STATUS_FILTER_OPTIONS.filter((option) => allowed.includes(option.value))
            : STATUS_FILTER_OPTIONS;
    });

    /** Whether anything is being withheld, so the panel can say so rather than just look shorter. */
    protected readonly $isBounded = computed(
        () => this.$options().length < STATUS_FILTER_OPTIONS.length
    );

    /**
     * Current selection, read from the filter bag.
     *
     * `getFilterValue` can return `string | string[]`; only an array is valid here. A bare string
     * would mean the URL decoder lost the array shape — see the explicit `status` entry in
     * `decodeByFilterKey`.
     */
    readonly $selection = linkedSignal<string[]>(
        () => {
            const raw = this.#filters.getFilterValue(STATUS_FILTER_KEY);

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

        return this.$options()
            .filter((option) => selected.includes(option.value))
            .map((option) => this.#dotMessageService.get(option.labelKey));
    });

    protected isSelected(value: string): boolean {
        return this.$selection().includes(value);
    }

    constructor() {
        this.#syncStore(this.$selection);

        // Drop any selection the bound no longer admits. Restored or stale state must not keep
        // applying a condition the control cannot offer, or the request carries a filter the editor
        // has no way to see or clear. `untracked` because the write feeds back into `$selection`.
        effect(() => {
            const allowed = this.$allowedOptions();
            if (!allowed) {
                return;
            }

            const selection = this.$selection();
            const admitted = selection.filter((value) =>
                allowed.includes(value as DotContentStatus)
            );

            if (admitted.length !== selection.length) {
                untracked(() => this.$selection.set(admitted));
            }
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

    /**
     * Persists the selection to the filter bag whenever it changes.
     *
     * A `signalMethod` rather than an `effect`: it takes the signal directly, and runs its body
     * outside the reactive context, so the store writes cannot become dependencies of the very
     * thing that triggers them — no manual `untracked` to remember. Matches how the analytics
     * filters and the UVE palette do this.
     *
     * The selection is the single source of truth: handlers only `set()` it, so there is no way to
     * change the selection and forget to persist it.
     *
     * The write feeds back into `$selection` (it reads the filter bag), which is why that signal
     * compares by value — a write that does not change the selection produces no notification, so
     * the cycle cannot start.
     */
    readonly #syncStore = signalMethod<string[]>((selection) => {
        if (selection.length) {
            this.#filters.patchFilters({ [STATUS_FILTER_KEY]: selection });

            return;
        }

        // removeFilter rather than patching an empty array: an empty array would linger in the URL
        // and keep folders suppressed for a filter that is no longer doing anything.
        this.#filters.removeFilter(STATUS_FILTER_KEY);
    });
}
