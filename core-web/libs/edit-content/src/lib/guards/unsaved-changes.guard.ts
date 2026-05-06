import { Observable } from 'rxjs';

import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';

import { DotEditContentLayoutComponent } from '../components/dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * Route guard that warns the user about unsaved changes before navigating
 * away from the new edit-content editor. When the form is dirty, prompts
 * with a confirmation dialog: "Keep editing" cancels navigation, "Discard
 * changes" allows it. Pristine forms navigate immediately.
 *
 * Post-save navigations (e.g. publish creates a new inode and the workflow
 * flow programmatically navigates to it) bypass the prompt: the user's
 * changes were just committed, so there is nothing to discard. We detect
 * this via `workflowActionSuccess`, which the workflow feature sets before
 * triggering the navigation and the post-save effect clears immediately
 * after.
 */
export const unsavedChangesGuard: CanDeactivateFn<DotEditContentLayoutComponent> = (component) => {
    const dotAlertConfirmService = inject(DotAlertConfirmService);
    const dotMessageService = inject(DotMessageService);

    if (component.$store.workflowActionSuccess()) {
        return true;
    }

    if (!component.hasUnsavedChanges()) {
        return true;
    }

    return new Observable<boolean>((subscriber) => {
        const resolve = (allow: boolean) => {
            if (subscriber.closed) {
                return;
            }
            subscriber.next(allow);
            subscriber.complete();
        };

        dotAlertConfirmService.confirm({
            header: dotMessageService.get('edit.content.unsaved.changes.title'),
            message: dotMessageService.get('edit.content.unsaved.changes.message'),
            footerLabel: {
                accept: dotMessageService.get('edit.content.unsaved.changes.keep'),
                reject: dotMessageService.get('edit.content.unsaved.changes.discard')
            },
            // Primary "Keep editing": cancel navigation, user stays on the editor.
            accept: () => resolve(false),
            // Secondary "Discard changes": allow navigation. Reset the form's
            // dirty state first so a downstream `beforeunload` (e.g. when the
            // destination triggers a hard navigation) does not re-prompt with
            // the browser's native dialog.
            reject: () => {
                component.markFormPristine();
                resolve(true);
            }
        });
    });
};
