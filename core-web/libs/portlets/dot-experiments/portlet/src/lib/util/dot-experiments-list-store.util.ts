import { endOfDay, format, isValid, parse, startOfDay } from 'date-fns';

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
    DEFAULT_EXPERIMENTS_LIST_STATUSES,
    SCHEDULE_BOUND_FORMAT
} from '../shared/constants';
import {
    DotExperimentPageInfo,
    DotExperimentsListViewState,
    ExperimentsListSchedulePeriod
} from '../shared/models';

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
        selectedCreators: parseCreators(reader.getAll('created_by')),
        scheduleFrom: normalizeScheduleBound(reader.get('schedule_from')),
        scheduleTo: normalizeScheduleBound(reader.get('schedule_to')),
        page: parsePositiveInteger(reader.get('page'), DEFAULT_EXPERIMENTS_LIST_PAGE),
        perPage: parsePositiveInteger(reader.get('per_page'), DEFAULT_EXPERIMENTS_LIST_PER_PAGE),
        orderBy: reader.get('orderby') || DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
        direction: reader.get('direction')?.toUpperCase() === 'ASC' ? 'ASC' : 'DESC',
        // `pageId`, not `page`: `page` is the pagination cursor a few lines up. The name matches
        // the one the Configure screen already prefills from (#37003 AC-3), so one datum has one
        // name across the portlet. An empty
        // value is no filter rather than a page named "" — same rule as `filter` above.
        selectedPageId: reader.get('pageId') || null,
        selectedPageUrl: normalizePagePath(reader.get('url')),
        /**
         * The language the editor was standing in, when it sent us here.
         *
         * Not a filter: the list narrows on `pageId` alone, and an experiment belongs to a page
         * rather than to one of its language versions. It is carried so a return to the editor
         * can return to the version the editor came from — a page identifier cannot say which one
         * that was, and assuming the default sent them somewhere they had not been.
         *
         * `null` when absent or unusable, which is what a directly typed list URL looks like.
         */
        languageId: parsePositiveInteger(reader.get('language_id'), null)
    };
}

/** Inclusive instant bounds of a schedule period. `Infinity` on a side the period leaves open. */
export interface ScheduleBounds {
    min: number;
    max: number;
}

/**
 * The period's bounds as instants, or `null` when it constrains nothing.
 *
 * Whole local days (FR-021): the lower bound opens at the first instant of its day and the upper
 * closes at the last. The filter is picked as dates while the data it compares is an instant, so
 * without this a period of a single day would match only what is scheduled for exactly midnight.
 *
 * An open side is `±Infinity` rather than a missing field, so the comparison downstream has no
 * branches: every start is either inside the pair or outside it.
 *
 * Returns `null` for a period with neither bound, and also for one whose end precedes its start —
 * the caller has to ask {@link isScheduleRangeInverted} about that case and report it, because
 * applying an impossible period would read as a site with no experiments (FR-021a).
 */
export function scheduleBoundsOf(period: ExperimentsListSchedulePeriod): ScheduleBounds | null {
    const from = parseScheduleBound(period.from);
    const to = parseScheduleBound(period.to);

    if (!from && !to) {
        return null;
    }

    const bounds = {
        min: from ? startOfDay(from).getTime() : -Infinity,
        max: to ? endOfDay(to).getTime() : Infinity
    };

    return bounds.min > bounds.max ? null : bounds;
}

/**
 * Whether the period names two real dates in the wrong order.
 *
 * Only an address can produce one — picking a range on a calendar cannot — so this is about a
 * hand-edited or stale link. Kept separate from {@link scheduleBoundsOf} because the two answers
 * go to different places: the narrowing needs to know not to filter, and the user needs to be told
 * why nothing changed.
 */
export function isScheduleRangeInverted(period: ExperimentsListSchedulePeriod): boolean {
    const from = parseScheduleBound(period.from);
    const to = parseScheduleBound(period.to);

    return !!from && !!to && startOfDay(from).getTime() > endOfDay(to).getTime();
}

/**
 * Whether the experiment's scheduled start falls inside the period.
 *
 * Both shapes of "not scheduled" are excluded (FR-022), and they are not interchangeable:
 * `scheduling` may be absent altogether, or present carrying a null `startDate`. Reading through
 * the first shape without care throws; treating the second as scheduled lets it into every period.
 */
export function matchesSchedulePeriod(experiment: DotExperiment, bounds: ScheduleBounds): boolean {
    const startDate = experiment.scheduling?.startDate;

    return startDate != null && startDate >= bounds.min && startDate <= bounds.max;
}

/**
 * One schedule bound as a local `Date` at midnight, or `null` when it is absent or unusable.
 *
 * Strict about the format rather than handing the string to `new Date()`: that parses far too much
 * — `2026-6-1`, `June 2026`, an ISO instant — and each of those would silently mean a different day
 * than the address appears to name. Anything but `SCHEDULE_BOUND_FORMAT` is dropped, which is the
 * rule `status` and `goal` already follow (FR-048).
 */
export function parseScheduleBound(raw: string | null | undefined): Date | null {
    if (!raw) {
        return null;
    }

    const parsed = parse(raw, SCHEDULE_BOUND_FORMAT, new Date());

    // `parse` is lenient about overflow — `2026-02-31` rolls into March — so the round trip is
    // what proves the address named the day it appears to.
    return isValid(parsed) && format(parsed, SCHEDULE_BOUND_FORMAT) === raw ? parsed : null;
}

/** A `Date` as the address carries it. */
export function formatScheduleBound(date: Date): string {
    return format(date, SCHEDULE_BOUND_FORMAT);
}

/**
 * A bound as the view state holds it: still a string, but one the address could have written.
 *
 * Round-tripping through a `Date` is what drops the unusable values, so nothing downstream has to
 * ask whether a bound it was handed is really a date.
 */
function normalizeScheduleBound(raw: string | null | undefined): string | null {
    const parsed = parseScheduleBound(raw);

    return parsed ? formatScheduleBound(parsed) : null;
}

/**
 * A page path as the Page column resolves it, or `null` when there is nothing to narrow by.
 *
 * Both sides of the comparison come from different places — one from an address someone pasted,
 * the other from `htmlpageasset` — so the two ways the same path can be spelled are settled here:
 * a missing leading slash, and a trailing one. Case is left alone; dotCMS paths are not
 * case-insensitive, and lowercasing here would claim a match the backend would not make.
 */
export function normalizePagePath(rawPath: string | null | undefined): string | null {
    const path = (rawPath ?? '').trim();

    if (!path) {
        return null;
    }

    const withLeadingSlash = path.startsWith('/') ? path : `/${path}`;

    return withLeadingSlash.length > 1 && withLeadingSlash.endsWith('/')
        ? withLeadingSlash.slice(0, -1)
        : withLeadingSlash;
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

/**
 * Creator ids from the address, blanks removed.
 *
 * Deliberately **not** the rule {@link parseStatuses} and {@link parseGoals} follow. Those narrow
 * over closed sets, so a value outside the set can only be a mistake and is dropped. A user id is
 * drawn from an open set — any string can be one — so there is nothing to validate it against
 * here, and an id this installation does not know is kept rather than discarded. It then matches
 * no experiment, which is the honest answer to "show me that person's experiments": none.
 *
 * Case is preserved: dotCMS user ids are not case-insensitive, and upper-casing them the way the
 * status parser does would stop them matching anything at all.
 */
export function parseCreators(rawCreators: string[]): string[] {
    return rawCreators.map((rawCreator) => rawCreator.trim()).filter(Boolean);
}

export function parsePositiveInteger<T extends number | null>(
    rawValue: string | null,
    fallback: T
): number | T {
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
                : view.selectedGoals,
        pageId: view.selectedPageId || null,
        url: view.selectedPageUrl || null,
        created_by: view.selectedCreators.length ? view.selectedCreators : null,
        // Two independent bounds, each omitted when the period leaves that side open (FR-049a).
        schedule_from: view.scheduleFrom || null,
        schedule_to: view.scheduleTo || null,
        // Written back so it survives filtering, sorting and paging: `writeUrl` merges, and the
        // back-link reads it from the address rather than from a value held only on entry.
        language_id: view.languageId ? String(view.languageId) : null
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
