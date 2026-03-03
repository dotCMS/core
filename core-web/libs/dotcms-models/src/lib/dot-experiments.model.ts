import { ChartDataset } from 'chart.js';

import type { MenuItem } from 'primeng/api';

import {
    BayesianStatusResponse,
    DotExperimentStatus,
    TrafficProportionTypes
} from './dot-experiments-constants';
import { ComponentStatus } from './shared-models';

export interface DotExperiment {
    id: string;
    pageId: string;
    name: string;
    description: string;
    status: DotExperimentStatus;
    readyToStart: boolean;
    archived: boolean;
    trafficProportion: TrafficProportion;
    trafficAllocation: number;
    scheduling: RangeOfDateAndTime | null;
    creationDate: Date;
    modDate: number;
    goals: Goals | null;
}

export type DotExperimentsWithActions = DotExperiment & { actionsItemsMenu: MenuItem[] };

export interface DotExperimentResults {
    bayesianResult: DotResultBayesian;
    goals: Record<GoalsLevels, DotResultGoal>;
    sessions: DotResultSessions;
}

export interface DotResultBayesian {
    value: number;
    suggestedWinner: BayesianStatusResponse | string;
    results: DotBayesianVariantResult[];
}

export interface DotBayesianVariantResult {
    conversionRate: number;
    credibilityInterval: DotCreditabilityInterval;
    probability: number;
    risk: number;
    variant: string;
}

export interface DotCreditabilityInterval {
    lower: number;
    upper: number;
}

export interface DotResultGoal {
    goal: Goal;
    variants: Record<'DEFAULT' | string, DotResultVariant>;
}

export interface DotResultVariant {
    details: Record<string, DotResultDate>;
    multiBySession: number;
    uniqueBySession: DotResultUniqueBySession;
    variantName: string;
    variantDescription: string;
    totalPageViews: number;
}

export interface DotResultUniqueBySession {
    count: number;
    totalPercentage: number;
    variantPercentage: number;
}

export interface DotResultDate {
    multiBySession: number;
    uniqueBySession: number;
    conversionRate: number;
}

export interface DotResultSessions {
    total: number;
    variants: Record<string, number>;
}

export interface TrafficProportion {
    type: TrafficProportionTypes;
    variants: Array<Variant>;
}

export interface DotExperimentVariantDetail {
    id: string;
    name: string;
    conversions: number;
    conversionRate: string;
    conversionRateRange: string;
    sessions: number;
    probabilityToBeBest: string;
    isWinner: boolean;
    isPromoted: boolean;
}

export interface Variant {
    id: string;
    name: string;
    weight: number;
    url?: string;
    promoted?: boolean;
}

export type GoalsLevels = 'primary';

export interface Goal {
    name: string;
    type: GOAL_TYPES;
    conditions: Array<UrlParameterGoalCondition | ReachPageGoalCondition>;
}

export type Goals = Record<GoalsLevels, Goal>;

export interface ReachPageGoalCondition {
    parameter: GOAL_PARAMETERS | string;
    operator: GOAL_OPERATORS;
    value: string;
}
export interface UrlParameterGoalCondition {
    parameter: GOAL_PARAMETERS;
    operator: GOAL_OPERATORS;
    value: {
        name: string;
        value: string;
    };
}

export interface RangeOfDateAndTime {
    startDate: number | null;
    endDate: number | null;
}

export type GroupedExperimentByStatus = {
    status: DotExperimentStatus;
    experiments: DotExperimentsWithActions[];
};

export interface SidebarStatus {
    status: ComponentStatus;
    isOpen: boolean;
}

export type StepStatus = SidebarStatus & {
    experimentStep: ExperimentSteps | null;
};

export enum ExperimentSteps {
    VARIANTS = 'variants',
    GOAL = 'goal',
    TARGETING = 'targeting',
    TRAFFIC_LOAD = 'trafficLoad',
    TRAFFICS_SPLIT = 'trafficSplit',
    SCHEDULING = 'scheduling',
    EXPERIMENT_DESCRIPTION = 'EXPERIMENT_DESCRIPTION'
}

export enum GOAL_TYPES {
    REACH_PAGE = 'REACH_PAGE',
    BOUNCE_RATE = 'BOUNCE_RATE',
    CLICK_ON_ELEMENT = 'CLICK_ON_ELEMENT',
    URL_PARAMETER = 'URL_PARAMETER',
    EXIT_RATE = 'EXIT_RATE'
}

export enum GOAL_OPERATORS {
    EQUALS = 'EQUALS',
    CONTAINS = 'CONTAINS',
    EXISTS = 'EXISTS'
}

export enum GOAL_PARAMETERS {
    URL = 'url',
    REFERER = 'referer',
    QUERY_PARAM = 'queryParam'
}

/**
 * Allowed condition operators by type of goal
 */
export const AllowedConditionOperatorsByTypeOfGoal = {
    [GOAL_TYPES.REACH_PAGE]: GOAL_PARAMETERS.URL,
    [GOAL_TYPES.URL_PARAMETER]: 'queryParameter'
};

const dotCMSThemeColors = {
    black: '#14151a',
    white: '#FFFFFF',
    accentTurquoise: 'rgb(66,194,240)',
    accentTurquoiseOp40: 'rgba(66,194,240,0.40)',

    accentFuchsia: 'rgb(195,54,229)',
    accentFuchsiaOp40: 'rgba(195,54,229,0.40)',

    accentYellow: 'rgb(255, 180, 68)',
    accentYellowOp40: 'rgba(255, 180, 68,0.40)',

    colorPaletteBlackOp20: getComputedStyle(document.body).getPropertyValue(
        '--color-palette-black-op-20'
    ),
    colorPaletteBlackOp30: getComputedStyle(document.body).getPropertyValue(
        '--color-palette-black-op-30'
    ),
    colorPaletteBlackOp50: getComputedStyle(document.body).getPropertyValue(
        '--color-palette-black-op-50'
    ),
    colorPaletteBlackOp70: getComputedStyle(document.body).getPropertyValue(
        '--color-palette-black-op-70'
    )
};

export const ChartColors = {
    original: {
        line: dotCMSThemeColors.accentTurquoise,
        fill: dotCMSThemeColors.accentTurquoiseOp40
    },
    variant_1: {
        line: dotCMSThemeColors.accentFuchsia,
        fill: dotCMSThemeColors.accentFuchsiaOp40
    },
    variant_2: {
        line: dotCMSThemeColors.accentYellow,
        fill: dotCMSThemeColors.accentYellowOp40
    },
    // Chart colors
    xAxis: {
        border: dotCMSThemeColors.colorPaletteBlackOp20,
        gridLine: dotCMSThemeColors.colorPaletteBlackOp30
    },
    yAxis: {
        border: dotCMSThemeColors.colorPaletteBlackOp20,
        gridLine: dotCMSThemeColors.colorPaletteBlackOp50
    },
    ticks: {
        color: dotCMSThemeColors.colorPaletteBlackOp70
    },
    white: dotCMSThemeColors.white,
    black: dotCMSThemeColors.black
};

export type LineChartColorsProperties = Pick<
    ChartDataset<'line'>,
    'borderColor' | 'backgroundColor' | 'pointBackgroundColor'
>;

export const ExperimentChartDatasetColorsVariants: Array<LineChartColorsProperties> = [
    {
        borderColor: ChartColors.original.line,
        pointBackgroundColor: ChartColors.original.line,
        backgroundColor: ChartColors.original.fill
    },
    {
        borderColor: ChartColors.variant_1.line,
        pointBackgroundColor: ChartColors.variant_1.line,
        backgroundColor: ChartColors.variant_1.fill
    },
    {
        borderColor: ChartColors.variant_2.line,
        pointBackgroundColor: ChartColors.variant_2.line,
        backgroundColor: ChartColors.variant_2.fill
    }
];

export const ExperimentLineChartDatasetDefaultProperties: Partial<ChartDataset<'line'>> = {
    type: 'line',
    pointRadius: 4,
    pointHoverRadius: 6,
    fill: true,
    cubicInterpolationMode: 'monotone',
    borderWidth: 2
};

export const ExperimentLinearChartDatasetDefaultProperties: Partial<ChartDataset<'line'>> = {
    ...ExperimentLineChartDatasetDefaultProperties,
    pointRadius: 0,
    pointHoverRadius: 2.5
};

export type GoalConditionsControlsNames = 'parameter' | 'operator' | 'value';
