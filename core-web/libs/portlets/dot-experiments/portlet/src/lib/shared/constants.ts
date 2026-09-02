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

/**
 * Style of the list table.
 *
 * `table-layout: fixed` is what makes the per-column widths in the header authoritative, so every
 * cell truncates instead of stretching its column. On its own that leaves the one elastic column —
 * Name, the only `w-full` — absorbing every pixel the others do not need, and shrinking towards
 * zero once the viewport is narrow enough. The `min-width` is the floor that stops it: below that
 * the table stops shrinking and the scroll container takes over horizontally.
 *
 * The value is the sum of the fixed columns (14 + 11 + 7 + 15 + 8 + 8 + 4 = 67rem) plus the floor
 * granted to Name (14rem). Keep it in step with the header widths — a column added or resized
 * there without updating this number silently eats into Name's floor again.
 */
export const LIST_TABLE_STYLE: Record<string, string> = {
    'table-layout': 'fixed',
    'min-width': '81rem'
};

/** Height of the status filter's option list before it scrolls. */
export const LISTBOX_SCROLL_HEIGHT = '320px';

/** Idle time before a search term is applied, in ms. */
export const SEARCH_DEBOUNCE_MS = 300;

/** Mount point of the portlet. Absolute, since the list is always at the root of it. */
export const EXPERIMENTS_URL = '/experiments';

/** Segment the Configure screen is reached at while the experiment does not exist yet. */
export const NEW_EXPERIMENT_SEGMENT = 'new';

/** Trailing segment of the Configure URL of an experiment that already exists. */
export const CONFIGURATION_SEGMENT = 'configuration';

/** Hides a `p-panel`'s footer band while its footer slot has nothing to show (see the theme). */
export const DOT_PANEL_NO_FOOTER = 'dot-panel-no-footer';
/** Trailing segment of the Results URL. Reachable on every status, including DRAFT (AC1). */
export const RESULTS_SEGMENT = 'results';

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
 * Idle time before the accumulated field changes are flushed as a PATCH, in ms.
 *
 * One timer for the whole screen: every edit inside the window is merged into one multi-key body,
 * whichever card it came from, and reaches the server as a single call.
 */
export const AUTOSAVE_DEBOUNCE_MS = 500;

/**
 * Shortest time the saving bar stays on screen once it has appeared, in ms.
 *
 * A PATCH against a local backend can answer in a handful of milliseconds, and a bar that appears
 * and vanishes inside a frame or two reads as a glitch rather than as feedback — the eye catches
 * that something blue flickered without ever resolving it into "saved". Holding it for a beat makes
 * the affordance legible; a save still running past the window keeps it up for as long as it takes.
 */
export const MIN_PROGRESS_BAR_VISIBLE_MS = 400;

/** Read-only banner copy while an experiment is running, which is not the generic one (AC35). */
export const LOCKED_BANNER_KEY_RUNNING = 'experiments.configure.locked.running';

/** Read-only banner copy for every other non-DRAFT status. */
export const LOCKED_BANNER_KEY_READ_ONLY = 'experiments.configure.locked.read-only';

/** Page card's inline error when `?pageId=`/`?url=` named a page that is not there. */
export const PAGE_PREFILL_ERROR_KEY = 'experiments.configure.page.prefill.not-found';

/**
 * Page card's inline error when the lookup itself failed. A rejected request says nothing about
 * whether the page exists, so it must not read as "not found" — the error behind it is reported
 * by `DotHttpErrorManagerService` like every other failed call on this screen.
 */
export const PAGE_PREFILL_LOOKUP_ERROR_KEY = 'experiments.configure.page.prefill.lookup-failed';

/** Fallback header the old screen supplies when the backend rejects a start with no header of its own. */
export const START_ERROR_HEADER_KEY =
    'dot.common.http.error.400.experiment.run-scheduling-error.header';

/**
 * Share of the page's traffic the experiment takes when nothing has been chosen yet.
 *
 * Also what an experiment with no allocation of its own is diffed against, so a form that was never
 * touched reports no change.
 */
export const DEFAULT_TRAFFIC_ALLOCATION = 100;

/** A page cannot be excluded from its own experiment entirely, so the slider starts at 1%. */
export const MIN_TRAFFIC_ALLOCATION = 1;

export const MAX_TRAFFIC_ALLOCATION = 100;

/** Total the variant weights must add up to, and the cap on any single one. */
export const TOTAL_WEIGHT = 100;

/** Weights are stored as percentages with two decimals, so compare at that resolution. */
export const WEIGHT_PRECISION = 100;

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

/**
 * How the AssetPicker browses when it is opened to choose the page an experiment runs on.
 *
 * Only what the picker's browse options can say: what the list may *contain* is settled by
 * `allowedBaseTypes`, which narrows it to pages. Folders and menu links are turned off because an
 * experiment runs on a page, and neither of those can be returned as one.
 *
 * `showWorking` keeps unpublished pages in: an experiment can be configured against a draft, and the
 * old screen listed them too.
 */
export const SELECT_PAGE_BROWSER_PARAMS = {
    showFolders: false,
    showLinks: false,
    showWorking: true,
    showArchived: false,
    sortByDesc: true
} as const;

/** Narrower than the 700px form default: the dialog holds a single optional name field. */
export const ADD_VARIANT_DIALOG_WIDTH = '440px';

/** The standard confirmation width (see `libs/portlets/CLAUDE.md`), not the 700px form one. */
export const CHANGE_PAGE_DIALOG_WIDTH = '500px';

/**
 * Row colours of the Variants card, in the order the rows are drawn.
 *
 * Shared with the Change Page dialog, which lists the variants a page change would delete: the two
 * are looking at the same list, so a variant keeps the same colour in both. The Results screen has
 * its own palette.
 */
export const VARIANT_COLORS: readonly string[] = [
    '#0ea5e9',
    '#a855f7',
    '#fb923c',
    '#22c55e',
    '#f43f5e'
];

/**
 * Kind of the cross-field error the weights raise when they do not add up to 100.
 *
 * Deliberately the same string as the `weightsTotal` validation rule the store publishes on a Start
 * press: the two say the same thing at two different moments — the form's is live (AC25), the
 * store's is what turns it into a scroll target (AC28) — and the card reads both.
 */
export const WEIGHTS_TOTAL_ERROR_KIND = 'weightsTotal';

/**
 * Key of the Results screen's `p-confirmDialog`, which the Stop confirmation is raised on.
 *
 * Its own key rather than the Configure screen's `CONFIGURATION_CONFIRM_DIALOG_KEY`: the two
 * screens never share a dialog instance, and the summary table mounts a second dialog of its own
 * for Promote — a key shared between two mounted dialogs opens both at once.
 */
export const RESULTS_CONFIRM_DIALOG_KEY = 'resultsConfirmDialog';

/**
 * A chart's own skeleton: axes, gridlines and label placeholders, no series.
 *
 * Shared because two screens need the same silhouette — the chart draws it in place of a plot it
 * cannot draw yet, and the Results screen draws it behind its empty state so a report with no
 * sessions still reads as the report it will become.
 */
export const EMPTY_CHART_BACKGROUND_IMAGE = `url("data:image/svg+xml,%3Csvg width='917' height='515' viewBox='0 0 917 515' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cg clip-path='url(%23clip0_3061_22368)'%3E%3Crect width='855' height='515' transform='translate(61.5)' fill='white'/%3E%3Cline x1='136.863' y1='437' x2='136.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='315.863' y1='437' x2='315.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='482.863' y1='437' x2='482.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='649.863' y1='437' x2='649.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='816.863' y1='437' x2='816.863' y2='18' stroke='%23F4F4F6' stroke-width='1.27393'/%3E%3Cline x1='62.137' y1='437.05' x2='1397.22' y2='437.049' stroke='%23F4F4F6' stroke-width='1.27393' stroke-linecap='round'/%3E%3Cline x1='61.9612' y1='185.225' x2='1397.39' y2='185.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='17.2254' x2='1397.39' y2='17.2254' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='59.2254' x2='1397.39' y2='59.2254' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='101.225' x2='1397.39' y2='101.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='143.225' x2='1397.39' y2='143.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='227.225' x2='1397.39' y2='227.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='269.225' x2='1397.39' y2='269.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='311.225' x2='1397.39' y2='311.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='353.225' x2='1397.39' y2='353.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Cline x1='61.9612' y1='395.225' x2='1397.39' y2='395.225' stroke='%23F4F4F6' stroke-width='0.922339' stroke-linecap='round' stroke-dasharray='7.38 7.38'/%3E%3Crect x='100.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='90.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3Crect x='270.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='260.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3Crect x='440.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='430.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3Crect x='610.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='600.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3Crect x='780.5' y='458' width='72' height='15.7241' rx='7.86207' fill='%23F4F4F6'/%3E%3Crect x='770.5' y='477.725' width='92' height='16.1758' rx='8.08791' fill='%23F4F4F6'/%3E%3C/g%3E%3Cline x1='61.9375' y1='59.4612' x2='31.5003' y2='59.4612' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='17.4612' x2='31.5003' y2='17.4612' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='101.461' x2='31.5003' y2='101.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='143.461' x2='31.5003' y2='143.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='185.461' x2='31.5003' y2='185.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='227.461' x2='31.5003' y2='227.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='269.461' x2='31.5003' y2='269.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='311.461' x2='31.5003' y2='311.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='353.461' x2='31.5003' y2='353.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='395.461' x2='31.5003' y2='395.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Cline x1='61.9375' y1='437.461' x2='31.5003' y2='437.461' stroke='%23F4F4F6' stroke-width='0.922339'/%3E%3Crect x='0.5' y='10' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='52' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='93' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='136' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='180' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='220' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='261' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='304' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='346' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='386' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Crect x='0.5' y='430' width='31' height='16' rx='8' fill='%23F4F4F6'/%3E%3Cdefs%3E%3CclipPath id='clip0_3061_22368'%3E%3Crect width='855' height='515' fill='white' transform='translate(61.5)'/%3E%3C/clipPath%3E%3C/defs%3E%3C/svg%3E")`;
