import { DotPageBrowserPage } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotExperiment,
    DotExperimentPatchBody,
    GOAL_OPERATORS,
    GOAL_TYPES,
    ReachPageGoalCondition,
    UrlParameterGoalCondition,
    Variant
} from '@dotcms/dotcms-models';

import { TOTAL_WEIGHT, WEIGHT_PRECISION } from '../shared/constants';
import {
    ConfigureValidationRule,
    DotExperimentConfigurePage,
    DotExperimentsConfigureViewState,
    WeightedVariant
} from '../shared/models';

/**
 * Pure helpers behind the Configure store: variant weights, the Start/Schedule rules, the pending
 * autosave diff and the page shapes the two lookups answer with. Kept out of the store so each can
 * be read — and tested — on its own, without standing up the store, its injected services or its
 * lifecycle hooks.
 */

/**
 * Splits 100% across the items: `floor(100/n)` each, with the remainder absorbed by the first one so
 * the total is exactly 100 for any variant count (AC23).
 *
 * Generic over what carries the weight, so the same maths serves the form rows the Variants card
 * splits and the persisted variants a proportion is rebuilt from.
 */
export function splitWeightsEvenly<T extends WeightedVariant>(items: readonly T[]): T[] {
    if (!items.length) {
        return [];
    }

    const share = Math.floor(TOTAL_WEIGHT / items.length);
    const remainder = TOTAL_WEIGHT - share * items.length;

    return items.map((item, index) => ({
        ...item,
        weight: index === 0 ? share + remainder : share
    }));
}

/** Sum of the weights, rounded to the precision they are stored at. A cleared one counts as zero. */
export function totalWeight(items: readonly WeightedVariant[]): number {
    const total = items.reduce((sum, { weight }) => sum + (weight ?? 0), 0);

    return Math.round(total * WEIGHT_PRECISION) / WEIGHT_PRECISION;
}

/**
 * The eight rules Start/Schedule checks, in the order the screen reads top to bottom — which is
 * also the order the shell scrolls through to find the first failing field.
 *
 * Nothing here runs before Start is pressed (AC28), so this is a plain function over the state
 * rather than a computed: materialising it early is exactly what the screen must not do.
 *
 * These are the UX net, not the authority: the server already enforces most of them per aspect and
 * earlier — the name and page on every save, the goal through `MetricsUtil.validateGoals`, the
 * weights on `TrafficProportion` construction — so what Start adds is naming the fields *before*
 * the request rather than after a rejection. `goalName` is one of them: it maps to `Metric.name()`,
 * which is non-optional server-side (`AbstractMetric.java`).
 */
export function validateConfigure(
    state: Pick<DotExperimentsConfigureViewState, 'draftName' | 'selectedPage' | 'experiment'>
): ConfigureValidationRule[] {
    const errors: ConfigureValidationRule[] = [];
    const experiment = state.experiment;
    const goal = experiment?.goals?.primary ?? null;

    if (!state.draftName.trim()) {
        errors.push('name');
    }

    if (!state.selectedPage && !experiment?.pageId) {
        errors.push('page');
    }

    if (!goal?.type) {
        errors.push('goalType');
    }

    if (!goal?.name?.trim()) {
        errors.push('goalName');
    }

    errors.push(...validateGoalCondition(goal?.type, goal?.conditions?.[0]));

    const variants = experiment?.trafficProportion?.variants ?? [];

    if (variants.length < 2) {
        errors.push('minVariants');
    }

    if (variants.length && totalWeight(variants) !== TOTAL_WEIGHT) {
        errors.push('weightsTotal');
    }

    return errors;
}

/**
 * Condition rules for the two goal types that have conditions. BOUNCE_RATE and EXIT_RATE have no
 * server-side conditions, so they are complete without one.
 */
function validateGoalCondition(
    goalType: GOAL_TYPES | undefined,
    condition: ReachPageGoalCondition | UrlParameterGoalCondition | undefined
): ConfigureValidationRule[] {
    if (goalType === GOAL_TYPES.REACH_PAGE) {
        const value = condition?.value as string | undefined;

        return value?.trim() ? [] : ['goalConditionValue'];
    }

    if (goalType !== GOAL_TYPES.URL_PARAMETER) {
        return [];
    }

    const value = condition?.value as { name?: string; value?: string } | undefined;
    const errors: ConfigureValidationRule[] = [];

    // EXISTS only asks whether the parameter is there, so it needs no value — the name it looks
    // for is still required.
    if (condition?.operator !== GOAL_OPERATORS.EXISTS && !value?.value?.trim()) {
        errors.push('goalConditionValue');
    }

    if (!value?.name?.trim()) {
        errors.push('goalParameterName');
    }

    return errors;
}

/** True while at least one key is waiting to be written. */
export function hasPendingChanges(patch: DotExperimentPatchBody | null): boolean {
    return !!patch && Object.keys(patch).length > 0;
}

/**
 * The experiment as the cards should already see it, with the keys an edit changed applied.
 *
 * The PATCH is debounced, so a card cannot wait for the server to redraw: the weight that was just
 * typed has to be in the row, and a goal that was just completed has to count as configured when
 * Start is pressed half a second later.
 *
 * `name` and `description` are deliberately left out. They live on `draftName`/`draftDescription`
 * until the server answers, which is what lets the store tell a typed value from a persisted one —
 * and therefore what stops it from re-sending a name it already holds.
 */
export function applyPatchToExperiment(
    experiment: DotExperiment | null,
    patch: DotExperimentPatchBody
): DotExperiment | null {
    if (!experiment) {
        return experiment;
    }

    return {
        ...experiment,
        ...(patch.goals !== undefined && { goals: patch.goals }),
        ...(patch.scheduling !== undefined && { scheduling: patch.scheduling }),
        ...(patch.trafficAllocation !== undefined && {
            trafficAllocation: patch.trafficAllocation
        }),
        ...(patch.trafficProportion !== undefined && {
            trafficProportion: patch.trafficProportion
        })
    };
}

/**
 * The body a pending diff actually goes out as, or `null` when there is nothing left to send.
 *
 * Three keys can be held back. A blank `name`, which the backend rejects — the typed value stays on
 * screen and the experiment keeps the name it was saved with until a real one replaces it. A `name`
 * or `description` the experiment already holds, which is what makes the flush that follows creation
 * a no-op: the POST carried both, so re-sending them would be a second write for nothing. And a
 * `trafficProportion` whose weights do not add up to 100, which is a guaranteed 400: the immutable's
 * `@Value.Check` rejects it on *construction*, so the PATCH fails before it is even applied
 * (`AbstractTrafficProportion.java:44-58`). Typing a weight passes through intermediate totals, and
 * none of them is worth a failed request or an error toast.
 *
 * Everything else is sent as it stands, `scheduling: null` included: clearing the schedule is a
 * change like any other, and the cards are what decide a value is worth dispatching in the first
 * place.
 */
export function toOutgoingPatch(
    patch: DotExperimentPatchBody | null,
    experiment: DotExperiment
): DotExperimentPatchBody | null {
    if (!patch) {
        return null;
    }

    const outgoing = { ...patch };

    if (outgoing.name === undefined || !outgoing.name.trim() || outgoing.name === experiment.name) {
        delete outgoing.name;
    }

    if (outgoing.description === experiment.description) {
        delete outgoing.description;
    }

    if (hasUnsendableWeights(outgoing.trafficProportion?.variants)) {
        delete outgoing.trafficProportion;
    }

    return hasPendingChanges(outgoing) ? outgoing : null;
}

/**
 * Mirrors the backend check, empty list included: it only asserts the total when the proportion
 * carries variants, so a proportion without any says nothing about weights.
 */
function hasUnsendableWeights(variants: Variant[] | undefined): boolean {
    return !!variants?.length && totalWeight(variants) !== TOTAL_WEIGHT;
}

/**
 * What is left of a pending diff once the values a PATCH actually carried are taken out of it —
 * `null` when the body carried them all.
 *
 * What remains is what `toOutgoingPatch` held back, and it is still unsaved: a `trafficProportion`
 * whose weights are mid-edit has to survive the response of the call that went out beside it, or
 * the rows would snap back to the older weights the server answered with (#37003).
 *
 * A key only settles while its pending value is still the one that was sent. The flush is
 * `switchMap`ped over the edits, but that only cancels a request once the *next* debounce emits —
 * an edit made while the response is travelling lands in the diff before it arrives. Settling by
 * key name would drop that edit: the field would read as saved, the form would keep showing it,
 * and the server would keep the older value with nothing left to resend it (#37003).
 */
export function withoutSentKeys(
    patch: DotExperimentPatchBody | null,
    sent: DotExperimentPatchBody
): DotExperimentPatchBody | null {
    if (!patch) {
        return null;
    }

    const remaining = { ...patch };

    (Object.keys(sent) as (keyof DotExperimentPatchBody)[]).forEach((key) => {
        if (isSameValue(remaining[key], sent[key])) {
            delete remaining[key];
        }
    });

    return hasPendingChanges(remaining) ? remaining : null;
}

/**
 * Whether a pending value is still the one that went out. The patch keys hold primitives and the
 * plain objects the reducers rebuild on every edit, so a structural comparison is what tells an
 * untouched key from one re-edited to an equal value — reference identity would not.
 */
function isSameValue(pending: unknown, sent: unknown): boolean {
    return JSON.stringify(pending) === JSON.stringify(sent);
}

/** The page a content-search contentlet stands for, as the Page card shows it. */
export function toConfigurePage(contentlet: DotCMSContentlet): DotExperimentConfigurePage {
    const path = contentlet.url ?? '';

    return {
        pageId: contentlet.identifier,
        title: contentlet.title || path,
        path
    };
}

/** The same shape from a page-browser result, which already carries a title and a path. */
export function fromBrowserPage(page: DotPageBrowserPage): DotExperimentConfigurePage {
    return {
        pageId: page.identifier,
        title: page.title,
        path: page.path || page.url
    };
}

/** Trailing slashes and casing are not part of the identity of a page path. */
export function normalizePath(path: string): string {
    return path.trim().toLowerCase().replace(/\/+$/, '');
}
