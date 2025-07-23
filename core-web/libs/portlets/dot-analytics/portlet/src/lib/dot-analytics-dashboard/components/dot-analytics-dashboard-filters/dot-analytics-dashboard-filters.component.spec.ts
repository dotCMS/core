import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { TimeRange } from '@dotcms/portlets/dot-analytics/data-access';

import { DotAnalyticsDashboardFiltersComponent } from './dot-analytics-dashboard-filters.component';

import { DEFAULT_TIME_PERIOD, TIME_PERIOD_OPTIONS } from '../../constants';

describe('DotAnalyticsDashboardFiltersComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardFiltersComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardFiltersComponent,
        mocks: [DotMessageService]
    });

    beforeEach(() => {
        spectator = createComponent();

        // Set the required input after component creation
        spectator.setInput('selectedTimeRange', DEFAULT_TIME_PERIOD as TimeRange);
        spectator.detectChanges();

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
    });

    describe('Default Values', () => {
        it('should initialize with default time period from constants', () => {
            expect(spectator.component.$selectedTimeRange()).toBe(DEFAULT_TIME_PERIOD);
        });

        it('should have time period options from constants', () => {
            expect(spectator.component.$timeOptions()).toEqual(TIME_PERIOD_OPTIONS);
        });
    });

    describe('Event Emissions', () => {
        it('should emit timeRangeChanged when onTimeRangeChange is called', () => {
            const timeRangeChangedSpy = jest.fn();
            spectator.output('$timeRangeChanged').subscribe(timeRangeChangedSpy);

            const newTimeRange: TimeRange = 'from 30 days ago to now';
            spectator.component.onTimeRangeChange(newTimeRange);

            expect(timeRangeChangedSpy).toHaveBeenCalledWith(newTimeRange);
        });

        it('should emit timeRangeChanged with different time range values', () => {
            const timeRangeChangedSpy = jest.fn();
            spectator.output('$timeRangeChanged').subscribe(timeRangeChangedSpy);

            const timeRanges: TimeRange[] = ['from 7 days ago to now', 'from 30 days ago to now'];

            timeRanges.forEach((timeRange) => {
                spectator.component.onTimeRangeChange(timeRange);
                expect(timeRangeChangedSpy).toHaveBeenCalledWith(timeRange);
            });

            expect(timeRangeChangedSpy).toHaveBeenCalledTimes(2);
        });
    });

    describe('Dropdown Interaction', () => {
        it('should emit timeRangeChanged when dropdown value changes', () => {
            const timeRangeChangedSpy = jest.fn();
            spectator.output('$timeRangeChanged').subscribe(timeRangeChangedSpy);

            const newTimeRange: TimeRange = 'from 30 days ago to now';

            // Simulate dropdown change by calling the component method directly
            spectator.component.onTimeRangeChange(newTimeRange);

            expect(timeRangeChangedSpy).toHaveBeenCalledWith(newTimeRange);
        });

        it('should have correct dropdown properties', () => {
            const dropdown = spectator.query(byTestId('period-dropdown'));

            expect(dropdown).toHaveAttribute('optionLabel', 'label');
            expect(dropdown).toHaveAttribute('optionValue', 'value');
            expect(dropdown).toHaveAttribute('size', 'small');
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
    });
});
