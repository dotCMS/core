import { Injectable, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';

import { DotRolesStore } from '../dot-roles-page/store/dot-roles.store';

/**
 * The delete-a-role flow, shared by every place that offers it: the tree's
 * context menu and the detail header's actions menu.
 *
 * Not `providedIn: 'root'` on purpose: it asks the host component's
 * `ConfirmationService`, so the confirmation renders in that component's
 * `<p-confirmDialog>`. Provide it next to `ConfirmationService`.
 */
@Injectable()
export class DotRoleDeleteService {
    readonly #store = inject(DotRolesStore);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(DotMessageService);
    readonly #alertService = inject(DotAlertConfirmService);

    /**
     * Asks the admin to confirm, then deletes the role.
     *
     * The store prunes the role from the tree and clears the selection when it
     * was the selected one. An HTTP failure is already reported by the store's
     * error manager; a delete the backend refuses with a 200 (child roles, a
     * workflow reference) gets an alert here, so it is never a silent no-op.
     */
    confirmDelete(role: { id: string; name: string }): void {
        this.#confirmationService.confirm({
            message: this.#messageService.get('roles.confirm.delete.message', role.name),
            header: this.#messageService.get('roles.confirm.delete.header'),
            acceptLabel: this.#messageService.get('roles.action.delete'),
            rejectLabel: this.#messageService.get('roles.action.cancel'),
            // Default (primary) accept styling — no red — per UX guidance for
            // this confirm even though the action is destructive.
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: async () => {
                const result = await this.#store.deleteRole(role.id);
                if (result && result.deleted === false) {
                    this.#alertService.alert({
                        header: this.#messageService.get('roles.confirm.delete.header'),
                        message: this.#messageService.get('roles.delete.rejected')
                    });
                }
            }
        });
    }
}
