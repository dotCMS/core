import { inject, Signal } from '@angular/core';
import { CanDeactivateFn, RouterStateSnapshot } from '@angular/router';

import { ConfirmationService, ConfirmEventType } from 'primeng/api';

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
 *
 * The prompt itself lives in {@link confirmLeavingUnsavedChanges}, because the panel needs the same
 * question and has no route to hang a guard on. All this adds is the one thing that *is* routing:
 * the creation redirect, which only exists as a pair of URLs.
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

    return confirmLeavingUnsavedChanges(component, inject(DotMessageService));
};

/**
 * Whoever is holding unsaved work and can raise the dialog over it.
 *
 * Structural on purpose: the Configure screen, its header and the panel reach this from three
 * different places in the tree, and only two members are common to all of them. The
 * `ConfirmationService` matters as much as the store — it is provided by the Configure component,
 * so an injector below it resolves the instance whose `<p-confirmDialog>` is actually rendered,
 * and one resolved from anywhere else would ask into a dialog nobody can see.
 */
export interface UnsavedWorkSource {
    store: { $hasUnsavedChanges: Signal<boolean> };
    confirmationService: ConfirmationService;
}

/**
 * The same question, asked without a route to hang it on.
 *
 * The panel has no `canDeactivate`: closing it is a click on an X, a mask or Escape, and going
 * Back inside it changes a store field rather than a URL. Router-shaped protection reaches none of
 * those, so the prompt has to be callable directly — and it has to be *this* prompt, not a second
 * one worded and behaved almost the same. What the editor loses is identical either way.
 *
 * Resolves `true` when it is safe to proceed: nothing unsaved, or the editor chose to discard.
 *
 * @param component the Configure screen holding the work — its store answers what is unsaved, and
 * its `ConfirmationService` is the instance whose `<p-confirmDialog>` is actually on screen
 * @param dotMessageService injected by the caller, which is in an injection context where this is
 * not
 */
export function confirmLeavingUnsavedChanges(
    source: UnsavedWorkSource,
    dotMessageService: DotMessageService
): boolean | Promise<boolean> {
    if (!source.store.$hasUnsavedChanges()) {
        return true;
    }

    return new Promise<boolean>((resolve) => {
        source.confirmationService.confirm({
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
}

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
