import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { AnalyticsChartColors } from '../../types';

import {
    DotAnalyticsSparklineComponent,
    SparklineDataPoint
} from './dot-analytics-sparkline.component';

describe('DotAnalyticsSparklineComponent', () => {
    let spectator: Spectator<DotAnalyticsSparklineComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsSparklineComponent
    });

    const mockData: SparklineDataPoint[] = [
        { date: 'Oct 1', value: 10 },
        { date: 'Oct 2', value: 25 },
        { date: 'Oct 3', value: 15 },
        { date: 'Oct 4', value: 30 },
        { date: 'Oct 5', value: 22 },
        { date: 'Oct 6', value: 35 },
        { date: 'Oct 7', value: 28 }
    ];

    beforeEach(() => {
        spectator = createComponent({
            props: {
                data: mockData,
                status: ComponentStatus.LOADED
            } as unknown,
            detectChanges: false
        });
    });

    describe('Component Initialization', () => {
        it('should create component successfully', () => {
            spectator.detectChanges();
            expect(spectator.component).toBeTruthy();
        });

        it('should render p-chart element', () => {
            spectator.detectChanges();
            expect(spectator.query('p-chart')).toExist();
        });

        it('should pass height to p-chart', () => {
            spectator.detectChanges();
            const chart = spectator.query('p-chart');
            expect(chart).toExist();
        });
    });

    describe('Default Values', () => {
        it('should use default primary color', () => {
            spectator.detectChanges();
            const chartData = spectator.component['$chartData']();
            expect(chartData.datasets[0].borderColor).toBe(AnalyticsChartColors.primary.line);
        });

        it('should have fill enabled by default', () => {
            spectator.detectChanges();
            const chartData = spectator.component['$chartData']();
            expect(chartData.datasets[0].fill).toBe(true);
        });

        it('should have default height of 4.5rem', () => {
            spectator.detectChanges();
            expect(spectator.component.$height()).toBe('4.5rem');
        });

        it('should have interactive enabled by default', () => {
            spectator.detectChanges();
            expect(spectator.component.$interactive()).toBe(true);
        });

        it('should have plugins configured', () => {
            spectator.detectChanges();
            expect(spectator.component.chartPlugins).toBeDefined();
            expect(spectator.component.chartPlugins.length).toBe(2);
            expect(spectator.component.chartPlugins[0].id).toBe('gradientFill');
            expect(spectator.component.chartPlugins[1].id).toBe('lineDrawAnimation');
        });
    });

    describe('Custom Inputs', () => {
        it('should apply custom color', () => {
            spectator.setInput('color', '#FF0000');
            spectator.detectChanges();

            const chartData = spectator.component['$chartData']();
            expect(chartData.datasets[0].borderColor).toBe('#FF0000');
        });

        it('should apply custom height', () => {
            spectator.setInput('height', '5rem');
            spectator.detectChanges();

            expect(spectator.component.$height()).toBe('5rem');
        });

        it('should disable fill when filled is false', () => {
            spectator.setInput('filled', false);
            spectator.detectChanges();

            const chartData = spectator.component['$chartData']();
            expect(chartData.datasets[0].fill).toBe(false);
        });
    });

    describe('Chart Data', () => {
        it('should transform data array into chart data format', () => {
            spectator.detectChanges();

            const chartData = spectator.component['$chartData']();
            expect(chartData.datasets[0].data).toEqual(mockData.map((d) => d.value));
            expect(chartData.labels).toEqual(mockData.map((d) => d.date));
        });

        it('should have fill enabled', () => {
            spectator.detectChanges();

            const chartData = spectator.component['$chartData']();
            expect(chartData.datasets[0].fill).toBe(true);
        });

        it('should have fallback background color (gradient applied via plugin)', () => {
            spectator.detectChanges();

            const chartData = spectator.component['$chartData']();
            // Fallback color with opacity until plugin applies gradient
            expect(chartData.datasets[0].backgroundColor).toContain('rgba');
        });
    });

    describe('Chart Options', () => {
        it('should have legend disabled', () => {
            spectator.detectChanges();

            const options = spectator.component['$chartOptions']();
            expect(options.plugins?.legend?.display).toBe(false);
        });

        it('should have tooltip enabled by default (interactive)', () => {
            spectator.detectChanges();

            const options = spectator.component['$chartOptions']();
            expect(options.plugins?.tooltip?.enabled).toBe(true);
        });

        it('should have tooltip disabled when not interactive', () => {
            spectator.setInput('interactive', false);
            spectator.detectChanges();

            const options = spectator.component['$chartOptions']();
            expect(options.plugins?.tooltip?.enabled).toBe(false);
        });

        it('should have axes hidden', () => {
            spectator.detectChanges();

            const options = spectator.component['$chartOptions']();
            const scales = options.scales as Record<string, { display?: boolean }>;
            expect(scales?.x?.display).toBe(false);
            expect(scales?.y?.display).toBe(false);
        });

        it('should be responsive', () => {
            spectator.detectChanges();

            const options = spectator.component['$chartOptions']();
            expect(options.responsive).toBe(true);
            expect(options.maintainAspectRatio).toBe(false);
        });

        it('should have hover radius when interactive', () => {
            spectator.detectChanges();

            const options = spectator.component['$chartOptions']();
            expect(options.elements?.point?.hoverRadius).toBe(6);
        });

        it('should have no hover radius when not interactive', () => {
            spectator.setInput('interactive', false);
            spectator.detectChanges();

            const options = spectator.component['$chartOptions']();
            expect(options.elements?.point?.hoverRadius).toBe(0);
        });
    });

    describe('Loading State', () => {
        it('should show skeleton when status is LOADING', () => {
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query('.sparkline-skeleton')).toExist();
            expect(spectator.query('p-skeleton')).toExist();
            expect(spectator.query('p-chart')).not.toExist();
        });

        it('should show skeleton when status is INIT', () => {
            spectator.setInput('status', ComponentStatus.INIT);
            spectator.detectChanges();

            expect(spectator.query('.sparkline-skeleton')).toExist();
            expect(spectator.query('p-chart')).not.toExist();
        });

        it('should show chart when status is LOADED', () => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            expect(spectator.query('p-chart')).toExist();
            expect(spectator.query('.sparkline-skeleton')).not.toExist();
        });
    });
});
