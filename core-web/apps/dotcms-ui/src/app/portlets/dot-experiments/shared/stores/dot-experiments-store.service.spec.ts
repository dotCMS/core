import { TestBed } from '@angular/core/testing';

import { DotExperimentsStore } from './dot-experiments-store.service';

import { LoadingState } from '@portlets/shared/models/shared-models';
import {
    DotExperiment,
    DotExperimentStatusList,
    GroupedExperimentByStatus,
    TrafficProportionTypes
} from '../../shared/models/dot-experiments.model';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { Observable, of } from 'rxjs';
import { HttpClientTestingModule } from '@angular/common/http/testing';

class MockDotExperimentsService {
    get(): Observable<DotExperiment[]> {
        return of([]);
    }

    delete(): Observable<string | DotExperiment[]> {
        return of([]);
    }

    archive(): Observable<string | DotExperiment[]> {
        return of([]);
    }
}

const initialStateStoreMock = {
    identifier: '12345-6789-9876-5432',
    title: 'Name of the experiment'
};

const experimentsMock: DotExperiment[] = [
    {
        id: '111',
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
describe('DotExperimentsStore', () => {
    let experimentsStore: DotExperimentsStore;
    let dotExperimentsService: DotExperimentsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotExperimentsStore,
                { provide: DotExperimentsService, useClass: MockDotExperimentsService }
            ]
        });

        experimentsStore = TestBed.inject(DotExperimentsStore);
        dotExperimentsService = TestBed.inject(DotExperimentsService);
    });

    it('should set initial data', (done) => {
        const expectedInitialState = {
            pageId: '',
            pageTitle: '',
            experiments: [],
            experimentsFilterStatus: [
                DotExperimentStatusList.DRAFT,
                DotExperimentStatusList.ENDED,
                DotExperimentStatusList.RUNNING,
                DotExperimentStatusList.SCHEDULED,
                DotExperimentStatusList.ARCHIVED
            ],
            status: LoadingState.INIT
        };

        experimentsStore.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
    });

    it('should have getState$ from the store', () => {
        experimentsStore.getStatus$.subscribe((data) => {
            expect(data).toEqual(LoadingState.INIT);
        });
    });

    it('should update status to the store', () => {
        experimentsStore.setComponentStatus(LoadingState.LOADED);
        experimentsStore.getStatus$.subscribe((status) => {
            expect(status).toEqual(LoadingState.LOADED);
        });
    });
    it('should update experiments to the store', () => {
        experimentsStore.setExperiments(experimentsMock);
        experimentsStore.getExperiments$.subscribe((experiments) => {
            expect(experiments).toEqual(experimentsMock);
        });
    });
    it('should update status filtered to the store', () => {
        const statusSelectedMock = [DotExperimentStatusList.DRAFT, DotExperimentStatusList.ENDED];
        experimentsStore.setExperimentsFilterStatus(statusSelectedMock);
        experimentsStore.getExperimentsFilterStatusList$.subscribe((experimentsFilterStatus) => {
            expect(experimentsFilterStatus).toEqual(statusSelectedMock);
        });
    });

    it('should delete experiment by id of the store', () => {
        const ID_TO_DELETE = '222';
        const expected: DotExperiment[] = [experimentsMock[0]];

        experimentsStore.setExperiments(experimentsMock);
        experimentsStore.deleteExperimentById(ID_TO_DELETE);
        experimentsStore.getExperiments$.subscribe((experiments) => {
            expect(experiments).toEqual(expected);
        });
    });

    it('should change status to archived status by experiment id of the store', () => {
        const ID_TO_ARCHIVE = '222';
        const expected: DotExperiment[] = [...experimentsMock];
        expected[1].status = DotExperimentStatusList.ARCHIVED;

        experimentsStore.setExperiments(experimentsMock);
        experimentsStore.archiveExperimentById(ID_TO_ARCHIVE);
        experimentsStore.getExperiments$.subscribe((exp) => {
            expect(exp).toEqual(expected);
        });
    });

    it('should get ordered experiment by status', () => {
        const endedExperiments: DotExperiment[] = [
            {
                id: '111',
                pageId: '456',
                status: DotExperimentStatusList.ENDED,
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
        const archivedExperiments: DotExperiment[] = [
            {
                id: '111',
                pageId: '456',
                status: DotExperimentStatusList.ARCHIVED,
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

        const expected: GroupedExperimentByStatus = {
            [DotExperimentStatusList.DRAFT]: [...experimentsMock],
            [DotExperimentStatusList.ENDED]: [...endedExperiments],
            [DotExperimentStatusList.ARCHIVED]: [...archivedExperiments]
        };

        experimentsStore.setExperiments([
            ...experimentsMock,
            ...endedExperiments,
            ...archivedExperiments
        ]);

        experimentsStore.getExperimentsFilteredAndGroupedByStatus$.subscribe((exp) => {
            expect(exp).toEqual(expected);
        });
    });

    describe('Effects', () => {
        it('should load experiments to store', () => {
            const pageId = '12345-6789-9876-5432';
            const dotExperimentsServiceSpy = spyOn(dotExperimentsService, 'get').and.returnValue(
                of(experimentsMock)
            );

            experimentsStore.initStore(initialStateStoreMock);

            experimentsStore.loadExperiments();
            expect(dotExperimentsServiceSpy).toHaveBeenCalledWith(pageId);
            experimentsStore.getExperiments$.subscribe((exp) => {
                expect(exp).toEqual(experimentsMock);
            });
        });

        it('should delete experiment from the store', () => {
            const expectedExperimentsInStore = [experimentsMock[1]];
            const experimentIdToDelete = experimentsMock[0].id;
            const dotExperimentsServiceSpy = spyOn(dotExperimentsService, 'delete').and.returnValue(
                of('Experiment deleted')
            );

            spyOn(dotExperimentsService, 'get').and.returnValue(of(experimentsMock));

            experimentsStore.initStore(initialStateStoreMock);

            experimentsStore.loadExperiments();

            experimentsStore.deleteExperiment(experimentIdToDelete);

            expect(dotExperimentsServiceSpy).toHaveBeenCalledWith(experimentIdToDelete);
            experimentsStore.getExperiments$.subscribe((experiment) => {
                expect(experiment).toEqual(expectedExperimentsInStore);
            });
        });

        it('should change experiment status to archive in the store', () => {
            const expectedExperimentsInStore = [...experimentsMock];
            expectedExperimentsInStore[0].status = DotExperimentStatusList.ARCHIVED;

            const experimentIdToArchive = experimentsMock[0].id;

            const dotExperimentsServiceSpy = spyOn(
                dotExperimentsService,
                'archive'
            ).and.returnValue(of([expectedExperimentsInStore[0]]));
            spyOn(dotExperimentsService, 'get').and.returnValue(of(experimentsMock));

            experimentsStore.initStore(initialStateStoreMock);
            experimentsStore.loadExperiments();
            experimentsStore.archiveExperiment(experimentIdToArchive);

            expect(dotExperimentsServiceSpy).toHaveBeenCalledWith(experimentIdToArchive);
            experimentsStore.getExperiments$.subscribe((experiment) => {
                expect(experiment).toEqual(expectedExperimentsInStore);
            });
        });
    });
});
