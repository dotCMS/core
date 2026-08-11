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
import { Popover, PopoverModule } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus, ExperimentsStatusList } from '@dotcms/dotcms-models';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotChipFilterComponent,
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

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
 * Renders a chip that opens a popover with a checkbox listbox. Checking options
 * only mutates a local pending selection — nothing is emitted until the user
 * presses Done (or Clear). Dismissing the popover discards the pending change.
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

    /** Emits the new selection when the user applies (Done) or clears it. */
    readonly selectionChange = output<DotExperimentStatus[]>();

    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
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

    /** Labels of the applied statuses — what the chip renders. */
    protected readonly $selectedLabels = computed<string[]>(() => {
        const selected = new Set(this.$selectedStatuses());

        return this.$options()
            .filter(({ value }) => selected.has(value))
            .map(({ label }) => label);
    });

    /** Selection being edited inside the popover, re-seeded whenever the applied selection changes. */
    protected readonly $pendingStatuses = linkedSignal<DotExperimentStatus[]>(() => [
        ...this.$selectedStatuses()
    ]);

    protected onDone(popover: Popover): void {
        this.selectionChange.emit(this.$pendingStatuses());
        popover.hide();
    }

    protected onClear(): void {
        this.$pendingStatuses.set([]);
        this.selectionChange.emit([]);
    }

    /** Dismissing the popover without applying discards the pending edit. */
    protected onPopoverHide(): void {
        this.$pendingStatuses.set([...this.$selectedStatuses()]);
    }
}
