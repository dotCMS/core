import { Component, computed, inject, input, linkedSignal, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import { DotChipFilterComponent, DotFilterListItemComponent } from '@dotcms/ui';

import { LISTBOX_SCROLL_HEIGHT, SCHEDULE_WINDOW_LABEL_KEYS } from '../../shared/constants';
import { ExperimentFilterOption, ExperimentsListScheduleWindow } from '../../shared/models';

/**
 * Single-select chip narrowing the list to experiments scheduled to start within a window.
 *
 * The one single-select chip in the portlet's chip family, which is why it is its own component
 * rather than a mode of `dot-experiment-list-filter`: the listbox's model is a single value here
 * and an array there, and every read of it would have to branch. Two small components beat one
 * that is two components wearing a flag.
 *
 * It also owns its options, unlike the multi-select chip, which is handed them already translated
 * and counted. These four never change and carry no counts, so there is nothing for a caller to
 * supply — and a caller supplying them could put them out of step with the tokens the address
 * carries.
 *
 * No constraint is the absence of a selection, not an "Any schedule" option (FR-025): the shared
 * chip reads "active" from having selections, so an `ANY` row would make it claim a filter it is
 * not applying. "Any schedule" is its empty label instead.
 */
@Component({
    selector: 'dot-experiment-schedule-filter',
    imports: [
        FormsModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent
    ],
    templateUrl: './dot-experiment-schedule-filter.component.html'
})
export class DotExperimentScheduleFilterComponent {
    /** Window currently in force, owned by the parent's store (URL-backed). */
    readonly $selected = input<ExperimentsListScheduleWindow | null>(null, { alias: 'selected' });

    /** Emits the chosen window, or `null` when the constraint is lifted. */
    readonly selectionChange = output<ExperimentsListScheduleWindow | null>();

    readonly #dotMessageService = inject(DotMessageService);

    protected readonly LISTBOX_SCROLL_HEIGHT = LISTBOX_SCROLL_HEIGHT;

    protected readonly $title = computed<string>(() =>
        this.#dotMessageService.get('experiments.list.filter.schedule')
    );

    /** What the chip reads while unfiltered. Not one of the options — see the class note. */
    protected readonly $emptyLabel = computed<string>(() =>
        this.#dotMessageService.get('experiments.list.filter.schedule.any')
    );

    readonly $options = computed<ExperimentFilterOption[]>(() =>
        [...SCHEDULE_WINDOW_LABEL_KEYS].map(([window, labelKey]) => ({
            value: window,
            label: this.#dotMessageService.get(labelKey),
            // No count, deliberately: the windows overlap and are measured from a moving "now",
            // so a number beside each would neither add up nor stay true.
            count: '',
            testId: `experiment-schedule-filter-option-${window}`
        }))
    );

    /**
     * Labels the chip renders, and what makes it read as active. A list of one, because the chip
     * takes a list — and the one is absent while unfiltered, which is what keeps it neutral.
     */
    protected readonly $selectedLabels = computed<string[]>(() => {
        const selected = this.$selected();

        return this.$options()
            .filter(({ value }) => value === selected)
            .map(({ label }) => label);
    });

    /**
     * Bound two-way to the listbox. Re-seeds from the applied selection whenever the parent
     * changes it (URL hydration, back/forward), while staying writable by the listbox.
     */
    protected readonly $selectedValue = linkedSignal<ExperimentsListScheduleWindow | null>(() =>
        this.$selected()
    );

    /**
     * A window was picked, or the one in force was clicked again — PrimeNG emits `null` for the
     * second, and that has to mean "no constraint" rather than "keep what you had", since the
     * listbox has already un-highlighted the row.
     */
    onChange(window: ExperimentsListScheduleWindow | null): void {
        this.$selectedValue.set(window);
        this.selectionChange.emit(window);
    }

    onRemove(): void {
        this.onChange(null);
    }
}
