import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { waitForAsync } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';

import {
    DotAnalyticsBarChartComponent,
    MAX_BAR_THICKNESS
} from './dot-analytics-bar-chart.component';

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
                        height: 200,
                        top: 0,
                        left: 0,
                        bottom: 200,
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

const SAMPLE_DATA: EngagementPlatformMetrics[] = [
    { name: 'Chrome', views: 750, percentage: 75, time: '2m 10s' },
    { name: 'Firefox', views: 100, percentage: 10, time: '1m 30s' },
    { name: 'Edge', views: 80, percentage: 8, time: '1m 05s' },
    { name: 'Safari', views: 70, percentage: 7, time: '0m 50s' }
];

describe('DotAnalyticsBarChartComponent', () => {
    const OriginalResizeObserver = global.ResizeObserver;

    beforeAll(() => {
        global.ResizeObserver = ResizeObserverMock as unknown as typeof ResizeObserver;
    });

    afterAll(() => {
        global.ResizeObserver = OriginalResizeObserver;
    });

    let spectator: Spectator<DotAnalyticsBarChartComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsBarChartComponent,
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
            data: SAMPLE_DATA,
            status: ComponentStatus.LOADED,
            title: ''
        });

        spectator.detectChanges();
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

    it('should render the D3 SVG and bar rects when loaded with data', () => {
        expect(spectator.query('[data-testid="analytics-d3-bar-svg"]')).toExist();
        expect(spectator.query('[data-testid="analytics-d3-bar-svg"] rect.bar')).toExist();
        expect(spectator.query('[data-testid="analytics-bar-chart-skeleton"]')).not.toExist();
    });

    it('should cap bar thickness when bands are tall (few categories)', () => {
        spectator.setInput({
            data: [{ name: 'Solo', views: 1, percentage: 50, time: '1m' }],
            status: ComponentStatus.LOADED
        });
        spectator.detectChanges();
        spectator.flushEffects();
        spectator.detectChanges();

        const h = Number(
            spectator.query('[data-testid="analytics-d3-bar-svg"] rect.bar')?.getAttribute('height')
        );
        expect(h).toBe(MAX_BAR_THICKNESS);
    });

    it('should keep the same SVG plot height regardless of bar count (empty slots at bottom)', () => {
        spectator.setInput({
            data: [{ name: 'Only', views: 1, percentage: 100, time: '1m' }],
            status: ComponentStatus.LOADED
        });
        spectator.detectChanges();
        spectator.flushEffects();
        spectator.detectChanges();

        const svgOneBar = spectator.query('[data-testid="analytics-d3-bar-svg"]') as SVGSVGElement;
        const heightOne = svgOneBar.getAttribute('height');

        spectator.setInput({
            data: SAMPLE_DATA,
            status: ComponentStatus.LOADED
        });
        spectator.detectChanges();
        spectator.flushEffects();
        spectator.detectChanges();

        const svgMany = spectator.query('[data-testid="analytics-d3-bar-svg"]') as SVGSVGElement;
        expect(svgMany.getAttribute('height')).toBe(heightOne);
    });

    it('should show only top 5 bars when data has more than 5 items', () => {
        const manyItems: EngagementPlatformMetrics[] = [
            { name: 'A', views: 100, percentage: 50, time: '1m' },
            { name: 'B', views: 80, percentage: 40, time: '1m' },
            { name: 'C', views: 60, percentage: 30, time: '1m' },
            { name: 'D', views: 40, percentage: 20, time: '1m' },
            { name: 'E', views: 20, percentage: 10, time: '1m' },
            { name: 'F', views: 10, percentage: 5, time: '1m' }
        ];

        spectator.setInput({ data: manyItems });
        spectator.detectChanges();
        spectator.flushEffects();
        spectator.detectChanges();

        const bars = spectator.queryAll('[data-testid="analytics-d3-bar-svg"] rect.bar');
        expect(bars.length).toBe(5);
    });

    it('should sort items by percentage descending', () => {
        const unsortedData: EngagementPlatformMetrics[] = [
            { name: 'Low', views: 10, percentage: 10, time: '1m' },
            { name: 'High', views: 90, percentage: 90, time: '1m' },
            { name: 'Mid', views: 50, percentage: 50, time: '1m' }
        ];

        spectator.setInput({ data: unsortedData });
        spectator.detectChanges();
        spectator.flushEffects();
        spectator.detectChanges();

        const labels = spectator.queryAll('[data-testid="analytics-d3-bar-svg"] text.label');
        expect(labels[0].textContent?.trim()).toBe('High');
        expect(labels[1].textContent?.trim()).toBe('Mid');
        expect(labels[2].textContent?.trim()).toBe('Low');
    });

    it('should show skeleton when status is LOADING', () => {
        spectator.setInput({ status: ComponentStatus.LOADING });
        spectator.detectChanges();

        expect(spectator.query('[data-testid="analytics-bar-chart-skeleton"]')).toExist();
        expect(spectator.query('[data-testid="analytics-d3-bar-svg"]')).not.toExist();
    });

    it('should show skeleton when status is INIT', () => {
        spectator.setInput({ status: ComponentStatus.INIT });
        spectator.detectChanges();

        expect(spectator.query('[data-testid="analytics-bar-chart-skeleton"]')).toExist();
    });

    it('should show error state when status is ERROR', () => {
        spectator.setInput({ status: ComponentStatus.ERROR });
        spectator.detectChanges();

        expect(spectator.query('dot-analytics-state-message')).toExist();
        expect(spectator.query('[data-testid="analytics-d3-bar-svg"]')).not.toExist();
    });

    it('should show empty state when data is empty and status is LOADED', () => {
        spectator.setInput({ data: [], status: ComponentStatus.LOADED });
        spectator.detectChanges();

        expect(spectator.query('dot-analytics-empty-state')).toExist();
        expect(spectator.query('[data-testid="analytics-d3-bar-svg"]')).not.toExist();
    });

    it('should resolve card title when title input is set', () => {
        spectator.setInput({ title: 'analytics.engagement.charts.browser.title' });
        spectator.detectChanges();

        const card = spectator.query('[data-testid="analytics-bar-chart"]');
        expect(card?.querySelector('.p-card-title')?.textContent?.trim()).toBe('Translated title');
    });

    it('should highlight bar on mouseover and restore on mouseout', () => {
        const bars = spectator.queryAll(
            '[data-testid="analytics-d3-bar-svg"] rect.bar'
        ) as SVGRectElement[];
        expect(bars.length).toBeGreaterThan(0);

        bars[0].dispatchEvent(new MouseEvent('mouseover', { bubbles: true, cancelable: true }));
        spectator.detectChanges();

        expect(bars[0].style.opacity).toBe('1');
        for (let i = 1; i < bars.length; i++) {
            expect(bars[i].style.opacity).toBe('0.4');
        }

        bars[0].dispatchEvent(new MouseEvent('mouseout', { bubbles: true, cancelable: true }));
        spectator.detectChanges();

        for (const bar of bars) {
            expect(bar.style.opacity).toBe('1');
        }
    });
});
