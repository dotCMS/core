import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    DEFAULT_VARIANT_NAME,
    DotExperiment,
    DotExperimentStatus,
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
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    PARENT_RESOLVERS_ACTIVE_ROUTE_DATA,
    getExperimentMock,
    GoalsMock,
    MockDotMessageService
} from '@dotcms/utils-testing';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import {
    DotExperimentsConfigurationState,
    DotExperimentsConfigurationStore
} from './dot-experiments-configuration-store';

const EXPERIMENT_MOCK = getExperimentMock(0);
const EXPERIMENT_MOCK_1 = getExperimentMock(1);
const EXPERIMENT_MOCK_2 = getExperimentMock(2);
const EXPERIMENT_MOCK_3 = getExperimentMock(3);

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: EXPERIMENT_MOCK.id,
            pageId: EXPERIMENT_MOCK.pageId
        },
        data: ACTIVE_ROUTE_MOCK_CONFIG.snapshot.data
    },
    parent: { ...PARENT_RESOLVERS_ACTIVE_ROUTE_DATA }
};

const EXPECTED_INITIAL_STATE: DotExperimentsConfigurationState = {
    experiment: EXPERIMENT_MOCK,
    status: ComponentStatus.IDLE,
    stepStatusSidebar: {
        status: ComponentStatus.IDLE,
        isOpen: false,
        experimentStep: null
    },
    configProps: ACTIVE_ROUTE_MOCK_CONFIG.snapshot.data.config,
    hasEnterpriseLicense: ActivatedRouteMock.parent.snapshot.data.isEnterprise,
    addToBundleContentId: null,
    pushPublishEnvironments: ActivatedRouteMock.parent.snapshot.data.pushPublishEnvironments
};

const messageServiceMock = new MockDotMessageService({
    'experiments.action.schedule-experiment': 'schedule-experiment',
    'experiments.action.start-experiment': 'Start Experiment',
    'experiments.action.end-experiment': 'End Experiment',
    'experiments.configure.scheduling.cancel': 'Cancel Scheduling',
    'experiments.action.stop.delete-confirm': 'Are you sure you want to stop this experiment?',
    stop: 'Stop',
    'dot.common.dialog.reject': 'Reject',
    'experiments.action.cancel.schedule-confirm':
        'Are you sure you want to cancel the scheduling of this experiment?',
    'dot.common.dialog.accept': 'Accept',
    'contenttypes.content.push_publish': 'Push Publish'
});

describe('DotExperimentsConfigurationStore', () => {
    let spectator: SpectatorService<DotExperimentsConfigurationStore>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;
    let dotPushPublishDialogService: SpyObject<DotPushPublishDialogService>;

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
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ConfirmationService),
            mockProvider(DotPushPublishDialogService)
        ]
    });

    beforeEach(() => {
        spectator = createStoreService();

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        dotPushPublishDialogService = spectator.inject(DotPushPublishDialogService);
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));
    });

    it('should set initial data', (done) => {
        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);
        store.state$.subscribe((state) => {
            expect(state).toEqual(EXPECTED_INITIAL_STATE);
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

    it('should set Menu items visibility when and experiment if is a DRAFT and has variants and Goal defined', (done) => {
        dotExperimentsService.getById.mockReturnValue(
            of({ ...EXPERIMENT_MOCK_2, status: DotExperimentStatus.DRAFT })
        );

        spectator.service.loadExperiment(EXPERIMENT_MOCK_2.id);

        store.vm$.subscribe(({ menuItems }) => {
            // Start Experiment
            expect(menuItems[0].visible).toEqual(true);
            expect(menuItems[0].disabled).toEqual(false);
            // End Experiment
            expect(menuItems[1].visible).toEqual(false);
            // Schedule Experiment
            expect(menuItems[2].visible).toEqual(false);
            // Add to Bundle
            expect(menuItems[3].visible).toEqual(true);
            done();
        });
    });

    it('should set Menu items visibility when experiment is RUNNING', (done) => {
        dotExperimentsService.getById.mockReturnValue(
            of({ ...EXPERIMENT_MOCK_2, status: DotExperimentStatus.RUNNING })
        );

        spectator.service.loadExperiment(EXPERIMENT_MOCK_2.id);

        store.vm$.subscribe(({ menuItems }) => {
            // Start Experiment
            expect(menuItems[0].visible).toEqual(false);
            // End Experiment
            expect(menuItems[1].visible).toEqual(true);
            // Schedule Experiment
            expect(menuItems[2].visible).toEqual(false);
            // Add to Bundle
            expect(menuItems[3].visible).toEqual(true);
            done();
        });
    });

    it('should set Menu items visibility when experiment SCHEDULED ', (done) => {
        dotExperimentsService.getById.mockReturnValue(
            of({ ...EXPERIMENT_MOCK_2, status: DotExperimentStatus.SCHEDULED })
        );

        spectator.service.loadExperiment(EXPERIMENT_MOCK_2.id);

        store.vm$.subscribe(({ menuItems }) => {
            // Start Experiment
            expect(menuItems[0].visible).toEqual(false);
            // End Experiment
            expect(menuItems[1].visible).toEqual(false);
            // Schedule Experiment
            expect(menuItems[2].visible).toEqual(true);
            // Add to Bundle
            expect(menuItems[3].visible).toEqual(true);
            done();
        });
    });

    it('should execute commands of menu items', (done) => {
        jest.spyOn(store, 'showAddToBundle');
        dotExperimentsService.getById.mockReturnValue(of({ ...EXPERIMENT_MOCK }));

        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        store.vm$.pipe(take(1)).subscribe(({ menuItems }) => {
            // Start Experiment
            menuItems[0].command();
            expect(dotExperimentsService.start).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);

            // Push Publish
            menuItems[3].command();
            expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
                assetIdentifier: EXPERIMENT_MOCK.identifier,
                title: 'Push Publish'
            });

            // Add to Bundle
            menuItems[4].command();
            expect(store.showAddToBundle).toHaveBeenCalledWith(EXPERIMENT_MOCK.identifier);

            // test the ones with confirm dialog in the DotExperimentsConfigurationComponent.
            done();
        });
    });

    it('should not show Push Publish is there is no environments', (done) => {
        dotExperimentsService.getById.mockReturnValue(of({ ...EXPERIMENT_MOCK }));

        spectator.service.patchState({ pushPublishEnvironments: [] });

        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        store.vm$.pipe(take(1)).subscribe(({ menuItems }) => {
            // Push Publish
            expect(menuItems[3].visible).toEqual(false);

            done();
        });
    });

    it('should not show Push Publish and Add to Bundle is there  no EnterpriseLicense', (done) => {
        dotExperimentsService.getById.mockReturnValue(of({ ...EXPERIMENT_MOCK }));

        spectator.service.patchState({ hasEnterpriseLicense: false });

        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        store.vm$.pipe(take(1)).subscribe(({ menuItems }) => {
            // Push Publish
            expect(menuItems[3].visible).toEqual(false);

            //Add to Bundle
            expect(menuItems[4].visible).toEqual(false);

            done();
        });
    });

    it('should return `Schedule Experiment` when the experiment has schedule set', (done) => {
        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);

        store.vm$.subscribe(({ menuItems }) => {
            expect(menuItems[0].label).toEqual('schedule-experiment');
            done();
        });
    });

    it('should return `Run Experiment` when the experiment has schedule `null` ', (done) => {
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK_1));
        spectator.service.loadExperiment(EXPERIMENT_MOCK_1.id);

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK_1.id);

        store.vm$.subscribe(({ menuItems }) => {
            expect(menuItems[0].label).toEqual('Start Experiment');
            done();
        });
    });

    it('should return `Run Experiment` when the experiment has schedule startDate/endDate `null`', (done) => {
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK_3));
        spectator.service.loadExperiment(EXPERIMENT_MOCK_3.id);

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK_3.id);

        store.vm$.subscribe(({ menuItems }) => {
            expect(menuItems[0].label).toEqual('Start Experiment');
            done();
        });
    });

    describe('Effects', () => {
        it('should load experiment to store', (done) => {
            dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK_1));

            store.loadExperiment(EXPERIMENT_MOCK_1.id);
            expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK_1.id);
            store.state$.subscribe(({ experiment }) => {
                expect(experiment).toEqual(EXPERIMENT_MOCK_1);
                done();
            });
        });

        it('should add a variant to the store', (done) => {
            dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK_1));
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
                            weight: 100,
                            promoted: false
                        }
                    ]
                }
            };

            dotExperimentsService.addVariant.mockReturnValue(of(expectedExperiment));

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
                { id: '111', name: DEFAULT_VARIANT_NAME, weight: 50, url: 'url', promoted: false },
                { id: '222', name: 'name to edit', weight: 50, url: 'url', promoted: false }
            ];
            const variantEdited: Variant[] = [
                { id: '111', name: DEFAULT_VARIANT_NAME, weight: 50, url: 'url', promoted: false },
                { id: '222', name: 'new name', weight: 50, url: 'url', promoted: false }
            ];

            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    trafficProportion: {
                        ...EXPERIMENT_MOCK.trafficProportion,
                        variants
                    }
                })
            );
            dotExperimentsService.editVariant.mockReturnValue(
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

        it('should edit a description of an experiment', (done) => {
            const newDescription = 'new description';

            dotExperimentsService.setDescription.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    description: newDescription
                })
            );

            store.loadExperiment(EXPERIMENT_MOCK.id);

            store.setDescription({
                experiment: EXPERIMENT_MOCK,
                data: { description: newDescription }
            });

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.description).toEqual(newDescription);
                done();
            });
        });

        it('should delete a variant from an experiment', (done) => {
            const variants: Variant[] = [
                { id: 'DEFAULT', name: 'DEFAULT', weight: 50, promoted: false },
                {
                    id: '111',
                    name: '1111',
                    weight: 50,
                    promoted: false
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
                    variants: [{ id: 'DEFAULT', name: 'DEFAULT', weight: 50, promoted: false }]
                }
            };

            dotExperimentsService.getById.mockReturnValue(of(experimentWithTwoVariants));
            dotExperimentsService.removeVariant.mockReturnValue(of(expectedResponseRemoveVariant));

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

            dotExperimentsService.getById.mockReturnValue(of({ ...EXPERIMENT_MOCK }));

            dotExperimentsService.setGoal.mockReturnValue(
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

        it('should remove the default conditions of type REACH_PAGE', (done) => {
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

            dotExperimentsService.getById.mockReturnValue(of({ ...experimentMock }));

            store.loadExperiment(EXPERIMENT_MOCK.id);

            store.goals$.subscribe(({ primary }) => {
                expect(primary.conditions.length).toBe(1);
                expect(primary.conditions[0].parameter).toBe(GOAL_PARAMETERS.URL);
                done();
            });
        });
        it('should remove the default conditions of type BOUNCE_RATE', (done) => {
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

            dotExperimentsService.getById.mockReturnValue(of({ ...experimentMock }));

            store.loadExperiment(EXPERIMENT_MOCK.id);

            store.goals$.subscribe(({ primary }) => {
                expect(primary.conditions.length).toBe(0);
                done();
            });
        });

        it('should delete a Goal from an experiment', (done) => {
            const goalLevelToDelete: GoalsLevels = 'primary';
            const experimentWithGoals: DotExperiment = {
                ...EXPERIMENT_MOCK,
                goals: { ...GoalsMock }
            };

            dotExperimentsService.getById.mockReturnValue(of({ ...experimentWithGoals }));

            dotExperimentsService.deleteGoal.mockReturnValue(
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

            dotExperimentsService.setScheduling.mockReturnValue(
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
                expect(dotExperimentsService.setScheduling).toHaveBeenCalledWith(
                    EXPERIMENT_MOCK.id,
                    expectedScheduling
                );
                done();
            });
        });

        it('should throw an error if update scheduling fails', () => {
            dotExperimentsService.setScheduling.mockReturnValue(throwError('error'));

            store.setSelectedScheduling({
                scheduling: null,
                experimentId: EXPERIMENT_MOCK.id
            });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                'error' as unknown as HttpErrorResponse
            );
        });

        it('should set Traffic Allocation to the experiment', (done) => {
            const expectedTrafficAllocation = 10;

            dotExperimentsService.setTrafficAllocation.mockReturnValue(
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
                expect(dotExperimentsService.setTrafficAllocation).toHaveBeenCalledWith(
                    EXPERIMENT_MOCK.id,
                    expectedTrafficAllocation
                );
                done();
            });
        });

        it('should throw an error if update Traffic Allocation fails', () => {
            dotExperimentsService.setTrafficAllocation.mockReturnValue(throwError('error'));

            store.setSelectedAllocation({
                trafficAllocation: 120,
                experimentId: EXPERIMENT_MOCK.id
            });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                'error' as unknown as HttpErrorResponse
            );
        });

        it('should set Traffic Proportion to the experiment', (done) => {
            const expectedTrafficProportion: TrafficProportion = {
                type: TrafficProportionTypes.SPLIT_EVENLY,
                variants: [
                    { id: '111', name: 'DEFAULT', weight: 50, promoted: false },
                    { id: '111', name: 'A', weight: 50, promoted: false }
                ]
            };

            dotExperimentsService.setTrafficProportion.mockReturnValue(
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
                expect(dotExperimentsService.setTrafficProportion).toHaveBeenCalledWith(
                    EXPERIMENT_MOCK.id,
                    expectedTrafficProportion
                );
                done();
            });
        });

        it('should throw an error if update Traffic Proportion fails', () => {
            dotExperimentsService.setTrafficProportion.mockReturnValue(throwError('error'));

            store.setSelectedTrafficProportion({
                trafficProportion: null,
                experimentId: EXPERIMENT_MOCK.id
            });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                'error' as unknown as HttpErrorResponse
            );
        });

        it('should change the experiment status when Start the experiment', (done) => {
            const experimentWithGoalsAndVariant: DotExperiment = {
                ...EXPERIMENT_MOCK_2
            };
            const expectedExperiment: DotExperiment = {
                ...EXPERIMENT_MOCK_2,
                status: DotExperimentStatus.RUNNING
            };

            dotExperimentsService.getById.mockReturnValue(of({ ...experimentWithGoalsAndVariant }));

            dotExperimentsService.start.mockReturnValue(
                of({
                    ...expectedExperiment
                })
            );

            spectator.service.loadExperiment(EXPERIMENT_MOCK_2.id);

            store.startExperiment(experimentWithGoalsAndVariant);

            store.state$.subscribe(({ experiment, status }) => {
                expect(experiment.status).toEqual(DotExperimentStatus.RUNNING);
                expect(status).toEqual(ComponentStatus.IDLE);
                done();
            });
        });

        it('should change the experiment status when Stop the experiment', (done) => {
            dotExperimentsService.getById.mockReturnValue(of({ ...EXPERIMENT_MOCK_2 }));

            dotExperimentsService.stop.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_2,
                    status: DotExperimentStatus.ENDED
                })
            );

            spectator.service.loadExperiment(EXPERIMENT_MOCK_2.id);

            store.stopExperiment(EXPERIMENT_MOCK_2);

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.status).toEqual(DotExperimentStatus.ENDED);
                done();
            });
        });

        it('should handle error when stopping the experiment', () => {
            dotExperimentsService.stop.mockReturnValue(throwError('error'));

            store.stopExperiment(EXPERIMENT_MOCK_2);

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                'error' as unknown as HttpErrorResponse
            );
        });

        it('should call the cancel experiment method when cancel scheduling', (done) => {
            dotExperimentsService.getById.mockReturnValue(
                of({ ...EXPERIMENT_MOCK_2, status: DotExperimentStatus.SCHEDULED })
            );

            dotExperimentsService.cancelSchedule.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_2,
                    status: DotExperimentStatus.DRAFT
                })
            );

            spectator.service.loadExperiment(EXPERIMENT_MOCK_2.id);

            store.cancelSchedule(EXPERIMENT_MOCK_2);

            store.state$.subscribe(() => {
                expect(dotExperimentsService.cancelSchedule).toHaveBeenCalledWith(
                    EXPERIMENT_MOCK_2.id
                );
                done();
            });
        });

        it('should handle error when canceling the experiment', () => {
            dotExperimentsService.cancelSchedule.mockReturnValue(throwError('error'));

            store.cancelSchedule(EXPERIMENT_MOCK_2);

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                'error' as unknown as HttpErrorResponse
            );
        });
    });
});
