import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';

import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../store/dot-publishing-queue.store';

/** Scope chosen by the user — emitted back via DynamicDialogRef.close(). */
export type DeleteBundlesScope = 'selected' | 'all' | 'success' | 'failed';

/**
 * "Select Bundles to Delete" dialog — mirrors the legacy JSP modal
 * (`view_publish_tool.jsp#deleteBundleActions`) with the same four scopes:
 *
 *   SELECTED · ALL · SUCCESS · FAILED
 *
 * The dialog only collects intent. It closes with a `DeleteBundlesScope`; the
 * shell dispatches the matching store action.
 *
 * SELECTED is disabled when there's no row selection (legacy hid the button —
 * we keep it visible but disabled so the user understands the option exists).
 */
@Component({
    selector: 'dot-publishing-queue-delete-dialog',
    standalone: true,
    imports: [ButtonModule, MessageModule, DotMessagePipe],
    templateUrl: './dot-publishing-queue-delete-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueDeleteDialogComponent {
    readonly store = inject(DotPublishingQueueStore);
    readonly dialogRef = inject(DynamicDialogRef);

    readonly selectedCount = computed(() => this.store.bundlesSelectedIds().length);
    readonly hasSelection = computed(() => this.selectedCount() > 0);

    choose(scope: DeleteBundlesScope): void {
        this.dialogRef.close(scope);
    }
}
