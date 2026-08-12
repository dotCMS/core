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
     * Labels the chip renders, and what makes it read as active. The filter starts empty, so
     * the chip is neutral until the user picks something — no special case needed.
     */
    protected readonly $selectedLabels = computed<string[]>(() => {
        const selected = new Set(this.$selectedStatuses());

        return this.$options()
            .filter(({ value }) => selected.has(value))
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
