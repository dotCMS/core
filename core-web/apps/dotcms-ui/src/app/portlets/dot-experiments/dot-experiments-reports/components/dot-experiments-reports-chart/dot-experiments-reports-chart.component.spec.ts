import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { ChartModule, UIChart } from 'primeng/chart';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessagePipe } from '@tests/dot-message-mock.pipe';

import { DotExperimentsReportsChartComponent } from './dot-experiments-reports-chart.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.reports.daily-results': 'Daily results'
});
describe('DotExperimentsReportsChartComponent', () => {
    let spectator: Spectator<DotExperimentsReportsChartComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsReportsChartComponent,
        imports: [ChartModule, DotMessagePipe],
        componentProviders: [],
        declarations: [],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
    });

    it('should has title, legends container and PrimeNG Chart Component', () => {
        spectator.detectChanges();
        expect(spectator.query(byTestId('chart-title'))).toContainText('Daily results');
        expect(spectator.query(byTestId('chart-legends'))).toExist();
        expect(spectator.query(UIChart)).toExist();
    });
});
3;
