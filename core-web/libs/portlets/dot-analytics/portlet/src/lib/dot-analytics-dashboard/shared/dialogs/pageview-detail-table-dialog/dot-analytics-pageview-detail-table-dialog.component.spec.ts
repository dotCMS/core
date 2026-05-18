import { byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotAnalyticsPageviewDetailTableDialogComponent } from './dot-analytics-pageview-detail-table-dialog.component';

import { ANALYTICS_DETAIL_DIALOG_TABLE } from '../../../shared/constants';

describe('DotAnalyticsPageviewDetailTableDialogComponent', () => {
    const MOCK_ROWS = [
        { dimensionLabel: 'Chrome', percentage: 43, totalViews: 17 },
        { dimensionLabel: 'Safari', percentage: 23, totalViews: 9 },
        { dimensionLabel: 'Firefox', percentage: 20, totalViews: 8 }
    ];

    const createComponent = createComponentFactory({
        component: DotAnalyticsPageviewDetailTableDialogComponent,
        providers: [
            {
                provide: DynamicDialogConfig,
                useValue: {
                    data: {
                        rows: MOCK_ROWS,
                        firstColumnHeaderKey: 'analytics.pageview.table.headers.browser'
                    }
                }
            },
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn((key: string, ...rest: unknown[]) =>
                        rest.length ? `${key}[${rest.join(',')}]` : key
                    )
                }
            }
        ]
    });

    it('should render the dialog container', () => {
        const spectator = createComponent();
        expect(spectator.query(byTestId('analytics-pageview-detail-table-dialog'))).toExist();
    });

    it('should render one row per entry', () => {
        const spectator = createComponent();
        const rows = spectator.queryAll(byTestId('analytics-pageview-detail-table-row'));
        expect(rows.length).toBe(MOCK_ROWS.length);
    });

    it('should display the dimension label in the first cell', () => {
        const spectator = createComponent();
        const rows = spectator.queryAll(byTestId('analytics-pageview-detail-table-row'));
        expect(rows[0].querySelector('td')?.textContent?.trim()).toBe('Chrome');
    });

    it('should set progress bar fill width to percentage', () => {
        const spectator = createComponent();
        const rows = spectator.queryAll(byTestId('analytics-pageview-detail-table-row'));
        const fill = rows[0].querySelector<HTMLElement>(
            '[data-testid="analytics-pageview-detail-bar-fill"]'
        );
        expect(fill?.style.width).toBe('43%');
    });

    it('should display the total views count', () => {
        const spectator = createComponent();
        const rows = spectator.queryAll(byTestId('analytics-pageview-detail-table-row'));
        const cells = rows[0].querySelectorAll('td');
        expect(cells[2].textContent?.trim()).toBe('17');
    });

    it('should paginate rows when count exceeds page size', () => {
        const manyRows = Array.from(
            { length: ANALYTICS_DETAIL_DIALOG_TABLE.ROWS_PER_PAGE + 3 },
            (_, i) => ({
                dimensionLabel: `Browser ${i + 1}`,
                percentage: 10,
                totalViews: i + 1
            })
        );
        const spectator = createComponent({
            providers: [
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {
                            rows: manyRows,
                            firstColumnHeaderKey: 'analytics.pageview.table.headers.browser'
                        }
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: {
                        get: jest.fn((key: string, ...rest: unknown[]) =>
                            rest.length ? `${key}[${rest.join(',')}]` : key
                        )
                    }
                }
            ]
        });

        expect(spectator.queryAll(byTestId('analytics-pageview-detail-table-row')).length).toBe(
            ANALYTICS_DETAIL_DIALOG_TABLE.ROWS_PER_PAGE
        );
        expect(spectator.query('.p-paginator')).toExist();
    });

    it('should render empty table when no rows are provided', () => {
        const spectator = createComponent({
            providers: [
                {
                    provide: DynamicDialogConfig,
                    useValue: { data: { rows: [], firstColumnHeaderKey: 'key' } }
                },
                {
                    provide: DotMessageService,
                    useValue: { get: jest.fn((k: string) => k) }
                }
            ]
        });
        const rows = spectator.queryAll(byTestId('analytics-pageview-detail-table-row'));
        expect(rows.length).toBe(0);
    });
});
