import { createServiceFactory, mockProvider, SpectatorService, SpyObject } from '@ngneat/spectator';
import { of } from 'rxjs';

import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotExperiment,
    DotExperimentStatusList,
    GroupedExperimentByStatus
} from '@dotcms/dotcms-models';
import {
    DotExperimentsListStore,
    DotExperimentsState
} from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentAllMocks, getExperimentMock } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

const routerParamsPageId = '1111-1111-111';
const ActivatedRouteMock = {
    snapshot: {
        params: {
            pageId: routerParamsPageId
        },
        parent: { parent: { parent: { data: { content: { page: { title: '' } } } } } }
    }
};

const EXPERIMENT_MOCK = getExperimentMock(0);
const EXPERIMENT_MOCK_1 = getExperimentMock(1);
const EXPERIMENT_MOCK_2 = getExperimentMock(2);
const EXPERIMENT_MOCK_ALL = getExperimentAllMocks();

describe('DotExperimentsListStore', () => {
    let spectator: SpectatorService<DotExperimentsListStore>;

    let store: DotExperimentsListStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let messageService: SpyObject<MessageService>;

    const storeService = createServiceFactory({
        service: DotExperimentsListStore,
        providers: [
            mockProvider(DotExperimentsService),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(Title),
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            },
            mockProvider(Router)
        ]
    });

    beforeEach(() => {
        spectator = storeService({});
        store = spectator.inject(DotExperimentsListStore);

        dotExperimentsService = spectator.inject(DotExperimentsService);
        messageService = spectator.inject(MessageService);

        dotExperimentsService.getById.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK));
    });

    it('should set initial data', (done) => {
        const expectedInitialState: DotExperimentsState = {
            page: {
                pageId: routerParamsPageId,
                pageTitle: ''
            },
            experiments: [],
            filterStatus: [
                DotExperimentStatusList.RUNNING,
                DotExperimentStatusList.SCHEDULED,
                DotExperimentStatusList.DRAFT,
                DotExperimentStatusList.ENDED,
                DotExperimentStatusList.ARCHIVED
            ],
            status: ComponentStatus.INIT,
            sidebar: {
                status: ComponentStatus.IDLE,
                isOpen: false
            }
        };

        store.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
    });

    it('should have getState$ from the store', () => {
        store.state$.subscribe(({ status }) => {
            expect(status).toEqual(ComponentStatus.INIT);
        });
    });

    it('should update status to the store', () => {
        store.setComponentStatus(ComponentStatus.LOADED);
        store.state$.subscribe(({ status }) => {
            expect(status).toEqual(ComponentStatus.LOADED);
        });
    });
    it('should update experiments to the store', () => {
        store.setExperiments([...EXPERIMENT_MOCK_ALL]);
        store.state$.subscribe(({ experiments }) => {
            expect(experiments).toEqual(EXPERIMENT_MOCK_ALL);
        });
    });
    it('should update status filtered to the store', () => {
        const statusSelectedMock = [DotExperimentStatusList.DRAFT, DotExperimentStatusList.ENDED];
        store.setFilterStatus(statusSelectedMock);
        store.state$.subscribe(({ filterStatus }) => {
            expect(filterStatus).toEqual(statusSelectedMock);
        });
    });

    it('should delete experiment by id of the store', () => {
        const expected: string[] = [EXPERIMENT_MOCK.id, EXPERIMENT_MOCK_2.id];

        store.setExperiments([...EXPERIMENT_MOCK_ALL]);
        store.deleteExperimentById(EXPERIMENT_MOCK_1.id);
        store.state$.subscribe(({ experiments }) => {
            expect(experiments.map((experiment) => experiment.id)).toEqual(expected);
        });
    });

    it('should change status to archived status by experiment id of the store', () => {
        store.setExperiments([{ ...getExperimentMock(1) }]);
        store.archiveExperimentById(EXPERIMENT_MOCK_1.id);
        store.state$.subscribe(({ experiments }) => {
            expect(experiments[0].status).toEqual(DotExperimentStatusList.ARCHIVED);
        });
    });

    it('should get ordered experiment by status', () => {
        const endedExperiments: DotExperiment[] = [
            { id: '111', status: DotExperimentStatusList.ENDED }
        ] as DotExperiment[];
        const archivedExperiments: DotExperiment[] = [
            { id: '10', status: DotExperimentStatusList.ARCHIVED }
        ] as DotExperiment[];

        const runningExperiments: DotExperiment[] = [
            { id: '45', status: DotExperimentStatusList.RUNNING }
        ] as DotExperiment[];

        const draftExperiments: DotExperiment[] = [
            { id: '33', status: DotExperimentStatusList.DRAFT }
        ] as DotExperiment[];

        const scheduledExperiments: DotExperiment[] = [
            { id: '1', status: DotExperimentStatusList.SCHEDULED }
        ] as DotExperiment[];

        const expected: GroupedExperimentByStatus[] = [
            { status: DotExperimentStatusList.RUNNING, experiments: [...runningExperiments] },
            { status: DotExperimentStatusList.SCHEDULED, experiments: [...scheduledExperiments] },
            { status: DotExperimentStatusList.DRAFT, experiments: [...draftExperiments] },
            { status: DotExperimentStatusList.ENDED, experiments: [...endedExperiments] },
            { status: DotExperimentStatusList.ARCHIVED, experiments: [...archivedExperiments] }
        ];

        store.setExperiments([
            ...draftExperiments,
            ...scheduledExperiments,
            ...endedExperiments,
            ...archivedExperiments,
            ...runningExperiments
        ]);

        store.getExperimentsFilteredAndGroupedByStatus$.subscribe((exp) => {
            expect(exp).toEqual(expected);
        });
    });

    describe('Effects', () => {
        beforeEach(() => {
            dotExperimentsService.getAll.and.returnValue(of([...EXPERIMENT_MOCK_ALL]));

            store.initStore();
            store.loadExperiments(routerParamsPageId);
        });
        it('should load experiments to store', (done) => {
            expect(dotExperimentsService.getAll).toHaveBeenCalledWith(routerParamsPageId);
            store.state$.subscribe(({ experiments }) => {
                expect(experiments).toEqual(EXPERIMENT_MOCK_ALL);
                done();
            });
        });

        it('should delete experiment from the store', (done) => {
            dotExperimentsService.delete.and.returnValue(of('deleted'));

            const expectedExperimentsInStore = [EXPERIMENT_MOCK_1, EXPERIMENT_MOCK_2];
            const experimentToDelete = { ...EXPERIMENT_MOCK };

            store.deleteExperiment(experimentToDelete);

            expect(dotExperimentsService.delete).toHaveBeenCalled();
            expect(dotExperimentsService.delete).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);
            store.state$.subscribe(({ experiments }) => {
                expect(experiments).toEqual(expectedExperimentsInStore);
                done();
            });
        });

        it('should change experiment status to archive in the store', (done) => {
            const expectedExperimentsInStore = [...EXPERIMENT_MOCK_ALL];

            expectedExperimentsInStore[0].status = DotExperimentStatusList.ARCHIVED;

            dotExperimentsService.archive.and.returnValue(of(expectedExperimentsInStore[0]));

            const experimentToArchive = EXPERIMENT_MOCK;

            store.archiveExperiment(experimentToArchive);

            expect(dotExperimentsService.archive).toHaveBeenCalled();
            expect(dotExperimentsService.archive).toHaveBeenCalledWith(experimentToArchive.id);
            store.state$.subscribe(({ experiments }) => {
                expect(experiments).toEqual(expectedExperimentsInStore);
                done();
            });
        });

        it('should update sidebar isSaving to the store', () => {
            store.setSidebarStatus({ status: ComponentStatus.SAVING, isOpen: true });
            store.isSidebarSaving$.subscribe((isSaving) => {
                expect(isSaving).toBe(true);
            });
        });

        it('should update isOpen and isSaving to the store', () => {
            store.setSidebarStatus({ status: ComponentStatus.IDLE, isOpen: false });
            store.createVm$.subscribe(({ sidebar, isSaving }) => {
                expect(sidebar.isOpen).toBe(false);
                expect(sidebar.status).toBe(ComponentStatus.IDLE);
                expect(isSaving).toBe(false);
            });
        });

        it('should save the experiment', () => {
            const isSavingSatuses = [];
            const experiment: Pick<DotExperiment, 'pageId' | 'name' | 'description'> = {
                pageId: '1111-1111-1111-1111',
                name: 'Experiment name',
                description: 'description or goal'
            };

            dotExperimentsService.add.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK));

            store.createVm$.subscribe(({ isSaving }) => {
                isSavingSatuses.push(isSaving);
            });

            store.addExperiments(experiment);

            expect(dotExperimentsService.add).toHaveBeenCalled();
            expect(messageService.add).toHaveBeenCalled();
        });
    });
});
