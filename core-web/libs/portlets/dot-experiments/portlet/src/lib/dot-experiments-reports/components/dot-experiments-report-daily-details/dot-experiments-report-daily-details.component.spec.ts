import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentVariantDetail } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsReportDailyDetailsComponent } from './dot-experiments-report-daily-details.component';

import { DotExperimentsDetailsTableComponent } from '../../../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';
import { DotExperimentsReportsStore } from '../../store/dot-experiments-reports-store';

const messageServiceMock = new MockDotMessageService({
    'experiment.reports.promote.warning': 'Are you sure you want to promote this variant?',
    Yes: 'Yes',
    No: 'No',
    'experiments.reports.resume': 'Resume',
    'experiments.reports.variants': 'Variants',
    'experiments.reports.conversions': 'Conversions',
    'experiments.reports.sessions': 'Sessions',
    'experiments.reports.conversions.rate': 'Conversion Rate',
    'experiments.reports.probability.best': 'Probability to be Best',
    'experiments.reports.conversion.rate.range': 'Conversion Rate Range',
    'experiments.reports.promote': 'Promote',
    promoted: 'Promoted'
});

const VARIANT_DETAIL_MOCK: DotExperimentVariantDetail = {
    id: '111',
    name: 'Variant A',
    conversions: 100,
    conversionRate: '10%',
    conversionRateRange: '8% to 12%',
    sessions: 1000,
    probabilityToBeBest: '85%',
    isWinner: false,
    isPromoted: false
};

const DETAIL_DATA_MOCK: DotExperimentVariantDetail[] = [
    VARIANT_DETAIL_MOCK,
    {
        id: 'DEFAULT',
        name: 'Original',
        conversions: 80,
        conversionRate: '8%',
        conversionRateRange: '6% to 10%',
        sessions: 1000,
        probabilityToBeBest: '15%',
        isWinner: false,
        isPromoted: false
    }
];

describe('DotExperimentsReportDailyDetailsComponent', () => {
    let spectator: Spectator<DotExperimentsReportDailyDetailsComponent>;
    let store: SpyObject<DotExperimentsReportsStore>;
    let confirmationService: SpyObject<ConfirmationService>;

    const createComponent = createComponentFactory({
        component: DotExperimentsReportDailyDetailsComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(DotExperimentsReportsStore, {
                promoteVariant: jest.fn()
            }),
            mockProvider(ConfirmationService),
            mockProvider(MessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                detailData: DETAIL_DATA_MOCK,
                hasEnoughSessions: true,
                experimentId: 'experiment-123',
                promotedVariantId: null
            } as unknown
        });

        store = spectator.inject(DotExperimentsReportsStore);
        confirmationService = spectator.inject(ConfirmationService);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should DotExperimentsDetailsTableComponent exist', () => {
        const detailsTableComponent = spectator.query(DotExperimentsDetailsTableComponent);

        expect(detailsTableComponent).toExist();
        expect(detailsTableComponent.$data()).toEqual(DETAIL_DATA_MOCK);
        expect(detailsTableComponent.$isEmpty()).toBe(false);
    });

    it('should show promote variant', () => {
        jest.spyOn(store, 'promoteVariant');
        jest.spyOn(confirmationService, 'confirm');

        // Simulate promoteVariant call with mock event
        const mockEvent = new MouseEvent('click');
        spectator.component.promoteVariant(mockEvent, 'experiment-123', VARIANT_DETAIL_MOCK);

        // Verify confirmation dialog was shown
        expect(confirmationService.confirm).toHaveBeenCalled();

        // Get the confirm options and call accept
        const confirmOptions = (confirmationService.confirm as jest.Mock).mock.calls[0][0];
        confirmOptions.accept();

        // Verify store method was called with correct parameters
        expect(store.promoteVariant).toHaveBeenCalledWith({
            experimentId: 'experiment-123',
            variant: VARIANT_DETAIL_MOCK
        });
    });
});
