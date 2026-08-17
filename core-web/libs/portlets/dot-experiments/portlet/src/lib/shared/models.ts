import { DotPageLockInfo } from '@dotcms/data-access';
import {
    AllowedActionsByExperimentStatus,
    ComponentStatus,
    DotExperiment,
    DotExperimentPatchBody,
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
    /**
     * The keys edited since the last successful save, merged into one PATCH body.
     *
     * `null` while everything on screen is persisted. It survives a failed save on purpose, so the
     * next edit re-sends what could not be written together with what just changed.
     */
    pendingPatch: DotExperimentPatchBody | null;
    /**
     * True when the last autosave came back as an error.
     *
     * Kept apart from {@link pendingPatch} because the two answer different questions: the diff is
     * still unsaved, but nothing is on its way any more — and the footer must not go on saying
     * "Saving…" for the rest of the session, which is the stuck flag #37003 set out to fix.
     */
    lastSaveFailed: boolean;
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

/**
 * The goal being edited, flattened.
 *
 * Both goal types that carry conditions carry exactly one, so the whole card fits in a single
 * signal-forms slice: no nested list, and no shape that differs per goal type. The two persisted
 * shapes are rebuilt on the way out — see `toGoals`.
 */
export interface GoalFormSlice extends GoalConditionFormModel {
    /** Empty until a goal type is picked, which is what the SET/REQUIRED chip reads. */
    type: GOAL_TYPES | '';
    name: string;
}

/** The schedule being edited. `null` in either field means "not scheduled from/until". */
export interface SchedulingFormSlice {
    startDate: Date | null;
    endDate: Date | null;
}

/**
 * One variant's share of the traffic, as the Variants card edits it.
 *
 * The identifier travels with the weight so the row maps back to the `TrafficProportion` entry it
 * stands for: the endpoint replaces the whole proportion, and the names and URLs the rest of the
 * card renders stay where they are persisted rather than being copied into the form.
 *
 * `null` is what a cleared number input writes, and it is kept: forcing it to `0` would fight the
 * user mid-edit, and a total that no longer adds up is exactly what the card is there to report.
 */
export interface VariantWeightFormRow {
    id: string;
    /** Share of the traffic, in percent, or `null` while the input is empty. */
    weight: number | null;
}

/** Anything carrying a variant weight: a persisted `Variant` or a {@link VariantWeightFormRow}. */
export interface WeightedVariant {
    weight?: number | null;
}

/**
 * Everything the Configure screen edits, as one model.
 *
 * The screen is a single signal form: the shell owns this model and the rules over it, and each
 * card renders the slice it is handed. That is what lets one binding turn any edit into one PATCH
 * body — a per-card form would have to negotiate that between five of them.
 *
 * A variant's *existence* is deliberately absent: adding, renaming and deleting one each have their
 * own endpoint, so none of them is a form value. Its *weight* is one: the weights are edited
 * together, they are only ever valid as a set, and `PATCH /api/v1/experiments/{id}` takes them as
 * one `trafficProportion` key. Holding them here is what makes "they add up to 100" a cross-field
 * rule of the form rather than a sum recomputed wherever it happens to be needed.
 */
export interface ConfigureFormModel {
    name: string;
    description: string;
    goal: GoalFormSlice;
    /** Percentage of the page's traffic that enters the experiment, 1–100. */
    trafficAllocation: number;
    scheduling: SchedulingFormSlice;
    /** One row per persisted variant, in the order they are drawn. Empty before creation. */
    variantWeights: VariantWeightFormRow[];
}

/**
 * Where the Scheduling card's two pickers may go.
 *
 * Derived by the shell from the backend's duration limits and the start date being edited, since
 * the schema needs the same bounds the pickers do and there must be one answer for both.
 */
export interface SchedulingDateBounds {
    /** Where an empty start picker opens, and the earliest day it offers. */
    initialStartDate: Date;
    /** An experiment has to run for at least the configured minimum, counted from its start. */
    minEndDate: Date;
    /** And for no longer than the configured maximum, counted from the same point. */
    maxEndDate: Date;
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
