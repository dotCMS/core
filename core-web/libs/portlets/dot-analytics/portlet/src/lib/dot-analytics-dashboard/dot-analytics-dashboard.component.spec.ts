import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { BehaviorSubject, of } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotAnalyticsDashboardStore, TimeRange } from '@dotcms/portlets/dot-analytics/data-access';

import DotAnalyticsDashboardComponent from './dot-analytics-dashboard.component';

describe('DotAnalyticsDashboardComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardComponent>;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let mockStore: any;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let mockActivatedRoute: any;

    const mockStoreSignals = {
        timeRange: jest.fn().mockReturnValue('from 7 days ago to now' as TimeRange),

        // Direct resource signals
        totalPageViews: jest.fn().mockReturnValue({
            status: ComponentStatus.LOADED,
            data: { 'request.totalRequest': '1250' },
            error: null
        }),
        uniqueVisitors: jest.fn().mockReturnValue({
            status: ComponentStatus.LOADED,
            data: { 'request.totalUsers': '342' },
            error: null
        }),
        topPagePerformance: jest.fn().mockReturnValue({
            status: ComponentStatus.LOADED,
            data: {
                'request.totalRequest': '89',
                'request.pageTitle': 'Home Page',
                'request.path': '/home'
            },
            error: null
        }),
        pageViewTimeLine: jest.fn().mockReturnValue({
            status: ComponentStatus.LOADED,
            data: [
                {
                    'request.totalRequest': '10',
                    'request.createdAt': '2024-01-01T00:00:00Z',
                    'request.createdAt.day': '2024-01-01'
                },
                {
                    'request.totalRequest': '20',
                    'request.createdAt': '2024-01-02T00:00:00Z',
                    'request.createdAt.day': '2024-01-02'
                }
            ],
            error: null
        }),
        pageViewDeviceBrowsers: Object.assign(
            () => ({
                status: ComponentStatus.LOADED,
                data: [
                    { 'request.totalRequest': '60', 'request.userAgent': 'Chrome' },
                    { 'request.totalRequest': '40', 'request.userAgent': 'Firefox' }
                ],
                error: null
            }),
            {
                status: jest.fn().mockReturnValue(ComponentStatus.LOADED)
            }
        ),
        topPagesTable: jest.fn().mockReturnValue({
            status: ComponentStatus.LOADED,
            data: [
                {
                    'request.pageTitle': 'Home',
                    'request.path': '/home',
                    'request.totalRequest': '100'
                }
            ],
            error: null
        }),

        // Computed/transformed data
        metricsData: jest.fn().mockReturnValue([
            {
                name: 'analytics.metrics.total-pageviews',
                value: 1250,
                subtitle: 'analytics.metrics.total-pageviews.subtitle',
                icon: 'pi-eye',
                status: ComponentStatus.LOADED,
                error: null
            },
            {
                name: 'analytics.metrics.unique-visitors',
                value: 342,
                subtitle: 'analytics.metrics.unique-visitors.subtitle',
                icon: 'pi-users',
                status: ComponentStatus.LOADED,
                error: null
            },
            {
                name: 'analytics.metrics.top-page-performance',
                value: 89,
                subtitle: 'Home Page',
                icon: 'pi-chart-bar',
                status: ComponentStatus.LOADED,
                error: null
            }
        ]),
        topPagesTableData: jest
            .fn()
            .mockReturnValue([{ pageTitle: 'Home', path: '/home', views: 100 }]),
        pageViewTimeLineData: jest.fn().mockReturnValue({
            labels: ['Jan', 'Feb'],
            datasets: [{ label: 'Page Views', data: [10, 20] }]
        }),
        pageViewDeviceBrowsersData: jest.fn().mockReturnValue({
            labels: ['Chrome', 'Firefox'],
            datasets: [{ label: 'Device Usage', data: [60, 40] }]
        })
    };

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        mocks: [DotMessageService],
        providers: [
            // Default ActivatedRoute mock for the filters component
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        queryParams: {}
                    }
                }
            }
        ]
    });

    // Helper function to set query params in both observable and snapshot
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const setMockQueryParams = (params: Record<string, any>) => {
        mockActivatedRoute.queryParams = of(params);
        mockActivatedRoute.snapshot.queryParams = params;
    };

    beforeEach(() => {
        // Reset all mocks
        jest.clearAllMocks();

        mockStore = {
            ...mockStoreSignals,
            setTimeRange: jest.fn(),
            loadTotalPageViews: jest.fn(),
            loadPageViewDeviceBrowsers: jest.fn(),
            loadPageViewTimeLine: jest.fn(),
            loadTopPagePerformance: jest.fn(),
            loadUniqueVisitors: jest.fn(),
            loadTopPagesTable: jest.fn(),
            loadAllDashboardData: jest.fn()
        };

        mockActivatedRoute = {
            queryParams: of({}),
            snapshot: {
                queryParams: {}
            }
        };

        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: DotAnalyticsDashboardStore,
                    useValue: mockStore
                },
                {
                    provide: ActivatedRoute,
                    useValue: mockActivatedRoute
                }
            ]
        });
    });

    describe('Component Initialization', () => {
        it('should create component successfully', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have all required child components with data-testid', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('analytics-filters'))).toExist();
            expect(spectator.query(byTestId('refresh-button'))).toExist();
            expect(spectator.query(byTestId('analytics-metric-card'))).toExist();
            expect(spectator.query(byTestId('analytics-timeline-chart'))).toExist();
            expect(spectator.query(byTestId('analytics-device-chart'))).toExist();
            expect(spectator.query(byTestId('analytics-table'))).toExist();
        });

        it('should render multiple metric cards', () => {
            spectator.detectChanges();

            const metricCards = spectator.queryAll(byTestId('analytics-metric-card'));
            expect(metricCards).toHaveLength(3);
        });
    });

    describe('URL Query Params Handling', () => {
        describe('Predefined Time Ranges', () => {
            it('should handle URL-friendly time range values', () => {
                setMockQueryParams({
                    time_range: 'last7days'
                });

                spectator.component.ngOnInit();

                expect(mockStore.setTimeRange).toHaveBeenCalledWith('from 7 days ago to now');
            });

            it('should handle all URL-friendly predefined ranges', () => {
                const testCases = [
                    { urlValue: 'today', internalValue: 'today' },
                    { urlValue: 'yesterday', internalValue: 'yesterday' },
                    { urlValue: 'last7days', internalValue: 'from 7 days ago to now' },
                    { urlValue: 'last30days', internalValue: 'from 30 days ago to now' }
                ];

                testCases.forEach(({ urlValue, internalValue }) => {
                    jest.clearAllMocks();
                    mockActivatedRoute.queryParams = of({
                        time_range: urlValue
                    });

                    spectator.component.ngOnInit();

                    expect(mockStore.setTimeRange).toHaveBeenCalledWith(internalValue);
                });
            });

            it('should ignore invalid time range values', () => {
                setMockQueryParams({
                    time_range: 'invalid-range'
                });

                spectator.component.ngOnInit();

                expect(mockStore.setTimeRange).not.toHaveBeenCalled();
            });
        });

        describe('Custom Date Ranges', () => {
            it('should handle custom date range with from/to params', () => {
                setMockQueryParams({
                    time_range: 'custom',
                    from: '2024-01-01',
                    to: '2024-01-31'
                });

                spectator.component.ngOnInit();

                expect(mockStore.setTimeRange).toHaveBeenCalledWith(['2024-01-01', '2024-01-31']);
            });

            it('should ignore custom range without from parameter', () => {
                setMockQueryParams({
                    time_range: 'custom',
                    to: '2024-01-31'
                });

                spectator.component.ngOnInit();

                expect(mockStore.setTimeRange).not.toHaveBeenCalled();
            });

            it('should ignore custom range without to parameter', () => {
                setMockQueryParams({
                    time_range: 'custom',
                    from: '2024-01-01'
                });

                spectator.component.ngOnInit();

                expect(mockStore.setTimeRange).not.toHaveBeenCalled();
            });

            it('should ignore incomplete custom range params', () => {
                setMockQueryParams({
                    time_range: 'custom'
                });

                spectator.component.ngOnInit();

                expect(mockStore.setTimeRange).not.toHaveBeenCalled();
            });

            it('should ignore custom range with invalid dates', () => {
                mockActivatedRoute.queryParams = of({
                    time_range: 'custom',
                    from: 'invalid-date',
                    to: '2024-01-31'
                });

                spectator.component.ngOnInit();

                expect(mockStore.setTimeRange).not.toHaveBeenCalled();
            });

            it('should ignore custom range where from date is after to date', () => {
                mockActivatedRoute.queryParams = of({
                    time_range: 'custom',
                    from: '2024-01-31', // After to date
                    to: '2024-01-01'
                });

                spectator.component.ngOnInit();

                expect(mockStore.setTimeRange).not.toHaveBeenCalled();
            });

            it('should ignore custom range with both invalid dates', () => {
                mockActivatedRoute.queryParams = of({
                    time_range: 'custom',
                    from: 'not-a-date',
                    to: 'also-not-a-date'
                });

                spectator.component.ngOnInit();

                expect(mockStore.setTimeRange).not.toHaveBeenCalled();
            });
        });

        describe('Query Params Changes', () => {
            it('should react to query params changes', () => {
                const queryParamsSubject = new BehaviorSubject({});
                mockActivatedRoute.queryParams = queryParamsSubject.asObservable();

                spectator.component.ngOnInit();

                // First change
                queryParamsSubject.next({ time_range: 'today' });
                expect(mockStore.setTimeRange).toHaveBeenCalledWith('today');

                // Second change
                queryParamsSubject.next({ time_range: 'last7days' });
                expect(mockStore.setTimeRange).toHaveBeenCalledWith('from 7 days ago to now');

                // Custom range change
                queryParamsSubject.next({
                    time_range: 'custom',
                    from: '2024-01-01',
                    to: '2024-01-31'
                });
                expect(mockStore.setTimeRange).toHaveBeenCalledWith(['2024-01-01', '2024-01-31']);
            });
        });
    });

    describe('Component State', () => {
        it('should expose store signals correctly', () => {
            expect(spectator.component['$currentTimeRange']()).toBe('from 7 days ago to now');

            expect(spectator.component['$totalPageViews']()).toEqual({
                status: ComponentStatus.LOADED,
                data: { 'request.totalRequest': '1250' },
                error: null
            });

            expect(spectator.component['$metricsData']()).toHaveLength(3);
            expect(spectator.component['$deviceBreakdownStatus']()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('Public Methods', () => {
        describe('onRefresh', () => {
            it('should call loadAllDashboardData with current time range', () => {
                const currentTimeRange = 'from 7 days ago to now';

                spectator.component.onRefresh();

                expect(mockStore.loadAllDashboardData).toHaveBeenCalledWith(currentTimeRange);
            });
        });
    });

    describe('Store Integration', () => {
        it('should have access to store methods', () => {
            expect(typeof spectator.component.onRefresh).toBe('function');
        });

        it('should handle different time ranges', () => {
            const timeRanges: TimeRange[] = ['from 7 days ago to now', 'from 30 days ago to now'];

            timeRanges.forEach((timeRange) => {
                // Simulate what happens when query params change
                mockActivatedRoute.queryParams = of({ time_range: timeRange });
                spectator.component.ngOnInit();
                expect(mockStore.setTimeRange).toHaveBeenCalledWith(timeRange);
            });
        });
    });

    describe('Component Properties', () => {
        it('should have all required signal properties', () => {
            expect(spectator.component['$currentTimeRange']).toBeDefined();
            expect(spectator.component['$totalPageViews']).toBeDefined();
            expect(spectator.component['$uniqueVisitors']).toBeDefined();
            expect(spectator.component['$topPagePerformance']).toBeDefined();
            expect(spectator.component['$pageViewTimeLine']).toBeDefined();
            expect(spectator.component['$pageViewDeviceBrowsers']).toBeDefined();
            expect(spectator.component['$topPagesTable']).toBeDefined();
            expect(spectator.component['$metricsData']).toBeDefined();
            expect(spectator.component['$topPagesTableData']).toBeDefined();
            expect(spectator.component['$pageviewsTimelineData']).toBeDefined();
            expect(spectator.component['$deviceBreakdownData']).toBeDefined();
            expect(spectator.component['$deviceBreakdownStatus']).toBeDefined();
        });
    });

    describe('Button Interactions', () => {
        it('should call onRefresh when refresh button is clicked', () => {
            const spy = jest.spyOn(spectator.component, 'onRefresh');

            spectator.detectChanges();

            const refreshButton = spectator.query(byTestId('refresh-button'));
            expect(refreshButton).toExist();

            spectator.triggerEventHandler('[data-testid="refresh-button"]', 'onClick', null);

            expect(spy).toHaveBeenCalledTimes(1);
        });

        it('should verify refresh button has correct configuration', () => {
            spectator.detectChanges();

            const refreshButton = spectator.query(byTestId('refresh-button'));
            expect(refreshButton).toExist();

            expect(refreshButton).toHaveAttribute('data-testid', 'refresh-button');
            expect(refreshButton).toHaveClass('analytics-dashboard__refresh-button');
        });
    });
});
