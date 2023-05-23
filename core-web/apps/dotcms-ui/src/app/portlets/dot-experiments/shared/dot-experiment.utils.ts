import {
    DEFAULT_VARIANT_ID,
    DotResultDate,
    ExperimentChartDatasetColorsVariants,
    LineChartColorsProperties
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
