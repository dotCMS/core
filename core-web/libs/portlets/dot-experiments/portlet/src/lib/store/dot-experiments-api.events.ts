import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotExperiment, HealthStatusTypes } from '@dotcms/dotcms-models';

import { DotExperimentPageInfo } from '../shared/models';

/**
 * What the backend answered: every event here is dispatched from a store event handler once a
 * request settles, never by a component. The matching `…Requested` events belong to
 * `dotExperimentsListPageEvents`.
 *
 * `…Succeeded` echoes the whole `DotExperiment` back for the CRUD flows so the component can
 * name the experiment in its toast without re-reading the list.
 */
export const dotExperimentsApiEvents = eventGroup({
    source: 'Experiments API',
    events: {
        // Analytics health gate
        healthCheckSucceeded: type<HealthStatusTypes>(),
        healthCheckFailed: type<unknown>(),

        // Load
        listSucceeded: type<DotExperiment[]>(),
        listFailed: type<unknown>(),

        // Page resolution (bulk lookup of the distinct pageIds)
        pageInfoSucceeded: type<Record<string, DotExperimentPageInfo>>(),
        pageInfoFailed: type<unknown>(),

        // CRUD
        archiveSucceeded: type<DotExperiment>(),
        archiveFailed: type<unknown>(),

        deleteSucceeded: type<DotExperiment>(),
        deleteFailed: type<unknown>(),

        endSucceeded: type<DotExperiment>(),
        endFailed: type<unknown>(),

        abortSucceeded: type<DotExperiment>(),
        abortFailed: type<unknown>(),

        cancelScheduleSucceeded: type<DotExperiment>(),
        cancelScheduleFailed: type<unknown>()
    }
});
