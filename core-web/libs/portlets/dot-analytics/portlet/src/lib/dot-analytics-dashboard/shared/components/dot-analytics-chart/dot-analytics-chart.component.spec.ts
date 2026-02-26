import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockModule } from 'ng-mocks';

import { ChartModule, UIChart } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsChartComponent } from './dot-analytics-chart.component';

import { ChartData, ChartType } from '../../types';

describe('DotAnalyticsChartComponent', () => {
    let spectator: Spectator<DotAnalyticsChartComponent>;

    const createMockChartData = (): ChartData => ({
        labels: ['2024-01-01', '2024-01-02', '2024-01-03'],
        datasets: [
            {
                label: 'analytics.charts.pageviews-timeline.dataset-label',
                data: [10, 20, 30],
                borderColor: '#3B82F6',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                borderWidth: 2,
                fill: true,
                tension: 0.4
            }
        ]
    });

    const createComponent = createComponentFactory({
        component: DotAnalyticsChartComponent,
        overrideComponents: [
            [
                DotAnalyticsChartComponent,
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
        spectator = createComponent({ detectChanges: false });
        spectator.setInput({
            type: 'line' as ChartType,
            data: createMockChartData(),
            status: ComponentStatus.LOADED,
            options: {}
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
            expect(spectator.component.$data()).toEqual(createMockChartData());
        });

        it('should initialize with status input', () => {
            // beforeEach sets status to LOADED
            expect(spectator.component.$status()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('Default Values', () => {
        it('should initialize with empty title by default and empty options', () => {
            expect(spectator.component.$title()).toBe('');
            expect(spectator.component.$options()).toEqual({});
        });

        it('should calculate height automatically based on chart type', () => {
            expect(spectator.component['$height']()).toBeDefined();
            expect(typeof spectator.component['$height']()).toBe('string');
        });
    });

    describe('Chart Title Input', () => {
        it('should display title above the card when provided', () => {
            spectator.setInput('title', 'analytics.chart.title');
            spectator.detectChanges();

            const title = spectator.query('[data-testid="chart-title"]');
            expect(title).toExist();
            expect(title.tagName).toBe('H3');
        });

        it('should not display title when not provided', () => {
            spectator.setInput('title', '');
            spectator.detectChanges();

            const title = spectator.query('[data-testid="chart-title"]');
            expect(title).not.toExist();
        });

        it('should render title outside the p-card element', () => {
            spectator.setInput('title', 'analytics.chart.title');
            spectator.detectChanges();

            const title = spectator.query('[data-testid="chart-title"]');
            const card = spectator.query('[data-testid="analytics-chart"]');

            expect(title).toExist();
            expect(card).toExist();
            expect(card.contains(title)).toBe(false);
        });
    });

    describe('Loading State', () => {
        it('should show loading skeleton when status is LOADING', () => {
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            const skeleton = spectator.query('.chart-skeleton');
            expect(skeleton).toExist();
            expect(spectator.query(UIChart)).not.toExist();
        });

        it('should show loading skeleton when status is INIT', () => {
            spectator.setInput('status', ComponentStatus.INIT);
            spectator.detectChanges();

            const skeleton = spectator.query('.chart-skeleton');
            expect(skeleton).toExist();
            expect(spectator.query(UIChart)).not.toExist();
        });

        it('should show line chart skeleton for line chart type', () => {
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            const lineSkeleton = spectator.query('.chart-skeleton--line');
            expect(lineSkeleton).toExist();
        });

        it('should show pie chart skeleton for pie chart type', () => {
            spectator.setInput('type', 'pie' as ChartType);
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            const pieSkeleton = spectator.query('.chart-skeleton--pie');
            expect(pieSkeleton).toExist();
        });

        it('should show pie chart skeleton for doughnut chart type', () => {
            spectator.setInput('type', 'doughnut' as ChartType);
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            const pieSkeleton = spectator.query('.chart-skeleton--pie');
            expect(pieSkeleton).toExist();
        });

        it('should show default skeleton for other chart types', () => {
            spectator.setInput('type', 'bar' as ChartType);
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            const defaultSkeleton = spectator.query('.chart-skeleton--default');
            expect(defaultSkeleton).toExist();
        });

        it('should hide chart when loading', () => {
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query(UIChart)).not.toExist();
        });
    });

    describe('Error State', () => {
        it('should show error message when status is ERROR', () => {
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();

            const errorElement = spectator.query('.chart-error');
            expect(errorElement).toExist();
            expect(spectator.query('dot-analytics-state-message')).toExist();
            expect(spectator.query(UIChart)).not.toExist();
        });

        it('should show error icon when in error state', () => {
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();

            const errorIcon = spectator.query('.pi-exclamation-triangle');
            expect(errorIcon).toExist();
        });
    });

    describe('Computed Properties', () => {
        it('should correctly identify loading state', () => {
            spectator.setInput('status', ComponentStatus.INIT);
            spectator.detectChanges();
            expect(spectator.query('.chart-skeleton')).toExist();

            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();
            expect(spectator.query('.chart-skeleton')).toExist();

            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.query('.chart-skeleton')).not.toExist();
        });

        it('should correctly identify error state', () => {
            spectator.setInput('status', ComponentStatus.ERROR);
            spectator.detectChanges();
            expect(spectator.query('.chart-error')).toExist();

            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();
            expect(spectator.query('.chart-error')).not.toExist();
        });

        it('should correctly identify empty state', () => {
            spectator.setInput('data', { labels: [], datasets: [] });
            spectator.detectChanges();
            expect(spectator.query('dot-analytics-empty-state')).toExist();

            spectator.setInput('data', createMockChartData());
            spectator.detectChanges();
            expect(spectator.query('dot-analytics-empty-state')).not.toExist();
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

        it('should show legend for pie chart types', () => {
            // Update component to use pie chart type (which should show legend)
            spectator.setInput('type', 'pie');
            spectator.detectChanges();

            const options = spectator.component['$chartOptions']();

            expect(options.responsive).toBe(true);
            expect(options.maintainAspectRatio).toBe(false);
            // Pie charts should show legend
            expect(options.plugins?.legend?.display).toBe(true);
            // Position can be 'bottom' or 'right' depending on mobile breakpoint
            expect(['bottom', 'right']).toContain(options.plugins?.legend?.position);
        });
    });

    describe('Custom Dimensions', () => {
        it('should calculate height automatically based on chart type', () => {
            expect(spectator.component['$height']()).toBeDefined();
            expect(typeof spectator.component['$height']()).toBe('string');

            // Test pie chart height
            spectator.setInput('type', 'pie' as ChartType);
            spectator.detectChanges();

            expect(spectator.component['$height']()).toBeDefined();
            expect(typeof spectator.component['$height']()).toBe('string');
        });
    });

    describe('Empty State', () => {
        it('should show empty state when data is empty', () => {
            spectator.setInput('data', { labels: [], datasets: [] });
            spectator.detectChanges();

            const emptyState = spectator.query('dot-analytics-empty-state');
            expect(emptyState).toExist();
            expect(spectator.query(UIChart)).not.toExist();
        });

        it('should show empty state icon and messages', () => {
            spectator.setInput('data', { labels: [], datasets: [] });
            spectator.detectChanges();

            const emptyState = spectator.query('dot-analytics-empty-state');
            expect(emptyState).toExist();
        });

        it('should not show empty state when data is available', () => {
            const emptyState = spectator.query('dot-analytics-empty-state');
            expect(emptyState).not.toExist();
            expect(spectator.query(UIChart)).toExist();
        });
    });
});
