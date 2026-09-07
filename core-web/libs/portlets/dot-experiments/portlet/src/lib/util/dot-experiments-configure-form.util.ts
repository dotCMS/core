import {
    DotExperiment,
    DotExperimentPatchBody,
    ExperimentsConfigProperties,
    Goal,
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goals,
    PROP_NOT_FOUND,
    RangeOfDateAndTime,
    ReachPageGoalCondition,
    TIME_7_DAYS,
    TIME_90_DAYS,
    TrafficProportionTypes,
    UrlParameterGoalCondition,
    Variant
} from '@dotcms/dotcms-models';

import { DEFAULT_TRAFFIC_ALLOCATION, TOTAL_WEIGHT, WEIGHT_PRECISION } from '../shared/constants';
import {
    ConfigureFormModel,
    ConfigureFormValidity,
    GoalFormSlice,
    SchedulingFormSlice,
    VariantWeightFormRow
} from '../shared/models';

/**
 * The Configure screen's one form model: how it is filled from the store, and how it goes back out
 * as a PATCH body.
 *
 * Kept out of the shell so each mapping can be read — and tested — on its own, and so the two
 * directions sit next to each other: every value that is hydrated has to round-trip, or the form
 * would report a change the moment it was filled in.
 */

/** Condition parameter name the backend validates for URL_PARAMETER, outside the `GOAL_PARAMETERS` enum. */
const QUERY_PARAMETER = 'queryParameter';

/**
 * Condition parameter each goal type is persisted with. The user never picks it for
 * URL_PARAMETER — the parameter *name* is what they type — and REACH_PAGE only has one, so both
 * are fixed here rather than derived from a selection.
 */
export const CONDITION_PARAMETER_BY_TYPE: Partial<Record<GOAL_TYPES, string>> = {
    [GOAL_TYPES.REACH_PAGE]: GOAL_PARAMETERS.URL,
    [GOAL_TYPES.URL_PARAMETER]: QUERY_PARAMETER
};

/** A goal nobody has picked yet. */
export const EMPTY_GOAL_SLICE: GoalFormSlice = {
    type: '',
    name: '',
    parameter: '',
    operator: '',
    parameterName: '',
    value: ''
};

/** What the form is filled from, and diffed against: the values the store holds. */
export interface ConfigureFormSource {
    /** `null` until the first Name + Page combination creates the draft. */
    experiment: DotExperiment | null;
    /** Name as typed, which is what the store compares an outgoing name against. */
    draftName: string;
    draftDescription: string;
}

/** The form as it opens on `/experiments/new`: nothing chosen, and all of the page's traffic. */
export function emptyConfigureForm(): ConfigureFormModel {
    return toConfigureFormModel({ experiment: null, draftName: '', draftDescription: '' });
}

/**
 * The form as the store's values read.
 *
 * `name` and `description` come from the drafts rather than from the experiment: they are what the
 * user typed, and the experiment keeps the persisted pair until the server answers.
 */
export function toConfigureFormModel({
    experiment,
    draftName,
    draftDescription
}: ConfigureFormSource): ConfigureFormModel {
    return {
        name: draftName,
        description: draftDescription,
        goal: toGoalSlice(experiment?.goals),
        trafficAllocation: experiment?.trafficAllocation ?? DEFAULT_TRAFFIC_ALLOCATION,
        scheduling: toSchedulingSlice(experiment?.scheduling),
        variantWeights: toVariantWeightRows(experiment?.trafficProportion?.variants),
        trafficProportionType:
            experiment?.trafficProportion?.type ?? TrafficProportionTypes.SPLIT_EVENLY
    };
}

/**
 * One weight row per persisted variant, in the order the proportion holds them — which is the order
 * the card draws, so a row and its input are the same thing.
 *
 * The weights arrive as the server stores them, which for an even split across three variants is
 * 33.33/33.33/33.34. The inputs are whole percentages, so the rows are rounded — and the rounding
 * loss is settled on the first row, the same place `splitWeightsEvenly` puts its own remainder,
 * because three rows reading 33 would total 99 and the card would flag a split nobody had touched.
 *
 * Only a proportion that already added up gets that correction: one that did not is left reading
 * as broken, which it is.
 */
export function toVariantWeightRows(
    variants: Variant[] | null | undefined
): VariantWeightFormRow[] {
    const stored = (variants ?? []).map(({ id, weight }) => ({ id, weight: weight ?? 0 }));

    if (!stored.length || sumWeights(stored) !== TOTAL_WEIGHT) {
        return stored.map(({ id, weight }) => ({ id, weight: Math.round(weight) }));
    }

    const rows = stored.map(({ id, weight }) => ({ id, weight: Math.round(weight) }));
    const drift = TOTAL_WEIGHT - sumWeights(rows);

    return rows.map((row, index) => (index === 0 ? { ...row, weight: row.weight + drift } : row));
}

/** Sum of the rows, rounded to the precision the weights are stored at. */
function sumWeights(rows: VariantWeightFormRow[]): number {
    const total = rows.reduce((sum, { weight }) => sum + (weight ?? 0), 0);

    return Math.round(total * WEIGHT_PRECISION) / WEIGHT_PRECISION;
}

/**
 * The proportion the rows describe: the persisted variants with the edited weights written onto
 * them.
 *
 * The variants are what the endpoint replaces — names and URLs included — so the form only ever
 * supplies the weights, and a cleared input travels as the zero it reads as. A variant no row
 * mentions keeps the weight it was persisted with.
 */
export function mergeVariantWeights(variants: Variant[], rows: VariantWeightFormRow[]): Variant[] {
    const weightById = new Map(rows.map(({ id, weight }) => [id, weight ?? 0]));

    return variants.map((variant) => ({
        ...variant,
        weight: weightById.get(variant.id) ?? variant.weight
    }));
}

/**
 * Whether the rows still stand for the same variants, in the same order.
 *
 * This is what tells an added or deleted variant — which the slice has to be re-seeded for — from a
 * save response merely echoing the weights back, which it must not be.
 */
export function hasSameVariantIdentity(rows: VariantWeightFormRow[], variants: Variant[]): boolean {
    return (
        rows.length === variants.length && rows.every((row, index) => row.id === variants[index].id)
    );
}

/** Whether every row already holds the weight its variant is persisted with. */
export function isSameVariantWeights(rows: VariantWeightFormRow[], variants: Variant[]): boolean {
    return (
        hasSameVariantIdentity(rows, variants) &&
        rows.every((row, index) => (row.weight ?? 0) === (variants[index].weight ?? 0))
    );
}

/**
 * The form, as the PATCH body that stores it.
 *
 * Everything the form holds travels on every save. There is no comparison against what the server
 * already has: `PATCH /api/v1/experiments/{id}` applies each key it receives in one atomic update,
 * so re-sending an unchanged value costs a field assignment and buys the absence of an entire
 * bookkeeping layer. It is also what the rest of the admin does — the content editor posts
 * `this.form.value` whole, over forms far larger than this one.
 *
 * What is still filtered is what the backend would *reject*, which is a different question from
 * what changed: a blank name, a half-typed goal, an out-of-bounds number or date, and a set of
 * weights that does not total 100. Those stay on screen rather than turning a save of the whole
 * form into a 400 over one field.
 *
 * `pageId` is passed in rather than read off the model — the page lives beside the form, not in
 * it — and an unchanged one is an unconditional no-op server-side, so it needs no guard here.
 */
export function toConfigurePatch(
    model: ConfigureFormModel,
    validity: ConfigureFormValidity,
    variants: Variant[],
    pageId?: string
): DotExperimentPatchBody {
    const patch: DotExperimentPatchBody = {};

    if (pageId) {
        patch.pageId = pageId;
    }

    // A blank name is never sent: the backend rejects it, so it would fail the save of every other
    // field with it. What was typed stays on screen.
    if (model.name.trim()) {
        patch.name = model.name;
    }

    patch.description = model.description;

    if (isGoalComplete(model.goal)) {
        patch.goals = toGoals(model.goal);
    }

    if (validity.trafficAllocation) {
        patch.trafficAllocation = model.trafficAllocation;
    }

    if (validity.scheduling) {
        patch.scheduling = toRange(model.scheduling);
    }

    // Weights reach the server only as a complete, valid split. An intermediate total is a state
    // the form is allowed to be in and the backend is not.
    if (isSendableSplit(model.variantWeights, variants)) {
        patch.trafficProportion = {
            type: model.trafficProportionType,
            variants: mergeVariantWeights(variants, model.variantWeights)
        };
    }

    return patch;
}

/**
 * Whether the rows are a split the backend would take.
 *
 * Two conditions, and both matter. The rows must stand for the current variants — a slice caught
 * mid-reseed describes a set that no longer exists — and they must total exactly 100, which is the
 * same assertion `TrafficProportion` makes server-side.
 */
export function isSendableSplit(rows: VariantWeightFormRow[], variants: Variant[]): boolean {
    if (!rows.length || !hasSameVariantIdentity(rows, variants)) {
        return false;
    }

    const total = rows.reduce((sum, row) => sum + (row.weight ?? 0), 0);

    return Math.round(total * WEIGHT_PRECISION) / WEIGHT_PRECISION === TOTAL_WEIGHT;
}

/** A goal is only worth sending when the server would accept it. */
export function isGoalComplete(goal: GoalFormSlice): boolean {
    if (!goal.type || !goal.name.trim()) {
        return false;
    }

    if (goal.type === GOAL_TYPES.REACH_PAGE) {
        return !!goal.operator && !!goal.value.trim();
    }

    if (goal.type === GOAL_TYPES.URL_PARAMETER) {
        return (
            !!goal.operator &&
            !!goal.parameterName.trim() &&
            (goal.operator === GOAL_OPERATORS.EXISTS || !!goal.value.trim())
        );
    }

    return true;
}

/** Two goals are the same when every field the form holds matches. */
export function isSameGoal(a: GoalFormSlice, b: GoalFormSlice): boolean {
    return (
        a.type === b.type &&
        a.name === b.name &&
        a.parameter === b.parameter &&
        a.operator === b.operator &&
        a.parameterName === b.parameterName &&
        a.value === b.value
    );
}

/**
 * The persisted shape of a goal: one condition for the two types that have one, none for the rest.
 */
export function toGoals(goal: GoalFormSlice): Goals {
    const primary: Goal = {
        name: goal.name.trim(),
        type: goal.type as GOAL_TYPES,
        conditions: toConditions(goal)
    };

    return { primary };
}

/** The inverse of {@link toGoals}, used both to fill the form and to detect a no-op edit. */
export function toGoalSlice(goals: Goals | null | undefined): GoalFormSlice {
    const primary = goals?.primary;

    if (!primary?.type) {
        return EMPTY_GOAL_SLICE;
    }

    const condition = primary.conditions?.[0];
    const isUrlParameter = primary.type === GOAL_TYPES.URL_PARAMETER;
    const urlParameterValue = isUrlParameter
        ? (condition?.value as UrlParameterGoalCondition['value'] | undefined)
        : undefined;

    return {
        type: primary.type,
        name: primary.name ?? '',
        parameter: condition?.parameter ?? CONDITION_PARAMETER_BY_TYPE[primary.type] ?? '',
        operator: condition?.operator ?? '',
        parameterName: urlParameterValue?.name ?? '',
        value: isUrlParameter
            ? (urlParameterValue?.value ?? '')
            : ((condition?.value as string | undefined) ?? '')
    };
}

function toConditions(
    goal: GoalFormSlice
): Array<ReachPageGoalCondition | UrlParameterGoalCondition> {
    const operator = goal.operator as GOAL_OPERATORS;

    if (goal.type === GOAL_TYPES.REACH_PAGE) {
        return [{ parameter: goal.parameter, operator, value: goal.value.trim() }];
    }

    if (goal.type === GOAL_TYPES.URL_PARAMETER) {
        return [
            {
                // `queryParameter` is outside the `GOAL_PARAMETERS` enum, which the persisted
                // shape is typed against — the backend validates this spelling, not the enum's.
                parameter: goal.parameter as GOAL_PARAMETERS,
                operator,
                value: {
                    name: goal.parameterName.trim(),
                    value: operator === GOAL_OPERATORS.EXISTS ? '' : goal.value.trim()
                }
            }
        ];
    }

    return [];
}

/** The form shape a persisted range hydrates as. */
export function toSchedulingSlice(
    scheduling: RangeOfDateAndTime | null | undefined
): SchedulingFormSlice {
    return {
        startDate: toDate(scheduling?.startDate ?? null),
        endDate: toDate(scheduling?.endDate ?? null)
    };
}

/**
 * And the persisted shape it goes back out as: instants, or `null` for the whole range when neither
 * date is set — which is what `PATCH /api/v1/experiments/{id}` reads as "no schedule at all", and
 * therefore what both ways of emptying the card mean.
 */
export function toRange(scheduling: SchedulingFormSlice): RangeOfDateAndTime | null {
    const startDate = toTime(scheduling.startDate);
    const endDate = toTime(scheduling.endDate);

    return startDate === null && endDate === null ? null : { startDate, endDate };
}

/** Two schedules are the same when they name the same two instants, whatever `Date`s carry them. */
export function isSameScheduling(a: SchedulingFormSlice, b: SchedulingFormSlice): boolean {
    return toTime(a.startDate) === toTime(b.startDate) && toTime(a.endDate) === toTime(b.endDate);
}

/** How long an experiment may run: the backend's own limits, in milliseconds. */
export interface ExperimentDurationBounds {
    minDuration: number;
    maxDuration: number;
}

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

/** Half an hour, in minutes: the granularity the initial start date is rounded up to. */
const HALF_AN_HOUR = 30;

/**
 * The backend reports both limits in days, and `NOT_FOUND` when the property is unset — which is
 * also what an unresolved route means, so both fall back to the same defaults.
 */
export function resolveDurationBounds(
    configProps: Record<string, string | boolean> | undefined
): ExperimentDurationBounds {
    return {
        minDuration: toMilliseconds(
            configProps?.[ExperimentsConfigProperties.EXPERIMENTS_MIN_DURATION],
            TIME_7_DAYS
        ),
        maxDuration: toMilliseconds(
            configProps?.[ExperimentsConfigProperties.EXPERIMENTS_MAX_DURATION],
            TIME_90_DAYS
        )
    };
}

/** The next half hour from `date`, so a start date never opens on a minute already gone. */
export function nextHalfHour(date: Date): Date {
    const initialDate = new Date(date);

    if (initialDate.getMinutes() > HALF_AN_HOUR) {
        initialDate.setMinutes(0);
        initialDate.setHours(initialDate.getHours() + 1);
    } else {
        initialDate.setMinutes(HALF_AN_HOUR);
    }

    return initialDate;
}

function toMilliseconds(days: string | boolean | undefined, fallback: number): number {
    if (typeof days !== 'string' || days === PROP_NOT_FOUND) {
        return fallback;
    }

    const parsed = Number(days);

    return Number.isFinite(parsed) && parsed > 0 ? parsed * MILLISECONDS_PER_DAY : fallback;
}

function toDate(timestamp: number | null): Date | null {
    return timestamp ? new Date(timestamp) : null;
}

function toTime(date: Date | null): number | null {
    return date?.getTime() ?? null;
}
