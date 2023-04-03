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
import {
    getExperimentMock,
    getExperimentResultsMock,
    VARIANT_RESULT_MOCK_1
} from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

const EXPERIMENT_MOCK = getExperimentMock(1);
const EXPERIMENT_MOCK_RESULTS = getExperimentResultsMock(0);

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
        dotExperimentsService.getResults.and
            .callThrough()
            .and.returnValue(of(EXPERIMENT_MOCK_RESULTS));
    });

    it('should set initial data', (done) => {
        spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

        const expectedInitialState: DotExperimentsReportsState = {
            experiment: EXPERIMENT_MOCK,
            status: ComponentStatus.IDLE,
            results: EXPERIMENT_MOCK_RESULTS,
            variantResults: VARIANT_RESULT_MOCK_1,
            chartResults: null
        };

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);
        store.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
    });

    it('should have isLoading$ from the store', (done) => {
        spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);
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
        spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

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
        spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

        store.showExperimentSummary$.subscribe((value) => {
            expect(value).toEqual(true);
            done();
        });
    });

    describe('Effects', () => {
        it('should load experiment to store', (done) => {
            dotExperimentsService.getById.and.callThrough().and.returnValue(of(EXPERIMENT_MOCK));

            store.loadExperimentAndResults(EXPERIMENT_MOCK.id);
            expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);

            store.state$.subscribe(({ experiment }) => {
                expect(experiment).toEqual(EXPERIMENT_MOCK);
                done();
            });
        });
        it('should promote variant', () => {
            dotExperimentsService.promoteVariant.and
                .callThrough()
                .and.returnValue(of(EXPERIMENT_MOCK));

            store.promoteVariant('variantName');

            expect(dotExperimentsService.promoteVariant).toHaveBeenCalledWith('variantName');
        });
    });
});
