import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockModule } from 'ng-mocks';

import { ChartModule, UIChart } from 'primeng/chart';

import { DotMessageService } from '@dotcms/data-access';
import {
    CHARTJS_DATA_MOCK_EMPTY,
    DAILY_CHARTJS_DATA_MOCK_WITH_DATA,
    DotMessagePipe,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsReportsChartComponent } from './dot-experiments-reports-chart.component';

import * as Utilities from '../../../shared/dot-experiment.utils';

const messageServiceMock = new MockDotMessageService({
    'experiments.reports.chart.empty.title': 'x axis label',
    'experiments.reports.chart.empty.description': 'y axis label'
});

// spyOn an exported function with Jest
jest.spyOn(Utilities, 'getRandomUUID').mockReturnValue('1-2-3-4-5');

describe('DotExperimentsReportsChartComponent', () => {
    let spectator: Spectator<DotExperimentsReportsChartComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsReportsChartComponent,
        overrideComponents: [
            [
                DotExperimentsReportsChartComponent,
                {
                    remove: { imports: [ChartModule] },
                    add: { imports: [MockModule(ChartModule)] }
                }
            ]
        ],
        imports: [DotMessagePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
    });

    it('should has title, legends container and PrimeNG Chart Component', () => {
        spectator.setInput({
            isLoading: false,
            isEmpty: false,
            data: DAILY_CHARTJS_DATA_MOCK_WITH_DATA,
            config: {
                xAxisLabel: 'experiments.chart.xAxisLabel',
                yAxisLabel: 'experiments.chart.yAxisLabel'
            }
        });

        expect(spectator.query(byTestId('chart-legends'))).toExist();
        expect(spectator.query(UIChart)).toExist();
    });

    it('should show the loading state', () => {
        spectator.setInput({
            isLoading: true
        });
        expect(spectator.query(byTestId('loading-skeleton'))).toExist();
    });
    it('should show the empty state', () => {
        spectator.setInput({
            isLoading: false,
            data: CHARTJS_DATA_MOCK_EMPTY,
            config: {
                xAxisLabel: 'experiments.chart.xAxisLabel',
                yAxisLabel: 'experiments.chart.yAxisLabel'
            }
        });

        expect(spectator.query(byTestId('empty-data-msg'))).toExist();
    });
});
