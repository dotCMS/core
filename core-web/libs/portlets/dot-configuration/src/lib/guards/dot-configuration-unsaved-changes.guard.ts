import { inject, Signal } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

import { ConfirmationService, ConfirmEventType } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

/** Key shared by every confirmation the Configuration page raises through its own dialog. */
export const DOT_CONFIGURATION_CONFIRM_KEY = 'dot-configuration-confirm';

/**
 * What the guard needs from the page: whether there are unsaved edits, and the
 * `ConfirmationService` instance whose `<p-confirmDialog>` the page renders.
 */
export interface DotConfigurationUnsavedWork {
    readonly $hasUnsavedChanges: Signal<boolean>;
    readonly confirmationService: ConfirmationService;
}

/**
 * Asks before leaving the Configuration page with edits that were never saved. Nothing on the
 * page persists until Save Changes, so leaving loses every edit.
 *
 * Only the Discard button lets the navigation through. Closing the dialog with the X, Escape or
 * a click on the mask keeps the user on the page.
 */
export const dotConfigurationUnsavedChangesGuard: CanDeactivateFn<DotConfigurationUnsavedWork> = (
    component
) => {
    if (!component.$hasUnsavedChanges()) {
        return true;
    }

    const dotMessageService = inject(DotMessageService);

    return new Promise<boolean>((resolve) => {
        component.confirmationService.confirm({
            key: DOT_CONFIGURATION_CONFIRM_KEY,
            header: dotMessageService.get('configuration.unsaved.header'),
            message: dotMessageService.get('configuration.unsaved.message'),
            acceptLabel: dotMessageService.get('configuration.unsaved.keep'),
            rejectLabel: dotMessageService.get('configuration.unsaved.discard'),
            acceptIcon: 'hidden',
            rejectIcon: 'hidden',
            rejectButtonStyleClass: 'p-button-outlined',
            closable: true,
            closeOnEscape: true,
            accept: () => resolve(false),
            // PrimeNG sends dismissals (X, Escape, mask) through `reject` too, as CANCEL. Only
            // the Discard button is REJECT, and only that may throw the edits away.
            reject: (type?: ConfirmEventType) => resolve(type === ConfirmEventType.REJECT)
        });
    });
};
