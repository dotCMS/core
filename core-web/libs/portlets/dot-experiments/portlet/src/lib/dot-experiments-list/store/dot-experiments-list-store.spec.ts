import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import {
    AllowedActionsByExperimentStatus,
    ComponentStatus,
    DotExperiment,
    DotExperimentStatus,
    DotExperimentsWithActions,
    GroupedExperimentByStatus
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import {
    DotExperimentsStoreMock,
    getExperimentAllMocks,
    getExperimentMock,
    PARENT_RESOLVERS_ACTIVE_ROUTE_DATA
} from '@dotcms/utils-testing';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsListStore } from './dot-experiments-list-store';

import { DotExperimentsStore } from '../../dot-experiments-shell/store/dot-experiments.store';

const routerParamsPageId = '1111-1111-111';
const ActivatedRouteMock = {
    snapshot: {
        params: {
            pageId: routerParamsPageId
        },
        parent: { parent: { parent: { data: { content: { page: { title: '' } } } } } }
    },
    parent: { ...PARENT_RESOLVERS_ACTIVE_ROUTE_DATA }
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
            mockProvider(ConfirmationService),
            mockProvider(Router),
            mockProvider(DotExperimentsStore, DotExperimentsStoreMock)
        ]
    });

    beforeEach(() => {
        // No show warnings about store livecycle
        jest.spyOn(console, 'warn').mockImplementation(jest.fn());

        spectator = storeService();
        dotExperimentsService = spectator.inject(DotExperimentsService);

        dotExperimentsService.getAll.mockReturnValue(of(EXPERIMENT_MOCK_ALL));
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));

        store = spectator.inject(DotExperimentsListStore);
        messageService = spectator.inject(MessageService);
        store.ngrxOnStateInit();
    });

    it('should have getState$ from the store', () => {
        store.state$.subscribe((state) => {
            expect(state.status).toEqual(ComponentStatus.LOADED);
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
        const statusSelectedMock = [DotExperimentStatus.DRAFT, DotExperimentStatus.ENDED];
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
            expect(experiments[0].status).toEqual(DotExperimentStatus.ARCHIVED);
        });
    });

    it('should get ordered experiment by status', () => {
        const endedExperiments: DotExperimentsWithActions[] = [
            { id: '111', status: DotExperimentStatus.ENDED }
        ] as DotExperimentsWithActions[];
        const archivedExperiments: DotExperimentsWithActions[] = [
            { id: '10', status: DotExperimentStatus.ARCHIVED }
        ] as DotExperimentsWithActions[];

        const runningExperiments: DotExperimentsWithActions[] = [
            { id: '45', status: DotExperimentStatus.RUNNING }
        ] as DotExperimentsWithActions[];

        const draftExperiments: DotExperimentsWithActions[] = [
            { id: '33', status: DotExperimentStatus.DRAFT }
        ] as DotExperimentsWithActions[];

        const scheduledExperiments: DotExperimentsWithActions[] = [
            { id: '1', status: DotExperimentStatus.SCHEDULED }
        ] as DotExperimentsWithActions[];

        const expected: GroupedExperimentByStatus[] = [
            { status: DotExperimentStatus.RUNNING, experiments: [...runningExperiments] },
            { status: DotExperimentStatus.SCHEDULED, experiments: [...scheduledExperiments] },
            { status: DotExperimentStatus.DRAFT, experiments: [...draftExperiments] },
            { status: DotExperimentStatus.ENDED, experiments: [...endedExperiments] },
            { status: DotExperimentStatus.ARCHIVED, experiments: [...archivedExperiments] }
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

    it('should have the MenuItems in all status', (done) => {
        const EXPERIMENTS_MOCK: DotExperiment[] = [
            { ...EXPERIMENT_MOCK, status: DotExperimentStatus.DRAFT },
            { ...EXPERIMENT_MOCK, status: DotExperimentStatus.RUNNING },
            { ...EXPERIMENT_MOCK, status: DotExperimentStatus.ARCHIVED },
            { ...EXPERIMENT_MOCK, status: DotExperimentStatus.ENDED },
            { ...EXPERIMENT_MOCK, status: DotExperimentStatus.SCHEDULED }
        ];
        const MENU_ITEMS_QTY = 4;
        const MENU_ITEMS_DELETE_INDEX = 0;
        const MENU_ITEMS_CONFIGURATION_INDEX = 1;
        const MENU_ITEMS_ARCHIVE_INDEX = 2;
        const MENU_ITEMS_ADD_T0_BUNDLE_INDEX = 3;

        store.setExperiments([...EXPERIMENTS_MOCK]);

        store.getExperimentsFilteredAndGroupedByStatus$.subscribe(
            (experimentsFilteredAndGroupedByStatus: GroupedExperimentByStatus[]) => {
                experimentsFilteredAndGroupedByStatus.forEach((groupedExperiment) => {
                    const { status, experiments } = groupedExperiment;

                    expect(experiments[0].actionsItemsMenu.length).toEqual(MENU_ITEMS_QTY);

                    expect(
                        experiments[0].actionsItemsMenu[MENU_ITEMS_DELETE_INDEX].visible
                    ).toEqual(AllowedActionsByExperimentStatus['delete'].includes(status));
                    expect(
                        experiments[0].actionsItemsMenu[MENU_ITEMS_CONFIGURATION_INDEX].visible
                    ).toEqual(AllowedActionsByExperimentStatus['configuration'].includes(status));
                    expect(
                        experiments[0].actionsItemsMenu[MENU_ITEMS_ARCHIVE_INDEX].visible
                    ).toEqual(AllowedActionsByExperimentStatus['archive'].includes(status));
                    expect(
                        experiments[0].actionsItemsMenu[MENU_ITEMS_ADD_T0_BUNDLE_INDEX].visible
                    ).toEqual(AllowedActionsByExperimentStatus['addToBundle'].includes(status));
                });

                done();
            }
        );
    });

    describe('Effects', () => {
        beforeEach(() => {
            dotExperimentsService.getAll.mockReturnValue(of([...EXPERIMENT_MOCK_ALL]));

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
            dotExperimentsService.delete.mockReturnValue(of('deleted'));

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

            expectedExperimentsInStore[0].status = DotExperimentStatus.ARCHIVED;

            dotExperimentsService.archive.mockReturnValue(of(expectedExperimentsInStore[0]));

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

            dotExperimentsService.add.mockReturnValue(of(EXPERIMENT_MOCK));
            store.createVm$.subscribe(({ isSaving }) => {
                isSavingSatuses.push(isSaving);
            });

            store.addExperiments(experiment);

            expect(dotExperimentsService.add).toHaveBeenCalled();
            expect(messageService.add).toHaveBeenCalled();
        });
    });
});
