import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import {
    ConfigureFormModel,
    ConfigureFormValidity,
    DotExperimentConfigurePage
} from '../shared/models';

/** What `?pageId=`/`?url=` asked the Page card to preselect. Either one may be present. */
export interface ConfigurePagePrefill {
    pageId?: string | null;
    url?: string | null;
}

/** A rename of a single variant, which has its own endpoint rather than a `trafficProportion` PATCH. */
export interface ConfigureVariantRename {
    variantId: string;
    name: string;
}

/**
 * What the Configure page asks for: user intent and lifecycle, never a result.
 *
 * Every event here is dispatched by the screen itself — a keystroke in a card, a page picked in
 * the dialog, a Start press, the shell coming up. What comes *back* lives in
 * `dotExperimentsConfigureApiEvents`, so the two halves of an async flow are never confused.
 *
 * Field changes are one single event carrying the keys that changed, not one event per field:
 * `PATCH /api/v1/experiments/{id}` applies every key of its body in one atomic update, so the
 * store accumulates the edits and flushes them as a single call.
 *
 * Confirmations and toasts are the shell's job — the store never opens UI.
 */
export const dotExperimentsConfigurePageEvents = eventGroup({
    source: 'Experiments Configure Page',
    events: {
        // Entry. `/experiments/new` starts empty; `/experiments/:id/configuration` loads.
        enterNew: type<void>(),
        enterExisting: type<string>(),

        // Page selection. Applies before creation, and afterwards while the experiment is a
        // draft whose only variant is the control — the rule `PATCH /api/v1/experiments/{id}`
        // enforces server-side (see specs/37176-draft-experiment-page-change).
        pageSelected: type<DotExperimentConfigurePage>(),
        pagePrefillRequested: type<ConfigurePagePrefill>(),

        /**
         * Change Page was pressed on a draft whose variants have to go first, so the confirmation
         * is about to open.
         *
         * Reported rather than kept in the card because the outcome of the *last* attempt is store
         * state: a run that was refused, then cancelled, would otherwise greet the next press with
         * the error it ended on.
         */
        pageChangeRequested: type<void>(),

        /**
         * The confirmation was accepted: the variants are to be deleted so the page can move.
         *
         * It covers the deletion alone. The page that replaces the current one arrives afterwards
         * as its own `pageSelected`, once the picker the dialog hands over to answers.
         */
        pageChangeConfirmed: type<void>(),

        /**
         * The form changed, carrying its whole current value.
         *
         * The whole value rather than a diff: the store keeps a mirror of what is on screen and a
         * snapshot of what was last written, and comparing those two is all the dirty state this
         * screen needs. Working out *which* keys moved bought nothing once saving stopped racing
         * the keystrokes.
         */
        formChanged: type<{ value: ConfigureFormModel; validity: ConfigureFormValidity }>(),

        // Variants: dedicated endpoints, not a `trafficProportion` PATCH. Their *weights* are not
        // here at all — they are a slice of the screen's form, and travel with `formChanged`.
        variantAdded: type<string>(),
        variantRenamed: type<ConfigureVariantRename>(),
        variantDeleted: type<string>(),

        /**
         * Save draft was pressed.
         *
         * The only thing that writes the form to the server. Before the experiment exists it is
         * what creates it — the POST carries the name, description and page — and afterwards it
         * flushes the accumulated diff as a single PATCH.
         */
        saveDraftRequested: type<void>(),

        // Transitions. Each is already confirmed in the shell where a confirmation applies.
        startRequested: type<void>(),
        stopRequested: type<void>(),
        cancelScheduleRequested: type<void>(),
        abortRequested: type<void>()
    }
});
