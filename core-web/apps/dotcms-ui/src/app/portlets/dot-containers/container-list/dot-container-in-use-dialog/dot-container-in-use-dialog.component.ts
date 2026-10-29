import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';

import { DotContainerDependencies } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/**
 * Dialog shown when a delete is blocked because templates still use the containers. It lists,
 * for each container, the templates the user has to clean up first. Opened with the
 * {@link DotContainerDependencies} of the containers in use as the dialog `data`.
 */
@Component({
    selector: 'dot-container-in-use-dialog',
    templateUrl: './dot-container-in-use-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, MessageModule, DotMessagePipe]
})
export class DotContainerInUseDialogComponent {
    readonly ref = inject(DynamicDialogRef);

    readonly containers: DotContainerDependencies[] = inject(DynamicDialogConfig).data;

    /** With several containers each list needs its container's name; with one the header has it. */
    readonly hasMany = this.containers.length > 1;
}
