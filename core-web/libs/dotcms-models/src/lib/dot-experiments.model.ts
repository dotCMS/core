import {
    DotExperimentStatusList,
    DotPage,
    Status,
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
    trafficAllocation: string;
    scheduling: RangeOfDateAndTime | null;
    creationDate: Date;
    modDate: Date;
}

export type RenderedPageExperiments = Pick<DotPage, 'title' | 'identifier'>;

export type GroupedExperimentByStatus = Partial<Record<DotExperimentStatusList, DotExperiment[]>>;

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

interface RangeOfDateAndTime {
    startDate: Date;
    endDate: Date;
}

export interface StepStatus {
    status: Status;
    isOpen: boolean;
    experimentStep: ExperimentSteps | null;
}

export interface DotStoreWithSidebar {
    isOpenSidebar: boolean;
    isSaving: boolean;
}

export type EditPageTabs = 'edit' | 'preview';

export enum ExperimentSteps {
    VARIANTS = 'variants',
    GOAL = 'goal',
    TARGETING = 'targeting',
    TRAFFIC = 'traffic',
    SCHEDULING = 'scheduling'
}
