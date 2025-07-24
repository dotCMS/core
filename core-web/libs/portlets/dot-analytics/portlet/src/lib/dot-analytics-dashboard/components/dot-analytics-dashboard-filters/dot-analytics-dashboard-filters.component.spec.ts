import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { TimeRange } from '@dotcms/portlets/dot-analytics/data-access';

import { DotAnalyticsDashboardFiltersComponent } from './dot-analytics-dashboard-filters.component';

import { DEFAULT_TIME_PERIOD, TIME_PERIOD_OPTIONS } from '../../constants';
import { DateRange } from '../../types';

describe('DotAnalyticsDashboardFiltersComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardFiltersComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardFiltersComponent,
        mocks: [DotMessageService]
    });

    beforeEach(() => {
        spectator = createComponent();

        // Setup the mock to return translated values
        const messageService = spectator.inject(DotMessageService);
        (messageService.get as jest.Mock).mockImplementation((key: string) => `Translated ${key}`);
    });

    describe('Component Initialization', () => {
        it('should create component successfully', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have analytics filters container', () => {
            const filtersContainer = spectator.query(byTestId('analytics-filters'));
            expect(filtersContainer).toBeTruthy();
        });

        it('should have period dropdown', () => {
            const dropdown = spectator.query(byTestId('period-dropdown'));
            expect(dropdown).toBeTruthy();
        });

        it('should not show custom calendar initially', () => {
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeFalsy();
        });
    });

    describe('Default Values', () => {
        it('should initialize with default time period from constants', () => {
            expect(spectator.component.$selectedTimeRange()).toBe(DEFAULT_TIME_PERIOD);
        });

        it('should have time period options from constants', () => {
            expect(spectator.component.$timeOptions()).toEqual(TIME_PERIOD_OPTIONS);
        });

        it('should initialize custom date range as null', () => {
            expect(spectator.component.$customDateRange()).toBeNull();
        });
    });

    describe('Custom Time Range Visibility', () => {
        it('should show custom calendar when CUSTOM_TIME_RANGE is selected', () => {
            // Set to custom time range
            spectator.component.$selectedTimeRange.set('CUSTOM_TIME_RANGE');
            spectator.detectChanges();

            expect(spectator.component.$showCustomTimeRange()).toBe(true);
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeTruthy();
        });

        it('should hide custom calendar when switching away from CUSTOM_TIME_RANGE', () => {
            // First set to custom
            spectator.component.$selectedTimeRange.set('CUSTOM_TIME_RANGE');
            spectator.detectChanges();
            expect(spectator.query(byTestId('custom-date-range-calendar'))).toBeTruthy();

            // Then switch away to a valid predefined option
            spectator.component.$selectedTimeRange.set('from 7 days ago to now');
            spectator.detectChanges();

            expect(spectator.component.$showCustomTimeRange()).toBe(false);
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeFalsy();
        });

        it('should compute $showCustomTimeRange correctly for different values', () => {
            const testCases = [
                { value: 'CUSTOM_TIME_RANGE', expected: true },
                { value: 'today', expected: false },
                { value: 'yesterday', expected: false },
                { value: 'from 7 days ago to now', expected: false },
                { value: 'from 30 days ago to now', expected: false }
            ];

            testCases.forEach(({ value, expected }) => {
                spectator.component.$selectedTimeRange.set(value as TimeRange);
                expect(spectator.component.$showCustomTimeRange()).toBe(expected);
            });
        });
    });

    describe('Event Emissions - Predefined Ranges', () => {
        it('should emit timeRangeChanged when onTimeRangeChange is called with predefined range', () => {
            const timeRangeChangedSpy = jest.fn();
            spectator.output('$timeRangeChanged').subscribe(timeRangeChangedSpy);

            const newTimeRange: TimeRange = 'from 30 days ago to now';
            spectator.component.onTimeRangeChange(newTimeRange);

            expect(timeRangeChangedSpy).toHaveBeenCalledWith(newTimeRange);
        });

        it('should NOT emit timeRangeChanged when onTimeRangeChange is called with CUSTOM_TIME_RANGE', () => {
            const timeRangeChangedSpy = jest.fn();
            spectator.output('$timeRangeChanged').subscribe(timeRangeChangedSpy);

            spectator.component.onTimeRangeChange('CUSTOM_TIME_RANGE');

            expect(timeRangeChangedSpy).not.toHaveBeenCalled();
        });

        it('should emit timeRangeChanged with all valid predefined time range values', () => {
            const timeRangeChangedSpy = jest.fn();
            spectator.output('$timeRangeChanged').subscribe(timeRangeChangedSpy);

            // Test all valid predefined TimeRange values (excluding CUSTOM_TIME_RANGE)
            const validPredefinedRanges: TimeRange[] = [
                'today',
                'yesterday',
                'from 7 days ago to now',
                'from 30 days ago to now'
            ];

            validPredefinedRanges.forEach((timeRange) => {
                spectator.component.onTimeRangeChange(timeRange);
                expect(timeRangeChangedSpy).toHaveBeenCalledWith(timeRange);
            });

            expect(timeRangeChangedSpy).toHaveBeenCalledTimes(4);
        });
    });

    describe('Event Emissions - Custom Date Range', () => {
        it('should emit customDateRangeChanged when valid date range is set', async () => {
            const customDateRangeChangedSpy = jest.fn();
            spectator.output('$customDateRangeChanged').subscribe(customDateRangeChangedSpy);

            const startDate = new Date('2025-07-01');
            const endDate = new Date('2025-07-31');
            const dateRange = [startDate, endDate];

            spectator.component.$customDateRange.set(dateRange);
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const expectedRange: DateRange = ['2025-07-01', '2025-07-31'];
            expect(customDateRangeChangedSpy).toHaveBeenCalledWith(expectedRange);
        });

        it('should not emit customDateRangeChanged when date range is incomplete', async () => {
            const customDateRangeChangedSpy = jest.fn();
            spectator.output('$customDateRangeChanged').subscribe(customDateRangeChangedSpy);

            // Test with null
            spectator.component.$customDateRange.set(null);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            expect(customDateRangeChangedSpy).not.toHaveBeenCalled();

            // Test with empty array
            spectator.component.$customDateRange.set([]);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            expect(customDateRangeChangedSpy).not.toHaveBeenCalled();

            // Test with single date
            spectator.component.$customDateRange.set([new Date('2025-07-01')]);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            expect(customDateRangeChangedSpy).not.toHaveBeenCalled();
        });

        it('should format dates correctly to ISO string format', async () => {
            const customDateRangeChangedSpy = jest.fn();
            spectator.output('$customDateRangeChanged').subscribe(customDateRangeChangedSpy);

            const startDate = new Date('2025-12-25T10:30:00.000Z');
            const endDate = new Date('2025-12-31T23:59:59.999Z');
            const dateRange = [startDate, endDate];

            spectator.component.$customDateRange.set(dateRange);
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const expectedRange: DateRange = ['2025-12-25', '2025-12-31'];
            expect(customDateRangeChangedSpy).toHaveBeenCalledWith(expectedRange);
        });
    });

    describe('Effects Behavior', () => {
        it('should clear custom date range when switching away from CUSTOM_TIME_RANGE', async () => {
            const timeRangeChangedSpy = jest.fn();
            spectator.output('$timeRangeChanged').subscribe(timeRangeChangedSpy);

            // Set some custom date range first
            const dateRange = [new Date('2025-07-01'), new Date('2025-07-31')];
            spectator.component.$customDateRange.set(dateRange);
            spectator.component.$selectedTimeRange.set('CUSTOM_TIME_RANGE');
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(spectator.component.$customDateRange()).toEqual(dateRange);

            // Switch to a valid predefined range
            const newTimeRange: TimeRange = 'from 7 days ago to now';
            spectator.component.$selectedTimeRange.set(newTimeRange);
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            // Should clear custom date range and emit the new time range
            expect(spectator.component.$customDateRange()).toBeNull();
            expect(timeRangeChangedSpy).toHaveBeenCalledWith(newTimeRange);
        });

        it('should emit timeRangeChanged automatically when switching from CUSTOM_TIME_RANGE to predefined', async () => {
            const timeRangeChangedSpy = jest.fn();
            spectator.output('$timeRangeChanged').subscribe(timeRangeChangedSpy);

            spectator.component.$selectedTimeRange.set('CUSTOM_TIME_RANGE');
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            timeRangeChangedSpy.mockClear(); // Clear any previous calls

            spectator.component.$selectedTimeRange.set('today');
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(timeRangeChangedSpy).toHaveBeenCalledWith('today');
        });

        it('should not emit timeRangeChanged when setting to CUSTOM_TIME_RANGE', async () => {
            const timeRangeChangedSpy = jest.fn();
            spectator.output('$timeRangeChanged').subscribe(timeRangeChangedSpy);

            spectator.component.$selectedTimeRange.set('from 7 days ago to now');
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            timeRangeChangedSpy.mockClear(); // Clear any previous calls

            spectator.component.$selectedTimeRange.set('CUSTOM_TIME_RANGE');
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(timeRangeChangedSpy).not.toHaveBeenCalled();
        });
    });

    describe('Dropdown Interaction', () => {
        it('should have correct dropdown properties', () => {
            const dropdown = spectator.query(byTestId('period-dropdown'));

            expect(dropdown).toHaveAttribute('optionLabel', 'label');
            expect(dropdown).toHaveAttribute('optionValue', 'value');
            expect(dropdown).toHaveAttribute('size', 'small');
        });
    });

    describe('Calendar Properties', () => {
        beforeEach(() => {
            spectator.component.$selectedTimeRange.set('CUSTOM_TIME_RANGE');
            spectator.detectChanges();
        });

        it('should have correct calendar properties when visible', () => {
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));

            expect(calendar).toHaveAttribute('selectionMode', 'range');
            expect(calendar).toHaveAttribute('dateFormat', 'mm/dd/yy');
        });
    });

    describe('Accessibility', () => {
        it('should have proper test ids for testing', () => {
            expect(spectator.query(byTestId('analytics-filters'))).toBeTruthy();
            expect(spectator.query(byTestId('period-dropdown'))).toBeTruthy();
        });

        it('should have proper dropdown id for accessibility', () => {
            const dropdown = spectator.query('#period-filter');
            expect(dropdown).toBeTruthy();
        });

        it('should have custom calendar test id when visible', () => {
            spectator.component.$selectedTimeRange.set('CUSTOM_TIME_RANGE');
            spectator.detectChanges();

            expect(spectator.query(byTestId('custom-date-range-calendar'))).toBeTruthy();
        });
    });
});
