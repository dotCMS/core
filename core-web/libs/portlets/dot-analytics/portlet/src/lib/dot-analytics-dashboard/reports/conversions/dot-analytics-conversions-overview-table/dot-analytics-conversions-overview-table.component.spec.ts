import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { ConversionsOverviewEntity } from '@dotcms/portlets/dot-analytics/data-access';

import DotAnalyticsConversionsOverviewTableComponent from './dot-analytics-conversions-overview-table.component';

describe('DotAnalyticsConversionsOverviewTableComponent', () => {
    let spectator: Spectator<DotAnalyticsConversionsOverviewTableComponent>;
    const createComponent = createComponentFactory({
        component: DotAnalyticsConversionsOverviewTableComponent,
        imports: [NoopAnimationsModule],
        mocks: [DotMessageService]
    });

    const mockData: ConversionsOverviewEntity[] = [
        {
            'Conversion.conversionName': 'Newsletter Signup',
            'Conversion.totalConversion': '150',
            'Conversion.convRate': '12.5',
            'Conversion.topAttributedContent': [
                {
                    conv_rate: '8.5',
                    conversions: '45',
                    event_type: 'content_impression',
                    identifier: '123-abc',
                    title: 'Home Page'
                },
                {
                    conv_rate: '4.0',
                    conversions: '20',
                    event_type: 'content_click',
                    identifier: '456-def',
                    title: 'Blog Post'
                }
            ]
        },
        {
            'Conversion.conversionName': 'Purchase',
            'Conversion.totalConversion': '75',
            'Conversion.convRate': '6.2',
            'Conversion.topAttributedContent': [
                {
                    conv_rate: '3.5',
                    conversions: '30',
                    event_type: 'content_impression',
                    identifier: '789-ghi',
                    title: 'Product Page'
                }
            ]
        }
    ];

    describe('Loading State', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    data: [],
                    status: ComponentStatus.LOADING
                } as unknown
            });
        });

        it('should display loading skeleton when isLoading is true', () => {
            expect(spectator.query(byTestId('conversions-overview-loading'))).toBeTruthy();
            expect(spectator.queryAll('.skeleton-cell').length).toBeGreaterThan(0);
        });

        it('should not display table when loading', () => {
            expect(spectator.query(byTestId('conversions-overview-table'))).toBeFalsy();
        });
    });

    describe('Error State', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    data: [],
                    status: ComponentStatus.ERROR
                } as unknown
            });
        });

        it('should display error message when isError is true', () => {
            expect(spectator.query(byTestId('conversions-overview-error'))).toBeTruthy();
            expect(spectator.query('dot-analytics-state-message')).toBeTruthy();
        });

        it('should not display table when error', () => {
            expect(spectator.query(byTestId('conversions-overview-table'))).toBeFalsy();
        });
    });

    describe('Empty State', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    data: [],
                    status: ComponentStatus.LOADED
                } as unknown
            });
        });

        it('should display empty message when isEmpty is true', () => {
            expect(spectator.query(byTestId('conversions-overview-empty'))).toBeTruthy();
            expect(spectator.query('dot-analytics-state-message')).toBeTruthy();
        });

        it('should not display table when empty', () => {
            expect(spectator.query(byTestId('conversions-overview-table'))).toBeFalsy();
        });
    });

    describe('Data Display', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    data: mockData,
                    status: ComponentStatus.LOADED
                } as unknown
            });
        });

        it('should display table with data', () => {
            const table = spectator.query(byTestId('conversions-overview-table'));

            expect(table).toBeTruthy();
        });

        it('should display correct number of rows', () => {
            const rows = spectator.queryAll('p-table tbody tr');

            expect(rows.length).toBe(mockData.length);
        });

        it('should display conversion name correctly', () => {
            const firstRowCells = spectator.queryAll('p-table tbody tr')[0].querySelectorAll('td');

            expect(firstRowCells[0].textContent?.trim()).toContain(
                mockData[0]['Conversion.conversionName']
            );
        });

        it('should display top attributed content items', () => {
            const firstRow = spectator.queryAll('p-table tbody tr')[0];
            const lastCell = firstRow.querySelectorAll('td')[3]; // Top Attributed Content column
            const contentItems = lastCell.querySelectorAll('.attributed-content-item');

            expect(contentItems.length).toBe(mockData[0]['Conversion.topAttributedContent'].length);
        });

        it('should display header columns', () => {
            const headers = spectator.queryAll('p-table thead th');

            expect(headers.length).toBe(4);
        });
    });
});
