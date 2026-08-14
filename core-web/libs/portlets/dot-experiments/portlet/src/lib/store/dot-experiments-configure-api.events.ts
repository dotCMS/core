import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotPageLockInfo } from '@dotcms/data-access';
import { DotExperiment } from '@dotcms/dotcms-models';

import { DotExperimentConfigurePage, ExperimentFieldGroup } from '../shared/models';

/**
 * What the backend answered on the Configure screen: every event here is dispatched from a store
 * event handler once a request settles, never by a component. The matching intents belong to
 * `dotExperimentsConfigurePageEvents`.
 *
 * Autosave has one `Succeeded`/`Failed` pair per field group rather than a shared pair, so a
 * failed Goal PATCH cannot roll back what the Scheduling PATCH just saved — each group settles
 * on its own.
 *
 * Every `…Succeeded` carries the experiment the server answered with: it is the source of truth
 * after any write, and the shell needs its name for the toast copy.
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

        /**
         * A debounced edit that reached its handler with nothing worth sending — the value the
         * server already holds, a goal that was cleared, a blank name. No call goes out, so this
         * is the only thing that can settle the group: without it the group would stay pending
         * for the rest of the session and the screen would report itself as autosaving forever.
         */
        autosaveSkipped: type<ExperimentFieldGroup>(),

        // Autosave, one pair per field group
        nameSucceeded: type<DotExperiment>(),
        nameFailed: type<unknown>(),

        descriptionSucceeded: type<DotExperiment>(),
        descriptionFailed: type<unknown>(),

        goalSucceeded: type<DotExperiment>(),
        goalFailed: type<unknown>(),

        schedulingSucceeded: type<DotExperiment>(),
        schedulingFailed: type<unknown>(),

        trafficAllocationSucceeded: type<DotExperiment>(),
        trafficAllocationFailed: type<unknown>(),

        trafficProportionSucceeded: type<DotExperiment>(),
        trafficProportionFailed: type<unknown>(),

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
