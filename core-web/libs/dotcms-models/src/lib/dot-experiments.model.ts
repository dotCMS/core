import { DotExperimentStatusList, Status, TrafficProportionTypes } from '@dotcms/dotcms-models';

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
    trafficAllocation: string;
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
    weight: string;
    url?: string;
}

export type GoalsLevels = 'primary';

export interface Goal {
    name: string;
    type: GOAL_TYPES;
    conditions: Array<GoalCondition>;
}

export type Goals = Record<GoalsLevels, Goal>;

export interface GoalCondition {
    parameter: GOAL_PARAMETERS;
    operator: GOAL_OPERATORS;
    value: string;
}

interface RangeOfDateAndTime {
    startDate: Date;
    endDate: Date;
}

export type GroupedExperimentByStatus = Partial<Record<DotExperimentStatusList, DotExperiment[]>>;

export interface StepStatus {
    status: Status;
    isOpen: boolean;
    experimentStep: ExperimentSteps | null;
}

export type GoalSelectOption = {
    label: string;
    value: string;
    inactive: boolean;
    description: string;
};

export type EditPageTabs = 'edit' | 'preview';

export enum ExperimentSteps {
    VARIANTS = 'variants',
    GOAL = 'goal',
    TARGETING = 'targeting',
    TRAFFIC = 'traffic',
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
    URL = 'url'
}
