import {
    ComponentStatus,
    DotExperimentStatusList,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';

export interface DotExperiment {
    id: string;
    identifier: string;
    pageId: string;
    name: string;
    description: string;
    status: DotExperimentStatusList;
    readyToStart: boolean;
    archived: boolean;
    trafficProportion: TrafficProportion;
    trafficAllocation: number;
    scheduling: RangeOfDateAndTime | null;
    creationDate: Date;
    modDate: Date;
    goals: Goals | null;
}

export interface TrafficProportion {
    type: TrafficProportionTypes;
    variants: Array<Variant>;
}

export interface Variant {
    id: string;
    name: string;
    weight: number;
    url?: string;
}

export type GoalsLevels = 'primary';

export interface Goal {
    name: string;
    type: GOAL_TYPES;
    conditions?: Array<GoalCondition>;
}

export type Goals = Record<GoalsLevels, Goal>;

export interface GoalCondition {
    parameter: GOAL_PARAMETERS;
    operator: GOAL_OPERATORS;
    value: string;
    isDefault?: boolean;
}

export interface RangeOfDateAndTime {
    startDate: number;
    endDate: number;
}

export type GroupedExperimentByStatus = Partial<Record<DotExperimentStatusList, DotExperiment[]>>;

export interface SidebarStatus {
    status: ComponentStatus;
    isOpen: boolean;
}

export type StepStatus = SidebarStatus & {
    experimentStep: ExperimentSteps | null;
};

export type EditPageTabs = 'edit' | 'preview';

export enum ExperimentSteps {
    VARIANTS = 'variants',
    GOAL = 'goal',
    TARGETING = 'targeting',
    TRAFFIC_LOAD = 'trafficLoad',
    TRAFFICS_SPLIT = 'trafficSplit',
    SCHEDULING = 'scheduling'
}

export enum GOAL_TYPES {
    REACH_PAGE = 'REACH_PAGE',
    BOUNCE_RATE = 'BOUNCE_RATE',
    CLICK_ON_ELEMENT = 'CLICK_ON_ELEMENT'
}

export enum GOAL_OPERATORS {
    EQUALS = 'EQUALS',
    CONTAINS = 'CONTAINS'
}

export enum GOAL_PARAMETERS {
    URL = 'url',
    REFERER = 'referer'
}

export const ConditionDefaultByTypeOfGoal: Record<GOAL_TYPES, GOAL_PARAMETERS> = {
    [GOAL_TYPES.BOUNCE_RATE]: GOAL_PARAMETERS.URL,
    [GOAL_TYPES.REACH_PAGE]: GOAL_PARAMETERS.REFERER,
    [GOAL_TYPES.CLICK_ON_ELEMENT]: GOAL_PARAMETERS.URL
};
