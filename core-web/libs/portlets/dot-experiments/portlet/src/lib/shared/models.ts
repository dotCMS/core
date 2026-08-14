import { DotPageLockInfo } from '@dotcms/data-access';
import {
    AllowedActionsByExperimentStatus,
    ComponentStatus,
    DotExperiment,
    DotExperimentStatus,
    GOAL_OPERATORS,
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

/**
 * A group of fields the Configure screen autosaves on its own debounce timer.
 *
 * One group is one PATCH body key, because there is no combined update endpoint: the experiment is
 * updated by calling `PATCH /api/v1/experiments/{id}` repeatedly with a single-key body. Keeping the
 * groups this granular is what lets an edit to Name and an edit to Goal in the same tick fire two
 * independent calls instead of collapsing into one.
 */
export type ExperimentFieldGroup =
    | 'name'
    | 'description'
    | 'goal'
    | 'scheduling'
    | 'trafficAllocation'
    | 'trafficProportion';

/**
 * The rules checked when Start/Schedule is pressed. Nothing is validated before that press, so this
 * doubles as the set of fields allowed to reveal an error.
 */
export type ConfigureValidationRule =
    | 'name'
    | 'page'
    | 'goalType'
    | 'goalName'
    | 'goalConditionValue'
    | 'goalParameterName'
    | 'minVariants'
    | 'weightsTotal';

/**
 * The page an experiment runs on, as much of it as the Configure screen renders.
 *
 * The page is chosen once and then immutable — `PATCH /api/v1/experiments/{id}` does not accept
 * `pageId` — so after creation this is only ever resolved *from* the experiment, never sent back.
 */
export interface DotExperimentConfigurePage {
    pageId: string;
    /** Page title, falling back to its path when the page has no title. */
    title: string;
    /** Site-relative path, e.g. `/about-us/index`. */
    path: string;
}

/** Everything the Configure screen renders from. */
export interface DotExperimentsConfigureViewState {
    /** `null` until the first Name + Page combination creates the draft. */
    experiment: DotExperiment | null;
    status: ComponentStatus;
    /** True while nothing has been persisted yet, i.e. the screen is on `/experiments/new`. */
    isNew: boolean;
    /** True while the creation POST is in flight, so a second edit cannot fire a second one. */
    creating: boolean;
    /** True while the start call is in flight, so a second Start press cannot fire a second one. */
    starting: boolean;
    /**
     * Name as typed. It seeds the creation POST and, once the experiment exists, is what the
     * debounced name PATCH sends — the server value stays on `experiment` until it answers.
     */
    draftName: string;
    /** Description as typed, on the same contract as {@link draftName}. */
    draftDescription: string;
    /** `null` until a page is picked, prefilled from the URL, or resolved for a loaded experiment. */
    selectedPage: DotExperimentConfigurePage | null;
    /** i18n key of the Page card's inline error when `?pageId=`/`?url=` did not resolve. */
    pagePrefillError: string | null;
    /** `null` until the page's lock state has been resolved, or while no page is selected. */
    pageLockInfo: DotPageLockInfo | null;
    /** Empty until Start/Schedule is pressed, so no field can show an error before then. */
    validationErrors: ConfigureValidationRule[];
    /** Field groups with an autosave PATCH debounced or in flight. */
    pendingFieldGroups: ExperimentFieldGroup[];
}

/** Publish state of a page, shown in the Select A Page table's State column. */
export type SelectPageDialogRowState = 'live' | 'working' | 'draft';

/** A page the Select A Page dialog offers, plus everything its row would otherwise have to derive. */
export interface SelectPageDialogRow {
    /** Page identifier — what the experiment is created against. */
    pageId: string;
    title: string;
    /** Site-relative path, e.g. `/about-us/index`. */
    url: string;
    template: string;
    modDate: number;
    state: SelectPageDialogRowState;
    /** True when the page already hosts a non-archived experiment, so it cannot be picked. */
    disabled: boolean;
    /** i18n key explaining `disabled`; `null` when the row is selectable. */
    disabledTooltipKey: string | null;
}

/**
 * The Goal card's single condition, flattened.
 *
 * Both goal types that have conditions hold exactly one, and their persisted shapes differ only in
 * where the query-parameter name sits (`ReachPageGoalCondition.value` is a plain string,
 * `UrlParameterGoalCondition.value` is `{ name, value }`). Flattening keeps the card on one
 * signal-forms slice instead of a nested tree; the store maps it back on the way out.
 */
export interface GoalConditionFormModel {
    /** `url` for REACH_PAGE, `queryParameter` for URL_PARAMETER. */
    parameter: string;
    /** Empty until the user picks one. */
    operator: GOAL_OPERATORS | '';
    /** URL_PARAMETER only: the query-parameter name. Empty for REACH_PAGE. */
    parameterName: string;
    /**
     * The URL to reach (REACH_PAGE) or the query-parameter value (URL_PARAMETER). Not required when
     * `operator` is EXISTS.
     */
    value: string;
}

/** A variant row, plus everything the Variants card would otherwise have to derive per row. */
export interface VariantRowViewModel {
    id: string;
    name: string;
    /** Share of the traffic, in percent. */
    weight: number;
    /** True for the `DEFAULT` variant, which is the control: never renamed, never deleted. */
    isControl: boolean;
    /** Colour of the row's dot. */
    color: string;
    /** URL for `dot-copy-button`, already carrying `&variantName=`; `null` while the page is unknown. */
    copyUrl: string | null;
    /** True when the row cannot be edited: status is not DRAFT, or the page is locked by another user. */
    disabled: boolean;
    /** i18n key explaining `disabled`; `null` when the row is editable. */
    disabledTooltipKey: string | null;
}
