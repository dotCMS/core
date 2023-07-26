import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, Input, NgModule } from '@angular/core';

import { ChartModule } from 'primeng/chart';

import { DotMessageService } from '@dotcms/data-access';
import {
    CHARTJS_DATA_MOCK_EMPTY,
    CHARTJS_DATA_MOCK_WITH_DATA,
    MockDotMessageService
} from '@dotcms/utils-testing';
import { DotMessagePipe } from '@tests/dot-message-mock.pipe';

import { DotExperimentsReportsChartComponent } from './dot-experiments-reports-chart.component';

import * as Utilities from '../../../shared/dot-experiment.utils';

const messageServiceMock = new MockDotMessageService({
    'experiments.reports.chart.empty.title': 'x axis label',
    'experiments.reports.chart.empty.description': 'y axis label'
});

// TODO: Use ng-mocks to mock the component automatically
@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'p-chart',
    template: 'chart - mocked component'
})
// eslint-disable-next-line @angular-eslint/component-class-suffix
class MockUIChart {
    @Input()
    data: unknown;
    @Input()
    options: unknown;
    @Input()
    plugins: unknown;
}

@NgModule({
    declarations: [MockUIChart],
    exports: [MockUIChart]
})
export class MockChartModule {}

// spyOn an exported function with Jest
jest.spyOn(Utilities, 'getRandomUUID').mockReturnValue('1234');

describe('DotExperimentsReportsChartComponent', () => {
    let spectator: Spectator<DotExperimentsReportsChartComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsReportsChartComponent,
        overrideComponents: [
            [
                DotExperimentsReportsChartComponent,
                {
                    remove: { imports: [ChartModule] },
                    add: { imports: [MockChartModule] }
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
            data: CHARTJS_DATA_MOCK_WITH_DATA,
            config: {
                xAxisLabel: 'experiments.chart.xAxisLabel',
                yAxisLabel: 'experiments.chart.yAxisLabel'
            }
        });

        expect(spectator.query(byTestId('chart-legends'))).toExist();
        expect(spectator.query(MockUIChart)).toExist();
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
