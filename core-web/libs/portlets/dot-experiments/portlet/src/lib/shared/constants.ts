import {
    DotExperimentStatus,
    ExperimentsStatusList,
    GOAL_TYPES,
    GOALS_METADATA_MAP
} from '@dotcms/dotcms-models';

import { DotExperimentsListSortDirection, ExperimentRow, TagSeverity } from './models';

export const DEFAULT_EXPERIMENTS_LIST_PAGE = 1;
export const DEFAULT_EXPERIMENTS_LIST_PER_PAGE = 25;
/**
 * Sortable columns. The values double as `pSortableColumn` fields, as the `orderby` URL param
 * and as the comparator keys, so the three can never drift apart.
 */
export const EXPERIMENTS_LIST_SORT_FIELDS = {
    NAME: 'name',
    PAGE: 'page',
    GOAL: 'goal',
    SCHEDULE: 'schedule',
    STATUS: 'status',
    MOD_DATE: 'modDate'
} as const;

export type ExperimentsListSortField =
    (typeof EXPERIMENTS_LIST_SORT_FIELDS)[keyof typeof EXPERIMENTS_LIST_SORT_FIELDS];

export const DEFAULT_EXPERIMENTS_LIST_ORDER_BY: ExperimentsListSortField =
    EXPERIMENTS_LIST_SORT_FIELDS.MOD_DATE;
export const DEFAULT_EXPERIMENTS_LIST_DIRECTION: DotExperimentsListSortDirection = 'DESC';

/**
 * No status is selected by default: the filter starts empty, like every other filter in the
 * admin, so nothing is pre-ticked and the chip reads as unfiltered.
 */
export const DEFAULT_EXPERIMENTS_LIST_STATUSES: DotExperimentStatus[] = [];

/** Same as the status filter: nothing pre-selected, so the chip reads as unfiltered. */
export const DEFAULT_EXPERIMENTS_LIST_GOALS: GOAL_TYPES[] = [];

/** i18n keys of the goal names, in the order the filter lists them. */
export const GOAL_LABEL_KEYS = new Map<GOAL_TYPES, string>(
    Object.values(GOAL_TYPES).map((goal) => [goal, GOALS_METADATA_MAP[goal].label])
);

/**
 * Statuses hidden while the filter is empty. Archived experiments are opt-in — an unfiltered
 * list means "everything still in play", and archived rows would otherwise pad it out
 * permanently with work nobody is looking at. Selecting ARCHIVED shows them.
 */
export const OPT_IN_STATUSES: readonly DotExperimentStatus[] = [DotExperimentStatus.ARCHIVED];

/**
 * Copied from `DotExperimentsUiHeaderComponent` so a status looks identical here and in the
 * UVE header. Duplicated rather than imported: the header is legacy code left untouched.
 */
export const STATUS_SEVERITIES: Record<DotExperimentStatus, TagSeverity> = {
    [DotExperimentStatus.RUNNING]: 'success',
    [DotExperimentStatus.SCHEDULED]: 'info',
    [DotExperimentStatus.DRAFT]: 'warn',
    [DotExperimentStatus.ENDED]: 'info',
    [DotExperimentStatus.ARCHIVED]: 'secondary'
};

/** Existing lowercase i18n keys (`draft`, `running`, …) already declared by `ExperimentsStatusList`. */
export const STATUS_LABEL_KEYS = new Map<string, string>(
    ExperimentsStatusList.map(({ value, label }) => [value, label])
);

/** Lifetime of the success toasts pushed after a row action. */
export const SUCCESS_MESSAGE_LIFE = 5000;

export const ROWS_PER_PAGE_OPTIONS = [10, 25, 50];

/** Placeholder rows drawn while the first page is still loading. */
export const SKELETON_ROW_COUNT = 5;

/**
 * Placeholder rows fed to the table while the first page loads.
 *
 * Empty objects rather than `null`: PrimeNG's table skips falsy rows entirely, so a
 * `null`-filled array renders no `<tr>` at all and the skeleton never appears. Their fields are
 * never read — the body template branches on the loading signal and renders skeleton cells.
 */
export const SKELETON_ROWS: ExperimentRow[] = Array.from(
    { length: SKELETON_ROW_COUNT },
    () => ({}) as ExperimentRow
);

/** One skeleton cell per table column. */
export const SKELETON_COLUMNS = Array.from({ length: 8 }, (_, index) => index);

/** Placeholder rendered in the Goal column when no goal is configured. */
export const NO_GOAL_PLACEHOLDER = '—';

/** Height of the status filter's option list before it scrolls. */
export const LISTBOX_SCROLL_HEIGHT = '320px';

/** Idle time before a search term is applied, in ms. */
export const SEARCH_DEBOUNCE_MS = 300;
