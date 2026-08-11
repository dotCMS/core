import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotExperiment, DotExperimentStatus } from '@dotcms/dotcms-models';

/** Sort direction used by the experiments list. */
export type DotExperimentsListSortDirection = 'ASC' | 'DESC';

/**
 * Page data resolved for an experiment's `pageId`. `DotExperiment` carries no host, so
 * `host` is the only way to scope the experiments list to the current site, and `url` is
 * the path rendered in the Page column.
 */
export interface DotExperimentPageInfo {
    url: string;
    host: string;
}

/** The URL-backed slice of the list view: filter, status selection, paging and sort. */
export interface DotExperimentsListViewState {
    filter: string;
    selectedStatuses: DotExperimentStatus[];
    page: number;
    perPage: number;
    orderBy: string;
    direction: DotExperimentsListSortDirection;
}

/** Paging change emitted by the table paginator. */
export interface DotExperimentsListPageChange {
    page: number;
    perPage: number;
}

/** Sort change emitted by the table header. */
export interface DotExperimentsListSortChange {
    orderBy: string;
    direction: DotExperimentsListSortDirection;
}

/**
 * Every event the experiments list can dispatch.
 *
 * Load and CRUD flows follow a `Requested -> Succeeded | Failed` triple. CRUD `Requested`
 * events carry the whole `DotExperiment` (not just the id) because the component needs the
 * name for its confirmation and toast copy, and `Succeeded` echoes it back for the same reason.
 *
 * Confirmations and toasts are the component's job — the store never opens UI.
 */
export const dotExperimentsListEvents = eventGroup({
    source: 'Experiments List',
    events: {
        // Load
        listRequested: type<void>(),
        listSucceeded: type<DotExperiment[]>(),
        listFailed: type<unknown>(),

        // Page resolution (bulk lookup of the distinct pageIds)
        pageInfoSucceeded: type<Record<string, DotExperimentPageInfo>>(),
        pageInfoFailed: type<unknown>(),

        // View state (URL-backed)
        filterChanged: type<string>(),
        statusesChanged: type<DotExperimentStatus[]>(),
        pageChanged: type<DotExperimentsListPageChange>(),
        sortChanged: type<DotExperimentsListSortChange>(),
        hydratedFromUrl: type<DotExperimentsListViewState>(),

        // Site
        siteChanged: type<string | null>(),

        // CRUD
        archiveRequested: type<DotExperiment>(),
        archiveSucceeded: type<DotExperiment>(),
        archiveFailed: type<unknown>(),

        deleteRequested: type<DotExperiment>(),
        deleteSucceeded: type<DotExperiment>(),
        deleteFailed: type<unknown>(),

        endRequested: type<DotExperiment>(),
        endSucceeded: type<DotExperiment>(),
        endFailed: type<unknown>(),

        abortRequested: type<DotExperiment>(),
        abortSucceeded: type<DotExperiment>(),
        abortFailed: type<unknown>(),

        cancelScheduleRequested: type<DotExperiment>(),
        cancelScheduleSucceeded: type<DotExperiment>(),
        cancelScheduleFailed: type<unknown>()
    }
});
