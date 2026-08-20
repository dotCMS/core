import { formatPercent } from '@angular/common';

import {
    ComponentStatus,
    DotBayesianVariantResult,
    DotCreditabilityInterval,
    ExperimentSteps,
    PROP_NOT_FOUND,
    TIME_7_DAYS,
    TIME_90_DAYS
} from '@dotcms/dotcms-models';

const ONE_DAY = 24 * 60 * 60 * 1000;

/**
 * Process the config properties that comes form the BE as days,
 * return the object with the values in milliseconds
 * @param configProps
 *
 * @private
 */
export const processExperimentConfigProps = (
    configProps: Record<string, string | boolean>
): Record<string, number> => {
    const config: Record<string, number> = {};

    const minDurationRaw = configProps['EXPERIMENTS_MIN_DURATION'];
    const maxDurationRaw = configProps['EXPERIMENTS_MAX_DURATION'];

    config['EXPERIMENTS_MIN_DURATION'] =
        typeof minDurationRaw !== 'string' || minDurationRaw === PROP_NOT_FOUND
            ? TIME_7_DAYS
            : daysToMilliseconds(+minDurationRaw);
    config['EXPERIMENTS_MAX_DURATION'] =
        typeof maxDurationRaw !== 'string' || maxDurationRaw === PROP_NOT_FOUND
            ? TIME_90_DAYS
            : daysToMilliseconds(+maxDurationRaw);

    return config;
};

export const daysToMilliseconds = (days: number): number => {
    return days * ONE_DAY;
};

export const checkIfExperimentDescriptionIsSaving = (stepStatusSidebar) =>
    stepStatusSidebar &&
    stepStatusSidebar.experimentStep === ExperimentSteps.EXPERIMENT_DESCRIPTION &&
    stepStatusSidebar.status === ComponentStatus.SAVING;

/* Start function to extract data from the experiment and results endpoint
 *  To put together the summary table in the experiment results screen  */
export const getConversionRateRage = (
    data: DotCreditabilityInterval,
    noDataLabel: string,
    separatorLabel: string
): string => {
    return data
        ? `${getPercentageFormat(data.lower)} ${separatorLabel} ${getPercentageFormat(data.upper)}`
        : noDataLabel;
};

export const getConversionRate = (uniqueBySession: number, sessions: number): string => {
    if (uniqueBySession !== 0 && sessions !== 0) {
        return getPercentageFormat(uniqueBySession / sessions);
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
    return probability ? getPercentageFormat(probability) : noDataLabel;
};

/**
 * Given a number, identify if is lower that 10% round 2 decimals if is higher than 10 round to 1 decimal
 */
const getPercentageFormat = (value: number): string => {
    return value < 0.1
        ? formatPercent(value, 'en-US', '1.0-2')
        : formatPercent(value, 'en-US', '1.0-1');
};
