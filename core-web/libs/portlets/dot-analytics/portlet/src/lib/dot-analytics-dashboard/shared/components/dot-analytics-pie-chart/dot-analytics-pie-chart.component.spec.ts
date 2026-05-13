import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { waitForAsync } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsPieChartComponent } from './dot-analytics-pie-chart.component';

class ResizeObserverMock {
    callback: ResizeObserverCallback;

    constructor(cb: ResizeObserverCallback) {
        this.callback = cb;
    }

    observe(target: Element): void {
        const el = target as HTMLElement;
        const width = el.getBoundingClientRect().width || el.clientWidth || 400;
        this.callback(
            [
                {
                    contentRect: {
                        width,
                        height: width,
                        top: 0,
                        left: 0,
                        bottom: width,
                        right: width,
                        x: 0,
                        y: 0,
                        toJSON: () => ({})
                    }
                } as ResizeObserverEntry
            ],
            this
        );
    }

    unobserve(): void {
        /* test double */
    }

    disconnect(): void {
        /* test double */
    }
}

describe('DotAnalyticsPieChartComponent', () => {
    const OriginalResizeObserver = global.ResizeObserver;

    beforeAll(() => {
        global.ResizeObserver = ResizeObserverMock as unknown as typeof ResizeObserver;
    });

    afterAll(() => {
        global.ResizeObserver = OriginalResizeObserver;
    });

    let spectator: Spectator<DotAnalyticsPieChartComponent>;

    const sampleResults = [{ name: 'Chrome', value: 400 }];

    const createComponent = createComponentFactory({
        component: DotAnalyticsPieChartComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn().mockReturnValue('Translated title')
                }
            }
        ]
    });

    beforeEach(waitForAsync(async () => {
        spectator = createComponent({ detectChanges: false });
        spectator.setInput({
            results: sampleResults,
            status: ComponentStatus.LOADED,
            title: ''
        });

        spectator.detectChanges();
        const measure = spectator.query('.pie-chart__measure-wrap');
        expect(measure).toBeTruthy();
        (measure as HTMLElement).style.width = '400px';
        await spectator.fixture.whenStable();
        spectator.detectChanges();
        spectator.flushEffects();
        await spectator.fixture.whenStable();
        spectator.detectChanges();
        spectator.flushEffects();
    }));

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render D3 pie paths and legend when loaded with data', () => {
        expect(spectator.query('[data-testid="analytics-d3-pie-svg"]')).toExist();
        expect(spectator.query('[data-testid="analytics-d3-pie-svg"] path')).toExist();
        expect(spectator.query('[data-testid="analytics-pie-legend-row"]')).toExist();
        expect(spectator.query('.chart-skeleton--pie')).not.toExist();
    });

    it('should highlight legend row when hovering pie slice', () => {
        const path = spectator.query('[data-testid="analytics-d3-pie-svg"] path');
        const row = spectator.query('[data-testid="analytics-pie-legend-row"]');
        expect(path).toExist();
        expect(row).toExist();

        path.dispatchEvent(new MouseEvent('mouseover', { bubbles: true, cancelable: true }));
        spectator.detectChanges();

        expect(row).toHaveClass('pie-chart__legend-row--active');
        expect(path.getAttribute('data-series')).toBe('Chrome');

        path.dispatchEvent(new MouseEvent('mouseout', { bubbles: true, cancelable: true }));
        spectator.detectChanges();

        expect(row).not.toHaveClass('pie-chart__legend-row--active');
    });

    it('should show pie skeleton when loading', () => {
        spectator.setInput({ status: ComponentStatus.LOADING });
        spectator.detectChanges();

        expect(spectator.query('.chart-skeleton--pie')).toExist();
        expect(spectator.query('[data-testid="analytics-d3-pie-svg"]')).not.toExist();
    });

    it('should show error state when status is ERROR', () => {
        spectator.setInput({ status: ComponentStatus.ERROR });
        spectator.detectChanges();

        expect(spectator.query('dot-analytics-state-message')).toExist();
        expect(spectator.query('[data-testid="analytics-d3-pie-svg"]')).not.toExist();
    });

    it('should show empty state when results are empty', () => {
        spectator.setInput({ results: [], status: ComponentStatus.LOADED });
        spectator.detectChanges();

        expect(spectator.query('dot-analytics-empty-state')).toExist();
        expect(spectator.query('[data-testid="analytics-d3-pie-svg"]')).not.toExist();
    });

    it('should resolve card title when title input is set', () => {
        spectator.setInput({ title: 'analytics.charts.device-breakdown.title' });
        spectator.detectChanges();

        const card = spectator.query('[data-testid="analytics-chart"]');
        expect(card?.querySelector('.p-card-title')?.textContent?.trim()).toBe('Translated title');
    });

    it('should display legend label as the full name without modification', () => {
        spectator.setInput({
            results: [
                { name: 'Chrome', value: 700 },
                { name: 'Desktop', value: 600 }
            ],
            status: ComponentStatus.LOADED
        });
        spectator.detectChanges();

        const legendRows = spectator.queryAll('[data-testid="analytics-pie-legend-row"]');
        const labels = legendRows.map((row) => row.querySelector('.truncate')?.textContent?.trim());

        expect(labels).toEqual(['Chrome', 'Desktop']);
    });
});
