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
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

interface StatusOption {
    value: PublishAuditStatus;
    label: string;
}

/** Ordered: active/scheduled first, success, warnings, failures. Keeps the
 * checkbox list in a sensible grouping when the popover opens. */
const STATUS_VALUES: readonly PublishAuditStatus[] = [
    PublishAuditStatus.BUNDLE_REQUESTED,
    PublishAuditStatus.WAITING_FOR_PUBLISHING,
    PublishAuditStatus.BUNDLING,
    PublishAuditStatus.SENDING_TO_ENDPOINTS,
    PublishAuditStatus.PUBLISHING_BUNDLE,
    PublishAuditStatus.RECEIVED_BUNDLE,
    PublishAuditStatus.SUCCESS,
    PublishAuditStatus.SUCCESS_WITH_WARNINGS,
    PublishAuditStatus.BUNDLE_SENT_SUCCESSFULLY,
    PublishAuditStatus.BUNDLE_SAVED_SUCCESSFULLY,
    PublishAuditStatus.FAILED_TO_SEND_TO_ALL_GROUPS,
    PublishAuditStatus.FAILED_TO_SEND_TO_SOME_GROUPS,
    PublishAuditStatus.FAILED_TO_BUNDLE,
    PublishAuditStatus.FAILED_TO_SENT,
    PublishAuditStatus.FAILED_TO_PUBLISH,
    PublishAuditStatus.FAILED_INTEGRITY_CHECK,
    PublishAuditStatus.INVALID_TOKEN,
    PublishAuditStatus.LICENSE_REQUIRED
];

@Component({
    selector: 'dot-publishing-queue-status-filter',
    standalone: true,
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

    protected readonly $options: StatusOption[] = STATUS_VALUES.map((value) => ({
        value,
        label: this.dotMessageService.get(`publishing-queue.status.${value}`)
    }));

    protected readonly $selected = linkedSignal(() => this.store.statusFilter());

    protected readonly $selectedLabels = computed(() => {
        const lookup = new Map(this.$options.map((o) => [o.value, o.label]));
        return this.$selected().map((s) => lookup.get(s) ?? s);
    });

    protected onChange(): void {
        this.store.setStatusFilter(this.$selected());
    }

    protected onRemoveAll(): void {
        this.$selected.set([]);
        this.onChange();
    }
}
