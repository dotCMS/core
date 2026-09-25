import { Confirmation } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

/**
 * The confirmation shown before a role is deleted, shared by every place that offers it — the
 * tree's context menu and the detail header's actions menu — so the copy and the button styling
 * cannot drift between them.
 *
 * Only the options: the component still opens it with its own `ConfirmationService`, so it renders
 * in that component's `<p-confirmDialog>`, and `onAccept` is where it calls the store. The store
 * owns everything after that, including the warning when the backend refuses the delete.
 *
 * @param role the role about to be deleted; its name goes into the message
 * @param messages resolves the copy
 * @param onAccept runs when the admin confirms
 */
export function roleDeleteConfirmation(
    role: { name: string },
    messages: DotMessageService,
    onAccept: () => void
): Confirmation {
    return {
        message: messages.get('roles.confirm.delete.message', role.name),
        header: messages.get('roles.confirm.delete.header'),
        acceptLabel: messages.get('roles.action.delete'),
        rejectLabel: messages.get('roles.action.cancel'),
        // Default (primary) accept styling — no red — per UX guidance for this confirm even
        // though the action is destructive.
        rejectButtonStyleClass: 'p-button-text',
        defaultFocus: 'reject',
        closable: true,
        closeOnEscape: true,
        position: 'center',
        accept: onAccept
    };
}
