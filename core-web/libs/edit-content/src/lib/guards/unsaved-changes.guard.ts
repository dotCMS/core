import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

import { ConfirmEventType } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

import { DotEditContentLayoutComponent } from '../components/dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * Route guard that warns the user about unsaved changes before navigating
 * away from the new edit-content editor. When the form is dirty, prompts
 * with a confirmation dialog: "Keep editing" cancels navigation, "Discard
 * changes" allows it. Pristine forms navigate immediately.
 *
 * The guard talks to PrimeNG's `ConfirmationService` directly via the
 * layout component (instead of going through the global
 * `DotAlertConfirmService` wrapper). This:
 *
 * - Avoids any change to the shared global wrapper or its template.
 * - Guarantees the `<p-confirmDialog />` rendered inside
 *   `dot-edit-content.layout.component.html` and the request emitted from
 *   here resolve to the same `ConfirmationService` instance — the layout
 *   component provides it at component level, so both the dialog and this
 *   guard's payload travel through the same Subject.
 *
 * Post-save navigations (e.g. publish creates a new inode and the workflow
 * flow programmatically navigates to it) bypass the prompt: the user's
 * changes were just committed, so there is nothing to discard. We detect
 * this via `workflowActionSuccess`, which the workflow feature sets before
 * triggering the navigation and the post-save effect clears immediately
 * after.
 */
export const unsavedChangesGuard: CanDeactivateFn<DotEditContentLayoutComponent> = (component) => {
    const dotMessageService = inject(DotMessageService);

    if (component.$store.workflowActionSuccess()) {
        return true;
    }

    if (!component.hasUnsavedChanges()) {
        return true;
    }

    return new Promise<boolean>((resolve) => {
        component.confirmationService.confirm({
            header: dotMessageService.get('edit.content.unsaved.changes.title'),
            message: dotMessageService.get('edit.content.unsaved.changes.message'),
            acceptLabel: dotMessageService.get('edit.content.unsaved.changes.keep'),
            rejectLabel: dotMessageService.get('edit.content.unsaved.changes.discard'),
            // Hide PrimeNG's default check / cancel icons — the dotCMS
            // unsaved-changes prompt is intentionally text-only.
            acceptIcon: 'hidden',
            rejectIcon: 'hidden',
            // Match the visual hierarchy of the legacy custom footer:
            // primary "Keep editing" (filled) on the right, secondary
            // "Discard changes" (outlined) on the left.
            rejectButtonStyleClass: 'p-button-outlined',
            // Primary "Keep editing": cancel navigation, user stays on the editor.
            accept: () => resolve(false),
            // PrimeNG routes three different user actions through this single
            // callback. Discriminate by `ConfirmEventType` so dismissals
            // (close icon, ESC, mask click) behave like "Keep editing"
            // instead of silently discarding the user's work.
            //   REJECT → user clicked the secondary "Discard changes" button.
            //            Reset the form's dirty state first so a downstream
            //            `beforeunload` (when the destination triggers a hard
            //            navigation) does not re-prompt with the browser's
            //            native dialog.
            //   CANCEL → user dismissed the dialog via the X icon or ESC.
            //            Treat as "Keep editing": stay on the editor with the
            //            form still dirty.
            reject: (type?: ConfirmEventType) => {
                if (type === ConfirmEventType.REJECT) {
                    component.markFormPristine();
                    resolve(true);

                    return;
                }
                resolve(false);
            }
        });
    });
};
