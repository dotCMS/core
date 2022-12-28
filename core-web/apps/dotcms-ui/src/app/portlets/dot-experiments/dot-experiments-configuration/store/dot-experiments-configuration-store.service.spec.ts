import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { ActivatedRoute } from '@angular/router';
import { ExperimentMocks } from '@portlets/dot-experiments/test/mocks';
import {
    DotExperimentsConfigurationState,
    DotExperimentsConfigurationStore
} from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';
import { createServiceFactory, mockProvider, SpectatorService, SpyObject } from '@ngneat/spectator';
import { Title } from '@angular/platform-browser';
import { of } from 'rxjs';
import { DotMessageService } from '@dotcms/data-access';
import {
    DotExperiment,
    ExperimentSteps,
    LoadingState,
    Status,
    Variant
} from '@dotcms/dotcms-models';
import { MessageService } from 'primeng/api';

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

    const createStoreService = createServiceFactory({
        service: DotExperimentsConfigurationStore,
        providers: [
            mockProvider(DotExperimentsService),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(Title),
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createStoreService({});

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.and.callThrough().and.returnValue(of(ExperimentMocks[0]));
    });

    it('should set initial data', (done) => {
        spectator.service.loadExperiment(EXPERIMENT_ID);

        const expectedInitialState: DotExperimentsConfigurationState = {
            experiment: ExperimentMocks[0],
            status: LoadingState.LOADED,
            stepStatusSidebar: {
                status: Status.IDLE,
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
        store.setComponentStatus(LoadingState.LOADED);
        store.isLoading$.subscribe((status) => {
            expect(status).toEqual(false);
            done();
        });
    });

    it('should update sidebar status(SAVING/IDLE/DONE) to the store', (done) => {
        store.setSidebarStatus(Status.SAVING);
        store.state$.subscribe(({ stepStatusSidebar }) => {
            expect(stepStatusSidebar.status).toEqual(Status.SAVING);
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
            expect(stepStatusSidebar.status).toEqual(Status.DONE);
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
            const newVariant: Variant = {
                id: '333',
                name: '3333',
                weight: '333'
            };

            const expectedExperiment = {
                ...ExperimentMocks[1],
                trafficProportion: {
                    ...ExperimentMocks[1].trafficProportion,
                    variants: [...ExperimentMocks[1].trafficProportion.variants, newVariant]
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

        it('should delete a variant from a experiment', (done) => {
            const variants: Variant[] = [
                { id: 'DEFAULT', name: 'DEFAULT', weight: 'xxx' },
                {
                    id: '111',
                    name: '1111',
                    weight: 'xxx'
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
                    variants: [{ id: 'DEFAULT', name: 'DEFAULT', weight: 'xxx' }]
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

            store.deleteVariant(variants[1]);

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.trafficProportion.variants).toEqual(
                    expectedResponseRemoveVariant.trafficProportion.variants
                );
                done();
            });
        });
    });
});
