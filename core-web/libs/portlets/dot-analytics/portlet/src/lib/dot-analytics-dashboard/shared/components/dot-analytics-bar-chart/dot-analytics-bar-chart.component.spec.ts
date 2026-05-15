import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';

import { DotAnalyticsBarChartComponent } from './dot-analytics-bar-chart.component';

const SAMPLE_DATA: EngagementPlatformMetrics[] = [
    { name: 'Chrome', views: 750, percentage: 75, totalSessions: 1000, time: '2m 10s' },
    { name: 'Firefox', views: 100, percentage: 10, totalSessions: 1000, time: '1m 30s' },
    { name: 'Edge', views: 80, percentage: 8, totalSessions: 1000, time: '1m 05s' },
    { name: 'Safari', views: 70, percentage: 7, totalSessions: 1000, time: '0m 50s' }
];

describe('DotAnalyticsBarChartComponent', () => {
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

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.setInput({
            data: SAMPLE_DATA,
            status: ComponentStatus.LOADED,
            title: ''
        });
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render bar rows when loaded with data', () => {
        expect(spectator.query(byTestId('analytics-bar-chart-list'))).toExist();
        const rows = spectator.queryAll(byTestId('analytics-bar-row'));
        expect(rows.length).toBe(SAMPLE_DATA.length);
    });

    it('should display the label and percentage for each bar row', () => {
        const rows = spectator.queryAll(byTestId('analytics-bar-row'));
        const firstRow = rows[0];
        expect(firstRow.querySelector('.bar-row__label')?.textContent?.trim()).toBe('Chrome');
        expect(firstRow.querySelector('.bar-row__value')?.textContent?.trim()).toBe('75%');
    });

    it('should set bar fill width matching the percentage', () => {
        const rows = spectator.queryAll(byTestId('analytics-bar-row'));
        const fill = rows[0].querySelector<HTMLElement>('.bar-row__fill');
        expect(fill?.style.width).toBe('75%');
    });

    it('should sort items by percentage descending', () => {
        const unsortedData: EngagementPlatformMetrics[] = [
            { name: 'Low', views: 10, percentage: 10, totalSessions: 100, time: '1m' },
            { name: 'High', views: 90, percentage: 90, totalSessions: 100, time: '1m' },
            { name: 'Mid', views: 50, percentage: 50, totalSessions: 100, time: '1m' }
        ];

        spectator.setInput({ data: unsortedData });
        spectator.detectChanges();

        const labels = spectator
            .queryAll(byTestId('analytics-bar-row'))
            .map((row) => row.querySelector('.bar-row__label')?.textContent?.trim());

        expect(labels).toEqual(['High', 'Mid', 'Low']);
    });

    it('should show only top 5 rows when data has more than 5 items', () => {
        const manyItems: EngagementPlatformMetrics[] = [
            { name: 'A', views: 100, percentage: 50, totalSessions: 200, time: '1m' },
            { name: 'B', views: 80, percentage: 40, totalSessions: 200, time: '1m' },
            { name: 'C', views: 60, percentage: 30, totalSessions: 200, time: '1m' },
            { name: 'D', views: 40, percentage: 20, totalSessions: 200, time: '1m' },
            { name: 'E', views: 20, percentage: 10, totalSessions: 200, time: '1m' },
            { name: 'F', views: 10, percentage: 5, totalSessions: 200, time: '1m' }
        ];

        spectator.setInput({ data: manyItems });
        spectator.detectChanges();

        const rows = spectator.queryAll(byTestId('analytics-bar-row'));
        expect(rows.length).toBe(5);
    });

    it('should show skeleton when status is LOADING', () => {
        spectator.setInput({ status: ComponentStatus.LOADING });
        spectator.detectChanges();

        expect(spectator.query(byTestId('analytics-bar-chart-skeleton'))).toExist();
        expect(spectator.query(byTestId('analytics-bar-chart-list'))).not.toExist();
    });

    it('should show skeleton when status is INIT', () => {
        spectator.setInput({ status: ComponentStatus.INIT });
        spectator.detectChanges();

        expect(spectator.query(byTestId('analytics-bar-chart-skeleton'))).toExist();
    });

    it('should show error state when status is ERROR', () => {
        spectator.setInput({ status: ComponentStatus.ERROR });
        spectator.detectChanges();

        expect(spectator.query('dot-analytics-state-message')).toExist();
        expect(spectator.query(byTestId('analytics-bar-chart-list'))).not.toExist();
    });

    it('should show empty state when data is empty and status is LOADED', () => {
        spectator.setInput({ data: [], status: ComponentStatus.LOADED });
        spectator.detectChanges();

        expect(spectator.query('dot-analytics-empty-state')).toExist();
        expect(spectator.query(byTestId('analytics-bar-chart-list'))).not.toExist();
    });

    it('should resolve card title when title input is set', () => {
        spectator.setInput({ title: 'analytics.engagement.charts.browser.title' });
        spectator.detectChanges();

        const card = spectator.query(byTestId('analytics-bar-chart'));
        expect(card?.querySelector('.p-card-title')?.textContent?.trim()).toBe('Translated title');
    });
});
