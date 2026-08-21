import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotExperimentPatchBody } from '@dotcms/dotcms-models';

import { DotExperimentConfigurePage } from '../shared/models';

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

        // Page selection. `pageSelected` only applies before creation — the page is immutable
        // afterwards, since `PATCH /api/v1/experiments/{id}` does not accept `pageId`.
        pageSelected: type<DotExperimentConfigurePage>(),
        pagePrefillRequested: type<ConfigurePagePrefill>(),

        /**
         * A field was edited, carrying only the PATCH keys it changed.
         *
         * The store merges the payload into the diff it is holding and flushes the whole thing
         * once the typing stops, so two cards edited in the same window reach the server as one
         * multi-key call. A key is only ever present when its value actually changed — the cards
         * compare against what is stored before dispatching.
         */
        formEdited: type<DotExperimentPatchBody>(),

        // Variants: dedicated endpoints, not a `trafficProportion` PATCH. Their *weights* are not
        // here at all — they are a slice of the screen's form, and travel as `formEdited`.
        variantAdded: type<string>(),
        variantRenamed: type<ConfigureVariantRename>(),
        variantDeleted: type<string>(),

        // Transitions. Each is already confirmed in the shell where a confirmation applies.
        startRequested: type<void>(),
        stopRequested: type<void>(),
        cancelScheduleRequested: type<void>(),
        abortRequested: type<void>()
    }
});
