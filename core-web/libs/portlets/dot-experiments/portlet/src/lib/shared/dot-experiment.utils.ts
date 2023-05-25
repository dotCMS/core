import { formatPercent } from '@angular/common';

import {
    ComponentStatus,
    DEFAULT_VARIANT_ID,
    DotBayesianVariantResult,
    DotCreditabilityInterval,
    DotExperiment,
    DotResultDate,
    ExperimentChartDatasetColorsVariants,
    ExperimentSteps,
    LineChartColorsProperties,
    PROP_NOT_FOUND,
    TIME_14_DAYS,
    TIME_90_DAYS
} from '@dotcms/dotcms-models';

export const orderVariants = (arrayToOrder: Array<string>): Array<string> => {
    const index = arrayToOrder.indexOf(DEFAULT_VARIANT_ID);
    if (index > -1) {
        arrayToOrder.splice(index, 1);
    }

    arrayToOrder.unshift(DEFAULT_VARIANT_ID);

    return arrayToOrder;
};

export const getParsedChartData = (data: Record<string, DotResultDate>): number[] => {
    return Object.values(data).map((day) => day.multiBySession);
};

export const getPropertyColors = (index: number): LineChartColorsProperties => {
    return ExperimentChartDatasetColorsVariants[index];
};

/**
 * Process the config properties that comes form the BE as days,
 * return the object with the values in milliseconds
 * @param configProps
 *
 * @private
 */
export const processExperimentConfigProps = (
    configProps: Record<string, string>
): Record<string, number> => {
    const config: Record<string, number> = {};

    config['EXPERIMENTS_MIN_DURATION'] =
        configProps['EXPERIMENTS_MIN_DURATION'] === PROP_NOT_FOUND
            ? TIME_14_DAYS
            : daysToMilliseconds(+configProps['EXPERIMENTS_MIN_DURATION']);
    config['EXPERIMENTS_MAX_DURATION'] =
        configProps['EXPERIMENTS_MAX_DURATION'] === PROP_NOT_FOUND
            ? TIME_90_DAYS
            : daysToMilliseconds(+configProps['EXPERIMENTS_MAX_DURATION']);

    return config;
};

export const daysToMilliseconds = (days: number): number => {
    return days * 24 * 60 * 60 * 1000;
};

export const checkIfExperimentDescriptionIsSaving = (stepStatusSidebar) =>
    stepStatusSidebar &&
    stepStatusSidebar.experimentStep === ExperimentSteps.EXPERIMENT_DESCRIPTION &&
    stepStatusSidebar.status === ComponentStatus.SAVING;

/* Start function to extract data from the experiment and results endpoint
 *  To put together the summary table in the experiment results screen  */
export const getConversionRateRage = (
    data: DotCreditabilityInterval,
    noDataLabel: string
): string => {
    return data
        ? `${formatPercent(data.lower, 'en-US', '1.0-2')} to ${formatPercent(
              data.upper,
              'en-US',
              '1.0-2'
          )}`
        : noDataLabel;
};

export const getConversionRate = (uniqueBySession: number, sessions: number): string => {
    if (uniqueBySession !== 0 && sessions !== 0) {
        return formatPercent(uniqueBySession / sessions, 'en-US', '1.0-2');
    }

    return '0%';
};

export const getBayesianVariantResult = (
    variantName: string,
    results: DotBayesianVariantResult[]
): DotBayesianVariantResult => {
    return results.find((variant) => variant.variant === variantName);
};

export const getProbabilityToBeBest = (probability: number, noDataLabel: string): string => {
    return probability ? formatPercent(probability, 'en-US', '1.0-2') : noDataLabel;
};

export const isPromotedVariant = (experiment: DotExperiment, variantName: string): boolean => {
    return experiment.trafficProportion.variants.find(({ id }) => id === variantName)?.promoted;
};

/*End summary functions */
