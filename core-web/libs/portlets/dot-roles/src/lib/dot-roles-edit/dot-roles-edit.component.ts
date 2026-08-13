import { Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRoleDetail } from '../models/dot-roles.models';

/**
 * Edit Role dialog.
 *
 * The full form (name / key / parent / can-grant / description) is not yet
 * wired to a save endpoint because `PUT /v1/roles/{roleId}` does not exist
 * (issue #36936). Same for `Delete Role` which needs #36939.
 *
 * This component ships as a **read-only placeholder** so the design flow is
 * navigable and the button surface is discoverable. The Save and Delete
 * actions are visible but disabled with a tooltip explaining the block.
 * When #36936 and #36939 land, wire the form and re-enable.
 */
@Component({
    selector: 'dot-roles-edit',
    standalone: true,
    imports: [ButtonModule, TooltipModule, DotMessagePipe],
    templateUrl: './dot-roles-edit.component.html'
})
export class DotRolesEditComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig);

    protected readonly role: DotRoleDetail = this.#config.data?.role;

    protected onCancel(): void {
        this.#ref.close();
    }
}
