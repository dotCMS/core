import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotPageLockInfo } from '@dotcms/data-access';
import { DotExperiment } from '@dotcms/dotcms-models';

import { ConfigureFormModel, DotExperimentConfigurePage } from '../shared/models';

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
         * `form` is the form the request was built from, not the form as it stands when the answer
         * arrives — those differ whenever the user kept typing during the flight, and only the
         * first one was ever sent. `experiment` is what came back, and the weights are read off it
         * rather than off `form`: variants are server state, and the card mirrors whatever the
         * response holds (#37003).
         */
        saveSucceeded: type<{ experiment: DotExperiment; form: ConfigureFormModel }>(),
        /** The form stays dirty, so pressing Save Draft again is the retry. */
        saveFailed: type<unknown>(),
        /**
         * Save Draft was pressed with nothing worth sending — a blank name, an experiment that
         * does not exist yet, or weights that are still mid-edit. No call went out, so nothing
         * settles: this changes no state and is here as the record that the press happened at all.
         */
        saveSkipped: type<void>(),

        // Variants
        addVariantSucceeded: type<DotExperiment>(),
        addVariantFailed: type<unknown>(),

        editVariantSucceeded: type<DotExperiment>(),
        editVariantFailed: type<unknown>(),

        removeVariantSucceeded: type<DotExperiment>(),
        removeVariantFailed: type<unknown>(),

        /**
         * The deletions are on the wire. Raised by the handler as the first call leaves, on the
         * same contract as `createRequested` and `startRequested`: the reducer closes the door on a
         * second run before the first one answers, which a reducer keyed on the *intent* could not
         * do — it runs before the handler, so the handler's own guard would already see the flag it
         * had just set.
         */
        deleteVariantsRequested: type<void>(),
        /**
         * Every variant standing in the way of a page change is gone, and the experiment is the
         * one the last deletion answered with.
         *
         * Also the answer when there was nothing left to delete: the page is free either way, and
         * the confirmation waiting on this would otherwise hang on a run that never went out.
         */
        deleteVariantsSucceeded: type<DotExperiment>(),
        /**
         * One of those deletions was refused, so the page change does not happen.
         *
         * `experiment` is whatever the last deletion that *did* succeed answered with, or `null`
         * when the first one failed: the calls run one after another, so a rejection halfway
         * leaves some variants already deleted and the card has to show the list as it now stands.
         *
         * Reported unobtrusively — a toast rather than the usual alert dialog — because the Change
         * Page dialog is still open and stays open: a modal stacked on top of it would bury the
         * retry. The dialog states the failure inline beside its own buttons.
         */
        deleteVariantsFailed: type<{ error: unknown; experiment: DotExperiment | null }>(),

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
        // The link named a page that is not there: an answer, not a failure of the call.
        pagePrefillFailed: type<unknown>(),
        // The lookup itself was rejected, so whether the page exists is unknown. Kept apart from
        // `pagePrefillFailed` because the two need different copy, and only this one is an error
        // to report.
        pagePrefillLookupFailed: type<unknown>(),
        pageLockResolved: type<DotPageLockInfo>()
    }
});
