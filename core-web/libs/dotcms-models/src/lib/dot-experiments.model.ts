import { ChartDataset } from 'chart.js';

import { MenuItem } from 'primeng/api';

import {
    BayesianStatusResponse,
    ComponentStatus,
    DotExperimentStatus,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';

export interface DotExperiment {
    id: string;
    identifier: string;
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
    modDate: Date;
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
    conditions?: Array<GoalCondition>;
}

export type Goals = Record<GoalsLevels, Goal>;

export interface GoalCondition {
    parameter: GOAL_PARAMETERS | string;
    operator: GOAL_OPERATORS;
    value: string;
    isDefault?: boolean;
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
 * Default condition by type of goal in Goal Selection Sidebar
 */
export const ConditionDefaultByTypeOfGoal: Partial<Record<GOAL_TYPES, GOAL_PARAMETERS>> = {
    [GOAL_TYPES.BOUNCE_RATE]: GOAL_PARAMETERS.URL,
    [GOAL_TYPES.REACH_PAGE]: GOAL_PARAMETERS.REFERER,
    [GOAL_TYPES.CLICK_ON_ELEMENT]: GOAL_PARAMETERS.URL
};

//Todo: Update the missing one with the new colors
export const ChartColors = {
    // Variants colors
    original: {
        line: 'rgb(66,194,240)',
        fill: 'rgba(66,194,240,0.40)'
    },
    variant_1: {
        line: 'rgb(195,54,229)',
        fill: 'rgba(195,54,229,0.40)'
    },
    variant_2: {
        line: 'rgb(255, 180, 68)',
        fill: 'rgba(255, 180, 68,0.40)'
    },
    // Chart colors
    xAxis: {
        border: getComputedStyle(document.body).getPropertyValue('--color-palette-black-op-20'),
        gridLine: getComputedStyle(document.body).getPropertyValue('--color-palette-black-op-30')
    },
    yAxis: {
        border: getComputedStyle(document.body).getPropertyValue('--color-palette-black-op-20'),
        gridLine: getComputedStyle(document.body).getPropertyValue('--color-palette-black-op-50')
    },
    ticks: {
        color: getComputedStyle(document.body).getPropertyValue('--color-palette-black-op-70')
    },
    white: '#FFFFFF',
    black: '#14151a'
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
