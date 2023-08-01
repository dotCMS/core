import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmPopup } from 'primeng/confirmpopup';
import { TabView } from 'primeng/tabview';

import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotExperimentStatus,
    DotExperimentVariantDetail
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import {
    BAYESIAN_CHARTJS_DATA_MOCK_WITH_DATA,
    DAILY_CHARTJS_DATA_MOCK_WITH_DATA,
    getExperimentMock,
    getExperimentResultsMock,
    MockDotMessageService
} from '@dotcms/utils-testing';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsReportDailyDetailsComponent } from './components/dot-experiments-report-daily-details/dot-experiments-report-daily-details.component';
import { DotExperimentsReportsChartComponent } from './components/dot-experiments-reports-chart/dot-experiments-reports-chart.component';
import { DotExperimentsReportsSkeletonComponent } from './components/dot-experiments-reports-skeleton/dot-experiments-reports-skeleton.component';
import { DotExperimentsReportsComponent } from './dot-experiments-reports.component';
import {
    DotExperimentsReportsStore,
    VmReportExperiment
} from './store/dot-experiments-reports-store';

import { DotExperimentsExperimentSummaryComponent } from '../shared/ui/dot-experiments-experiment-summary/dot-experiments-experiment-summary.component';
import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: '1111'
        }
    }
};

const EXPERIMENT_MOCK_3 = getExperimentMock(3);
const EXPERIMENT_RESULTS_MOCK = getExperimentResultsMock(0);

const defaultVmMock: VmReportExperiment = {
    experiment: EXPERIMENT_MOCK_3,
    results: EXPERIMENT_RESULTS_MOCK,
    dailyChartData: DAILY_CHARTJS_DATA_MOCK_WITH_DATA,
    bayesianChartData: BAYESIAN_CHARTJS_DATA_MOCK_WITH_DATA,
    detailData: [],
    isLoading: false,
    hasEnoughSessions: false,
    status: ComponentStatus.INIT,
    showSummary: false,
    winnerLegendSummary: { icon: 'icon', legend: 'legend' },
    suggestedWinner: null,
    promotedVariant: null
};

const EXPERIMENT_MOCK = getExperimentMock(0);

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.name': 'xx'
});

describe('DotExperimentsReportsComponent', () => {
    let spectator: Spectator<DotExperimentsReportsComponent>;
    let router: SpyObject<Router>;
    let store: DotExperimentsReportsStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let confirmPopupComponent: ConfirmPopup;

    const createComponent = createComponentFactory({
        component: DotExperimentsReportsComponent,
        overrideComponents: [
            [
                DotExperimentsReportsComponent,
                {
                    remove: { imports: [DotExperimentsReportsChartComponent] },
                    add: { imports: [MockComponent(DotExperimentsReportsChartComponent)] }
                }
            ]
        ],
        componentProviders: [DotExperimentsReportsStore],
        providers: [
            ConfirmationService,
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(Router),
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(MessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsReportsStore, true);

        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));
        dotExperimentsService.getResults.mockReturnValue(of({ ...EXPERIMENT_RESULTS_MOCK }));

        router = spectator.inject(Router);
    });

    it('should show the skeleton component when is loading', () => {
        spectator.component.vm$ = of({ ...defaultVmMock, isLoading: true });
        spectator.detectChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsReportsSkeletonComponent)).toExist();
    });

    it("shouldn't show the skeleton component when is not loading", () => {
        spectator.component.vm$ = of({ ...defaultVmMock });
        spectator.detectComponentChanges();

        expect(spectator.query(DotExperimentsUiHeaderComponent)).toExist();
        expect(spectator.query(DotExperimentsReportsSkeletonComponent)).not.toExist();
    });

    it('should show DotExperimentsReportsChartComponent when no loading', () => {
        spectator.component.vm$ = of({ ...defaultVmMock, isLoading: false });
        spectator.detectChanges();

        expect(spectator.query(TabView)).toExist();
        expect(spectator.query(DotExperimentsReportsChartComponent)).toExist();
        expect(spectator.query(DotExperimentsReportDailyDetailsComponent)).toExist();
    });

    it('should have a Daily Report and a Bayesian Report', (done) => {
        spectator.component.vm$ = of({ ...defaultVmMock, isLoading: false });
        spectator.detectChanges();

        expect(spectator.query(TabView)).toExist();
        expect(spectator.query(byTestId('daily-chart'))).toExist();
        expect(spectator.query(byTestId('bayesian-chart'))).toExist();
        expect(spectator.query(DotExperimentsReportDailyDetailsComponent)).toExist();

        spectator.component.vm$.subscribe((vm) => {
            expect(spectator.query(DotExperimentsReportsChartComponent).data).toEqual(
                vm.dailyChartData
            );

            expect(spectator.queryLast(DotExperimentsReportsChartComponent).data).toEqual(
                vm.bayesianChartData
            );
            expect(spectator.queryLast(DotExperimentsReportsChartComponent).isLinearAxis).toEqual(
                true
            );
            done();
        });
    });

    it('should show the SummaryComponent', () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            experiment: {
                ...defaultVmMock.experiment,
                status: DotExperimentStatus.RUNNING
            },
            isLoading: false,
            showSummary: true
        });
        spectator.detectComponentChanges();
        expect(spectator.query(DotExperimentsExperimentSummaryComponent)).toExist();
    });

    it('should back to Experiment List', async () => {
        spectator.component.vm$ = of({
            ...defaultVmMock,
            experiment: {
                ...defaultVmMock.experiment,
                status: DotExperimentStatus.RUNNING
            },
            isLoading: false,
            showSummary: true
        });

        spectator.detectComponentChanges();
        spectator.fixture.whenStable().then(() => {
            spectator.component.goToExperimentList(EXPERIMENT_MOCK.pageId);
            expect(router.navigate).toHaveBeenCalledWith(
                ['/edit-page/experiments/', EXPERIMENT_MOCK.pageId],
                {
                    queryParams: {
                        mode: null,
                        variantName: null,
                        experimentId: null
                    },
                    queryParamsHandling: 'merge'
                }
            );
        });
    });

    it('should show promote variant', () => {
        const variant: DotExperimentVariantDetail = {
            id: '111',
            name: 'Variant 111 Name',
            conversions: 0,
            conversionRate: '0%',
            conversionRateRange: '19.41% to 93.24%',
            sessions: 0,
            probabilityToBeBest: '7.69%',
            isWinner: false,
            isPromoted: false
        };

        spectator.detectChanges();
        jest.spyOn(store, 'promoteVariant');

        spectator.click(byTestId('promote-variant-button'));

        expect(spectator.query(ConfirmPopup)).toExist();

        confirmPopupComponent = spectator.query(ConfirmPopup);
        confirmPopupComponent.accept();

        expect(store.promoteVariant).toHaveBeenCalledWith({
            experimentId: EXPERIMENT_MOCK.id,
            variant: variant
        });
        expect(spectator.queryAll(byTestId('variant-promoted-tag')).length).toEqual(0);
    });
});
