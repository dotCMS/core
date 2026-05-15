import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { ConversionOverviewData } from '@dotcms/portlets/dot-analytics/data-access';

import DotAnalyticsConversionsOverviewTableComponent from './dot-analytics-conversions-overview-table.component';

describe('DotAnalyticsConversionsOverviewTableComponent', () => {
    let spectator: Spectator<DotAnalyticsConversionsOverviewTableComponent>;
    const createComponent = createComponentFactory({
        component: DotAnalyticsConversionsOverviewTableComponent,
        imports: [NoopAnimationsModule],
        mocks: [DotMessageService]
    });

    const mockData: ConversionOverviewData[] = [
        {
            conversionName: 'Newsletter Signup',
            totalConversions: 150,
            conversionRate: 12.5,
            totalEvents: 100,
            topContent: [
                {
                    attributionRate: 8.5,
                    attributionCount: 45,
                    eventType: 'content_impression',
                    identifier: '123-abc',
                    title: 'Home Page',
                    events: 100
                },
                {
                    attributionRate: 4.0,
                    attributionCount: 20,
                    eventType: 'content_click',
                    identifier: '456-def',
                    title: 'Blog Post',
                    events: 50
                }
            ]
        },
        {
            conversionName: 'Purchase',
            totalConversions: 75,
            conversionRate: 6.2,
            totalEvents: 80,
            topContent: [
                {
                    attributionRate: 3.5,
                    attributionCount: 30,
                    eventType: 'content_impression',
                    identifier: '789-ghi',
                    title: 'Product Page',
                    events: 70
                }
            ]
        }
    ];

    describe('Loading State', () => {
        beforeEach(() => {
            spectator = createComponent({ detectChanges: false });
            spectator.setInput({ data: [], status: ComponentStatus.LOADING });
        });

        it('should display loading skeleton when isLoading is true', () => {
            expect(spectator.query(byTestId('conversions-overview-loading'))).toBeTruthy();
            expect(spectator.queryAll('.skeleton-table__row').length).toBe(5);
        });

        it('should not display table when loading', () => {
            expect(spectator.query(byTestId('conversions-overview-table'))).toBeFalsy();
        });
    });

    describe('Error State', () => {
        beforeEach(() => {
            spectator = createComponent({ detectChanges: false });
            spectator.setInput({ data: [], status: ComponentStatus.ERROR });
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
            spectator = createComponent({ detectChanges: false });
            spectator.setInput({ data: [], status: ComponentStatus.LOADED });
        });

        it('should display empty message when isEmpty is true', () => {
            expect(spectator.query(byTestId('conversions-overview-empty'))).toBeTruthy();
            expect(spectator.query('dot-analytics-empty-state')).toBeTruthy();
        });

        it('should not display table when empty', () => {
            expect(spectator.query(byTestId('conversions-overview-table'))).toBeFalsy();
        });
    });

    describe('Data Display', () => {
        beforeEach(() => {
            spectator = createComponent({ detectChanges: false });
            spectator.setInput({ data: mockData, status: ComponentStatus.LOADED });
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

            expect(firstRowCells[0].textContent?.trim()).toContain(mockData[0].conversionName);
        });

        it('should display the first attributed content item', () => {
            const firstRow = spectator.queryAll('p-table tbody tr')[0];
            const lastCell = firstRow.querySelectorAll('td')[3];
            const contentItem = lastCell.querySelector('.attributed-content-item');

            expect(contentItem).toExist();
            expect(contentItem.textContent).toContain(mockData[0].topContent[0].title);
        });

        it('should display header columns', () => {
            const headers = spectator.queryAll('p-table thead th');

            expect(headers.length).toBe(4);
        });
    });
});
