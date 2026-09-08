import { Component, computed, input, linkedSignal, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { DotChipFilterComponent, DotFilterListItemComponent } from '@dotcms/ui';

import { LISTBOX_SCROLL_HEIGHT } from '../../shared/constants';
import { ExperimentFilterOption } from '../../shared/models';

/**
 * Multi-select chip filter for the experiments list, used once per filterable column.
 *
 * Renders a chip that opens a popover with a checkbox listbox. Each toggle applies immediately
 * and the chip's remove control clears the selection, matching every `dot-chip-filter` consumer
 * in content-drive — none of them batch behind an apply button, so this one does not either.
 *
 * Deliberately knows nothing about statuses or goals: options arrive already translated and
 * counted, so adding a filter is a matter of supplying a new option list rather than copying
 * this component. Values are plain strings for that reason; the caller owns the narrower type
 * and casts on the way out.
 */
@Component({
    selector: 'dot-experiment-list-filter',
    imports: [
        FormsModule,
        ButtonModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent
    ],
    templateUrl: './dot-experiment-list-filter.component.html'
})
export class DotExperimentListFilterComponent {
    /** Chip label, already translated. */
    readonly $title = input.required<string>({ alias: 'title' });

    /** Every value the filter offers, already translated and counted. */
    readonly $options = input.required<ExperimentFilterOption[]>({ alias: 'options' });

    /** Currently applied values, owned by the parent's store (URL-backed). */
    readonly $selected = input.required<string[]>({ alias: 'selected' });

    /**
     * What the chip reads while nothing is selected — `All`, since an empty selection applies no
     * filter. Kept as a placeholder rather than an entry in the list: see `emptyLabel` on
     * `DotChipFilterComponent`.
     */
    readonly $emptyLabel = input.required<string>({ alias: 'emptyLabel' });

    /** Emits on every toggle and on clear. */
    readonly selectionChange = output<string[]>();

    protected readonly LISTBOX_SCROLL_HEIGHT = LISTBOX_SCROLL_HEIGHT;

    /**
     * Labels the chip renders, and what makes it read as active. The filter starts empty, so the
     * chip is neutral until the user picks something — no special case needed.
     */
    protected readonly $selectedLabels = computed<string[]>(() => {
        const selected = new Set(this.$selected());

        return this.$options()
            .filter(({ value }) => selected.has(value))
            .map(({ label }) => label);
    });

    /**
     * Bound two-way to the listbox. Re-seeds from the applied selection whenever the parent
     * changes it (URL hydration, back/forward), while staying writable by the listbox.
     */
    protected readonly $selectedValues = linkedSignal<string[]>(() => [...this.$selected()]);

    protected onChange(): void {
        this.selectionChange.emit(this.$selectedValues() ?? []);
    }

    protected onRemoveAll(): void {
        this.$selectedValues.set([]);
        this.selectionChange.emit([]);
    }
}
