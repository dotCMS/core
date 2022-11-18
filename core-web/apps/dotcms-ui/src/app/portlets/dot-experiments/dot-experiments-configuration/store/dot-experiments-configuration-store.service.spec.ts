import { LoadingState } from '@portlets/shared/models/shared-models';
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

const EXPERIMENT_ID = '1111';
const PAGE_ID = '222';
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
            mockProvider(Title),
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createStoreService({});
        dotExperimentsService = spectator.inject(DotExperimentsService);
        store = spectator.inject(DotExperimentsConfigurationStore);

        dotExperimentsService.getById.and.returnValue(of(ExperimentMocks[0]));
    });

    it('should set initial data', (done) => {
        const expectedInitialState: DotExperimentsConfigurationState = {
            pageId: PAGE_ID,
            experimentId: EXPERIMENT_ID,
            experiment: null,
            status: LoadingState.LOADING
        };

        store.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
    });

    it('should have getPageId$ from the store', (done) => {
        store.state$.subscribe(({ pageId }) => {
            expect(pageId).toEqual(PAGE_ID);
            done();
        });
    });

    it('should have getExperimentId$ from the store', (done) => {
        store.state$.subscribe(({ experimentId }) => {
            expect(experimentId).toEqual(EXPERIMENT_ID);
            done();
        });
    });

    it('should have getExperiment$ from the store', (done) => {
        store.loadExperiment();
        store.state$.subscribe(({ experiment }) => {
            expect(experiment).toEqual(ExperimentMocks[0]);
            done();
        });
    });

    it('should have isLoading$ from the store', (done) => {
        store.isLoading$.subscribe((data) => {
            expect(data).toEqual(true);
            done();
        });
    });

    it('should update status to the store', (done) => {
        store.setComponentStatus(LoadingState.LOADED);
        store.isLoading$.subscribe((status) => {
            expect(status).toEqual(false);
            done();
        });
    });

    it('should update experiments to the store', (done) => {
        store.setExperiment(ExperimentMocks[0]);
        store.state$.subscribe(({ experiment }) => {
            expect(experiment).toEqual(ExperimentMocks[0]);
            done();
        });
    });

    describe('Effects', () => {
        it('should load experiment to store', (done) => {
            store.loadExperiment();
            expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_ID);
            store.state$.subscribe(({ experiment }) => {
                expect(experiment).toEqual(ExperimentMocks[0]);
                done();
            });
        });
    });
});
