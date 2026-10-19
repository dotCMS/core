import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus } from '@dotcms/dotcms-models';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotChipFilterComponent,
    DotFilterListItemComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

interface StatusFilterGroup {
    /** Stable key used as the listbox `value`. */
    value: string;
    labelKey: string;
    /** Bundle statuses sent to the API when the group is selected. */
    codes: readonly PublishAuditStatus[];
}

interface StatusOption {
    value: string;
    label: string;
    codes: readonly PublishAuditStatus[];
}

/**
 * The options shown in the status filter, in display order (lifecycle-first:
 * scheduled → queued → in-flight → success → failures).
 *
 * Only bundle-level statuses are listed. `ENDPOINT_ONLY_STATUSES` are left out
 * on purpose: they are never a bundle's status, so they would always return an
 * empty list. The unit test pins that every other `PublishAuditStatus` value
 * belongs to exactly one group, so a new enum value forces a placement decision.
 *
 * In-flight statuses change within seconds, so they share one "In progress"
 * option; the row chip still shows the exact status. Every group lists its codes
 * literally: the shared status sets in `@dotcms/dotcms-models` are defined by
 * cancellability, which is a different decision from what the filter offers.
 */
const STATUS_FILTER_GROUPS: readonly StatusFilterGroup[] = [
    {
        value: 'scheduled',
        labelKey: 'publishing-queue.filter.status.scheduled',
        codes: [PublishAuditStatus.SCHEDULED]
    },
    {
        value: 'pending',
        labelKey: 'publishing-queue.filter.status.pending',
        codes: [PublishAuditStatus.BUNDLE_REQUESTED]
    },
    {
        value: 'in-progress',
        labelKey: 'publishing-queue.filter.status.in-progress',
        codes: [
            PublishAuditStatus.BUNDLING,
            PublishAuditStatus.SENDING_TO_ENDPOINTS,
            PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
            PublishAuditStatus.WAITING_FOR_PUBLISHING,
            PublishAuditStatus.PUBLISHING_BUNDLE,
            PublishAuditStatus.RECEIVED_BUNDLE
        ]
    },
    {
        value: 'success',
        labelKey: 'publishing-queue.filter.status.success',
        codes: [PublishAuditStatus.SUCCESS, PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY]
    },
    {
        value: 'success-with-warnings',
        labelKey: 'publishing-queue.filter.status.success-with-warnings',
        codes: [PublishAuditStatus.SUCCESS_WITH_WARNINGS]
    },
    {
        value: 'failed',
        labelKey: 'publishing-queue.filter.status.failed',
        codes: [
            PublishAuditStatus.FAILED_TO_BUNDLE,
            PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS,
            PublishAuditStatus.FAILED_TO_PUBLISH
        ]
    },
    {
        value: 'partially-failed',
        labelKey: 'publishing-queue.filter.status.partially-failed',
        codes: [PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS]
    }
];

@Component({
    selector: 'dot-publishing-queue-status-filter',
    imports: [
        FormsModule,
        ListboxModule,
        PopoverModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-publishing-queue-status-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueStatusFilterComponent {
    readonly #store = inject(DotPublishingQueueStore);
    readonly #dotMessageService = inject(DotMessageService);

    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly LISTBOX_SCROLL_HEIGHT = '320px';

    protected readonly $options: StatusOption[] = STATUS_FILTER_GROUPS.map((group) => ({
        value: group.value,
        label: this.#dotMessageService.get(group.labelKey),
        codes: group.codes
    }));

    /** Selected group keys, derived from the store. A group counts as selected
     * only when **all** of its codes are in the store filter. */
    protected readonly $selected = linkedSignal<string[]>(() => {
        const filter = new Set(this.#store.statusFilter());
        return this.$options
            .filter((opt) => opt.codes.every((c) => filter.has(c)))
            .map((opt) => opt.value);
    });

    /** Translated labels of the selected groups, shown in the filter chip. */
    protected readonly $selectedLabels = computed<string[]>(() => {
        const selected = new Set(this.$selected());
        return this.$options.filter((opt) => selected.has(opt.value)).map((opt) => opt.label);
    });

    /** Exposed for the unit test that pins the source-of-truth invariant. */
    static readonly STATUS_FILTER_GROUPS = STATUS_FILTER_GROUPS;

    /**
     * Sends the union of the selected groups' statuses to the store.
     */
    protected onChange(): void {
        const selected = new Set(this.$selected());
        const codes = this.$options
            .filter((opt) => selected.has(opt.value))
            .flatMap((opt) => [...opt.codes]);
        this.#store.setStatusFilter(codes);
    }

    /**
     * Clears every selected group, which removes the status filter entirely.
     */
    protected onRemoveAll(): void {
        this.$selected.set([]);
        this.onChange();
    }
}
