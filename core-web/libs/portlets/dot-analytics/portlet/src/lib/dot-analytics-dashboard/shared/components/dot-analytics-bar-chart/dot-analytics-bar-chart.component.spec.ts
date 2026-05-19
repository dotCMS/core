import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

import { DialogService } from 'primeng/dynamicdialog';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';

import { DotAnalyticsBarChartComponent } from './dot-analytics-bar-chart.component';

import { DotAnalyticsPageviewDetailTableDialogComponent } from '../../dialogs/pageview-detail-table-dialog/dot-analytics-pageview-detail-table-dialog.component';

const SAMPLE_DATA: EngagementPlatformMetrics[] = [
    { name: 'Chrome', views: 750, percentage: 75, totalSessions: 1000, time: '2m 10s' },
    { name: 'Firefox', views: 100, percentage: 10, totalSessions: 1000, time: '1m 30s' },
    { name: 'Edge', views: 80, percentage: 8, totalSessions: 1000, time: '1m 05s' },
    { name: 'Safari', views: 70, percentage: 7, totalSessions: 1000, time: '0m 50s' }
];

describe('DotAnalyticsBarChartComponent', () => {
    let spectator: Spectator<DotAnalyticsBarChartComponent>;

    const dialogOpenSpy = jest.fn();

    const createComponent = createComponentFactory({
        component: DotAnalyticsBarChartComponent,
        componentProviders: [
            {
                provide: DialogService,
                useValue: { open: dialogOpenSpy }
            }
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn((key: string, ...args: string[]) => {
                        if (args.length) {
                            return `${key}[${args.join(',')}]`;
                        }

                        return key === 'analytics.charts.browser-breakdown.title'
                            ? 'Translated title'
                            : key;
                    })
                }
            }
        ]
    });

    beforeEach(() => {
        dialogOpenSpy.mockReset();
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

    it('should expose views tooltip and aria-label on bar rows with formatted view count', () => {
        const rows = spectator.queryAll(byTestId('analytics-bar-row'));
        const firstRow = rows[0] as HTMLElement;
        expect(spectator.component.viewsTooltip(SAMPLE_DATA[0])).toBe(
            'analytics.pageview.charts.multi-views-tooltip[750]'
        );
        expect(firstRow.getAttribute('aria-label')).toBe(
            'Chrome, analytics.pageview.charts.multi-views-tooltip[750]'
        );
        expect(rows[0].querySelector('[data-testid="analytics-bar-row-fill"]')).toBeTruthy();
    });

    it('should activate bar fill tooltip when the row is hovered', () => {
        const fillTooltip = spectator.debugElement
            .queryAll(By.directive(Tooltip))
            .map((de) => de.injector.get(Tooltip))[0];
        const activateSpy = jest.spyOn(fillTooltip, 'activate');

        spectator.component.onRowMouseEnter(0, SAMPLE_DATA[0]);

        expect(activateSpy).toHaveBeenCalled();
        activateSpy.mockRestore();
    });

    it('should use one-view tooltip key when views equals 1', () => {
        spectator.setInput({
            data: [{ name: 'Single', views: 1, percentage: 100, totalSessions: 1, time: '' }]
        });
        spectator.detectChanges();

        const row = spectator.query(byTestId('analytics-bar-row')) as HTMLElement;
        expect(row.getAttribute('aria-label')).toBe(
            'Single, analytics.pageview.charts.one-view-tooltip'
        );
    });

    it('should not set views tooltip when views is zero', () => {
        spectator.setInput({
            data: [{ name: 'Empty', views: 0, percentage: 0, totalSessions: 0, time: '' }]
        });
        spectator.detectChanges();

        const row = spectator.query(byTestId('analytics-bar-row')) as HTMLElement;
        expect(
            spectator.component.viewsTooltip({
                name: 'Empty',
                views: 0,
                percentage: 0,
                totalSessions: 0,
                time: ''
            })
        ).toBe('');
        expect(row.getAttribute('aria-label')).toBe('Empty');
    });

    it('should display the label and percentage for each bar row', () => {
        const rows = spectator.queryAll(byTestId('analytics-bar-row'));
        const firstRow = rows[0];
        expect(
            firstRow.querySelector('[data-testid="analytics-bar-row-label"]')?.textContent?.trim()
        ).toBe('Chrome');
        expect(
            firstRow.querySelector('[data-testid="analytics-bar-row-value"]')?.textContent?.trim()
        ).toBe('75%');
    });

    it('should set bar fill width matching the percentage', () => {
        const rows = spectator.queryAll(byTestId('analytics-bar-row'));
        const fill = rows[0].querySelector<HTMLElement>('[data-testid="analytics-bar-row-fill"]');
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
            .map((row) =>
                row.querySelector('[data-testid="analytics-bar-row-label"]')?.textContent?.trim()
            );

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
        spectator.setInput({ title: 'analytics.charts.browser-breakdown.title' });
        spectator.detectChanges();

        const card = spectator.query(byTestId('analytics-bar-chart'));
        expect(card?.querySelector('.p-card-title')?.textContent?.trim()).toBe('Translated title');
    });

    it('should not render view details link when detailsEnabled is false', () => {
        spectator.setInput({ detailsEnabled: false });
        spectator.detectChanges();

        expect(spectator.query(byTestId('analytics-bar-chart-view-details'))).not.toExist();
    });

    it('should render view details link when detailsEnabled and dimension header key are set', () => {
        spectator.setInput({
            detailsEnabled: true,
            detailsDimensionHeaderKey: 'analytics.pageview.table.headers.browser'
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('analytics-bar-chart-view-details'))).toExist();
    });

    it('should open pageview detail dialog with all rows when view details is clicked', () => {
        const allData: EngagementPlatformMetrics[] = Array.from({ length: 6 }, (_, i) => ({
            name: `Browser-${i}`,
            views: 10 + i,
            percentage: 30 - i * 5,
            totalSessions: 100,
            time: '1m'
        }));

        spectator.setInput({
            data: allData,
            status: ComponentStatus.LOADED,
            detailsEnabled: true,
            detailsDimensionHeaderKey: 'analytics.pageview.table.headers.browser',
            title: 'analytics.charts.browser-breakdown.title'
        });
        spectator.detectChanges();

        const detailsHost = spectator.query(byTestId('analytics-bar-chart-view-details'));
        expect(detailsHost).toExist();

        const btn = detailsHost?.querySelector('button');
        expect(btn).toBeTruthy();
        if (!btn) return;

        spectator.click(btn);

        expect(dialogOpenSpy).toHaveBeenCalledWith(
            DotAnalyticsPageviewDetailTableDialogComponent,
            expect.objectContaining({ closable: true, closeOnEscape: true })
        );

        const cfg = dialogOpenSpy.mock.calls[0][1] as {
            data: { rows: { dimensionLabel: string }[] };
        };
        expect(cfg.data.rows.length).toBe(6);
        expect(cfg.data.rows[0].dimensionLabel).toBe('Browser-0');
    });
});
