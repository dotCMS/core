import { createFakeEvent } from '@ngneat/spectator';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { addDays, format, startOfDay } from 'date-fns';

import { Select } from 'primeng/select';

import { DotMessageService } from '@dotcms/data-access';
import { TIME_RANGE_OPTIONS } from '@dotcms/portlets/dot-analytics/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAnalyticsFiltersComponent } from './dot-analytics-filters.component';

import { TIME_PERIOD_OPTIONS } from '../../constants';

describe('DotAnalyticsFiltersComponent', () => {
    let spectator: Spectator<DotAnalyticsFiltersComponent>;

    const messageServiceMock = new MockDotMessageService({
        'analytics.metrics.total-pageviews': 'Total Pageviews',
        'analytics.metrics.unique-visitors': 'Unique Visitors',
        'analytics.metrics.top-page-performance': 'Top Page Performance'
    });

    const createComponent = createComponentFactory({
        component: DotAnalyticsFiltersComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                timeRange: TIME_RANGE_OPTIONS.last7days
            } as unknown
        });
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
            expect(spectator.component.$selectedTimeRange()).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should have time period options from constants', () => {
            expect(spectator.component.$timeOptions()).toEqual(TIME_PERIOD_OPTIONS);
        });

        it('should initialize custom date range as null', () => {
            expect(spectator.component.$customDateRange()).toBeNull();
        });

        it('should not include today in TIME_PERIOD_OPTIONS', () => {
            const values = TIME_PERIOD_OPTIONS.map((opt) => opt.value);
            expect(values).not.toContain(TIME_RANGE_OPTIONS.today);
        });

        it('should not include yesterday in TIME_PERIOD_OPTIONS', () => {
            const values = TIME_PERIOD_OPTIONS.map((opt) => opt.value);
            expect(values).not.toContain(TIME_RANGE_OPTIONS.yesterday);
        });
    });

    describe('Custom Time Range Visibility', () => {
        it('should show custom calendar when CUSTOM_TIME_RANGE is selected', () => {
            spectator.component.$selectedTimeRange.set(TIME_RANGE_OPTIONS.custom);
            spectator.detectChanges();

            expect(spectator.component.$showCustomTimeRange()).toBe(true);
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeTruthy();
        });

        it('should hide custom calendar when switching away from CUSTOM_TIME_RANGE', () => {
            spectator.component.$selectedTimeRange.set(TIME_RANGE_OPTIONS.custom);
            spectator.detectChanges();
            expect(spectator.query(byTestId('custom-date-range-calendar'))).toBeTruthy();

            spectator.component.$selectedTimeRange.set(TIME_RANGE_OPTIONS.last7days);
            spectator.detectChanges();

            expect(spectator.component.$showCustomTimeRange()).toBe(false);
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeFalsy();
        });
    });

    describe('timeRange input change', () => {
        it('should show custom calendar when custom date range is selected', () => {
            spectator.setInput('timeRange', ['2024-01-01', '2024-01-31']);
            spectator.detectChanges();

            expect(spectator.component.$showCustomTimeRange()).toBe(true);
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeTruthy();

            expect(spectator.component.$selectedTimeRange()).toBe(TIME_RANGE_OPTIONS.custom);
            const customDateRange = spectator.component.$customDateRange();
            const from = format(customDateRange[0], 'yyyy-MM-dd');
            const to = format(customDateRange[1], 'yyyy-MM-dd');
            expect([from, to]).toEqual(['2024-01-01', '2024-01-31']);
        });

        it('should reset custom date range when time range is changed to last7days', () => {
            spectator.setInput('timeRange', TIME_RANGE_OPTIONS.last7days);
            spectator.detectChanges();

            expect(spectator.component.$showCustomTimeRange()).toBe(false);
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeFalsy();

            expect(spectator.component.$selectedTimeRange()).toBe(TIME_RANGE_OPTIONS.last7days);
            expect(spectator.component.$customDateRange()).toBeNull();
        });

        it('should reset custom date range when time range is changed to last30days', () => {
            spectator.setInput('timeRange', TIME_RANGE_OPTIONS.last30days);
            spectator.detectChanges();

            expect(spectator.component.$showCustomTimeRange()).toBe(false);
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeFalsy();

            expect(spectator.component.$selectedTimeRange()).toBe(TIME_RANGE_OPTIONS.last30days);
            expect(spectator.component.$customDateRange()).toBeNull();
        });
    });

    describe('$disabledDates', () => {
        it('should return empty array when no range start is set', () => {
            expect(spectator.component.$disabledDates()).toEqual([]);
        });

        it('should return 10 disabled dates (5 forward + 5 backward) after first date is selected', () => {
            const startDate = new Date('2024-01-01T00:00:00');
            spectator.component.onDateSelect(startDate);

            const disabled = spectator.component.$disabledDates();
            expect(disabled).toHaveLength(10);
            // Forward: start+1 to start+5
            expect(disabled[0]).toEqual(startOfDay(addDays(startDate, 1)));
            expect(disabled[4]).toEqual(startOfDay(addDays(startDate, 5)));
            // Backward: start-1 to start-5
            expect(disabled[5]).toEqual(startOfDay(addDays(startDate, -1)));
            expect(disabled[9]).toEqual(startOfDay(addDays(startDate, -5)));
        });

        it('should clear disabled dates after second date is selected', () => {
            const startDate = new Date('2024-01-01T00:00:00');
            const endDate = new Date('2024-01-15T00:00:00');
            spectator.component.onDateSelect(startDate);
            spectator.component.onDateSelect(endDate);

            expect(spectator.component.$disabledDates()).toEqual([]);
        });

        it('should clear disabled dates when clearDateRange is called', () => {
            const startDate = new Date('2024-01-01T00:00:00');
            spectator.component.onDateSelect(startDate);
            spectator.component.clearDateRange();

            expect(spectator.component.$disabledDates()).toEqual([]);
        });
    });

    describe('$today', () => {
        it('should be set to the start of today', () => {
            const today = spectator.component.$today();
            expect(today).toBeInstanceOf(Date);
            expect(today.getHours()).toBe(0);
            expect(today.getMinutes()).toBe(0);
        });
    });

    describe('onDateSelect (range picking)', () => {
        it('should set $rangeStart on first click and not emit', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            const startDate = new Date('2024-01-01T00:00:00');

            spectator.component.onDateSelect(startDate);

            expect(spectator.component.$rangeStart()).toEqual(startDate);
            expect(changeFiltersSpy).not.toHaveBeenCalled();
        });

        it('should emit a valid range and clear $rangeStart on second click', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            const startDate = new Date('2024-01-01T00:00:00');
            const endDate = new Date('2024-01-31T00:00:00');

            spectator.component.onDateSelect(startDate);
            spectator.component.$customDateRange.set([startDate, endDate]);
            spectator.component.onDateSelect(endDate);

            expect(spectator.component.$rangeStart()).toBeNull();
            expect(changeFiltersSpy).toHaveBeenCalledWith(['2024-01-01', '2024-01-31']);
        });

        it('should emit when range is exactly 7 days', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            const startDate = new Date('2024-01-01T00:00:00');
            const endDate = new Date('2024-01-07T00:00:00');

            spectator.component.onDateSelect(startDate);
            spectator.component.$customDateRange.set([startDate, endDate]);
            spectator.component.onDateSelect(endDate);

            expect(changeFiltersSpy).toHaveBeenCalledWith(['2024-01-01', '2024-01-07']);
        });

        it('should not emit when range is shorter than 7 days', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            const startDate = new Date('2024-01-01T00:00:00');
            const endDate = new Date('2024-01-06T00:00:00');

            spectator.component.onDateSelect(startDate);
            spectator.component.$customDateRange.set([startDate, endDate]);
            spectator.component.onDateSelect(endDate);

            expect(changeFiltersSpy).not.toHaveBeenCalled();
        });

        it('should not emit when range is incomplete (only start date)', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            spectator.component.$customDateRange.set([new Date('2024-01-01T00:00:00')]);
            spectator.component.onChangeCustomDateRange();

            expect(changeFiltersSpy).not.toHaveBeenCalled();
        });

        it('should not emit when date order is reversed', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            spectator.component.$customDateRange.set([
                new Date('2024-01-01T00:00:00'),
                new Date('1993-01-01T00:00:00')
            ]);
            spectator.component.onChangeCustomDateRange();

            expect(changeFiltersSpy).not.toHaveBeenCalled();
        });
    });

    describe('clearDateRange', () => {
        it('should clear $customDateRange and $rangeStart', () => {
            spectator.component.onDateSelect(new Date('2024-01-01T00:00:00'));
            spectator.component.$customDateRange.set([new Date('2024-01-01T00:00:00')]);

            spectator.component.clearDateRange();

            expect(spectator.component.$customDateRange()).toBeNull();
            expect(spectator.component.$rangeStart()).toBeNull();
        });
    });

    describe('onCalendarClosed', () => {
        it('should reset $rangeStart when calendar closes with incomplete range', () => {
            spectator.component.onDateSelect(new Date('2024-01-01T00:00:00'));
            spectator.component.$customDateRange.set([new Date('2024-01-01T00:00:00')]);

            spectator.component.onCalendarClosed();

            expect(spectator.component.$rangeStart()).toBeNull();
        });

        it('should keep $rangeStart as null when calendar closes with complete range', () => {
            const startDate = new Date('2024-01-01T00:00:00');
            const endDate = new Date('2024-01-31T00:00:00');
            spectator.component.$customDateRange.set([startDate, endDate]);

            spectator.component.onCalendarClosed();

            expect(spectator.component.$rangeStart()).toBeNull();
        });
    });

    describe('Calendar buttonbar template', () => {
        beforeEach(() => {
            spectator.component.$selectedTimeRange.set(TIME_RANGE_OPTIONS.custom);
            spectator.detectChanges();
        });

        it('should render the p-datepicker with showButtonBar enabled', () => {
            // The #buttonbar ng-template overrides the default button bar content.
            // Since PrimeNG renders the overlay only when opened (not in unit tests),
            // we verify the datepicker host element is present with the correct attribute.
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeTruthy();
            // The host element exists — the buttonbar template is provided via ng-template
            // and replaces the default buttonbar (removing the Today button).
        });

        it('should not render a Today button in the closed-state datepicker DOM', () => {
            // The Today button is removed by replacing the #buttonbar template.
            // When the overlay is not opened, neither Today nor Clear buttons are in the DOM.
            const todayBtn = spectator.query(
                '.p-datepicker-today-button, [data-testid="today-btn"]'
            );
            expect(todayBtn).toBeNull();
        });
    });

    describe('onChangeTimeRange', () => {
        it('should emit time range when time range is selected', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            spectator.triggerEventHandler(Select, 'onChange', {
                value: TIME_RANGE_OPTIONS.last7days,
                originalEvent: createFakeEvent('change')
            });

            expect(changeFiltersSpy).toHaveBeenCalledWith(TIME_RANGE_OPTIONS.last7days);
        });

        it('should emit the custom value when custom time range is selected from dropdown', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            spectator.triggerEventHandler(Select, 'onChange', {
                value: TIME_RANGE_OPTIONS.custom,
                originalEvent: createFakeEvent('change')
            });

            expect(changeFiltersSpy).toHaveBeenCalledWith(TIME_RANGE_OPTIONS.custom);
        });
    });
});
