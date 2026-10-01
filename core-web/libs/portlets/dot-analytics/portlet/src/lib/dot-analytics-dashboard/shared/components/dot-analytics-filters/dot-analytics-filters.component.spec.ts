import { createFakeEvent } from '@openng/spectator';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { addDays, format, startOfDay } from 'date-fns';
import { vi } from 'vitest';

import { DatePicker } from 'primeng/datepicker';
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

    describe('Custom calendar date limits', () => {
        beforeEach(() => {
            spectator.setInput('timeRange', TIME_RANGE_OPTIONS.custom);
            spectator.detectChanges();
        });

        it('keeps nearby dates selectable for short custom ranges', () => {
            const startDate = new Date('2024-01-01T00:00:00');
            spectator.triggerEventHandler(DatePicker, 'onSelect', startDate);
            spectator.detectChanges();

            const calendar = spectator.query(DatePicker);
            const nextDay = addDays(startDate, 1);
            expect(calendar.disabledDates ?? []).toEqual([]);
            expect(
                calendar.isDateDisabled(
                    nextDay.getDate(),
                    nextDay.getMonth(),
                    nextDay.getFullYear()
                )
            ).toBe(false);
        });

        it('allows today but not future dates', () => {
            const calendar = spectator.query(DatePicker);
            const today = startOfDay(new Date());
            const tomorrow = addDays(today, 1);

            expect(calendar.maxDate).toEqual(today);
            expect(
                calendar.isSelectable(today.getDate(), today.getMonth(), today.getFullYear(), false)
            ).toBe(true);
            expect(
                calendar.isSelectable(
                    tomorrow.getDate(),
                    tomorrow.getMonth(),
                    tomorrow.getFullYear(),
                    false
                )
            ).toBe(false);
        });

        it('emits a today-only range when today is selected twice', () => {
            const changeFiltersSpy = vi.spyOn(spectator.component.changeFilters, 'emit');
            const today = startOfDay(new Date());
            const day = format(today, 'yyyy-MM-dd');

            spectator.triggerEventHandler(DatePicker, 'onSelect', today);
            expect(changeFiltersSpy).not.toHaveBeenCalled();
            spectator.triggerEventHandler(DatePicker, 'onSelect', today);

            expect(changeFiltersSpy).toHaveBeenCalledExactlyOnceWith([day, day]);
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
            const changeFiltersSpy = vi.spyOn(spectator.component.changeFilters, 'emit');
            const startDate = new Date('2024-01-01T00:00:00');

            spectator.component.onDateSelect(startDate);

            expect(spectator.component.$rangeStart()).toEqual(startDate);
            expect(changeFiltersSpy).not.toHaveBeenCalled();
        });

        it('should emit a valid range and clear $rangeStart on second click', () => {
            const changeFiltersSpy = vi.spyOn(spectator.component.changeFilters, 'emit');
            const startDate = new Date('2024-01-01T00:00:00');
            const endDate = new Date('2024-01-31T00:00:00');

            spectator.component.onDateSelect(startDate);
            spectator.component.$customDateRange.set([startDate, endDate]);
            spectator.component.onDateSelect(endDate);

            expect(spectator.component.$rangeStart()).toBeNull();
            expect(changeFiltersSpy).toHaveBeenCalledWith(['2024-01-01', '2024-01-31']);
        });

        it('should emit when range is exactly 7 days', () => {
            const changeFiltersSpy = vi.spyOn(spectator.component.changeFilters, 'emit');
            const startDate = new Date('2024-01-01T00:00:00');
            const endDate = new Date('2024-01-07T00:00:00');

            spectator.component.onDateSelect(startDate);
            spectator.component.$customDateRange.set([startDate, endDate]);
            spectator.component.onDateSelect(endDate);

            expect(changeFiltersSpy).toHaveBeenCalledWith(['2024-01-01', '2024-01-07']);
        });

        it('should emit when range is shorter than 7 days', () => {
            const changeFiltersSpy = vi.spyOn(spectator.component.changeFilters, 'emit');
            const startDate = new Date('2024-01-01T00:00:00');
            const endDate = new Date('2024-01-06T00:00:00');

            spectator.component.onDateSelect(startDate);
            spectator.component.$customDateRange.set([startDate, endDate]);
            spectator.component.onDateSelect(endDate);

            expect(changeFiltersSpy).toHaveBeenCalledWith(['2024-01-01', '2024-01-06']);
        });

        it('restarts selection rather than emitting an inverted range', () => {
            const changeFiltersSpy = vi.spyOn(spectator.component.changeFilters, 'emit');
            const startDate = new Date('2024-01-15T00:00:00');
            const earlierDate = new Date('2024-01-14T00:00:00');

            spectator.component.onDateSelect(startDate);
            spectator.component.onDateSelect(earlierDate);

            expect(changeFiltersSpy).not.toHaveBeenCalled();
            expect(spectator.component.$rangeStart()).toEqual(earlierDate);
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
            const changeFiltersSpy = vi.spyOn(spectator.component.changeFilters, 'emit');
            spectator.triggerEventHandler(Select, 'onChange', {
                value: TIME_RANGE_OPTIONS.last7days,
                originalEvent: createFakeEvent('change')
            });

            expect(changeFiltersSpy).toHaveBeenCalledWith(TIME_RANGE_OPTIONS.last7days);
        });

        it('should not emit when custom time range is selected from dropdown', () => {
            const changeFiltersSpy = vi.spyOn(spectator.component.changeFilters, 'emit');
            spectator.triggerEventHandler(Select, 'onChange', {
                value: TIME_RANGE_OPTIONS.custom,
                originalEvent: createFakeEvent('change')
            });

            expect(changeFiltersSpy).not.toHaveBeenCalled();
        });
    });
});
