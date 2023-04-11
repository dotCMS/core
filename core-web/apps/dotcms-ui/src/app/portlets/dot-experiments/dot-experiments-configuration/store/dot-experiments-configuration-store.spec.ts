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
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goals,
    GoalsLevels,
    RangeOfDateAndTime,
    TrafficProportion,
    TrafficProportionTypes,
    Variant
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import {
    DotExperimentsConfigurationState,
    DotExperimentsConfigurationStore
} from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentMock, GoalsMock } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

const EXPERIMENT_MOCK = getExperimentMock(0);
const EXPERIMENT_MOCK_1 = getExperimentMock(1);
const EXPERIMENT_MOCK_2 = getExperimentMock(2);

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: EXPERIMENT_MOCK.id,
            pageId: EXPERIMENT_MOCK.pageId
        }
    }
};

const messageServiceMock = new MockDotMessageService({
    'experiments.action.schedule-experiment': 'schedule-experiment',
    'experiments.action.start-experiment': 'run-experiment'
});

describe('DotExperimentsConfigurationStore', () => {
    let spectator: SpectatorService<DotExperimentsConfigurationStore>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

    const createStoreService = createServiceFactory({
        service: DotExperimentsConfigurationStore,
        providers: [
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(Title),
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createStoreService({});

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        dotExperimentsService.getById.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK));
    });

    it('should set initial data', (done) => {
        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        const expectedInitialState: DotExperimentsConfigurationState = {
            experiment: EXPERIMENT_MOCK,
            status: ComponentStatus.IDLE,
            stepStatusSidebar: {
                status: ComponentStatus.IDLE,
                isOpen: false,
                experimentStep: null
            }
        };

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);
        store.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
    });

    it('should have isLoading$ from the store', (done) => {
        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);
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

    it('should return `Schedule Experiment` when the experiment has schedule set', (done) => {
        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);

        store.vm$.subscribe(({ runExperimentBtnLabel }) => {
            expect(runExperimentBtnLabel).toEqual('schedule-experiment');
            done();
        });
    });

    it('should return `Run Experiment` when the experiment has schedule set', (done) => {
        dotExperimentsService.getById.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK_1));
        spectator.service.loadExperiment(EXPERIMENT_MOCK_1.id);

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK_1.id);

        store.vm$.subscribe(({ runExperimentBtnLabel }) => {
            expect(runExperimentBtnLabel).toEqual('run-experiment');
            done();
        });
    });

    describe('Effects', () => {
        it('should load experiment to store', (done) => {
            dotExperimentsService.getById.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK_1));

            store.loadExperiment(EXPERIMENT_MOCK_1.id);
            expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK_1.id);
            store.state$.subscribe(({ experiment }) => {
                expect(experiment).toEqual(EXPERIMENT_MOCK_1);
                done();
            });
        });

        it('should add a variant to the store', (done) => {
            dotExperimentsService.getById.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK_1));
            const newVariant: { experimentId: string; name: string } = {
                name: '333',
                experimentId: EXPERIMENT_MOCK_1.id
            };

            const expectedExperiment = {
                ...EXPERIMENT_MOCK_1,
                trafficProportion: {
                    ...EXPERIMENT_MOCK_1.trafficProportion,
                    variants: [
                        ...EXPERIMENT_MOCK_1.trafficProportion.variants,
                        {
                            name: newVariant.name,
                            id: '222',
                            weight: 100
                        }
                    ]
                }
            };

            dotExperimentsService.addVariant.and
                .callThrough()
                .and.returnValue(of(expectedExperiment));

            store.loadExperiment(EXPERIMENT_MOCK.id);
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
                    ...EXPERIMENT_MOCK,
                    trafficProportion: {
                        ...EXPERIMENT_MOCK.trafficProportion,
                        variants
                    }
                })
            );
            dotExperimentsService.editVariant.and.callThrough().and.returnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    trafficProportion: {
                        ...EXPERIMENT_MOCK.trafficProportion,
                        variants: variantEdited
                    }
                })
            );

            store.loadExperiment(EXPERIMENT_MOCK.id);

            store.editVariant({
                experimentId: EXPERIMENT_MOCK.id,
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
                ...EXPERIMENT_MOCK_1,
                trafficProportion: {
                    ...EXPERIMENT_MOCK_1.trafficProportion,
                    variants: [...variants]
                }
            };
            const expectedResponseRemoveVariant: DotExperiment = {
                ...EXPERIMENT_MOCK_1,
                trafficProportion: {
                    ...EXPERIMENT_MOCK_1.trafficProportion,
                    variants: [{ id: 'DEFAULT', name: 'DEFAULT', weight: 50 }]
                }
            };

            dotExperimentsService.getById.and
                .callThrough()
                .and.returnValue(of(experimentWithTwoVariants));
            dotExperimentsService.removeVariant.and
                .callThrough()
                .and.returnValue(of(expectedResponseRemoveVariant));

            store.loadExperiment(EXPERIMENT_MOCK.id);
            expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);

            store.deleteVariant({ experimentId: EXPERIMENT_MOCK.id, variant: variants[1] });

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
                .and.returnValue(of({ ...EXPERIMENT_MOCK }));

            dotExperimentsService.setGoal.and.callThrough().and.returnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    goals: expectedGoals
                })
            );

            store.loadExperiment(EXPERIMENT_MOCK.id);

            store.setSelectedGoal({ experimentId: EXPERIMENT_MOCK.id, goals: expectedGoals });

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.goals).toEqual(expectedGoals);
                done();
            });
        });

        it('should get a isDefault true to the default conditions of type REACH_PAGE', (done) => {
            const experimentMock = {
                ...EXPERIMENT_MOCK,
                goals: {
                    primary: {
                        name: 'default',
                        type: GOAL_TYPES.REACH_PAGE,
                        conditions: [
                            {
                                parameter: GOAL_PARAMETERS.URL,
                                operator: GOAL_OPERATORS.EQUALS,
                                value: 'index'
                            },
                            {
                                parameter: GOAL_PARAMETERS.REFERER,
                                operator: GOAL_OPERATORS.CONTAINS,
                                value: 'index'
                            }
                        ]
                    }
                }
            };

            dotExperimentsService.getById.and
                .callThrough()
                .and.returnValue(of({ ...experimentMock }));

            store.loadExperiment(EXPERIMENT_MOCK.id);

            store.goals$.subscribe(({ primary }) => {
                expect(primary.conditions[0].isDefault).toBeFalse();
                expect(primary.conditions[1].parameter).toBe(GOAL_PARAMETERS.REFERER);
                expect(primary.conditions[1].isDefault).toBeTrue();
                done();
            });
        });
        it('should get a isDefault true to the default conditions of type BOUNCE_RATE', (done) => {
            const experimentMock = {
                ...EXPERIMENT_MOCK,
                goals: {
                    primary: {
                        name: 'default',
                        type: GOAL_TYPES.BOUNCE_RATE,
                        conditions: [
                            {
                                parameter: GOAL_PARAMETERS.URL,
                                operator: GOAL_OPERATORS.CONTAINS,
                                value: 'index'
                            }
                        ]
                    }
                }
            };

            dotExperimentsService.getById.and
                .callThrough()
                .and.returnValue(of({ ...experimentMock }));

            store.loadExperiment(EXPERIMENT_MOCK.id);

            store.goals$.subscribe(({ primary }) => {
                expect(primary.conditions[0].parameter).toBe(GOAL_PARAMETERS.URL);
                expect(primary.conditions[0].isDefault).toBeTrue();
                done();
            });
        });

        it('should delete a Goal from an experiment', (done) => {
            const goalLevelToDelete: GoalsLevels = 'primary';
            const experimentWithGoals: DotExperiment = {
                ...EXPERIMENT_MOCK,
                goals: { ...GoalsMock }
            };

            dotExperimentsService.getById.and
                .callThrough()
                .and.returnValue(of({ ...experimentWithGoals }));

            dotExperimentsService.deleteGoal.and.callThrough().and.returnValue(
                of({
                    ...EXPERIMENT_MOCK
                })
            );

            store.loadExperiment(EXPERIMENT_MOCK.id);

            store.deleteGoal({ experimentId: EXPERIMENT_MOCK.id, goalLevel: goalLevelToDelete });

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
                    ...EXPERIMENT_MOCK,
                    scheduling: expectedScheduling
                })
            );

            store.setSelectedScheduling({
                scheduling: expectedScheduling,
                experimentId: EXPERIMENT_MOCK.id
            });

            store.state$.pipe(take(1)).subscribe(({ experiment }) => {
                expect(experiment.scheduling).toEqual(expectedScheduling);
                expect(dotExperimentsService.setScheduling).toHaveBeenCalledOnceWith(
                    EXPERIMENT_MOCK.id,
                    expectedScheduling
                );
                done();
            });
        });

        it('should throw an error if update scheduling fails', () => {
            dotExperimentsService.setScheduling.and.returnValue(throwError('error'));

            store.setSelectedScheduling({
                scheduling: null,
                experimentId: EXPERIMENT_MOCK.id
            });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledOnceWith(
                'error' as unknown as HttpErrorResponse
            );
        });

        it('should set Traffic Allocation to the experiment', (done) => {
            const expectedTrafficAllocation = 10;

            dotExperimentsService.setTrafficAllocation.and.callThrough().and.returnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    trafficAllocation: expectedTrafficAllocation
                })
            );

            store.setSelectedAllocation({
                trafficAllocation: expectedTrafficAllocation,
                experimentId: EXPERIMENT_MOCK.id
            });

            store.state$.pipe(take(1)).subscribe(({ experiment }) => {
                expect(experiment.trafficAllocation).toEqual(expectedTrafficAllocation);
                expect(dotExperimentsService.setTrafficAllocation).toHaveBeenCalledOnceWith(
                    EXPERIMENT_MOCK.id,
                    expectedTrafficAllocation
                );
                done();
            });
        });

        it('should throw an error if update Traffic Allocation fails', () => {
            dotExperimentsService.setTrafficAllocation.and.returnValue(throwError('error'));

            store.setSelectedAllocation({
                trafficAllocation: 120,
                experimentId: EXPERIMENT_MOCK.id
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
                    ...EXPERIMENT_MOCK,
                    trafficProportion: expectedTrafficProportion
                })
            );

            store.setSelectedTrafficProportion({
                trafficProportion: expectedTrafficProportion,
                experimentId: EXPERIMENT_MOCK.id
            });

            store.state$.pipe(take(1)).subscribe(({ experiment }) => {
                expect(experiment.trafficProportion).toEqual(expectedTrafficProportion);
                expect(dotExperimentsService.setTrafficProportion).toHaveBeenCalledOnceWith(
                    EXPERIMENT_MOCK.id,
                    expectedTrafficProportion
                );
                done();
            });
        });

        it('should throw an error if update Traffic Proportion fails', () => {
            dotExperimentsService.setTrafficProportion.and.returnValue(throwError('error'));

            store.setSelectedTrafficProportion({
                trafficProportion: null,
                experimentId: EXPERIMENT_MOCK.id
            });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledOnceWith(
                'error' as unknown as HttpErrorResponse
            );
        });

        it('should change the experiment status when Start the experiment', (done) => {
            const experimentWithGoalsAndVariant: DotExperiment = {
                ...EXPERIMENT_MOCK_2
            };
            const expectedExperiment: DotExperiment = {
                ...EXPERIMENT_MOCK_2,
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

            spectator.service.loadExperiment(EXPERIMENT_MOCK_2.id);

            store.startExperiment(experimentWithGoalsAndVariant);

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.status).toEqual(DotExperimentStatusList.RUNNING);
                done();
            });
        });

        it('should change the experiment status when Stop the experiment', (done) => {
            dotExperimentsService.getById.and
                .callThrough()
                .and.returnValue(of({ ...EXPERIMENT_MOCK_2 }));

            dotExperimentsService.stop.and.callThrough().and.returnValue(
                of({
                    ...EXPERIMENT_MOCK_2,
                    status: DotExperimentStatusList.ENDED
                })
            );

            spectator.service.loadExperiment(EXPERIMENT_MOCK_2.id);

            store.stopExperiment(EXPERIMENT_MOCK_2);

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.status).toEqual(DotExperimentStatusList.ENDED);
                done();
            });
        });

        it('should handle error when stopping the experiment', () => {
            dotExperimentsService.stop.and.returnValue(throwError('error'));

            store.stopExperiment(EXPERIMENT_MOCK_2);

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledOnceWith(
                'error' as unknown as HttpErrorResponse
            );
        });
    });
});
