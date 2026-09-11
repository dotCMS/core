import { inject } from '@angular/core';
import { CanDeactivateFn, RouterStateSnapshot } from '@angular/router';

import { ConfirmEventType } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { CONFIGURATION_CONFIRM_DIALOG_KEY } from '@dotcms/dotcms-models';

import { DotExperimentsConfigureComponent } from '../dot-experiments-configure/dot-experiments-configure.component';
import { CONFIGURATION_SEGMENT, NEW_EXPERIMENT_SEGMENT } from '../shared/constants';

/**
 * Warns before leaving the Configure screen with work the server has not accepted.
 *
 * The screen writes only when Save Draft is pressed, so everything typed since the last press
 * lives in memory and dies with the component. This is the one thing standing between a stray
 * click on Back and losing it.
 *
 * Talks to PrimeNG's `ConfirmationService` through the component rather than through the global
 * `DotAlertConfirmService`, for the same reason the Edit Content guard does: the shell provides
 * the service at component level and renders `<p-confirmDialog>` from its own template, so going
 * through the component is what guarantees the request and the dialog resolve to the same
 * instance. The dialog is keyed, so the request carries the key too.
 *
 * A save that just succeeded needs no flag to bypass the prompt: settling the form is what makes
 * the screen clean, so the guard finds nothing to warn about.
 */
export const experimentsUnsavedChangesGuard: CanDeactivateFn<DotExperimentsConfigureComponent> = (
    component,
    _currentRoute,
    currentState,
    nextState
) => {
    if (isCreationRedirect(component, currentState, nextState)) {
        return true;
    }

    const dotMessageService = inject(DotMessageService);

    if (!component.store.$hasUnsavedChanges()) {
        return true;
    }

    return new Promise<boolean>((resolve) => {
        component.confirmationService.confirm({
            key: CONFIGURATION_CONFIRM_DIALOG_KEY,
            header: dotMessageService.get('experiments.configure.unsaved.title'),
            message: dotMessageService.get('experiments.configure.unsaved.message'),
            acceptLabel: dotMessageService.get('experiments.configure.unsaved.keep'),
            rejectLabel: dotMessageService.get('experiments.configure.unsaved.discard'),
            // Text-only, like every other unsaved-changes prompt in the admin.
            acceptIcon: 'hidden',
            rejectIcon: 'hidden',
            rejectButtonStyleClass: 'p-button-outlined',
            closable: true,
            closeOnEscape: true,
            // Primary "Keep Editing": cancel the navigation, the user stays put.
            accept: () => resolve(false),
            /**
             * PrimeNG funnels three different user actions through this one callback, so the type
             * has to be read rather than assumed. Only REJECT is the secondary button; CANCEL is a
             * dismissal — the X icon, ESC, a click on the mask — and a dismissal must never be
             * taken as permission to throw the work away.
             */
            reject: (type?: ConfirmEventType) => resolve(type === ConfirmEventType.REJECT)
        });
    });
};

/**
 * Whether this is the screen redirecting itself off `new` once the POST answered.
 *
 * It is not the user leaving, and it must never be challenged. Both routes are the same route
 * config, so the component is reused and nothing is lost — but `canDeactivate` fires all the same,
 * and at that instant the follow-up PATCH carrying whatever the POST could not write is still in
 * flight, which reads as unsaved work. Prompting there cancels the redirect and strands the screen
 * on `/experiments/new` behind a dialog about changes that were on their way to the server.
 */
function isCreationRedirect(
    component: DotExperimentsConfigureComponent,
    currentState: RouterStateSnapshot,
    nextState: RouterStateSnapshot
): boolean {
    const experimentId = component.store.experiment()?.id;

    return (
        !!experimentId &&
        currentState.url.endsWith(`/${NEW_EXPERIMENT_SEGMENT}`) &&
        nextState.url.endsWith(`/${experimentId}/${CONFIGURATION_SEGMENT}`)
    );
}
