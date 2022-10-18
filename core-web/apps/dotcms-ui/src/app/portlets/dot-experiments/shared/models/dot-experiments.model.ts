import {
    DotExperimentStatusList,
    TrafficProportionTypes
} from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotPage } from '@models/dot-page/dot-page.model';

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
}

export type RenderedPageExperiments = Pick<DotPage, 'title' | 'identifier'>;

export type GroupedExperimentByStatus = Partial<Record<DotExperimentStatusList, DotExperiment[]>>;

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
