import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    linkedSignal,
    output
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus, ExperimentsStatusList } from '@dotcms/dotcms-models';
import { DotChipFilterComponent, DotFilterListItemComponent, DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_EXPERIMENTS_LIST_STATUSES } from '../../store/dot-experiments-list.store';

/** True when the selection is the default set, whatever order it arrived in. */
const isDefaultSelection = (statuses: DotExperimentStatus[]): boolean =>
    statuses.length === DEFAULT_EXPERIMENTS_LIST_STATUSES.length &&
    DEFAULT_EXPERIMENTS_LIST_STATUSES.every((status) => statuses.includes(status));

/** A single status entry rendered inside the filter listbox. */
interface StatusFilterOption {
    value: DotExperimentStatus;
    /** Translated, human-readable status name. */
    label: string;
    /** Count for this status, stringified for the list item's secondary slot. */
    count: string;
    testId: string;
}

/**
 * Multi-select status filter for the experiments list.
 *
 * Renders a chip that opens a popover with a checkbox listbox. Each toggle applies
 * immediately and the chip's remove control clears the selection, matching every
 * `dot-chip-filter` consumer in content-drive — none of them batch behind an apply
 * button, so this one does not either.
 */
@Component({
    selector: 'dot-experiment-status-filter',
    imports: [
        FormsModule,
        ButtonModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-experiment-status-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentStatusFilterComponent {
    readonly #dotMessageService = inject(DotMessageService);

    /** Currently applied statuses, owned by the parent's store (URL-backed). */
    readonly $selectedStatuses = input.required<DotExperimentStatus[]>({
        alias: 'selectedStatuses'
    });

    /** Per-status counts over the site + search filtered set, independent of the selection. */
    readonly $statusCounts = input.required<Record<DotExperimentStatus, number>>({
        alias: 'statusCounts'
    });

    /** Emits on every toggle and on clear. */
    readonly selectionChange = output<DotExperimentStatus[]>();
    protected readonly LISTBOX_SCROLL_HEIGHT = '320px';

    protected readonly $options = computed<StatusFilterOption[]>(() => {
        const counts = this.$statusCounts();

        return ExperimentsStatusList.map(({ label, value }) => {
            const status = value as DotExperimentStatus;

            return {
                value: status,
                label: this.#dotMessageService.get(label),
                count: String(counts[status] ?? 0),
                testId: `experiment-status-filter-option-${status.toLowerCase()}`
            };
        });
    });

    /**
     * Labels the chip renders, and what makes it read as active.
     *
     * The default selection (every status but ARCHIVED) is "not filtered", not a filter, so it
     * renders as an empty selection: the chip stays neutral like the content-drive filters
     * instead of sitting permanently highlighted. Same notion as the URL contract, where the
     * default writes no `status` param.
     */
    protected readonly $selectedLabels = computed<string[]>(() => {
        const selected = this.$selectedStatuses();

        if (isDefaultSelection(selected)) {
            return [];
        }

        const set = new Set(selected);

        return this.$options()
            .filter(({ value }) => set.has(value))
            .map(({ label }) => label);
    });

    /**
     * Bound two-way to the listbox. Re-seeds from the applied selection whenever the parent
     * changes it (URL hydration, back/forward), while staying writable by the listbox.
     */
    protected readonly $selectedStatusValues = linkedSignal<DotExperimentStatus[]>(() => [
        ...this.$selectedStatuses()
    ]);

    protected onChange(): void {
        this.selectionChange.emit(this.$selectedStatusValues() ?? []);
    }

    protected onRemoveAll(): void {
        this.$selectedStatusValues.set([]);
        this.selectionChange.emit([]);
    }
}
