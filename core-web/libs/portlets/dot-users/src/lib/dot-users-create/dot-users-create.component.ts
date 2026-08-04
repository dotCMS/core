import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

/**
 * Placeholder Create/Edit User dialog. The four-tab implementation
 * (Profile, Roles, Permissions, API Tokens) is delivered by sibling
 * tickets. The list-view ticket only wires up the entry point.
 */
@Component({
    selector: 'dot-users-create',
    standalone: true,
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-users-create.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUsersCreateComponent {
    private readonly dialogRef = inject(DynamicDialogRef);

    close(): void {
        this.dialogRef.close();
    }
}
