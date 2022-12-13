import { of } from 'rxjs';

import {
    DotExperiment,
    DotExperimentStatusList,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';

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
        trafficAllocation: '100.0',
        scheduling: null,
        trafficProportion: {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [{ id: '111', name: 'DEFAULT', weight: '100.0' }]
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
        trafficAllocation: '100.0',
        scheduling: null,
        trafficProportion: {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [{ id: '222', name: 'DEFAULT', weight: '100.0' }]
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03')
    }
];

export const dotExperimentsCreateStoreStub = {
    state$: () =>
        of({
            isSaving: false,
            isOpenSidebar: false
        }),
    setCloseSidebar: () => of({}),
    addExperiments: () => of([])
};

export const DotExperimentsListStoreMock = {
    addExperiment: () => of({}),
    setCloseSidebar: () => of({}),
    getPage$: of({
        pageId: '1111'
    })
};

export const DotExperimentsConfigurationStoreMock = {
    deleteVariant: () => of([]),
    addVariant: () => of([]),
    openSidebar: () => of([]),
    closeSidebar: () => of([]),
    loadExperiment: () => of([]),
    getExperimentId$: of('1111111'),
    vm$: of({
        pageId: '',
        experimentId: '',
        experiment: null,
        isLoading: true
    })
};

export const DotExperimentsServiceMock = {
    add: () => of({}),
    get: () => of({}),
    delete: () => of({}),
    archive: () => of({}),
    getById: () => of({}),
    removeVariant: () => of({}),
    addVariant: () => of({})
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

export class ActivatedRouteListStoreMock {
    get snapshot() {
        return {
            params: {
                pageId: '1111'
            },
            parent: {
                parent: {
                    parent: {
                        parent: {
                            data: {
                                content: {
                                    page: {
                                        title: 'title'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }
}
