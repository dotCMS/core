import { ChartData } from 'chart.js';
import { of } from 'rxjs';

import {
    BayesianStatusResponse,
    ComponentStatus,
    DEFAULT_VARIANT_ID,
    DEFAULT_VARIANT_NAME,
    DotExperiment,
    DotExperimentResults,
    DotExperimentStatus,
    ExperimentLineChartDatasetDefaultProperties,
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goals,
    PROP_NOT_FOUND,
    SummaryLegend,
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

export const suggestedWinnerMock: SummaryLegend = { icon: 'icon', legend: 'legend' };

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
        status: DotExperimentStatus.DRAFT,
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
        status: DotExperimentStatus.DRAFT,
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
        status: DotExperimentStatus.DRAFT,
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
    },
    {
        id: '444',
        identifier: '3333-3333-3333-3333',
        pageId: '456',
        status: DotExperimentStatus.DRAFT,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100,
        scheduling: { startDate: null, endDate: null },
        trafficProportion: {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [
                { id: DEFAULT_VARIANT_ID, name: DEFAULT_VARIANT_NAME, weight: 50, promoted: false },
                { id: '222', name: 'Variant A', weight: 50, promoted: false }
            ]
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03'),
        goals: { ...GoalsMock }
    },
    {
        id: '555',
        identifier: '555-5555-5555-5555',
        pageId: '456',
        status: DotExperimentStatus.RUNNING,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100,
        scheduling: { startDate: null, endDate: null },
        trafficProportion: {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [
                { id: DEFAULT_VARIANT_ID, name: DEFAULT_VARIANT_NAME, weight: 50, promoted: false },
                { id: '111', name: 'Variant A', weight: 50, promoted: true }
            ]
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03'),
        goals: { ...GoalsMock }
    }
];

export const ExperimentResultsMocks: Array<DotExperimentResults> = [
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
                    [DEFAULT_VARIANT_ID]: {
                        details: {
                            '04/01/2023': { multiBySession: 1, uniqueBySession: 1 },
                            '04/02/2023': { multiBySession: 2, uniqueBySession: 2 },
                            '04/03/2023': { multiBySession: 3, uniqueBySession: 3 },
                            '04/04/2023': { multiBySession: 4, uniqueBySession: 4 },
                            '04/05/2023': { multiBySession: 5, uniqueBySession: 5 },
                            '04/06/2023': { multiBySession: 6, uniqueBySession: 6 },
                            '04/07/2023': { multiBySession: 7, uniqueBySession: 7 },
                            '04/08/2023': { multiBySession: 8, uniqueBySession: 8 },
                            '04/09/2023': { multiBySession: 9, uniqueBySession: 9 },
                            '04/10/2023': { multiBySession: 10, uniqueBySession: 10 },
                            '04/11/2023': { multiBySession: 11, uniqueBySession: 11 },
                            '04/12/2023': { multiBySession: 12, uniqueBySession: 12 },
                            '04/13/2023': { multiBySession: 13, uniqueBySession: 13 },
                            '04/14/2023': { multiBySession: 14, uniqueBySession: 14 },
                            '04/15/2023': { multiBySession: 15, uniqueBySession: 15 }
                        },
                        multiBySession: 2,
                        uniqueBySession: {
                            count: 2,
                            totalPercentage: 100.0,
                            variantPercentage: 100.0
                        },
                        variantName: 'DEFAULT',
                        variantDescription: 'DEFAULT Name',
                        totalPageViews: 10
                    },
                    '111': {
                        details: {
                            '04/01/2023': { multiBySession: 15, uniqueBySession: 15 },
                            '04/02/2023': { multiBySession: 14, uniqueBySession: 14 },
                            '04/03/2023': { multiBySession: 13, uniqueBySession: 13 },
                            '04/04/2023': { multiBySession: 12, uniqueBySession: 12 },
                            '04/05/2023': { multiBySession: 11, uniqueBySession: 11 },
                            '04/06/2023': { multiBySession: 10, uniqueBySession: 10 },
                            '04/07/2023': { multiBySession: 9, uniqueBySession: 9 },
                            '04/08/2023': { multiBySession: 8, uniqueBySession: 8 },
                            '04/09/2023': { multiBySession: 7, uniqueBySession: 7 },
                            '04/10/2023': { multiBySession: 6, uniqueBySession: 6 },
                            '04/11/2023': { multiBySession: 5, uniqueBySession: 5 },
                            '04/12/2023': { multiBySession: 4, uniqueBySession: 4 },
                            '04/13/2023': { multiBySession: 3, uniqueBySession: 3 },
                            '04/14/2023': { multiBySession: 2, uniqueBySession: 2 },
                            '04/15/2023': { multiBySession: 1, uniqueBySession: 1 }
                        },
                        multiBySession: 0,
                        uniqueBySession: { count: 0, totalPercentage: 0.0, variantPercentage: 0.0 },
                        variantName: '111',
                        variantDescription: 'Variant 111 Name',
                        totalPageViews: 10
                    }
                }
            }
        },
        sessions: { total: 2, variants: { DEFAULT: 2, '111': 0 } },
        bayesianResult: {
            value: 0.0,
            suggestedWinner: BayesianStatusResponse.NONE,
            results: [
                {
                    conversionRate: 1.0,
                    credibilityInterval: {
                        lower: 0.6637328830717519,
                        upper: 0.9971908632621475
                    },
                    probability: 0.9230769230769231,
                    risk: 0.28667531413857206,
                    variant: DEFAULT_VARIANT_ID
                },
                {
                    conversionRate: 0.6666666666666666,
                    credibilityInterval: {
                        lower: 0.19412044967368097,
                        upper: 0.932414013511487
                    },
                    probability: 0.07692307692307693,
                    risk: -0.3043212480282402,
                    variant: '111'
                }
            ]
        }
    },
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
                    [DEFAULT_VARIANT_ID]: {
                        details: {
                            '04/01/2023': { multiBySession: 1, uniqueBySession: 0 },
                            '04/02/2023': { multiBySession: 2, uniqueBySession: 0 },
                            '04/03/2023': { multiBySession: 3, uniqueBySession: 0 },
                            '04/04/2023': { multiBySession: 4, uniqueBySession: 0 },
                            '04/05/2023': { multiBySession: 5, uniqueBySession: 0 },
                            '04/06/2023': { multiBySession: 6, uniqueBySession: 0 },
                            '04/07/2023': { multiBySession: 7, uniqueBySession: 0 },
                            '04/08/2023': { multiBySession: 8, uniqueBySession: 0 },
                            '04/09/2023': { multiBySession: 9, uniqueBySession: 0 },
                            '04/10/2023': { multiBySession: 10, uniqueBySession: 0 },
                            '04/11/2023': { multiBySession: 11, uniqueBySession: 0 },
                            '04/12/2023': { multiBySession: 12, uniqueBySession: 0 },
                            '04/13/2023': { multiBySession: 13, uniqueBySession: 0 },
                            '04/14/2023': { multiBySession: 14, uniqueBySession: 0 },
                            '04/15/2023': { multiBySession: 15, uniqueBySession: 0 }
                        },
                        multiBySession: 2,
                        uniqueBySession: {
                            count: 2,
                            totalPercentage: 100.0,
                            variantPercentage: 100.0
                        },
                        variantName: 'DEFAULT',
                        variantDescription: 'DEFAULT Name',
                        totalPageViews: 10
                    },
                    '111': {
                        details: {
                            '04/01/2023': { multiBySession: 15, uniqueBySession: 0 },
                            '04/02/2023': { multiBySession: 14, uniqueBySession: 0 },
                            '04/03/2023': { multiBySession: 13, uniqueBySession: 0 },
                            '04/04/2023': { multiBySession: 12, uniqueBySession: 0 },
                            '04/05/2023': { multiBySession: 11, uniqueBySession: 0 },
                            '04/06/2023': { multiBySession: 10, uniqueBySession: 0 },
                            '04/07/2023': { multiBySession: 9, uniqueBySession: 0 },
                            '04/08/2023': { multiBySession: 8, uniqueBySession: 0 },
                            '04/09/2023': { multiBySession: 7, uniqueBySession: 0 },
                            '04/10/2023': { multiBySession: 6, uniqueBySession: 0 },
                            '04/11/2023': { multiBySession: 5, uniqueBySession: 0 },
                            '04/12/2023': { multiBySession: 4, uniqueBySession: 0 },
                            '04/13/2023': { multiBySession: 3, uniqueBySession: 0 },
                            '04/14/2023': { multiBySession: 2, uniqueBySession: 0 },
                            '04/15/2023': { multiBySession: 1, uniqueBySession: 0 }
                        },
                        multiBySession: 0,
                        uniqueBySession: { count: 0, totalPercentage: 0.0, variantPercentage: 0.0 },
                        variantName: '111',
                        variantDescription: 'Variant 111 Name',
                        totalPageViews: 10
                    }
                }
            }
        },
        sessions: { total: 2, variants: { DEFAULT: 2, '111': 0 } },
        bayesianResult: {
            value: 0.0,
            suggestedWinner: '111',
            results: [
                {
                    conversionRate: 0.6666666666666666,
                    credibilityInterval: {
                        lower: 0.19412044967368097,
                        upper: 0.932414013511487
                    },
                    probability: 0.07692307692307693,
                    risk: -0.3043212480282402,
                    variant: DEFAULT_VARIANT_ID
                },
                {
                    conversionRate: 1.0,
                    credibilityInterval: {
                        lower: 0.6637328830717519,
                        upper: 0.9971908632621475
                    },
                    probability: 0.9230769230769231,
                    risk: 0.28667531413857206,
                    variant: '111'
                }
            ]
        }
    }
];

export const CHARTJS_DATA_MOCK_WITH_DATA: ChartData<'line'> = {
    labels: [
        ['Thursday', '04/01/2023'],
        ['Friday', '04/02/2023'],
        ['Saturday', '04/03/2023'],
        ['Sunday', '04/04/2023']
    ],
    datasets: [
        {
            label: DEFAULT_VARIANT_NAME,
            data: [1, 2, 3, 4],
            ...ExperimentLineChartDatasetDefaultProperties
        }
    ]
};

export const CHARTJS_DATA_MOCK_EMPTY: ChartData<'line'> = {
    labels: [
        ['Thursday', '04/01/2023'],
        ['Friday', '04/02/2023'],
        ['Saturday', '04/03/2023'],
        ['Sunday', '04/04/2023']
    ],
    datasets: [
        {
            label: DEFAULT_VARIANT_NAME,
            data: [],
            ...ExperimentLineChartDatasetDefaultProperties
        }
    ]
};

export const DotExperimentsListStoreMock = {
    addExperiment: () => of({}),
    setCloseSidebar: () => of({}),
    addExperiments: () => of({}),
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
            DotExperimentStatus.DRAFT,
            DotExperimentStatus.ENDED,
            DotExperimentStatus.RUNNING,
            DotExperimentStatus.SCHEDULED,
            DotExperimentStatus.ARCHIVED
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

export const DotExperimentsStoreMock = {
    getPageId$: of('1111111'),
    getPageTitle$: of('title of page')
};

export const DotExperimentsReportsStoreMock = {
    loadExperimentAndResults: () => of([]),
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

export const PARENT_RESOLVERS_ACTIVE_ROUTE_DATA = {
    snapshot: {
        data: {
            isEnterprise: true,
            pushPublishEnvironments: [{ id: '01', name: 'test' }]
        }
    }
};

export const ACTIVE_ROUTE_MOCK_CONFIG = {
    snapshot: {
        data: {
            config: {
                EXPERIMENTS_MIN_DURATION: '5',
                EXPERIMENTS_MAX_DURATION: PROP_NOT_FOUND
            }
        }
    },
    parent: { ...PARENT_RESOLVERS_ACTIVE_ROUTE_DATA }
};

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

    get parent() {
        return { ...PARENT_RESOLVERS_ACTIVE_ROUTE_DATA };
    }
}
