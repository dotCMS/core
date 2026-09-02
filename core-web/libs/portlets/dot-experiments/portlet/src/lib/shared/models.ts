import { DotPageLockInfo } from '@dotcms/data-access';
import {
    AllowedActionsByExperimentStatus,
    ComponentStatus,
    DotExperiment,
    DotExperimentStatus,
    GOAL_OPERATORS,
    GOAL_TYPES,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

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
    /**
     * Page the list is narrowed to, by identifier; `null` for the full site-wide list (#37005).
     *
     * Matched by **equality**, never as a substring of a path. The free-text `filter` above already
     * searches the Page column, and reusing it would make `/about` match `/about-us` — which is
     * exactly the case FR-021b rules out ("all of that page's experiments and no other page's").
     *
     * Serialised as `pageAsset`, not `page`: `page` is the pagination cursor two fields up.
     */
    selectedPageId: string | null;
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
    /**
     * Language the page was resolved in — the `language_id` the variant deep link carries (#37005).
     *
     * Not optional, and deliberately not defaulted anywhere: `editEmaGuard` *substitutes*
     * `language_id=1` for a missing param rather than rejecting, so a page object without a
     * language does not produce a broken link, it produces one that silently opens the wrong
     * language's content. The deep-link builder refuses when this is absent (FR-004), which only
     * works if the absence is allowed to reach it.
     */
    languageId: number;
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
    /**
     * True while the variants that stand in the way of a page change are being deleted.
     *
     * Its own flag rather than the shared `status`: the Change Page confirmation shows this wait on
     * its own button, and an autosave that happened to be in flight when it opened would otherwise
     * have it spinning over nothing.
     */
    deletingVariants: boolean;
    /**
     * True when the last such run was refused, so the confirmation can say so and stay open.
     *
     * Cleared when the next one is asked for — a press or a confirmation — so a cancelled failure
     * is not still on screen the next time the dialog opens.
     */
    deleteVariantsFailed: boolean;
    /**
     * Whether the validation rules are allowed to show. `false` until Start/Schedule is pressed,
     * so no field can show an error before then (AC28).
     *
     * Only the *reveal* is latched: the errors themselves are derived live from the form, so
     * fixing a field clears its error without pressing Start again.
     */
    validationRevealed: boolean;
    /**
     * The form as it stands on screen, mirrored whole rather than as a diff against what is
     * stored. It is what a save sends and what the Start rules are checked against.
     */
    formValue: ConfigureFormModel | null;
    /** Which slices the form currently considers valid; a save skips the ones that are not. */
    formValidity: ConfigureFormValidity;
    /**
     * The form as it stood at the last successful write, or `null` before the first one.
     *
     * Comparing the two is the whole of the screen's dirty state. A failed save leaves this
     * untouched, so the work stays unsaved and the button stays live, with no flag to keep in
     * step.
     */
    savedFormValue: ConfigureFormModel | null;
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
/**
 * Which bounded slices currently hold a value worth sending.
 *
 * The bounds themselves live in the form's schema, so validity is read off the field tree rather
 * than re-derived here: an out-of-range allocation or an out-of-window end date is shown on screen
 * and simply not sent.
 */
export interface ConfigureFormValidity {
    trafficAllocation: boolean;
    scheduling: boolean;
}

export interface ConfigureFormModel {
    name: string;
    description: string;
    goal: GoalFormSlice;
    /** Percentage of the page's traffic that enters the experiment, 1–100. */
    trafficAllocation: number;
    scheduling: SchedulingFormSlice;
    /** One row per persisted variant, in the order they are drawn. Empty before creation. */
    variantWeights: VariantWeightFormRow[];
    /**
     * How the weights were arrived at.
     *
     * Carried because the weights alone cannot say it, and the backend acts on the difference:
     * `ExperimentsAPIImpl.addVariant` redistributes a newly added variant only while the
     * proportion is `SPLIT_EVENLY`. Pressing Split Evenly says so; typing a weight says the
     * opposite.
     */
    trafficProportionType: TrafficProportionTypes;
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
    /**
     * Mode the Universal Visual Editor opens this variant in (#37005, FR-008 – FR-010).
     *
     * `PREVIEW` when **any** of: this is the control, the experiment is not a draft, or the page is
     * locked by another user. Derived here rather than branched in the template, and deliberately
     * not read off `disabledTooltipKey`: that key reports only the strongest *reason* and is `null`
     * for the control on an editable draft, which would open the Original for editing.
     */
    editorMode: UVE_MODE;
}
