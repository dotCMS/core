import { DotExperimentsReportDailyDetailsComponent } from './dot-experiments-report-daily-details.component';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockProvider } from 'ng-mocks';
import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentsReportsStore } from '../../store/dot-experiments-reports-store';
import { ConfirmationService } from 'primeng/api';
import { DotExperimentsDetailsTableComponent } from '../../../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';

describe('DotExperimentsReportDailyDetailsComponent', () => {
    let spectator: Spectator<DotExperimentsReportDailyDetailsComponent>;
    const createComponent = createComponentFactory({
        component: DotExperimentsReportDailyDetailsComponent,
        providers: [
            MockProvider(ConfirmationService),
            MockProvider(DotExperimentsReportsStore),
            MockProvider(DotMessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.query(DotExperimentsDetailsTableComponent)).toExist();
    });
});
