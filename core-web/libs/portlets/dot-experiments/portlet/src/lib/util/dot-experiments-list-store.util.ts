import { Params } from '@angular/router';

import {
    DotCMSContentlet,
    DotExperiment,
    DotExperimentStatus,
    GOAL_TYPES
} from '@dotcms/dotcms-models';

import { goalTypeOf } from './dot-experiments-list.util';

import {
    DEFAULT_EXPERIMENTS_LIST_DIRECTION,
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
        direction: reader.get('direction')?.toUpperCase() === 'ASC' ? 'ASC' : 'DESC'
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
