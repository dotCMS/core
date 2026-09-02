import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';
import { ChartData } from 'chart.js';
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

import * as Utilities from '../../dot-experiment-results.utils';

const messageServiceMock = new MockDotMessageService({
    'experiments.reports.chart.empty.title': 'x axis label',
    'experiments.reports.chart.empty.description': 'y axis label'
});

// spyOn an exported function with Jest
jest.spyOn(Utilities, 'getRandomUUID').mockReturnValue('1-2-3-4-5');

/** Two series, because the point of the legend is telling one from the other. */
const TWO_SERIES: ChartData<'line'> = {
    labels: ['Mon', 'Tue'],
    datasets: [
        { label: 'Original', data: [1, 2], backgroundColor: 'rgb(1, 2, 3)' },
        { label: 'Variant A', data: [3, 4], backgroundColor: 'rgb(4, 5, 6)' }
    ]
};

const AXIS_CONFIG = {
    xAxisLabel: 'experiments.chart.xAxisLabel',
    yAxisLabel: 'experiments.chart.yAxisLabel'
};

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
            isLoading: true,
            data: DAILY_CHARTJS_DATA_MOCK_WITH_DATA
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

    describe('legend', () => {
        const render = () => {
            spectator.setInput({ isLoading: false, isEmpty: false, data: TWO_SERIES });
            spectator.setInput({ config: AXIS_CONFIG });
            spectator.detectChanges();
        };

        const items = () => spectator.queryAll(byTestId('chart-legend-item'));

        const hiddenFlags = (): (boolean | undefined)[] =>
            (spectator.query(UIChart)?.data?.datasets ?? []).map(
                (dataset: { hidden?: boolean }) => dataset.hidden
            );

        it('draws one tag per series, labelled and swatched with its colour', () => {
            render();

            expect(items().map((item) => item.textContent?.trim())).toEqual([
                'Original',
                'Variant A'
            ]);
            expect(
                items().map(
                    (item) => (item.querySelector('span span') as HTMLElement).style.backgroundColor
                )
            ).toEqual(['rgb(1, 2, 3)', 'rgb(4, 5, 6)']);
        });

        it('starts with every series showing', () => {
            render();

            expect(items().map((item) => item.getAttribute('aria-pressed'))).toEqual([
                'true',
                'true'
            ]);
            expect(hiddenFlags()).toEqual([false, false]);
        });

        it('hides only the series whose tag was clicked', () => {
            render();

            spectator.click(items()[0]);
            spectator.detectChanges();

            expect(items().map((item) => item.getAttribute('aria-pressed'))).toEqual([
                'false',
                'true'
            ]);
            // The chart is redrawn from its input, so the toggle has to reach the dataset itself.
            expect(hiddenFlags()).toEqual([true, false]);
        });

        it('brings a hidden series back on a second click', () => {
            render();

            spectator.click(items()[0]);
            spectator.detectChanges();
            spectator.click(items()[0]);
            spectator.detectChanges();

            expect(items()[0].getAttribute('aria-pressed')).toBe('true');
            expect(hiddenFlags()).toEqual([false, false]);
        });

        it('toggles from the keyboard, so the legend is not mouse-only', () => {
            render();

            spectator.dispatchKeyboardEvent(items()[1], 'keydown', 'Enter');
            spectator.detectChanges();

            expect(hiddenFlags()).toEqual([false, true]);
        });
    });
});
