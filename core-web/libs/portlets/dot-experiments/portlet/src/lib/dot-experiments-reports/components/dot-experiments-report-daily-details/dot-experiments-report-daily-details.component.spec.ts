import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

import { DotExperimentsReportDailyDetailsComponent } from './dot-experiments-report-daily-details.component';

import { DotExperimentsDetailsTableComponent } from '../../../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';
import { DotExperimentsReportsStore } from '../../store/dot-experiments-reports-store';

describe('DotExperimentsReportDailyDetailsComponent', () => {
    let spectator: Spectator<DotExperimentsReportDailyDetailsComponent>;
    const createComponent = createComponentFactory({
        component: DotExperimentsReportDailyDetailsComponent,
        providers: [
            mockProvider(ConfirmationService),
            mockProvider(DotExperimentsReportsStore),
            mockProvider(DotMessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.query(DotExperimentsDetailsTableComponent)).toExist();
    });
});
