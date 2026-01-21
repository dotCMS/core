import { createFakeEvent } from '@ngneat/spectator';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { format } from 'date-fns';

import { Dropdown } from 'primeng/dropdown';

import { DotMessageService } from '@dotcms/data-access';
import { TIME_RANGE_OPTIONS } from '@dotcms/portlets/dot-analytics/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAnalyticsDashboardFiltersComponent } from './dot-analytics-dashboard-filters.component';

import { TIME_PERIOD_OPTIONS } from '../../constants';

describe('DotAnalyticsDashboardFiltersComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardFiltersComponent>;

    const messageServiceMock = new MockDotMessageService({
        'analytics.metrics.total-pageviews': 'Total Pageviews',
        'analytics.metrics.unique-visitors': 'Unique Visitors',
        'analytics.metrics.top-page-performance': 'Top Page Performance'
    });

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardFiltersComponent,
        mocks: [DotMessageService],
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

    describe('Custom Date Range Change', () => {
        it('should emit custom date range when custom date range is selected', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            const customDateRange = [
                new Date('2024-01-01T00:00:00'),
                new Date('2024-01-31T00:00:00')
            ];
            spectator.component.$customDateRange.set(customDateRange);
            spectator.component.onChangeCustomDateRange();

            expect(changeFiltersSpy).toHaveBeenCalledWith(['2024-01-01', '2024-01-31']);
        });

        it('should not emit custom date range when custom date range is selected with incomplete date range', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            const customDateRange = [new Date('2024-01-01T00:00:00')];
            spectator.component.$customDateRange.set(customDateRange);
            spectator.component.onChangeCustomDateRange();

            expect(changeFiltersSpy).not.toHaveBeenCalled();
        });

        it('should not emit custom date range when custom date range is selected with invalid date range', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            const customDateRange = [
                new Date('2024-01-01T00:00:00'),
                new Date('1993-01-01T00:00:00')
            ];
            spectator.component.$customDateRange.set(customDateRange);
            spectator.component.onChangeCustomDateRange();

            expect(changeFiltersSpy).not.toHaveBeenCalled();
        });
    });

    describe('onChangeTimeRange', () => {
        it('should emit time range when time range is selected', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            spectator.triggerEventHandler(Dropdown, 'onChange', {
                value: TIME_RANGE_OPTIONS.last7days,
                originalEvent: createFakeEvent('change')
            });

            expect(changeFiltersSpy).toHaveBeenCalledWith(TIME_RANGE_OPTIONS.last7days);
        });

        it('should not emit when time range is a custom date range', () => {
            const changeFiltersSpy = jest.spyOn(spectator.component.changeFilters, 'emit');
            spectator.triggerEventHandler(Dropdown, 'onChange', {
                value: TIME_RANGE_OPTIONS.custom,
                originalEvent: createFakeEvent('change')
            });

            expect(changeFiltersSpy).not.toHaveBeenCalled();
        });
    });
});
