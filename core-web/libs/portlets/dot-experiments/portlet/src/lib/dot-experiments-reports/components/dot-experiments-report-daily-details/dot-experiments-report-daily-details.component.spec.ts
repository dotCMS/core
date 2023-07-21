import { DotExperimentsReportDailyDetailsComponent } from "./dot-experiments-report-daily-details.component";
import { DotExperimentsReportsStore } from "../../store/dot-experiments-reports-store";
import { byTestId, createComponentFactory, mockProvider, Spectator } from "@ngneat/spectator/jest";
import { ConfirmationService, MessageService } from "primeng/api";
import { DotExperimentsService } from "@dotcms/portlets/dot-experiments/data-access";
import { DotMessageService } from "@dotcms/data-access";
import { DotHttpErrorManagerService } from "@services/dot-http-error-manager/dot-http-error-manager.service";
import { ConfirmPopup } from "primeng/confirmpopup";
import {
  DotExperimentsDetailsTableComponent
} from "../../../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component";
import { DotExperimentVariantDetail } from "@dotcms/dotcms-models";

describe('DotExperimentsReportDailyDetailsComponent', () => {
    let spectator: Spectator<DotExperimentsReportDailyDetailsComponent>;
    let store: DotExperimentsReportsStore;

    const createComponent = createComponentFactory({
        component: DotExperimentsReportDailyDetailsComponent,
        componentProviders: [DotExperimentsReportsStore],
        providers: [
            ConfirmationService,
            mockProvider(DotExperimentsService),
            mockProvider(DotMessageService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(MessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();

        jest.spyOn(ConfirmPopup.prototype, 'bindScrollListener').mockImplementation(jest.fn());

        store = spectator.inject(DotExperimentsReportsStore, true);
    });

    it('should DotExperimentsDetailsTableComponent exist', () => {
        expect(spectator.query(DotExperimentsDetailsTableComponent)).toExist();
    });

    it('should show promote variant', () => {
        const EXPERIMENT_ID = '111';
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

        spectator.setInput('detailData', [variant]);
        spectator.setInput('experimentId', EXPERIMENT_ID);
        spectator.setInput('hasEnoughSessions', true);

        jest.spyOn(store, 'promoteVariant');

        spectator.click(spectator.query(byTestId('promote-variant-button')));
        spectator.detectComponentChanges();

        expect(spectator.query(ConfirmPopup)).toExist();

        spectator.query(ConfirmPopup).accept();

        expect(store.promoteVariant).toHaveBeenCalledWith({
            experimentId: EXPERIMENT_ID,
            variant: variant
        });
        expect(spectator.queryAll(byTestId('variant-promoted-tag')).length).toEqual(0);
    });
});
