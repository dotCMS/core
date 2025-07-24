import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockModule } from 'ng-mocks';

import { ChartModule, UIChart } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsDashboardChartComponent } from './dot-analytics-dashboard-chart.component';

import { ChartData, ChartType } from '../../types';

describe('DotAnalyticsDashboardChartComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardChartComponent>;

    const mockChartData: ChartData = {
        labels: ['January', 'February', 'March'],
        datasets: [
            {
                label: 'Test Dataset',
                data: [10, 20, 30],
                borderColor: 'rgb(75, 192, 192)',
                backgroundColor: 'rgba(75, 192, 192, 0.2)'
            }
        ]
    };

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
                data: mockChartData,
                title: '',
                width: '100%',
                height: '300px',
                options: {},
                status: ComponentStatus.LOADED
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

        it('should initialize with required data input', () => {
            expect(spectator.component.$data()).toEqual(mockChartData);
        });
    });

    describe('Default Values', () => {
        it('should have default values for optional inputs', () => {
            expect(spectator.component.$title()).toBe('');
            expect(spectator.component.$width()).toBe('100%');
            expect(spectator.component.$height()).toBe('300px');
            expect(spectator.component.$options()).toEqual({});
            expect(spectator.component.$status()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('Chart Title Display', () => {
        it('should not show header when title is empty', () => {
            const header = spectator.query('.chart-title');
            expect(header).toBeFalsy();
        });

        it('should show header with title when title is provided', () => {
            // Create new component with title
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    data: mockChartData,
                    title: 'Test Chart Title',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.LOADED
                } as unknown
            });

            const header = spectator.query('.chart-title');
            expect(header).toExist();
            expect(header).toHaveText('Test Chart Title');
        });
    });

    describe('Loading State', () => {
        it('should show loading skeleton when status is LOADING', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    data: mockChartData,
                    title: '',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.LOADING
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
                    data: mockChartData,
                    title: '',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.INIT
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
                    data: mockChartData,
                    title: '',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.LOADING
                } as unknown
            });

            const lineSkeleton = spectator.query('.chart-skeleton--line');
            expect(lineSkeleton).toExist();
        });

        it('should show pie chart skeleton for pie chart type', () => {
            spectator = createComponent({
                props: {
                    type: 'pie' as ChartType,
                    data: mockChartData,
                    title: '',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.LOADING
                } as unknown
            });

            const pieSkeleton = spectator.query('.chart-skeleton--pie');
            expect(pieSkeleton).toExist();
        });

        it('should show pie chart skeleton for doughnut chart type', () => {
            spectator = createComponent({
                props: {
                    type: 'doughnut' as ChartType,
                    data: mockChartData,
                    title: '',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.LOADING
                } as unknown
            });

            const pieSkeleton = spectator.query('.chart-skeleton--pie');
            expect(pieSkeleton).toExist();
        });

        it('should show default skeleton for other chart types', () => {
            spectator = createComponent({
                props: {
                    type: 'bar' as ChartType,
                    data: mockChartData,
                    title: '',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.LOADING
                } as unknown
            });

            const defaultSkeleton = spectator.query('.chart-skeleton--default');
            expect(defaultSkeleton).toExist();
        });

        it('should hide chart when loading', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    data: mockChartData,
                    title: '',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.LOADING
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
                    data: mockChartData,
                    title: '',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.ERROR
                } as unknown
            });

            const errorElement = spectator.query('.chart-error');
            expect(errorElement).toExist();
            expect(spectator.query('.error-message')).toExist();
            expect(spectator.query(UIChart)).not.toExist();
        });

        it('should show error icon when in error state', () => {
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    data: mockChartData,
                    title: '',
                    width: '100%',
                    height: '300px',
                    options: {},
                    status: ComponentStatus.ERROR
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
                    data: mockChartData,
                    status: ComponentStatus.INIT
                } as unknown
            });
            expect(spectator.component['$isLoading']()).toBe(true);

            // Test LOADING state
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    data: mockChartData,
                    status: ComponentStatus.LOADING
                } as unknown
            });
            expect(spectator.component['$isLoading']()).toBe(true);

            // Test LOADED state
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    data: mockChartData,
                    status: ComponentStatus.LOADED
                } as unknown
            });
            expect(spectator.component['$isLoading']()).toBe(false);
        });

        it('should correctly identify error state', () => {
            // Test ERROR state
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    data: mockChartData,
                    status: ComponentStatus.ERROR
                } as unknown
            });
            expect(spectator.component['$isError']()).toBe(true);

            // Test non-ERROR state
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    data: mockChartData,
                    status: ComponentStatus.LOADED
                } as unknown
            });
            expect(spectator.component['$isError']()).toBe(false);
        });
    });

    describe('Chart Options Configuration', () => {
        it('should have default chart options', () => {
            const options = spectator.component['$chartOptions']();

            expect(options.responsive).toBe(true);
            expect(options.maintainAspectRatio).toBe(false);
            expect(options.plugins?.legend?.display).toBe(true);
            expect(options.plugins?.legend?.position).toBe('bottom');
        });
    });

    describe('Custom Dimensions', () => {
        it('should accept custom width and height', () => {
            // Create new component with custom dimensions
            spectator = createComponent({
                props: {
                    type: 'line' as ChartType,
                    data: mockChartData,
                    title: '',
                    width: '600px',
                    height: '400px',
                    options: {},
                    status: ComponentStatus.LOADED
                } as unknown
            });

            expect(spectator.component.$width()).toBe('600px');
            expect(spectator.component.$height()).toBe('400px');
        });
    });
});
