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

/**
 * Multiplier applied to the page-lookup limit.
 *
 * Elasticsearch holds one document per identifier *and* language, so a multilingual site returns
 * several documents per page. Limiting to the number of pages therefore truncated the response at
 * HTTP 200, and every page that fell off the end took its experiments with it — the site filter
 * fails closed, so they vanished from the list with no error to show for it.
 *
 * Any language's document carries the `host` and `url` this lookup needs, and duplicates collapse
 * by identifier, so over-asking is harmless. This is deliberately far above any realistic language
 * count rather than tuned; the shortfall check in the store is what catches it being wrong.
 */
export const PAGE_LOOKUP_LANGUAGE_HEADROOM = 25;

/**
 * Idle time before a field change is flushed as a PATCH, in ms.
 *
 * One timer per field group: rapid edits to the same group collapse into a single call, while an
 * edit to another group in the same window still fires its own.
 */
export const AUTOSAVE_DEBOUNCE_MS = 500;

/**
 * The variant cap and the condition option lists come from `@dotcms/dotcms-models` unchanged, and
 * are re-exported here so the Configure screen has one place to look. Redeclaring them would let
 * the new screen offer operators the backend does not validate — the lists are exactly what the old
 * screen offers: CONTAINS/EQUALS for REACH_PAGE, plus EXISTS for URL_PARAMETER.
 */
export {
    GoalsConditionsOperatorsListByType,
    GoalsConditionsParametersListByType,
    MAX_VARIANTS_ALLOWED
} from '@dotcms/dotcms-models';

/**
 * Goal types the selector offers, in the order the old screen renders them. `CLICK_ON_ELEMENT`
 * exists in `GOAL_TYPES` but has never been offered.
 */
export const CONFIGURE_GOAL_TYPES: readonly GOAL_TYPES[] = [
    GOAL_TYPES.BOUNCE_RATE,
    GOAL_TYPES.EXIT_RATE,
    GOAL_TYPES.REACH_PAGE,
    GOAL_TYPES.URL_PARAMETER
];

/**
 * Goal types with a working condition sub-panel. The rest are offered but have no server-side
 * conditions, so they render a "coming soon" placeholder instead — same as the old screen.
 */
export const GOAL_TYPES_WITH_CONDITIONS: readonly GOAL_TYPES[] = [
    GOAL_TYPES.REACH_PAGE,
    GOAL_TYPES.URL_PARAMETER
];

/** Wide enough for the folder tree beside a four-column page table. */
export const SELECT_PAGE_DIALOG_SIZE = { width: '900px', height: '560px' } as const;

/** Narrower than the 700px form default: the dialog holds a single optional name field. */
export const ADD_VARIANT_DIALOG_WIDTH = '440px';
