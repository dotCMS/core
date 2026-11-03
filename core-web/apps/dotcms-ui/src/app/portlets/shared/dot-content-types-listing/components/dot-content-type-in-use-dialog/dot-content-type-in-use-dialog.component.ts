import { Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';

import { DotContainer } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/**
 * Dialog shown before deleting a content type that containers use: it lists the containers and
 * lets the user delete anyway or cancel. Closes with `true` on Delete and `false` on Cancel.
 */
@Component({
    selector: 'dot-content-type-in-use-dialog',
    templateUrl: './dot-content-type-in-use-dialog.component.html',
    imports: [ButtonModule, MessageModule, DotMessagePipe]
})
export class DotContentTypeInUseDialogComponent {
    /** Reference used to close the dialog with the user's choice. */
    readonly ref = inject(DynamicDialogRef);
    /** Containers that use the content type, passed as the dialog data. */
    readonly containers: DotContainer[] =
        inject<DynamicDialogConfig<DotContainer[]>>(DynamicDialogConfig).data ?? [];
}
