import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { ContentConversionRow } from '@dotcms/portlets/dot-analytics/data-access';

import DotAnalyticsContentConversionsTableComponent from './dot-analytics-content-conversions-table.component';

describe('DotAnalyticsContentConversionsTableComponent', () => {
    let spectator: Spectator<DotAnalyticsContentConversionsTableComponent>;
    const createComponent = createComponentFactory({
        component: DotAnalyticsContentConversionsTableComponent,
        mocks: [DotMessageService]
    });

    const mockData: ContentConversionRow[] = [
        {
            eventType: 'content_impression',
            identifier: '123-abc',
            title: 'Test Content',
            events: 100,
            conversions: 25,
            conversionRate: 25.5
        },
        {
            eventType: 'content_click',
            identifier: '456-def',
            title: 'Another Content',
            events: 50,
            conversions: 10,
            conversionRate: 20.0
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
            expect(spectator.query(byTestId('content-conversions-loading'))).toBeTruthy();
            // Updated: Using Tailwind classes for skeleton (bg-gray-200 rounded)
            expect(spectator.queryAll('.bg-gray-200').length).toBeGreaterThan(0);
        });

        it('should not display table when loading', () => {
            expect(spectator.query(byTestId('content-conversions-table'))).toBeFalsy();
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
            expect(spectator.query(byTestId('content-conversions-error'))).toBeTruthy();
            expect(spectator.query('dot-analytics-state-message')).toBeTruthy();
        });

        it('should not display table when error', () => {
            expect(spectator.query(byTestId('content-conversions-table'))).toBeFalsy();
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
            expect(spectator.query(byTestId('content-conversions-empty'))).toBeTruthy();
            expect(spectator.query('dot-analytics-state-message')).toBeTruthy();
        });

        it('should not display table when empty', () => {
            expect(spectator.query(byTestId('content-conversions-table'))).toBeFalsy();
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
            const table = spectator.query(byTestId('content-conversions-table'));

            expect(table).toBeTruthy();
        });

        it('should display correct number of rows', () => {
            const rows = spectator.queryAll('p-table tbody tr');

            expect(rows.length).toBe(mockData.length);
        });

        it('should display content data correctly', () => {
            const firstRowCells = spectator.queryAll('p-table tbody tr')[0].querySelectorAll('td');

            // Column 0: Event Type badge
            expect(firstRowCells[0].querySelector('p-tag')).toBeTruthy();

            // Column 1: Title cell contains both title and identifier (updated selectors for Tailwind)
            const titleCell = firstRowCells[1];
            const titleCellDivs = titleCell.querySelectorAll('div > div');
            expect(titleCellDivs[0]?.textContent?.trim()).toBe(mockData[0].title);
            expect(titleCellDivs[1]?.textContent?.trim()).toBe(mockData[0].identifier);

            // Column 2: Events count
            expect(firstRowCells[2].textContent?.trim()).toBe(mockData[0].events.toString());

            // Column 3: Conversions
            expect(firstRowCells[3].textContent?.trim()).toBe(mockData[0].conversions.toString());

            // Column 4: Conversion Rate
            expect(firstRowCells[4].textContent?.trim()).toContain('25.5');
        });

        it('should display header columns', () => {
            const headers = spectator.queryAll('p-table thead th');

            expect(headers.length).toBe(5);
        });
    });

    describe('getTagSeverity', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    data: mockData,
                    status: ComponentStatus.LOADED
                } as unknown
            });
        });

        it.each([
            ['conversion', 'success'],
            ['content_click', 'info'],
            ['content_impression', 'warn'],
            ['unknown_event', 'secondary'],
            ['', 'secondary']
        ])("should return '%s' severity for '%s' event type", (eventType, expectedSeverity) => {
            expect(spectator.component['getTagSeverity'](eventType)).toBe(expectedSeverity);
        });
    });
});
