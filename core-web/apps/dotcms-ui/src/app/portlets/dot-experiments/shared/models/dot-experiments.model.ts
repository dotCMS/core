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
    weight: number;
    url?: string;
}

interface RangeOfDateAndTime {
    startDate: Date;
    endDate: Date;
}
