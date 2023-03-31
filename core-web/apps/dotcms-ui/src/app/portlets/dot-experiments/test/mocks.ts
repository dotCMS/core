import { of } from 'rxjs';

import {
    ComponentStatus,
    DEFAULT_VARIANT_ID,
    DEFAULT_VARIANT_NAME,
    DotExperiment,
    DotExperimentResults,
    DotExperimentStatusList,
    DotResultSimpleVariant,
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goals,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';

export const GoalsMock: Goals = {
    primary: {
        name: 'default',
        type: GOAL_TYPES.REACH_PAGE,
        conditions: [
            {
                parameter: GOAL_PARAMETERS.URL,
                operator: GOAL_OPERATORS.EQUALS,
                value: 'to-define'
            }
        ]
    }
};

export const getExperimentMock = (index: number): DotExperiment => {
    return { ...ExperimentMocks[index] };
};

export const getExperimentAllMocks = (): Array<DotExperiment> => {
    return [{ ...getExperimentMock(0) }, { ...getExperimentMock(1) }, { ...getExperimentMock(2) }];
};

export const getExperimentResultsMock = (index: number): DotExperimentResults => {
    return { ...ExperimentResultsMocks[index] };
};

const ExperimentMocks: Array<DotExperiment> = [
    {
        id: '111',
        identifier: '1111-1111-1111-1111',
        pageId: '456',
        status: DotExperimentStatusList.DRAFT,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 98,
        scheduling: { startDate: 1, endDate: 2 },
        trafficProportion: {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [{ id: DEFAULT_VARIANT_ID, name: DEFAULT_VARIANT_NAME, weight: 100 }]
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03'),
        goals: null
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
        trafficAllocation: 100,
        scheduling: null,
        trafficProportion: {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [
                { id: DEFAULT_VARIANT_ID, name: DEFAULT_VARIANT_NAME, weight: 50, url: 'test/1' },
                { id: '111', name: 'variant a', weight: 50, url: 'test/2' }
            ]
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03'),
        goals: null
    },
    {
        id: '333',
        identifier: '3333-3333-3333-3333',
        pageId: '456',
        status: DotExperimentStatusList.DRAFT,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100,
        scheduling: null,
        trafficProportion: {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [
                { id: DEFAULT_VARIANT_ID, name: DEFAULT_VARIANT_NAME, weight: 50 },
                { id: '222', name: 'Variant A', weight: 50 }
            ]
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03'),
        goals: { ...GoalsMock }
    }
];

const ExperimentResultsMocks: Array<DotExperimentResults> = [
    {
        goals: {
            primary: {
                goal: {
                    conditions: [
                        {
                            operator: GOAL_OPERATORS.CONTAINS,
                            parameter: GOAL_PARAMETERS.URL,
                            value: '/destinations'
                        },
                        {
                            operator: GOAL_OPERATORS.CONTAINS,
                            parameter: GOAL_PARAMETERS.REFERER,
                            value: '/blog/'
                        }
                    ],
                    name: 'Primary Goal',
                    type: GOAL_TYPES.REACH_PAGE
                },
                variants: {
                    DEFAULT: {
                        details: { '03/29/2023': { multiBySession: 2, uniqueBySession: 2 } },
                        multiBySession: 2,
                        uniqueBySession: {
                            count: 2,
                            totalPercentage: 100.0,
                            variantPercentage: 100.0
                        },
                        variantName: 'DEFAULT'
                    },
                    'dotexperiment-c92a20073d-variant-1': {
                        details: { '03/29/2023': { multiBySession: 0, uniqueBySession: 0 } },
                        multiBySession: 0,
                        uniqueBySession: { count: 0, totalPercentage: 0.0, variantPercentage: 0.0 },
                        variantName: 'dotexperiment-c92a20073d-variant-1'
                    }
                }
            }
        },
        sessions: { total: 2, variants: { DEFAULT: 2, 'dotexperiment-c92a20073d-variant-1': 0 } }
    }
];

export const VARIANT_RESULT_MOCK: DotResultSimpleVariant[] = [
    {
        id: 'DEFAULT',
        name: 'Original',
        uniqueBySession: { count: 2, totalPercentage: 100, variantPercentage: 100 }
    },
    {
        id: 'dotexperiment-c92a20073d-variant-1',
        name: 'Test Variant 1',
        uniqueBySession: { count: 0, totalPercentage: 0, variantPercentage: 0 }
    }
];

export const DotExperimentsListStoreMock = {
    addExperiment: () => of({}),
    setCloseSidebar: () => of({}),
    getPage$: of({
        pageId: '1111'
    }),
    vm$: of({
        page: {
            pageId: '',
            pageTitle: ''
        },
        experiments: [],
        filterStatus: [
            DotExperimentStatusList.DRAFT,
            DotExperimentStatusList.ENDED,
            DotExperimentStatusList.RUNNING,
            DotExperimentStatusList.SCHEDULED,
            DotExperimentStatusList.ARCHIVED
        ],
        status: ComponentStatus.INIT,
        sidebar: {
            status: ComponentStatus.IDLE,
            isOpen: false
        }
    }),
    createVm$: of({
        pageId: '',
        sidebar: {
            status: ComponentStatus.IDLE,
            isOpen: true
        },
        isSaving: false
    })
};

export const DotExperimentsConfigurationStoreMock = {
    deleteVariant: () => of([]),
    addVariant: () => of([]),
    openSidebar: () => of([]),
    closeSidebar: () => of([]),
    loadExperiment: () => of([]),
    stopExperiment: () => of([]),
    getExperimentId$: of('1111111'),
    vm$: of({
        pageId: '',
        experimentId: '',
        experiment: null,
        isLoading: true
    }),
    goalsStepVm$: of({
        goals: '',
        status: ''
    }),
    schedulingStepVm$: of({}),
    trafficStepVm$: of({
        experimentId: '',
        trafficProportion: '',
        trafficAllocation: '',
        status: ''
    }),
    variantsStepVm$: of({
        status: '',
        isExperimentADraft: true
    }),
    targetStepVm$: of({})
};

export const DotExperimentsReportsStoreMock = {
    loadExperiment: () => of([]),
    promoteVariant: () => of([])
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
