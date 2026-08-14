import { DotPageBrowserPage } from '@dotcms/data-access';
import {
    DotCMSContentlet,
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
    ExperimentFieldGroup
} from '../shared/models';

/**
 * Pure helpers behind the Configure store: variant weights, the Start/Schedule rules, the pending
 * autosave bookkeeping and the page shapes the two lookups answer with. Kept out of the store so
 * each can be read — and tested — on its own, without standing up the store, its injected services
 * or its lifecycle hooks.
 */

/**
 * Splits 100% across the variants: `floor(100/n)` each, with the remainder absorbed by the first
 * one so the total is exactly 100 for any variant count (AC23).
 */
export function splitWeightsEvenly(variants: Variant[]): Variant[] {
    if (!variants.length) {
        return variants;
    }

    const share = Math.floor(TOTAL_WEIGHT / variants.length);
    const remainder = TOTAL_WEIGHT - share * variants.length;

    return variants.map((variant, index) => ({
        ...variant,
        weight: index === 0 ? share + remainder : share
    }));
}

/** Sum of the variant weights, rounded to the precision they are stored at. */
export function totalWeight(variants: Variant[]): number {
    const total = variants.reduce((sum, { weight }) => sum + (weight ?? 0), 0);

    return Math.round(total * WEIGHT_PRECISION) / WEIGHT_PRECISION;
}

/**
 * The eight rules Start/Schedule checks, in the order the screen reads top to bottom — which is
 * also the order the shell scrolls through to find the first failing field.
 *
 * Nothing here runs before Start is pressed (AC28), so this is a plain function over the state
 * rather than a computed: materialising it early is exactly what the screen must not do.
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

/** Marks a field group as having an autosave pending, tolerating a group already marked. */
export function addPendingGroup(
    groups: ExperimentFieldGroup[],
    group: ExperimentFieldGroup
): ExperimentFieldGroup[] {
    return groups.includes(group) ? groups : [...groups, group];
}

/** Settles a field group, whether its autosave resulted in a call or not. */
export function removePendingGroup(
    groups: ExperimentFieldGroup[],
    group: ExperimentFieldGroup
): ExperimentFieldGroup[] {
    return groups.filter((pending) => pending !== group);
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
