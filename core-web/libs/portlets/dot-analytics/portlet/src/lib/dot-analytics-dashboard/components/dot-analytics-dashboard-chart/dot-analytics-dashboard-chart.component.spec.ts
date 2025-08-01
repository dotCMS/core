import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockModule } from 'ng-mocks';

import { ChartModule, UIChart } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { PageViewTimeLineEntity, RequestState } from '@dotcms/portlets/dot-analytics/data-access';

import { DotAnalyticsDashboardChartComponent } from './dot-analytics-dashboard-chart.component';

import { ChartType } from '../../types';

describe('DotAnalyticsDashboardChartComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardChartComponent>;

    const mockTimeLineData: PageViewTimeLineEntity[] = [
        {
            'request.totalRequest': '10',
            'request.createdAt': '2024-01-01T00:00:00Z',
            'request.createdAt.day': '2024-01-01'
        },
        {
            'request.totalRequest': '20',
            'request.createdAt': '2024-01-02T00:00:00Z',
            'request.createdAt.day': '2024-01-02'
        },
        {
            'request.totalRequest': '30',
            'request.createdAt': '2024-01-03T00:00:00Z',
            'request.createdAt.day': '2024-01-03'
        }
    ];

    const createMockChartState = (
        data: PageViewTimeLineEntity[] | null = mockTimeLineData,
        status: ComponentStatus = ComponentStatus.LOADED
    ): RequestState<PageViewTimeLineEntity[]> => ({
        data,
        status,
        error: null
    });

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardChartComponent,
        overrideComponents: [
            [
                DotAnalyticsDashboardChartComponent,
                {
                    remove: { imports: [ChartModule, SkeletonModule] },
                    add: { imports: [MockModule(ChartModule), MockModule(SkeletonModule)] }
                }
            ]
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn().mockReturnValue('Translated message')
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                type: 'line' as ChartType,
                chartState: createMockChartState(),
                title: 'Test Chart',
                options: {}
            } as unknown
        });
    });

    describe('Component Initialization', () => {
        it('should create component successfully', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have chart element when loaded', () => {
            expect(spectator.query(UIChart)).toExist();
        });
    });

    describe('Required Inputs', () => {
        it('should initialize with required type input', () => {
            expect(spectator.component.$type()).toBe('line');
        });

        it('should initialize with required chartState input', () => {
            expect(spectator.component.$chartState()).toEqual(createMockChartState());
        });
    });

    describe('Default Values', () => {
        it('should initialize with provided title and default options', () => {
            expect(spectator.component.$title()).toBe('Test Chart');
            expect(spectator.component.$options()).toEqual({});
        });

        it('should calculate height automatically based on chart type', () => {
            expect(spectator.component['$height']()).toBeDefined();
            expect(typeof spectator.component['$height']()).toBe('string');
        });
    });

    describe('Chart Title Display', () => {
        it('should show header with the provided title', () => {
            const header = spectator.query('.chart-title');
            expect(header).toExist();
            expect(header).toHaveText('Test Chart');
        });

        it('should update header when title changes', () => {
            // Update the title
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(),
                    title: 'Updated Chart Title',
                    options: {}
                } as unknown
            });

            const header = spectator.query('.chart-title');
            expect(header).toExist();
            expect(header).toHaveText('Updated Chart Title');
        });
    });

    describe('Loading State', () => {
        it('should show loading skeleton when status is LOADING', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADING),
                    title: 'Test Chart',

                    options: {}
                } as unknown
            });

            const skeleton = spectator.query('.chart-skeleton');
            expect(skeleton).toExist();
            expect(spectator.query(UIChart)).not.toExist();
        });

        it('should show loading skeleton when status is INIT', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.INIT),
                    title: 'Test Chart',

                    options: {}
                } as unknown
            });

            const skeleton = spectator.query('.chart-skeleton');
            expect(skeleton).toExist();
            expect(spectator.query(UIChart)).not.toExist();
        });

        it('should show line chart skeleton for line chart type', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADING),
                    title: 'Test Chart',

                    options: {}
                } as unknown
            });

            const lineSkeleton = spectator.query('.chart-skeleton--line');
            expect(lineSkeleton).toExist();
        });

        it('should show pie chart skeleton for pie chart type', () => {
            spectator = createComponent({
                props: {
                    type: 'pie' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADING),
                    title: 'Test Chart',

                    options: {}
                } as unknown
            });

            const pieSkeleton = spectator.query('.chart-skeleton--pie');
            expect(pieSkeleton).toExist();
        });

        it('should show pie chart skeleton for doughnut chart type', () => {
            spectator = createComponent({
                props: {
                    type: 'doughnut' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADING),
                    title: 'Test Chart',

                    options: {}
                } as unknown
            });

            const pieSkeleton = spectator.query('.chart-skeleton--pie');
            expect(pieSkeleton).toExist();
        });

        it('should show default skeleton for other chart types', () => {
            spectator = createComponent({
                props: {
                    type: 'bar' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADING),
                    title: 'Test Chart',

                    options: {}
                } as unknown
            });

            const defaultSkeleton = spectator.query('.chart-skeleton--default');
            expect(defaultSkeleton).toExist();
        });

        it('should hide chart when loading', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADING),
                    title: 'Test Chart',

                    options: {}
                } as unknown
            });

            expect(spectator.query(UIChart)).not.toExist();
        });
    });

    describe('Error State', () => {
        it('should show error message when status is ERROR', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.ERROR),
                    title: 'Test Chart',

                    options: {}
                } as unknown
            });

            const errorElement = spectator.query('.chart-error');
            expect(errorElement).toExist();
            expect(spectator.query('dot-analytics-state-message')).toExist();
            expect(spectator.query(UIChart)).not.toExist();
        });

        it('should show error icon when in error state', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.ERROR),
                    title: 'Test Chart',

                    options: {}
                } as unknown
            });

            const errorIcon = spectator.query('.pi-exclamation-triangle');
            expect(errorIcon).toExist();
        });
    });

    describe('Computed Properties', () => {
        it('should correctly identify loading state', () => {
            // Test INIT state
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.INIT),
                    title: 'Test Chart'
                } as unknown
            });
            expect(spectator.component['$isLoading']()).toBe(true);

            // Test LOADING state
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADING),
                    title: 'Test Chart'
                } as unknown
            });
            expect(spectator.component['$isLoading']()).toBe(true);

            // Test LOADED state
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADED),
                    title: 'Test Chart'
                } as unknown
            });
            expect(spectator.component['$isLoading']()).toBe(false);
        });

        it('should correctly identify error state', () => {
            // Test ERROR state
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.ERROR),
                    title: 'Test Chart'
                } as unknown
            });
            expect(spectator.component['$isError']()).toBe(true);

            // Test non-ERROR state
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADED),
                    title: 'Test Chart'
                } as unknown
            });
            expect(spectator.component['$isError']()).toBe(false);
        });

        it('should correctly identify empty state', () => {
            // Test with empty data array
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState([], ComponentStatus.LOADED),
                    title: 'Test Chart'
                } as unknown
            });
            expect(spectator.component['$isEmpty']()).toBe(true);

            // Test with null data
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(null, ComponentStatus.LOADED),
                    title: 'Test Chart'
                } as unknown
            });
            expect(spectator.component['$isEmpty']()).toBe(true);

            // Test with valid data
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADED),
                    title: 'Test Chart'
                } as unknown
            });
            expect(spectator.component['$isEmpty']()).toBe(false);
        });
    });

    describe('Chart Options Configuration', () => {
        it('should have default chart options with hidden legend for line charts', () => {
            // This test uses 'line' chart type by default (set in beforeEach)
            const options = spectator.component['$chartOptions']();

            expect(options.responsive).toBe(true);
            expect(options.maintainAspectRatio).toBe(false);
            // Line charts should have hidden legend
            expect(options.plugins?.legend?.display).toBe(false);
            expect(options.plugins?.legend?.position).toBe('bottom');
        });

        it('should show legend for non-line chart types', () => {
            // Update component to use a different chart type
            spectator.setInput('type', 'bar');
            spectator.detectChanges();

            const options = spectator.component['$chartOptions']();

            expect(options.responsive).toBe(true);
            expect(options.maintainAspectRatio).toBe(false);
            // Non-line charts should show legend
            expect(options.plugins?.legend?.display).toBe(true);
            expect(options.plugins?.legend?.position).toBe('bottom');
        });
    });

    describe('Custom Dimensions', () => {
        it('should calculate height automatically based on chart type', () => {
            // Test line chart height
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(),
                    title: 'Test Chart'
                } as unknown
            });

            expect(spectator.component['$height']()).toBeDefined();
            expect(typeof spectator.component['$height']()).toBe('string');

            // Test pie chart height
            spectator = createComponent({
                props: {
                    type: 'pie' as ChartType,
                    chartState: createMockChartState(),
                    title: 'Test Chart'
                } as unknown
            });

            expect(spectator.component['$height']()).toBeDefined();
            expect(typeof spectator.component['$height']()).toBe('string');
        });
    });

    describe('Empty State', () => {
        it('should show empty state when data is empty', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState([], ComponentStatus.LOADED),
                    title: 'Test Chart'
                } as unknown
            });

            const emptyState = spectator.query('.chart-empty');
            expect(emptyState).toExist();
            expect(spectator.query(UIChart)).not.toExist();
        });

        it('should show empty state icon and messages', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState([], ComponentStatus.LOADED),
                    title: 'Test Chart'
                } as unknown
            });

            const stateMessage = spectator.query('dot-analytics-state-message');
            expect(stateMessage).toExist();
        });

        it('should not show empty state when data is available', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    chartState: createMockChartState(mockTimeLineData, ComponentStatus.LOADED),
                    title: 'Test Chart'
                } as unknown
            });

            const emptyState = spectator.query('.chart-empty');
            expect(emptyState).not.toExist();
            expect(spectator.query(UIChart)).toExist();
        });
    });
});
