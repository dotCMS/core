import { Params } from '@angular/router';

import {
    DotCMSContentlet,
    DotExperiment,
    DotExperimentStatus,
    ExperimentsStatusList,
    GOAL_TYPES
} from '@dotcms/dotcms-models';

import { goalTypeOf } from './dot-experiments-list.util';

import {
    DEFAULT_EXPERIMENTS_LIST_DIRECTION,
    EXPERIMENTS_LIST_SORT_FIELDS,
    DEFAULT_EXPERIMENTS_LIST_GOALS,
    DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
    DEFAULT_EXPERIMENTS_LIST_PAGE,
    DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
    DEFAULT_EXPERIMENTS_LIST_STATUSES
} from '../shared/constants';
import { DotExperimentPageInfo, DotExperimentsListViewState } from '../shared/models';

/**
 * Pure helpers behind the experiments list store: URL parsing on the way in, response shaping
 * on the way out. Kept out of the store so each can be read — and tested — on its own, without
 * standing up the store, its injected services or its lifecycle hooks.
 */

/** Reads query params from either an `ActivatedRoute` snapshot or a parsed popstate URL. */
export interface QueryParamReader {
    get(key: string): string | null;
    getAll(key: string): string[];
}

export function fromRouteParams(params: Params): QueryParamReader {
    const values = (key: string): string[] => {
        const value: unknown = params[key];

        if (value == null) {
            return [];
        }

        return Array.isArray(value) ? value.map(String) : [String(value)];
    };

    return {
        get: (key) => values(key)[0] ?? null,
        getAll: values
    };
}

export function parseViewState(reader: QueryParamReader): DotExperimentsListViewState {
    return {
        filter: reader.get('filter') ?? '',
        selectedStatuses: parseStatuses(reader.getAll('status')),
        selectedGoals: parseGoals(reader.getAll('goal')),
        page: parsePositiveInteger(reader.get('page'), DEFAULT_EXPERIMENTS_LIST_PAGE),
        perPage: parsePositiveInteger(reader.get('per_page'), DEFAULT_EXPERIMENTS_LIST_PER_PAGE),
        orderBy: reader.get('orderby') || DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
        direction: reader.get('direction')?.toUpperCase() === 'ASC' ? 'ASC' : 'DESC',
        selectedPageId: null
    };
}

/**
 * An absent `status` param means "the default selection"; a present but unusable one (e.g.
 * `?status=`) means the user deselected everything, which is not the same thing.
 */
export function parseStatuses(rawStatuses: string[]): DotExperimentStatus[] {
    if (rawStatuses.length === 0) {
        return DEFAULT_EXPERIMENTS_LIST_STATUSES;
    }

    const allStatuses = Object.values(DotExperimentStatus);

    return rawStatuses
        .map((rawStatus) => rawStatus.toUpperCase() as DotExperimentStatus)
        .filter((status) => allStatuses.includes(status));
}

/** Same rule as {@link parseStatuses}: unknown values are dropped rather than trusted. */
export function parseGoals(rawGoals: string[]): GOAL_TYPES[] {
    if (rawGoals.length === 0) {
        return DEFAULT_EXPERIMENTS_LIST_GOALS;
    }

    const allGoals = Object.values(GOAL_TYPES);

    return rawGoals
        .map((rawGoal) => rawGoal.toUpperCase() as GOAL_TYPES)
        .filter((goal) => allGoals.includes(goal));
}

export function parsePositiveInteger(rawValue: string | null, fallback: number): number {
    const parsed = Number.parseInt(rawValue ?? '', 10);

    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export function distinctPageIds(experiments: DotExperiment[]): string[] {
    return [...new Set(experiments.map(({ pageId }) => pageId).filter(Boolean))];
}

export function toPageInfoByPageId(
    contentlets: DotCMSContentlet[]
): Record<string, DotExperimentPageInfo> {
    return contentlets.reduce<Record<string, DotExperimentPageInfo>>((pageInfo, contentlet) => {
        if (contentlet.identifier) {
            pageInfo[contentlet.identifier] = {
                url: contentlet.url ?? '',
                host: contentlet.host ?? ''
            };
        }

        return pageInfo;
    }, {});
}

/**
 * An experiment's goal type, or `null` when it has none. `goals` is keyed by level and the list
 * only ever shows the primary one, which is the same one the Goal column renders.
 */
export function goalTypeOfExperiment(experiment: DotExperiment): GOAL_TYPES | null {
    return goalTypeOf(experiment.goals);
}

export function emptyGoalCounts(): Record<GOAL_TYPES, number> {
    return Object.values(GOAL_TYPES).reduce(
        (counts, goal) => {
            counts[goal] = 0;

            return counts;
        },
        {} as Record<GOAL_TYPES, number>
    );
}

export function emptyStatusCounts(): Record<DotExperimentStatus, number> {
    return Object.values(DotExperimentStatus).reduce(
        (counts, status) => {
            counts[status] = 0;

            return counts;
        },
        {} as Record<DotExperimentStatus, number>
    );
}

/**
 * The inverse of {@link parseViewState}: the view state as query params.
 *
 * A value equal to its default is written as `null`, which removes the param — so a pristine
 * list has no query string at all rather than a URL restating every default.
 */
export function toQueryParams(
    view: DotExperimentsListViewState
): Record<string, string | string[] | null> {
    return {
        page: nullWhenDefault(view.page, DEFAULT_EXPERIMENTS_LIST_PAGE),
        per_page: nullWhenDefault(view.perPage, DEFAULT_EXPERIMENTS_LIST_PER_PAGE),
        orderby: nullWhenDefault(view.orderBy, DEFAULT_EXPERIMENTS_LIST_ORDER_BY),
        direction: nullWhenDefault(view.direction, DEFAULT_EXPERIMENTS_LIST_DIRECTION),
        filter: view.filter || null,
        status: isDefaultStatusSelection(view.selectedStatuses) ? null : view.selectedStatuses,
        goal:
            view.selectedGoals.length === DEFAULT_EXPERIMENTS_LIST_GOALS.length
                ? null
                : view.selectedGoals
    };
}

function nullWhenDefault<T extends string | number>(value: T, defaultValue: T): string | null {
    return value === defaultValue ? null : String(value);
}

/** Order-insensitive set comparison: a reordered default selection is still the default. */
function isDefaultStatusSelection(statuses: DotExperimentStatus[]): boolean {
    if (statuses.length !== DEFAULT_EXPERIMENTS_LIST_STATUSES.length) {
        return false;
    }

    const selected = new Set(statuses);

    return DEFAULT_EXPERIMENTS_LIST_STATUSES.every((status) => selected.has(status));
}

/** Comparator applied to a pair of experiments, before the direction factor. */
type ExperimentComparator = (a: DotExperiment, b: DotExperiment) => number;

/**
 * Lifecycle order, not alphabetical: sorting by status is only useful if Draft, Scheduled,
 * Running, Ended and Archived come out in the order an experiment actually moves through them.
 * Taken from `ExperimentsStatusList`, which is the same order the filter lists them in.
 */
const STATUS_ORDER = new Map<string, number>(
    ExperimentsStatusList.map(({ value }, index) => [value, index])
);

/** Case-insensitive, locale-aware, so `alpha` and `Alpha` sort together. */
function compareText(a: string, b: string): number {
    return a.localeCompare(b, undefined, { sensitivity: 'base' });
}

/**
 * The comparator for a sortable column, or `null` for anything unrecognised — an unknown
 * `orderby` (a hand-edited URL) then leaves the API order untouched rather than throwing.
 *
 * Missing values sort as empty or as `Infinity`, which puts unscheduled experiments and those
 * with no goal at the end while ascending.
 */
export function comparatorFor(
    field: string,
    pageInfoByPageId: Record<string, DotExperimentPageInfo>
): ExperimentComparator | null {
    switch (field) {
        case EXPERIMENTS_LIST_SORT_FIELDS.NAME:
            return (a, b) => compareText(a.name, b.name);

        case EXPERIMENTS_LIST_SORT_FIELDS.PAGE:
            return (a, b) =>
                compareText(
                    pageInfoByPageId[a.pageId]?.url ?? '',
                    pageInfoByPageId[b.pageId]?.url ?? ''
                );

        case EXPERIMENTS_LIST_SORT_FIELDS.GOAL:
            return (a, b) =>
                compareText(
                    goalTypeOfExperiment(a) ?? '\uffff',
                    goalTypeOfExperiment(b) ?? '\uffff'
                );

        case EXPERIMENTS_LIST_SORT_FIELDS.SCHEDULE:
            return (a, b) => startTimeOf(a) - startTimeOf(b);

        case EXPERIMENTS_LIST_SORT_FIELDS.STATUS:
            return (a, b) =>
                (STATUS_ORDER.get(a.status) ?? Number.MAX_SAFE_INTEGER) -
                (STATUS_ORDER.get(b.status) ?? Number.MAX_SAFE_INTEGER);

        case EXPERIMENTS_LIST_SORT_FIELDS.MOD_DATE:
            return (a, b) => a.modDate - b.modDate;

        default:
            return null;
    }
}

/** Unscheduled experiments have no start date, so they sort after every scheduled one. */
function startTimeOf(experiment: DotExperiment): number {
    // Already an epoch, so it compares directly.
    return experiment.scheduling?.startDate ?? Number.POSITIVE_INFINITY;
}

/** Shape of the `/api/content/_search` entity the page lookup reads contentlets from. */
interface PageLookupEntity {
    jsonObjectView?: { contentlets?: DotCMSContentlet[] };
}

/**
 * Page info for a lookup response, and a warning when the response did not cover every page
 * asked for.
 *
 * An unresolved page is dropped by the site filter, which fails closed — so a short response
 * shortens the list with no error anywhere and a total that agrees with it. That is
 * indistinguishable from reality on screen, so the shortfall is at least made diagnosable here.
 */
export function resolvedPageInfo(
    entity: PageLookupEntity | null | undefined,
    requestedPageIds: string[]
): Record<string, DotExperimentPageInfo> {
    const pageInfo = toPageInfoByPageId(entity?.jsonObjectView?.contentlets ?? []);
    const missing = requestedPageIds.filter((pageId) => !pageInfo[pageId]);

    if (missing.length) {
        console.warn(
            `[experiments] page lookup resolved ${requestedPageIds.length - missing.length} of ${
                requestedPageIds.length
            } pages. Experiments on the rest are hidden from the list.`,
            missing
        );
    }

    return pageInfo;
}
