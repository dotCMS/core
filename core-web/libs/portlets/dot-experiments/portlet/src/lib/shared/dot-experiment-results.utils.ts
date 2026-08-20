import { ChartData } from 'chart.js';
import { jStat } from 'jstat';

import { DotMessageService } from '@dotcms/data-access';
import {
    BayesianStatusResponse,
    DEFAULT_VARIANT_ID,
    DotExperiment,
    DotExperimentResults,
    DotExperimentStatus,
    DotResultDate,
    DotResultGoal,
    ExperimentChartDatasetColorsVariants,
    ExperimentLinearChartDatasetDefaultProperties,
    ExperimentLineChartDatasetDefaultProperties,
    LineChartColorsProperties,
    MonthsOfTheYear,
    ReportSummaryLegendByBayesianStatus,
    SummaryLegend
} from '@dotcms/dotcms-models';

const ONE_DAY = 24 * 60 * 60 * 1000;

export const orderVariants = (arrayToOrder: Array<string>): Array<string> => {
    const index = arrayToOrder.indexOf(DEFAULT_VARIANT_ID);
    if (index > -1) {
        arrayToOrder.splice(index, 1);
    }

    arrayToOrder.unshift(DEFAULT_VARIANT_ID);

    return arrayToOrder;
};

/**
 * Retrieves an array of uniqueBySession values from the given data.
 *
 * @param {Record<string, DotResultDate>} data - The data object containing DotResultDate values.
 * @return {number[]} - An array of conversion Rate values.
 */
export const getParsedChartData = (data: Record<string, DotResultDate>): number[] => {
    return [0, ...Object.values(data).map((day) => Math.round(day.conversionRate * 100) / 100)];
};

export const getPropertyColors = (index: number): LineChartColorsProperties => {
    return ExperimentChartDatasetColorsVariants[index];
};

export const isPromotedVariant = (experiment: DotExperiment, variantName: string): boolean => {
    return !!experiment.trafficProportion.variants.find(({ id }) => id === variantName)?.promoted;
};

export const getPreviousDay = (givenDate: string) => {
    const [year, month, day] = givenDate.split('-').map(Number);

    // Create a Date object in UTC | - 1 - Months are zero-based in JavaScript
    const inputDateUTC = new Date(Date.UTC(year, month - 1, day));

    // in milliseconds to avoid TIMEZONE issues & month change.
    inputDateUTC.setTime(inputDateUTC.getTime() - ONE_DAY);

    // Format the date as "YYYY-MM-dd"
    return inputDateUTC.toISOString().split('T')[0];
};

export const getRandomUUID = () => self.crypto.randomUUID();

export const getSuggestedWinner = (
    experiment: DotExperiment,
    results: DotExperimentResults
): SummaryLegend => {
    const { bayesianResult, sessions } = results;

    if (!bayesianResult) {
        return ReportSummaryLegendByBayesianStatus.NO_ENOUGH_SESSIONS;
    }

    const hasSessions = sessions.total > 0;
    const isATieBayesianSuggestionWinner =
        bayesianResult?.suggestedWinner === BayesianStatusResponse.TIE;
    const isNoneBayesianSuggestionWinner =
        bayesianResult?.suggestedWinner === BayesianStatusResponse.NONE;

    if (!hasSessions || isNoneBayesianSuggestionWinner) {
        return experiment.status === DotExperimentStatus.ENDED
            ? ReportSummaryLegendByBayesianStatus.NO_WINNER_FOUND
            : ReportSummaryLegendByBayesianStatus.NO_ENOUGH_SESSIONS;
    }

    if (isATieBayesianSuggestionWinner) {
        return { ...ReportSummaryLegendByBayesianStatus.NO_WINNER_FOUND };
    }

    return experiment.status === DotExperimentStatus.ENDED
        ? { ...ReportSummaryLegendByBayesianStatus.WINNER }
        : { ...ReportSummaryLegendByBayesianStatus.PRELIMINARY_WINNER };
};

/**
 * Generate the data to use in the Bayesian chart
 * @param results
 */
export const getBayesianDatasets = (
    results: DotExperimentResults
): ChartData<'line'>['datasets'] => {
    const { variants } = results.goals.primary;
    const { sessions, bayesianResult } = results;

    // If we don't have a suggested winner, return an empty array
    if (!bayesianResult || bayesianResult.suggestedWinner === BayesianStatusResponse.NONE) {
        return [];
    }

    // Iterate through all the variants
    return Object.entries(variants).map(([variantId, variant], index) => {
        // Calculate the number of successes and failures
        const success = variant.uniqueBySession.count;
        const failure = sessions.variants[variantId] - variant.uniqueBySession.count;
        const label = variant.variantDescription;

        // Generate the data for the chart, I need at least 1 failure to generate data
        const data: { x: number; y: number }[] =
            failure > 0 ? generateProbabilityDensityData(success, failure) : [];

        // Create the dataset
        return {
            label,
            data,
            ...getPropertyColors(index),
            ...ExperimentLinearChartDatasetDefaultProperties
        };
    });
};

/**
 * Builds one line dataset per variant, control first, for the daily conversion rate chart.
 *
 * @param variants - Primary goal results keyed by variant id
 * @returns Chart.js line datasets in display order
 */
export const buildDailyChartData = (
    variants: DotResultGoal['variants']
): ChartData<'line'>['datasets'] => {
    const variantsOrdered = orderVariants(Object.keys(variants));

    let colorIndex = 0;

    return variantsOrdered.map((variantName) => {
        const { details } = variants[variantName];

        return {
            label: variants[variantName].variantDescription,
            data: getParsedChartData(details),
            ...getPropertyColors(colorIndex++),
            ...ExperimentLineChartDatasetDefaultProperties
        };
    });
};

/**
 * Builds the translated `month-day` axis labels for the daily conversion rate chart.
 *
 * The label list is prefixed with the day before the first result so the chart starts at zero.
 *
 * @param variants - Primary goal results keyed by variant id
 * @param dotMessageService - Used to translate the month name
 * @returns Ordered axis labels, empty when the control variant has no daily details
 */
export const buildDailyChartLabels = (
    variants: DotResultGoal['variants'],
    dotMessageService: DotMessageService
): string[] => {
    return variants[DEFAULT_VARIANT_ID].details
        ? parseDaysLabels(Object.keys(variants[DEFAULT_VARIANT_ID].details), dotMessageService)
        : [];
};

/**
 * Generates the data for the probability density function of a beta distribution.
 * @param {number} alpha - The alpha parameter of the beta distribution.
 * @param {number} beta - The beta parameter of the beta distribution.
 * @param {number} step
 * @returns {object[]} An array of objects with x and y values.
 */
const generateProbabilityDensityData = (
    alpha: number,
    beta: number,
    step = 0.01
): { x: number; y: number }[] => {
    // Create a beta distribution object using the alpha and beta parameters.
    const betaDist = new jStat.beta(alpha, beta);

    const data = [];
    // Loop through the x values from 0 to 1.
    for (let i = 0; i <= 1; i += step) {
        // Set the x value to the current value of i.
        const x = Number(i.toFixed(2));
        // Set the y value to the value of the pdf at the current value of i.
        const y = Number(betaDist.pdf(x).toFixed(2));

        if (!isFinite(y)) {
            continue;
        }

        // Add the x and y values to the data array.
        data.push({ x, y });
    }

    return arePointsALine(data) ? [] : data;
};

/**
 * Check if a set of points are all on the same line.
 *
 * @param {Array<{ x: number; y: number }>} points - The array of points to check.
 * @returns {boolean} - True if all points are on the same line, false otherwise.
 */
const arePointsALine = (points: { x: number; y: number }[]): boolean => {
    if (points.length < 3) {
        return true;
    }

    const referenceSlope = (points[1].y - points[0].y) / (points[1].x - points[0].x);

    for (let i = 1; i < points.length - 1; i++) {
        const slope = (points[i + 1].y - points[i].y) / (points[i + 1].x - points[i].x);
        if (Math.abs(slope - referenceSlope) > 1e-6) {
            return false;
        }
    }

    return true;
};

const parseDaysLabels = (labels: Array<string>, dotMessageService: DotMessageService): string[] => {
    return [getPreviousDay(labels[0]), ...labels].map((item) => {
        const [, month, day] = item.split('-').map(Number);
        const monthTranslated = dotMessageService.get(MonthsOfTheYear[month - 1]);

        return `${monthTranslated}-${day}`;
    });
};
