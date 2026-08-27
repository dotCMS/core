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
         * `form` is the form as it stood when the PATCH was built, not as it stands when the
         * answer arrives. Those differ whenever the user kept typing during the flight, and it is
         * the first one the server was told about — snapshotting the later one would declare that
         * typing saved and lose it (#37003).
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
