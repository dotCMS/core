import { DotPageBrowserPage } from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_ID,
    DEFAULT_VARIANT_NAME,
    DotCMSContentlet,
    DotExperiment,
    DotExperimentStatus,
    GOAL_OPERATORS,
    GOAL_TYPES,
    Variant
} from '@dotcms/dotcms-models';

import { TOTAL_WEIGHT, WEIGHT_PRECISION } from '../shared/constants';
import {
    ConfigureFormModel,
    ConfigureValidationRule,
    DotExperimentConfigurePage,
    DotExperimentsConfigureViewState,
    GoalFormSlice,
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

/** The control is the `DEFAULT` variant; older experiments identify it by its name instead. */
export function isControlVariant(variant: Variant): boolean {
    return variant.id === DEFAULT_VARIANT_ID || variant.name === DEFAULT_VARIANT_NAME;
}

/**
 * Whether the experiment's page can still be changed.
 *
 * Mirrors what `ExperimentsAPIImpl.save()` enforces, so the screen refuses in the same cases the
 * server would rather than offering a choice that comes back a 400. Two conditions, and the second
 * is the substantive one: a non-control variant holds a copy of *this* page's layout, so repointing
 * the experiment would orphan it. The control holds no copy — it is the page — which is exactly why
 * it does not block.
 *
 * An experiment that does not exist yet has nothing to protect, so the page is free.
 */
export function canChangePage(experiment: DotExperiment | null): boolean {
    if (!experiment) {
        return true;
    }

    const variants = experiment.trafficProportion?.variants ?? [];

    return (
        experiment.status === DotExperimentStatus.DRAFT &&
        variants.length > 0 &&
        variants.every((variant) => isControlVariant(variant))
    );
}

/**
 * The variants standing in the way of a page change: everything but the control.
 *
 * The control is exempt because it holds no copy of the page — it *is* the page — which is why an
 * experiment whose only variant is the control may already repoint freely ({@link canChangePage}).
 * These are the ones the Change Page dialog lists, and the ones confirming it deletes.
 */
export function deletableVariants(experiment: DotExperiment | null): Variant[] {
    const variants = experiment?.trafficProportion?.variants ?? [];

    return variants.filter((variant) => !isControlVariant(variant));
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
    state: Pick<
        DotExperimentsConfigureViewState,
        'draftName' | 'selectedPage' | 'experiment' | 'formValue'
    >
): ConfigureValidationRule[] {
    const errors: ConfigureValidationRule[] = [];
    const experiment = state.experiment;

    // Read off the form, never off the experiment: the form is what the user is looking at, and
    // on `/experiments/new` it is the only thing that exists at all. Checking what is stored would
    // report a goal as missing while it sits complete on screen, unsaved.
    const goal = state.formValue?.goal ?? null;

    if (!state.draftName.trim()) {
        errors.push('name');
    }

    /**
     * The selected page is the form's value for the page, so it is the only thing asked.
     *
     * Falling back to `experiment.pageId` would report a page the user cannot see: a cleared
     * selection, a `?pageId=` that resolved to nothing, and a lookup that failed all leave the
     * card empty while the stored id is still there, and Start would go ahead on a page the
     * screen is not showing.
     */
    if (!state.selectedPage) {
        errors.push('page');
    }

    if (!goal?.type) {
        errors.push('goalType');
    }

    if (!goal?.name?.trim()) {
        errors.push('goalName');
    }

    errors.push(...validateGoalCondition(goal));

    /**
     * Variants are server state — adding, renaming and deleting one each have their own endpoint —
     * so the experiment is the only place they exist. The card keeps `Add new variant` disabled
     * until the POST answers, which is why `minVariants` is unsatisfiable before creation.
     *
     * Their *weights* are form state, and those come off the form for the same reason the goal
     * does: the user may be mid-edit on a total that is not stored anywhere yet.
     */
    const variants = experiment?.trafficProportion?.variants ?? [];
    const weights = state.formValue?.variantWeights ?? [];

    if (variants.length < 2) {
        errors.push('minVariants');
    }

    if (weights.length && totalWeight(weights) !== TOTAL_WEIGHT) {
        errors.push('weightsTotal');
    }

    return errors;
}

/**
 * Condition rules for the two goal types that have conditions. BOUNCE_RATE and EXIT_RATE have no
 * server-side conditions, so they are complete without one.
 *
 * Reads the form's flat slice rather than the persisted condition shape: the two goal types nest
 * their value differently once stored, and the screen is validating what is on it.
 */
function validateGoalCondition(goal: GoalFormSlice | null): ConfigureValidationRule[] {
    if (goal?.type === GOAL_TYPES.REACH_PAGE) {
        return goal.value.trim() ? [] : ['goalConditionValue'];
    }

    if (goal?.type !== GOAL_TYPES.URL_PARAMETER) {
        return [];
    }

    const errors: ConfigureValidationRule[] = [];

    // EXISTS only asks whether the parameter is there, so it needs no value — the name it looks
    // for is still required.
    if (goal.operator !== GOAL_OPERATORS.EXISTS && !goal.value.trim()) {
        errors.push('goalConditionValue');
    }

    if (!goal.parameterName.trim()) {
        errors.push('goalParameterName');
    }

    return errors;
}

/**
 * Whether the form is exactly as it was at the last successful write.
 *
 * Structural comparison over the whole model: the slices are plain data — strings, numbers, a flat
 * goal, two dates, a list of weights — so serialising both sides answers it without a per-key
 * walk that would have to be revisited every time a field is added. Key order is stable because
 * both values come from the same builder.
 */
export function isSameFormValue(
    a: ConfigureFormModel | null,
    b: ConfigureFormModel | null
): boolean {
    return JSON.stringify(a) === JSON.stringify(b);
}

/**
 * The page a content-search contentlet stands for, as the Page card shows it.
 *
 * `languageId` is copied through rather than defaulted: the variant deep link sends it as
 * `language_id`, and a page whose language is unknown must reach the builder as unknown so the
 * action can be refused (FR-004). Defaulting to 1 here would open the wrong language's content
 * with nothing reporting an error.
 */
export function toConfigurePage(contentlet: DotCMSContentlet): DotExperimentConfigurePage {
    const path = contentlet.url ?? '';

    return {
        pageId: contentlet.identifier,
        title: contentlet.title || path,
        path,
        languageId: contentlet.languageId
    };
}

/** The same shape from a page-browser result, which already carries a title, path and language. */
export function fromBrowserPage(page: DotPageBrowserPage): DotExperimentConfigurePage {
    return {
        pageId: page.identifier,
        title: page.title,
        path: page.path || page.url,
        languageId: page.languageId
    };
}

/** Trailing slashes and casing are not part of the identity of a page path. */
export function normalizePath(path: string): string {
    return path.trim().toLowerCase().replace(/\/+$/, '');
}
