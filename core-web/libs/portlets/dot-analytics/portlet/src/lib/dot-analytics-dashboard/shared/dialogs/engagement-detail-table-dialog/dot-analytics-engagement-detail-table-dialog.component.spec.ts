import { createComponentFactory, byTestId } from '@openng/spectator/jest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotAnalyticsEngagementDetailTableDialogComponent } from './dot-analytics-engagement-detail-table-dialog.component';

describe('DotAnalyticsEngagementDetailTableDialogComponent', () => {
    const createComponent = createComponentFactory({
        component: DotAnalyticsEngagementDetailTableDialogComponent,
        providers: [
            {
                provide: DynamicDialogConfig,
                useValue: {
                    data: {
                        rows: [
                            {
                                dimensionLabel: 'Chrome',
                                percentage: 40,
                                timeLabel: '0m 30s',
                                engagedSessions: 4,
                                totalSessions: 10
                            },
                            {
                                dimensionLabel: 'Safari',
                                percentage: 0,
                                timeLabel: '0m 0s',
                                engagedSessions: 0,
                                totalSessions: 3
                            }
                        ],
                        firstColumnHeaderKey: 'analytics.engagement.table.headers.browser'
                    }
                }
            }
        ]
    });

    it('should render engagement detail table rows from dialog config data', () => {
        const spectator = createComponent();
        expect(spectator.query(byTestId('analytics-engagement-detail-table-dialog'))).toExist();

        const dataRows = spectator.queryAll(byTestId('analytics-engagement-detail-table-row'));
        expect(dataRows.length).toBe(2);

        expect(spectator.debugElement.nativeElement.innerText.includes('Chrome')).toBe(true);
    });

    it('should render a stacked bar for each row', () => {
        const spectator = createComponent();

        const dataRows = spectator.queryAll(byTestId('analytics-engagement-detail-table-row'));
        dataRows.forEach((row) => {
            expect(row.querySelector('dot-analytics-stacked-bar')).toExist();
        });
    });

    it('should render the sessions bar column header', () => {
        const spectator = createComponent();

        const headers = spectator.queryAll('th');
        const headerTexts = headers.map((h) => h.textContent?.trim());
        expect(
            headerTexts.some((t) =>
                t?.includes('analytics.engagement.charts.detail.column.sessions-bar')
            )
        ).toBe(true);
    });
});
