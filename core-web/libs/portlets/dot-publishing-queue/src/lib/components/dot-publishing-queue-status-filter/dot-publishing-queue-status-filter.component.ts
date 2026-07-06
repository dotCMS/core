import { ChangeDetectionStrategy, Component, inject, linkedSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import { PublishAuditStatus } from '@dotcms/dotcms-models';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotChipFilterComponent,
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

/**
 * Display order for the status filter options.
 *
 * - Drives the visual ordering inside the listbox (lifecycle-first:
 *   scheduled → queued → in-flight → terminal success → failures).
 * - Acts as the single source of truth for which enum values appear in the
 *   filter. The unit test `covers every value of PublishAuditStatus` asserts
 *   that this array contains every member of the enum, so any future enum
 *   addition forces an explicit placement decision here instead of silently
 *   disappearing from the dropdown — which is exactly how `SCHEDULED` was
 *   missing before.
 *
 * Several enum values share a translated label (e.g. SUCCESS and
 * BUNDLE_SENT_SUCCESSFULLY both render as "Success"). They are grouped into a
 * single dropdown option at render time — see `$options` below.
 */
const STATUS_ORDER: readonly PublishAuditStatus[] = [
    PublishAuditStatus.SCHEDULED,
    PublishAuditStatus.BUNDLE_REQUESTED,
    PublishAuditStatus.WAITING_FOR_PUBLISHING,
    PublishAuditStatus.BUNDLING,
    PublishAuditStatus.SENDING_TO_ENDPOINTS,
    PublishAuditStatus.PUBLISHING_BUNDLE,
    PublishAuditStatus.RECEIVED_BUNDLE,
    PublishAuditStatus.SUCCESS,
    PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
    PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY,
    PublishAuditStatus.SUCCESS_WITH_WARNINGS,
    PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS,
    PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS,
    PublishAuditStatus.FAILED_TO_BUNDLE,
    PublishAuditStatus.FAILED_TO_SENT,
    PublishAuditStatus.FAILED_TO_PUBLISH,
    PublishAuditStatus.FAILED_INTEGRITY_CHECK,
    PublishAuditStatus.INVALID_TOKEN,
    PublishAuditStatus.LICENSE_REQUIRED
];

interface StatusOption {
    /** The translated label — also doubles as the listbox `value` so picking
     * "Success" emits the string "Success", which we expand back to its codes
     * on change. */
    value: string;
    label: string;
    /** Every enum value that maps to this label. Picking the option in the UI
     * sets the store filter to the union of these codes. */
    codes: readonly PublishAuditStatus[];
}

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
    private readonly store = inject(DotPublishingQueueStore);
    private readonly dotMessageService = inject(DotMessageService);

    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly LISTBOX_SCROLL_HEIGHT = '320px';

    /** Listbox options, deduplicated by translated label. Order follows
     * `STATUS_ORDER` (first occurrence wins). */
    protected readonly $options: StatusOption[] = this.buildOptions();

    /** Selected labels, reactively derived from the store. An option is
     * considered selected when **all** of its codes are present in the store
     * filter — picking "Success" only counts when SUCCESS *and*
     * BUNDLE_SENT_SUCCESSFULLY are both in the filter. */
    protected readonly $selected = linkedSignal<string[]>(() => {
        const filter = new Set(this.store.statusFilter());
        return this.$options
            .filter((opt) => opt.codes.every((c) => filter.has(c)))
            .map((opt) => opt.value);
    });

    /** Exposed for the unit test that pins the source-of-truth invariant. */
    static readonly STATUS_ORDER = STATUS_ORDER;

    protected onChange(): void {
        const selectedLabels = new Set(this.$selected());
        const codes = this.$options
            .filter((opt) => selectedLabels.has(opt.value))
            .flatMap((opt) => [...opt.codes]);
        this.store.setStatusFilter(codes);
    }

    protected onRemoveAll(): void {
        this.$selected.set([]);
        this.onChange();
    }

    private buildOptions(): StatusOption[] {
        // Group consecutive-or-not enum values by their translated label, in
        // STATUS_ORDER. First occurrence determines display position; later
        // occurrences just append to the same option's `codes`.
        const byLabel = new Map<string, { label: string; codes: PublishAuditStatus[] }>();
        for (const value of STATUS_ORDER) {
            const label = this.dotMessageService.get(`publishing-queue.status.${value}`);
            const existing = byLabel.get(label);
            if (existing) {
                existing.codes.push(value);
            } else {
                byLabel.set(label, { label, codes: [value] });
            }
        }
        return Array.from(byLabel.values()).map((g) => ({
            value: g.label,
            label: g.label,
            codes: g.codes
        }));
    }
}
