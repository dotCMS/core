import { formatPercent } from '@angular/common';

import {
    DEFAULT_VARIANT_ID,
    DotBayesianVariantResult,
    DotCreditabilityInterval,
    DotExperiment,
    DotExperimentResults,
    DotResultVariant
} from '@dotcms/dotcms-models';

import { isPromotedVariant } from '../shared/dot-experiment-results.utils';
import { DotExperimentResultVariantDetail, LiftTone, VariantDetailLabels } from '../shared/models';

/**
 * Pure helpers behind the Results store: the percentage formats the summary table renders and the
 * Lift vs Original the backend does not send. Kept out of the store so each can be read — and
 * tested — on its own, without standing up the store or its injected services.
 */

/** What the control row, and any row measured against a control that never converted, renders. */
const NO_LIFT_LABEL = '—';

/** Lift is expressed in percentage points, so the rates are compared on their percent scale. */
const PERCENT_SCALE = 100;

/**
 * Below 10% two decimals carry the signal, above it one is enough — same rounding the old reports
 * screen used, so a rate reads identically on both.
 */
const getPercentageFormat = (value: number): string =>
    value < 0.1 ? formatPercent(value, 'en-US', '1.0-2') : formatPercent(value, 'en-US', '1.0-1');

/** Share of a variant's sessions that converted, as a `0..1` rate. No sessions is no conversion. */
export const conversionRateOf = (
    variant: DotResultVariant,
    results: DotExperimentResults
): number => {
    const sessions = results.sessions.variants[variant.variantName];

    return sessions ? variant.uniqueBySession.count / sessions : 0;
};

/**
 * Lift of a variant over the control, in percentage points.
 *
 * The control has nothing to be lifted over, and a control that never converted gives no baseline
 * to measure against — both render an em dash rather than a number that would read as a result
 * (AC16). Everything else is signed to one decimal, ties counting as positive.
 *
 * @param rate - The variant's conversion rate, `0..1`
 * @param controlRate - The control's conversion rate, `0..1`
 * @param isControl - Whether the row being built is the control itself
 */
export const buildLiftVsOriginal = (
    rate: number,
    controlRate: number,
    isControl: boolean
): { label: string; tone: LiftTone } => {
    if (isControl || controlRate === 0) {
        return { label: NO_LIFT_LABEL, tone: 'neutral' };
    }

    const points = (rate - controlRate) * PERCENT_SCALE;
    const isGain = points >= 0;

    return {
        label: `${isGain ? '+' : ''}${points.toFixed(1)} pts`,
        tone: isGain ? 'positive' : 'negative'
    };
};

/**
 * One summary-table row per variant of the primary goal, control included.
 *
 * The three payloads it reads name the same variant three different ways —
 * `DotResultVariant.variantName`, `DotBayesianVariantResult.variant` and `Variant.id` — so each
 * lookup is keyed on `variantName` and translated at the boundary.
 *
 * @param experiment - Carries `trafficProportion.variants`, the only place `promoted` lives
 * @param results - The results payload the rates, ranges and probabilities come from
 * @param labels - Already-translated copy for the values the backend cannot supply
 */
export const buildVariantDetails = (
    experiment: DotExperiment,
    results: DotExperimentResults,
    labels: VariantDetailLabels
): DotExperimentResultVariantDetail[] => {
    const variants = results.goals.primary.variants;
    const control = variants[DEFAULT_VARIANT_ID];
    const controlRate = control ? conversionRateOf(control, results) : 0;

    return Object.values(variants).map((variant) => {
        const bayesianResult = findBayesianVariantResult(
            variant.variantName,
            results.bayesianResult.results
        );
        const rate = conversionRateOf(variant, results);
        const lift = buildLiftVsOriginal(
            rate,
            controlRate,
            variant.variantName === DEFAULT_VARIANT_ID
        );

        return {
            id: variant.variantName,
            name: variant.variantDescription,
            conversions: variant.uniqueBySession.count,
            conversionRate: formatConversionRate(
                variant.uniqueBySession.count,
                results.sessions.variants[variant.variantName]
            ),
            conversionRateRange: formatConversionRateRange(
                bayesianResult?.credibilityInterval,
                labels
            ),
            sessions: results.sessions.variants[variant.variantName],
            probabilityToBeBest: formatProbabilityToBeBest(
                bayesianResult?.probability,
                labels.noDataLabel
            ),
            isWinner: results.bayesianResult.suggestedWinner === variant.variantName,
            isPromoted: isPromotedVariant(experiment, variant.variantName),
            liftVsOriginal: lift.label,
            liftTone: lift.tone
        };
    });
};

/** The Bayesian entry for a variant, which names it `variant` rather than `variantName`. */
const findBayesianVariantResult = (
    variantName: string,
    results: DotBayesianVariantResult[]
): DotBayesianVariantResult | undefined => results.find(({ variant }) => variant === variantName);

/** A variant that converted nothing, or was never served, reads as a flat `0%` rather than blank. */
const formatConversionRate = (conversions: number, sessions: number): string =>
    conversions !== 0 && sessions !== 0 ? getPercentageFormat(conversions / sessions) : '0%';

/** The 95% credibility interval, or the no-data copy while the backend has not computed one. */
const formatConversionRateRange = (
    interval: DotCreditabilityInterval | undefined,
    { noDataLabel, rangeSeparatorLabel }: VariantDetailLabels
): string =>
    interval
        ? `${getPercentageFormat(interval.lower)} ${rangeSeparatorLabel} ${getPercentageFormat(interval.upper)}`
        : noDataLabel;

/** Zero probability is as meaningless as a missing one here, so both read as no data. */
const formatProbabilityToBeBest = (probability: number | undefined, noDataLabel: string): string =>
    probability ? getPercentageFormat(probability) : noDataLabel;
