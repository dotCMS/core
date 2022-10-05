import { DotPage } from '@models/dot-page/dot-page.model';

export interface DotExperiment {
    id: string;
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
}

export type RenderedPageExperiments = Pick<DotPage, 'title' | 'identifier'>;

export type GroupedExperimentByStatus = Partial<Record<DotExperimentStatusList, DotExperiment[]>>;

export enum DotExperimentStatusList {
    RUNNING = 'RUNNING',
    SCHEDULED = 'SCHEDULED',
    ENDED = 'ENDED',
    DRAFT = 'DRAFT',
    ARCHIVED = 'ARCHIVED'
}

export const ExperimentsStatusList = [
    {
        label: 'experimentspage.experiment.status.draft',
        value: DotExperimentStatusList.DRAFT
    },
    {
        label: 'experimentspage.experiment.status.running',
        value: DotExperimentStatusList.RUNNING
    },
    {
        label: 'experimentspage.experiment.status.ended',
        value: DotExperimentStatusList.ENDED
    },
    {
        label: 'experimentspage.experiment.status.archived',
        value: DotExperimentStatusList.ARCHIVED
    },
    {
        label: 'experimentspage.experiment.status.scheduled',
        value: DotExperimentStatusList.SCHEDULED
    }
];

interface TrafficProportion {
    percentages: {
        [index: string]: number;
    };
    type: TrafficProportionTypes;
}

interface RangeOfDateAndTime {
    startDate: Date;
    endDate: Date;
}

export const enum TrafficProportionTypes {
    SPLIT_EVENLY = 'SPLIT_EVENLY',
    CUSTOM_PERCENTAGES = 'CUSTOM_PERCENTAGES'
}
