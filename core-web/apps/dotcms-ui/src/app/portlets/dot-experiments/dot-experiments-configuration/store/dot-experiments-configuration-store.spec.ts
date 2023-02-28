import { createServiceFactory, mockProvider, SpectatorService, SpyObject } from '@ngneat/spectator';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DEFAULT_VARIANT_NAME,
    DotExperiment,
    DotExperimentStatusList,
    ExperimentSteps,
    Goals,
    GoalsLevels,
    RangeOfDateAndTime,
    TrafficProportion,
    TrafficProportionTypes,
    Variant
} from '@dotcms/dotcms-models';
import {
    DotExperimentsConfigurationState,
    DotExperimentsConfigurationStore
} from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { ExperimentMocks, GoalsMock } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

const EXPERIMENT_ID = ExperimentMocks[0].id;
const PAGE_ID = ExperimentMocks[0].pageId;
const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: EXPERIMENT_ID,
            pageId: PAGE_ID
        }
    }
};

describe('DotExperimentsConfigurationStore', () => {
    let spectator: SpectatorService<DotExperimentsConfigurationStore>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

    const createStoreService = createServiceFactory({
        service: DotExperimentsConfigurationStore,
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
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createStoreService({});

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        dotExperimentsService.getById.and.callThrough().and.returnValue(of(ExperimentMocks[0]));
    });

    it('should set initial data', (done) => {
        spectator.service.loadExperiment(EXPERIMENT_ID);

        const expectedInitialState: DotExperimentsConfigurationState = {
            experiment: ExperimentMocks[0],
            status: ComponentStatus.IDLE,
            stepStatusSidebar: {
                status: ComponentStatus.IDLE,
                isOpen: false,
                experimentStep: null
            }
        };

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_ID);
        store.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
    });

    it('should have isLoading$ from the store', (done) => {
        spectator.service.loadExperiment(ExperimentMocks[0].id);
        store.isLoading$.subscribe((data) => {
            expect(data).toEqual(false);
            done();
        });
    });

    it('should update component status to the store', (done) => {
        store.setComponentStatus(ComponentStatus.LOADED);
        store.isLoading$.subscribe((status) => {
            expect(status).toEqual(false);
            done();
        });
    });

    it('should update sidebar status(SAVING/IDLE/DONE) to the store', (done) => {
        store.setSidebarStatus({ status: ComponentStatus.SAVING });
        store.state$.subscribe(({ stepStatusSidebar }) => {
            expect(stepStatusSidebar.status).toEqual(ComponentStatus.SAVING);
            done();
        });
    });

    it('should update open sidebar and set the experiment step (card)', (done) => {
        store.openSidebar(ExperimentSteps.VARIANTS);
        store.state$.subscribe(({ stepStatusSidebar }) => {
            expect(stepStatusSidebar.experimentStep).toEqual(ExperimentSteps.VARIANTS);
            expect(stepStatusSidebar.isOpen).toEqual(true);
            done();
        });
    });

    it('should update close sidebar and initialize stepStatusSidebar', (done) => {
        store.closeSidebar();
        store.state$.subscribe(({ stepStatusSidebar }) => {
            expect(stepStatusSidebar.status).toEqual(ComponentStatus.IDLE);
            expect(stepStatusSidebar.isOpen).toEqual(false);
            expect(stepStatusSidebar.experimentStep).toEqual(null);
            done();
        });
    });

    describe('Effects', () => {
        it('should load experiment to store', (done) => {
            dotExperimentsService.getById.and.callThrough().and.returnValue(of(ExperimentMocks[1]));

            store.loadExperiment(EXPERIMENT_ID);
            expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_ID);
            store.state$.subscribe(({ experiment }) => {
                expect(experiment).toEqual(ExperimentMocks[1]);
                done();
            });
        });

        it('should add a variant to the store', (done) => {
            dotExperimentsService.getById.and.callThrough().and.returnValue(of(ExperimentMocks[1]));
            const newVariant: { experimentId: string; data: Pick<DotExperiment, 'name'> } = {
                data: { name: '333' },
                experimentId: EXPERIMENT_ID
            };

            const expectedExperiment = {
                ...ExperimentMocks[1],
                trafficProportion: {
                    ...ExperimentMocks[1].trafficProportion,
                    variants: [
                        ...ExperimentMocks[1].trafficProportion.variants,
                        {
                            ...newVariant.data,
                            id: '222',
                            weight: 100
                        }
                    ]
                }
            };

            dotExperimentsService.addVariant.and
                .callThrough()
                .and.returnValue(of(expectedExperiment));

            store.loadExperiment(EXPERIMENT_ID);
            store.addVariant(newVariant);

            store.state$.subscribe(({ experiment, stepStatusSidebar }) => {
                expect(experiment).toEqual(expectedExperiment);
                expect(stepStatusSidebar.isOpen).toEqual(false);
                done();
            });
        });

        it('should edit a variant name of an experiment', (done) => {
            const variants: Variant[] = [
                { id: '111', name: DEFAULT_VARIANT_NAME, weight: 50, url: 'url' },
                { id: '222', name: 'name to edit', weight: 50, url: 'url' }
            ];
            const variantEdited: Variant[] = [
                { id: '111', name: DEFAULT_VARIANT_NAME, weight: 50, url: 'url' },
                { id: '222', name: 'new name', weight: 50, url: 'url' }
            ];

            dotExperimentsService.getById.and.callThrough().and.returnValue(
                of({
                    ...ExperimentMocks[0],
                    trafficProportion: {
                        ...ExperimentMocks[0].trafficProportion,
                        variants
                    }
                })
            );
            dotExperimentsService.editVariant.and.callThrough().and.returnValue(
                of({
                    ...ExperimentMocks[0],
                    trafficProportion: {
                        ...ExperimentMocks[0].trafficProportion,
                        variants: variantEdited
                    }
                })
            );

            store.loadExperiment(EXPERIMENT_ID);

            store.editVariant({
                experimentId: EXPERIMENT_ID,
                data: { id: variants[1].id, name: 'new name' }
            });

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.trafficProportion.variants[1].name).toEqual(
                    variantEdited[1].name
                );
                done();
            });
        });

        it('should delete a variant from an experiment', (done) => {
            const variants: Variant[] = [
                { id: 'DEFAULT', name: 'DEFAULT', weight: 50 },
                {
                    id: '111',
                    name: '1111',
                    weight: 50
                }
            ];

            const experimentWithTwoVariants: DotExperiment = {
                ...ExperimentMocks[1],
                trafficProportion: {
                    ...ExperimentMocks[1].trafficProportion,
                    variants: [...variants]
                }
            };
            const expectedResponseRemoveVariant: DotExperiment = {
                ...ExperimentMocks[1],
                trafficProportion: {
                    ...ExperimentMocks[1].trafficProportion,
                    variants: [{ id: 'DEFAULT', name: 'DEFAULT', weight: 50 }]
                }
            };

            dotExperimentsService.getById.and
                .callThrough()
                .and.returnValue(of(experimentWithTwoVariants));
            dotExperimentsService.removeVariant.and
                .callThrough()
                .and.returnValue(of(expectedResponseRemoveVariant));

            store.loadExperiment(EXPERIMENT_ID);
            expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_ID);

            store.deleteVariant({ experimentId: EXPERIMENT_ID, variant: variants[1] });

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.trafficProportion.variants).toEqual(
                    expectedResponseRemoveVariant.trafficProportion.variants
                );
                done();
            });
        });

        it('should set a Goal to the experiment', (done) => {
            const expectedGoals: Goals = { ...GoalsMock };

            dotExperimentsService.getById.and
                .callThrough()
                .and.returnValue(of({ ...ExperimentMocks[0] }));

            dotExperimentsService.setGoal.and.callThrough().and.returnValue(
                of({
                    ...ExperimentMocks[0],
                    goals: expectedGoals
                })
            );

            store.loadExperiment(EXPERIMENT_ID);

            store.setSelectedGoal({ experimentId: EXPERIMENT_ID, goals: expectedGoals });

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.goals).toEqual(expectedGoals);
                done();
            });
        });

        it('should delete a Goal from an experiment', (done) => {
            const goalLevelToDelete: GoalsLevels = 'primary';
            const experimentWithGoals: DotExperiment = {
                ...ExperimentMocks[0],
                goals: { ...GoalsMock }
            };

            dotExperimentsService.getById.and
                .callThrough()
                .and.returnValue(of({ ...experimentWithGoals }));

            dotExperimentsService.deleteGoal.and.callThrough().and.returnValue(
                of({
                    ...ExperimentMocks[0]
                })
            );

            store.loadExperiment(EXPERIMENT_ID);

            store.deleteGoal({ experimentId: EXPERIMENT_ID, goalLevel: goalLevelToDelete });

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.goals).toEqual(null);
                done();
            });
        });

        it('should set a Scheduling to the experiment', (done) => {
            const expectedScheduling: RangeOfDateAndTime = {
                startDate: 1,
                endDate: 2
            };

            dotExperimentsService.setScheduling.and.callThrough().and.returnValue(
                of({
                    ...ExperimentMocks[0],
                    scheduling: expectedScheduling
                })
            );

            store.setSelectedScheduling({
                scheduling: expectedScheduling,
                experimentId: EXPERIMENT_ID
            });

            store.state$.pipe(take(1)).subscribe(({ experiment }) => {
                expect(experiment.scheduling).toEqual(expectedScheduling);
                expect(dotExperimentsService.setScheduling).toHaveBeenCalledOnceWith(
                    EXPERIMENT_ID,
                    expectedScheduling
                );
                done();
            });
        });

        it('should throw an error if update scheduling fails', () => {
            dotExperimentsService.setScheduling.and.returnValue(throwError('error'));

            store.setSelectedScheduling({
                scheduling: null,
                experimentId: EXPERIMENT_ID
            });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledOnceWith(
                'error' as unknown as HttpErrorResponse
            );
        });

        it('should set Traffic Allocation to the experiment', (done) => {
            const expectedTrafficAllocation = 10;

            dotExperimentsService.setTrafficAllocation.and.callThrough().and.returnValue(
                of({
                    ...ExperimentMocks[0],
                    trafficAllocation: expectedTrafficAllocation
                })
            );

            store.setSelectedAllocation({
                trafficAllocation: expectedTrafficAllocation,
                experimentId: EXPERIMENT_ID
            });

            store.state$.pipe(take(1)).subscribe(({ experiment }) => {
                expect(experiment.trafficAllocation).toEqual(expectedTrafficAllocation);
                expect(dotExperimentsService.setTrafficAllocation).toHaveBeenCalledOnceWith(
                    EXPERIMENT_ID,
                    expectedTrafficAllocation
                );
                done();
            });
        });

        it('should throw an error if update Traffic Allocation fails', () => {
            dotExperimentsService.setTrafficAllocation.and.returnValue(throwError('error'));

            store.setSelectedAllocation({
                trafficAllocation: 120,
                experimentId: EXPERIMENT_ID
            });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledOnceWith(
                'error' as unknown as HttpErrorResponse
            );
        });

        it('should set Traffic Proportion to the experiment', (done) => {
            const expectedTrafficProportion: TrafficProportion = {
                type: TrafficProportionTypes.SPLIT_EVENLY,
                variants: [
                    { id: '111', name: 'DEFAULT', weight: 50 },
                    { id: '111', name: 'A', weight: 50 }
                ]
            };

            dotExperimentsService.setTrafficProportion.and.callThrough().and.returnValue(
                of({
                    ...ExperimentMocks[0],
                    trafficProportion: expectedTrafficProportion
                })
            );

            store.setSelectedTrafficProportion({
                trafficProportion: expectedTrafficProportion,
                experimentId: EXPERIMENT_ID
            });

            store.state$.pipe(take(1)).subscribe(({ experiment }) => {
                expect(experiment.trafficProportion).toEqual(expectedTrafficProportion);
                expect(dotExperimentsService.setTrafficProportion).toHaveBeenCalledOnceWith(
                    EXPERIMENT_ID,
                    expectedTrafficProportion
                );
                done();
            });
        });

        it('should throw an error if update Traffic Proportion fails', () => {
            dotExperimentsService.setTrafficProportion.and.returnValue(throwError('error'));

            store.setSelectedTrafficProportion({
                trafficProportion: null,
                experimentId: EXPERIMENT_ID
            });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledOnceWith(
                'error' as unknown as HttpErrorResponse
            );
        });

        it('should change the experiment status when Start the experiment', (done) => {
            const experimentWithGoalsAndVariant: DotExperiment = {
                ...ExperimentMocks[3]
            };
            const expectedExperiment: DotExperiment = {
                ...ExperimentMocks[3],
                status: DotExperimentStatusList.RUNNING
            };

            dotExperimentsService.getById.and
                .callThrough()
                .and.returnValue(of({ ...experimentWithGoalsAndVariant }));

            dotExperimentsService.start.and.callThrough().and.returnValue(
                of({
                    ...expectedExperiment
                })
            );

            spectator.service.loadExperiment(EXPERIMENT_ID);

            store.startExperiment(experimentWithGoalsAndVariant);

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.status).toEqual(DotExperimentStatusList.RUNNING);
                done();
            });
        });
    });
});
