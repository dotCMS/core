import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';

import { DotContainersByContentType } from '@dotcms/data-access';
import { DotContainer } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/**
 * Dialog shown before deleting a content type that containers use: it lists the containers and
 * lets the user delete anyway or cancel. Closes with `true` on Delete and `false` on Cancel.
 * Opened with the {@link DotContainersByContentType} of the content type as the dialog `data`.
 */
@Component({
    selector: 'dot-content-type-in-use-dialog',
    templateUrl: './dot-content-type-in-use-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, MessageModule, TagModule, DotMessagePipe]
})
export class DotContentTypeInUseDialogComponent {
    /** Reference used to close the dialog with the user's choice. */
    readonly ref = inject(DynamicDialogRef);

    readonly #usage: DotContainersByContentType = inject<
        DynamicDialogConfig<DotContainersByContentType>
    >(DynamicDialogConfig).data ?? { containers: [], total: 0 };

    /** Containers that use the content type, passed as the dialog data. */
    readonly containers: DotContainer[] = this.#usage.containers;

    /** How many containers use the content type in all, as the message arguments. */
    readonly countArgs: string[] = [String(this.containers.length), String(this.#usage.total)];

    /** Whether more containers use the content type than the list shows. */
    readonly truncated = this.#usage.total > this.containers.length;
}
