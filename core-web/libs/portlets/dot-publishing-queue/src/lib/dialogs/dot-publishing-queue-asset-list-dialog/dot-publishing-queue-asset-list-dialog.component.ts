import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

@Component({
    selector: 'dot-publishing-queue-asset-list-dialog',
    standalone: true,
    imports: [TableModule, SkeletonModule, DotMessagePipe],
    templateUrl: './dot-publishing-queue-asset-list-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueAssetListDialogComponent {
    readonly store = inject(DotPublishingQueueStore);
}
