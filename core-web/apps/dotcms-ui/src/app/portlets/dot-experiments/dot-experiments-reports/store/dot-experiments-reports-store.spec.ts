import { createServiceFactory, mockProvider, SpectatorService, SpyObject } from '@ngneat/spectator';
import { of } from 'rxjs';

import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { ComponentStatus, DotExperimentStatusList } from '@dotcms/dotcms-models';
import {
    DotExperimentsReportsState,
    DotExperimentsReportsStore
} from '@portlets/dot-experiments/dot-experiments-reports/store/dot-experiments-reports-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentMock } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

const EXPERIMENT_MOCK = getExperimentMock(0);

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: EXPERIMENT_MOCK.id,
            pageId: EXPERIMENT_MOCK.pageId
        }
    }
};

describe('DotExperimentsReportsStore', () => {
    let spectator: SpectatorService<DotExperimentsReportsStore>;
    let store: DotExperimentsReportsStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createStoreService = createServiceFactory({
        service: DotExperimentsReportsStore,
        providers: [
            mockProvider(Title),
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            },
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createStoreService({});

        store = spectator.inject(DotExperimentsReportsStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK));
    });

    it('should set initial data', (done) => {
        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        const expectedInitialState: DotExperimentsReportsState = {
            experiment: EXPERIMENT_MOCK,
            status: ComponentStatus.IDLE
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
        store.state$.subscribe(({ status }) => {
            expect(status).toBe(ComponentStatus.LOADED);
            done();
        });
    });

    it('should get FALSE from showExperimentSummary$ if Experiment status is different of Running', (done) => {
        dotExperimentsService.getById.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK));
        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        store.showExperimentSummary$.subscribe((value) => {
            expect(value).toEqual(false);
            done();
        });
    });
    it('should get TRUE from showExperimentSummary$ if Experiment status is different of Running', (done) => {
        dotExperimentsService.getById.and.callThrough().and.returnValue(
            of({
                ...EXPERIMENT_MOCK,
                status: DotExperimentStatusList.RUNNING
            })
        );
        spectator.service.loadExperiment(EXPERIMENT_MOCK.id);

        store.showExperimentSummary$.subscribe((value) => {
            expect(value).toEqual(true);
            done();
        });
    });

    describe('Effects', () => {
        it('should load experiment to store', (done) => {
            dotExperimentsService.getById.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK));

            store.loadExperiment(EXPERIMENT_MOCK.id);
            expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);

            store.state$.subscribe(({ experiment }) => {
                expect(experiment).toEqual(EXPERIMENT_MOCK);
                done();
            });
        });
    });
});
