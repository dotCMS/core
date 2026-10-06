import { Component, computed, inject } from '@angular/core';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotRolesEditComponent } from '../../../dot-roles-edit/dot-roles-edit.component';
import { roleDeleteConfirmation } from '../../../utils/dot-role-delete.utils';
import { DotRolesStore } from '../../store/dot-roles.store';

@Component({
    selector: 'dot-roles-detail-header',
    imports: [
        ButtonModule,
        ConfirmDialogModule,
        DynamicDialogModule,
        MenuModule,
        SkeletonModule,
        DotMessagePipe
    ],
    providers: [DialogService, ConfirmationService],
    templateUrl: './dot-roles-detail-header.component.html',
    host: { class: 'block' }
})
export class DotRolesDetailHeaderComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #dialogService = inject(DialogService);
    readonly #messageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);

    /**
     * The header's actions menu: a "Role" group heading over Edit, a divider,
     * and Delete. Both actions follow `canModifyRole` — the backend rejects
     * editing or deleting a system or locked role with a 403.
     */
    protected readonly $actionsMenuItems = computed<MenuItem[]>(() => {
        const readOnly = !this.store.canModifyRole();

        return [
            {
                label: this.#messageService.get('roles.menu.header'),
                items: [
                    {
                        label: this.#messageService.get('roles.action.edit'),
                        disabled: readOnly,
                        command: () => this.onEditRole()
                    },
                    { separator: true },
                    {
                        label: this.#messageService.get('roles.action.delete'),
                        disabled: readOnly,
                        command: () => this.onDeleteRole()
                    }
                ]
            }
        ];
    });

    /** Opens the Edit dialog for the selected role. */
    protected onEditRole(): void {
        const role = this.store.selectedRole();
        if (!role) {
            return;
        }

        this.#dialogService.open(DotRolesEditComponent, {
            header: this.#messageService.get('roles.edit.title'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            data: { role }
        });
    }

    /** Confirms, then deletes the selected role. */
    protected onDeleteRole(): void {
        const role = this.store.selectedRole();
        if (!role) {
            return;
        }

        this.#confirmationService.confirm(
            roleDeleteConfirmation(role, this.#messageService, () => this.store.deleteRole(role.id))
        );
    }
}
