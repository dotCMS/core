import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ActivatedRoute, Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';

import { DotAnalyticsDashboardFiltersComponent } from './dot-analytics-dashboard-filters.component';

import { CUSTOM_TIME_RANGE, DEFAULT_TIME_PERIOD, TIME_PERIOD_OPTIONS } from '../../constants';
import { fromUrlFriendly, toUrlFriendly } from '../../utils/dot-analytics.utils';

describe('DotAnalyticsDashboardFiltersComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardFiltersComponent>;
    let router: Router;

    const mockActivatedRoute = {
        snapshot: {
            queryParams: {}
        }
    };

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardFiltersComponent,
        mocks: [DotMessageService, Router],
        providers: [
            {
                provide: ActivatedRoute,
                useValue: mockActivatedRoute
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        router = spectator.inject(Router);

        // Setup the mock to return translated values
        const messageService = spectator.inject(DotMessageService);
        (messageService.get as jest.Mock).mockImplementation((key: string) => `Translated ${key}`);

        // Reset router mock
        (router.navigate as jest.Mock).mockClear();
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
            spectator.component.$selectedTimeRange.set(CUSTOM_TIME_RANGE);
            spectator.detectChanges();

            expect(spectator.component.$showCustomTimeRange()).toBe(true);
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeTruthy();
        });

        it('should hide custom calendar when switching away from CUSTOM_TIME_RANGE', () => {
            spectator.component.$selectedTimeRange.set(CUSTOM_TIME_RANGE);
            spectator.detectChanges();
            expect(spectator.query(byTestId('custom-date-range-calendar'))).toBeTruthy();

            spectator.component.$selectedTimeRange.set('from 7 days ago to now');
            spectator.detectChanges();

            expect(spectator.component.$showCustomTimeRange()).toBe(false);
            const calendar = spectator.query(byTestId('custom-date-range-calendar'));
            expect(calendar).toBeFalsy();
        });
    });

    describe('URL Mapping Functions', () => {
        describe('toUrlFriendly', () => {
            it('should convert internal values to URL-friendly values', () => {
                const testCases = [
                    { internal: 'today', urlFriendly: 'today' },
                    { internal: 'yesterday', urlFriendly: 'yesterday' },
                    { internal: 'from 7 days ago to now', urlFriendly: 'last7days' },
                    { internal: 'from 30 days ago to now', urlFriendly: 'last30days' },
                    { internal: CUSTOM_TIME_RANGE, urlFriendly: 'custom' }
                ];

                testCases.forEach(({ internal, urlFriendly }) => {
                    expect(toUrlFriendly(internal)).toBe(urlFriendly);
                });
            });

            it('should return original value for unknown internal values', () => {
                const unknownValue = 'unknown-value';
                expect(toUrlFriendly(unknownValue)).toBe(unknownValue);
            });
        });

        describe('fromUrlFriendly', () => {
            it('should convert URL-friendly values to internal values', () => {
                const testCases = [
                    { urlFriendly: 'today', internal: 'today' },
                    { urlFriendly: 'yesterday', internal: 'yesterday' },
                    { urlFriendly: 'last7days', internal: 'from 7 days ago to now' },
                    { urlFriendly: 'last30days', internal: 'from 30 days ago to now' },
                    { urlFriendly: 'custom', internal: CUSTOM_TIME_RANGE }
                ];

                testCases.forEach(({ urlFriendly, internal }) => {
                    expect(fromUrlFriendly(urlFriendly)).toBe(internal);
                });
            });

            it('should return original value for unknown URL values', () => {
                const unknownValue = 'unknown-url-value';
                expect(fromUrlFriendly(unknownValue)).toBe(unknownValue);
            });
        });
    });

    describe('URL Parameter Updates', () => {
        describe('Predefined Time Ranges', () => {
            it('should update URL with URL-friendly values for predefined ranges', () => {
                // Test the URL update method directly by calling the private method
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                const updateUrlParamsSpy = jest.spyOn(
                    spectator.component as any,
                    'updateUrlParams'
                );

                const timeRange = 'from 7 days ago to now';

                // Call the private method directly to test URL update behavior
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                (spectator.component as any).updateUrlParams(timeRange);

                expect(updateUrlParamsSpy).toHaveBeenCalledWith(timeRange);
                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        time_range: 'last7days',
                        from: null,
                        to: null
                    },
                    queryParamsHandling: 'merge',
                    replaceUrl: true
                });
            });

            it('should clear custom date params when setting predefined range', () => {
                spectator.component.onTimeRangeChange('today');
                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        time_range: 'today',
                        from: null,
                        to: null
                    },
                    queryParamsHandling: 'merge',
                    replaceUrl: true
                });
            });
        });

        describe('Custom Date Ranges', () => {
            it('should update URL with custom date range params', () => {
                const startDate = new Date('2024-01-01');
                const endDate = new Date('2024-01-31');
                spectator.component.$customDateRange.set([startDate, endDate]);
                spectator.detectChanges();

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: {
                        time_range: 'custom',
                        from: '2024-01-01',
                        to: '2024-01-31'
                    },
                    queryParamsHandling: 'merge',
                    replaceUrl: true
                });
            });

            it('should not update URL for incomplete date ranges', () => {
                // Test with null
                spectator.component.$customDateRange.set(null);
                spectator.detectChanges();
                expect(router.navigate).not.toHaveBeenCalled();

                // Test with single date
                spectator.component.$customDateRange.set([new Date('2024-01-01')]);
                spectator.detectChanges();
                expect(router.navigate).not.toHaveBeenCalled();
            });
        });
    });

    describe('URL Initialization', () => {
        it('should initialize from URL with predefined time range', () => {
            mockActivatedRoute.snapshot.queryParams = {
                time_range: 'last7days'
            };

            spectator = createComponent();

            expect(spectator.component.$selectedTimeRange()).toBe('from 7 days ago to now');
        });

        it('should initialize from URL with custom date range', () => {
            mockActivatedRoute.snapshot.queryParams = {
                time_range: 'custom',
                from: '2024-01-01',
                to: '2024-01-31'
            };

            spectator = createComponent();

            expect(spectator.component.$selectedTimeRange()).toBe(CUSTOM_TIME_RANGE);
            expect(spectator.component.$customDateRange()).toEqual([
                new Date('2024-01-01'),
                new Date('2024-01-31')
            ]);
        });

        it('should not initialize from invalid URL params', () => {
            mockActivatedRoute.snapshot.queryParams = {
                time_range: 'invalid-range'
            };

            spectator = createComponent();

            expect(spectator.component.$selectedTimeRange()).toBe(DEFAULT_TIME_PERIOD);
            expect(router.navigate).toHaveBeenCalledWith([], {
                queryParams: {
                    time_range: 'last7days',
                    from: null,
                    to: null
                },
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        });

        it('should not initialize custom range without from/to params', () => {
            mockActivatedRoute.snapshot.queryParams = {
                time_range: 'custom'
            };

            spectator = createComponent();

            expect(spectator.component.$selectedTimeRange()).toBe(DEFAULT_TIME_PERIOD);
            expect(spectator.component.$customDateRange()).toBeNull();
        });

        it('should fall back to default when custom dates are invalid', () => {
            mockActivatedRoute.snapshot.queryParams = {
                time_range: 'custom',
                from: 'invalid-date',
                to: '2024-01-31'
            };

            spectator = createComponent();

            expect(spectator.component.$selectedTimeRange()).toBe(DEFAULT_TIME_PERIOD);
            expect(spectator.component.$customDateRange()).toBeNull();
            expect(router.navigate).toHaveBeenCalledWith([], {
                queryParams: {
                    time_range: 'last7days', // URL-friendly for DEFAULT_TIME_PERIOD
                    from: null,
                    to: null
                },
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        });

        it('should fall back to default when from date is after to date', () => {
            mockActivatedRoute.snapshot.queryParams = {
                time_range: 'custom',
                from: '2024-01-31', // After to date
                to: '2024-01-01'
            };

            spectator = createComponent();

            expect(spectator.component.$selectedTimeRange()).toBe(DEFAULT_TIME_PERIOD);
            expect(spectator.component.$customDateRange()).toBeNull();
            expect(router.navigate).toHaveBeenCalledWith([], {
                queryParams: {
                    time_range: 'last7days',
                    from: null,
                    to: null
                },
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        });

        it('should fall back to default when both dates are invalid', () => {
            mockActivatedRoute.snapshot.queryParams = {
                time_range: 'custom',
                from: 'not-a-date',
                to: 'also-not-a-date'
            };

            spectator = createComponent();

            expect(spectator.component.$selectedTimeRange()).toBe(DEFAULT_TIME_PERIOD);
            expect(spectator.component.$customDateRange()).toBeNull();
        });

        it('should fall back to default when time_range is invalid predefined value', () => {
            mockActivatedRoute.snapshot.queryParams = {
                time_range: 'invalid-time-range'
            };

            spectator = createComponent();

            expect(spectator.component.$selectedTimeRange()).toBe(DEFAULT_TIME_PERIOD);
            expect(spectator.component.$customDateRange()).toBeNull();
            expect(router.navigate).toHaveBeenCalledWith([], {
                queryParams: {
                    time_range: 'last7days',
                    from: null,
                    to: null
                },
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        });
    });

    describe('Effects Behavior', () => {
        it('should clear custom date range when switching away from CUSTOM_TIME_RANGE', async () => {
            const dateRange = [new Date('2024-01-01'), new Date('2024-01-31')];
            spectator.component.$customDateRange.set(dateRange);
            spectator.component.$selectedTimeRange.set(CUSTOM_TIME_RANGE);
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(spectator.component.$customDateRange()).toEqual(dateRange);

            spectator.component.$selectedTimeRange.set('from 7 days ago to now');
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            expect(spectator.component.$customDateRange()).toBeNull();
        });

        it('should avoid infinite loops with URL synchronization', async () => {
            // Set up URL state that matches what we're about to set
            mockActivatedRoute.snapshot.queryParams = {
                time_range: 'last7days'
            };

            // This should not trigger URL update since it matches
            spectator.component.$selectedTimeRange.set('from 7 days ago to now');
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            // Should not have called navigate because values match
            expect(router.navigate).not.toHaveBeenCalled();
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
            spectator.component.$selectedTimeRange.set(CUSTOM_TIME_RANGE);
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
            spectator.component.$selectedTimeRange.set(CUSTOM_TIME_RANGE);
            spectator.detectChanges();

            expect(spectator.query(byTestId('custom-date-range-calendar'))).toBeTruthy();
        });
    });
});
