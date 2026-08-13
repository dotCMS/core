import {
    AllowedActionsByExperimentStatus,
    DotExperiment,
    DotExperimentStatus,
    GOAL_TYPES
} from '@dotcms/dotcms-models';

/** Every action of the list gated by `AllowedActionsByExperimentStatus`. */
export type ExperimentListAction = keyof typeof AllowedActionsByExperimentStatus;

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
    selectedGoals: GOAL_TYPES[];
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

/** `warn` (not `warning`) is PrimeNG's spelling — anything else yields no `p-tag-*` class. */
export type TagSeverity = 'success' | 'info' | 'warn' | 'secondary';

/** A table row: the experiment plus everything the template would otherwise have to derive. */
export interface ExperimentRow {
    experiment: DotExperiment;
    pagePath: string;
    /** i18n key of the primary goal type, or `null` when the experiment has no goal. */
    goalLabelKey: string | null;
    variants: number;
    schedule: string;
    statusSeverity: TagSeverity;
    statusLabelKey: string;
}

/** One selectable value inside a `dot-experiment-list-filter` popover. */
export interface ExperimentFilterOption {
    value: string;
    /** Translated, human-readable name. */
    label: string;
    /** Count for this value, stringified for the list item's secondary slot. */
    count: string;
    testId: string;
}
