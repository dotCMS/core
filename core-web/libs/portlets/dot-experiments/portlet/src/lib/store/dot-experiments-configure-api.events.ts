import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotPageLockInfo } from '@dotcms/data-access';
import { DotExperiment, DotExperimentPatchBody } from '@dotcms/dotcms-models';

import { DotExperimentConfigurePage } from '../shared/models';

/**
 * What the backend answered on the Configure screen: every event here is dispatched from a store
 * event handler once a request settles, never by a component. The matching intents belong to
 * `dotExperimentsConfigurePageEvents`.
 *
 * Autosave settles through one `save…` triple rather than a pair per field: the whole accumulated
 * diff travels in a single PATCH, so it succeeds, fails or turns out to be nothing to send as one
 * unit.
 *
 * Every `…Succeeded` carries the experiment the server answered with: it is the source of truth
 * after any write, and the shell needs its name for the toast copy. `saveSucceeded` carries the
 * body it wrote beside it, because the diff it settles is not necessarily the whole pending one.
 */
export const dotExperimentsConfigureApiEvents = eventGroup({
    source: 'Experiments Configure API',
    events: {
        // Load of an existing experiment
        loadSucceeded: type<DotExperiment>(),
        loadFailed: type<unknown>(),

        // Creation. `createRequested` is raised by the handler as the POST leaves, so the
        // reducer can close the door on a second one before the first answers.
        createRequested: type<void>(),
        createSucceeded: type<DotExperiment>(),
        createFailed: type<unknown>(),

        // Autosave. One triple for the whole accumulated diff.
        /**
         * A real PATCH left with a body. The debounce window before it is deliberately not part
         * of this: the visible progress indicator keys on the flight alone, while the footer's
         * "Saving…" copy covers the whole pending window through `$isAutosaving`.
         */
        saveRequested: type<void>(),
        /**
         * `sent` is the body the PATCH carried, which is not always every pending key:
         * `toOutgoingPatch` holds back what the backend would reject. Only the keys that were
         * written settle, so the rest stays pending instead of being dropped (#37003).
         */
        saveSucceeded: type<{ experiment: DotExperiment; sent: DotExperimentPatchBody }>(),
        /** The diff stays pending, so the next edit re-sends it merged with whatever changed. */
        saveFailed: type<unknown>(),
        /**
         * The debounce elapsed with nothing worth sending — a diff whose only key was a blank
         * name, an experiment that does not exist yet, or weights that are still mid-edit. No call
         * went out, so nothing settles: this changes no state and is here as the record that the
         * flush happened at all.
         */
        saveSkipped: type<void>(),

        // Variants
        addVariantSucceeded: type<DotExperiment>(),
        addVariantFailed: type<unknown>(),

        editVariantSucceeded: type<DotExperiment>(),
        editVariantFailed: type<unknown>(),

        removeVariantSucceeded: type<DotExperiment>(),
        removeVariantFailed: type<unknown>(),

        // Transitions. `startRequested` is raised by the handler as the call leaves, on the same
        // contract as `createRequested`: the reducer closes the door on a second Start press
        // before the first one answers.
        startRequested: type<void>(),
        startSucceeded: type<DotExperiment>(),
        startFailed: type<unknown>(),

        stopSucceeded: type<DotExperiment>(),
        stopFailed: type<unknown>(),

        cancelScheduleSucceeded: type<DotExperiment>(),
        cancelScheduleFailed: type<unknown>(),

        // Aborting a running experiment cancels it — there is no dedicated abort endpoint — so
        // this is a distinct event over the same call, for distinct toast copy.
        abortSucceeded: type<DotExperiment>(),
        abortFailed: type<unknown>(),

        // Page prefill and lock state
        pagePrefillResolved: type<DotExperimentConfigurePage>(),
        pagePrefillFailed: type<unknown>(),
        pageLockResolved: type<DotPageLockInfo>()
    }
});
