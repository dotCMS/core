import { of } from 'rxjs';
import {
    DotExperimentStatusList,
    TrafficProportionTypes
} from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotExperiment } from '@portlets/dot-experiments/shared/models/dot-experiments.model';

export const ExperimentMocks: Array<DotExperiment> = [
    {
        id: '111',
        identifier: '1111-1111-1111-1111',
        pageId: '456',
        status: DotExperimentStatusList.DRAFT,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100.0,
        scheduling: null,
        trafficProportion: {
            percentages: {},
            type: TrafficProportionTypes.SPLIT_EVENLY
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03')
    },
    {
        id: '222',
        identifier: '2222-2222-2222-2222',
        pageId: '456',
        status: DotExperimentStatusList.DRAFT,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100.0,
        scheduling: null,
        trafficProportion: {
            percentages: {},
            type: TrafficProportionTypes.SPLIT_EVENLY
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03')
    }
];

/*export class DotExperimentsShellStoreMock {
    getPageId$(): Observable<string> {
        return of('spy it');
    }
}*/

/*export class DotExperimentsListStoreMock {
    loadExperiments() {
        return of(ExperimentMocks);
    }
}*/

export const DotExperimentsListStoreMock = {
    addExperiment: () => of({}),
    setCloseSidebar: () => of({})
};

export const DotExperimentsShellStoreMock = {
    getPageId$: of('spy it')
};

export const DotExperimentsServiceMock = {
    add: () => of({}),
    get: () => of({}),
    delete: () => of({}),
    archive: () => of({})
};

export class ActivatedRouteMock {
    get parent() {
        return {
            parent: {
                parent: {
                    snapshot: {
                        params: {
                            pageId: 'pageId'
                        }
                    }
                }
            }
        };
    }
}
