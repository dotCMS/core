import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotAnalyticsDashboardStore, TimeRange } from '@dotcms/portlets/dot-analytics/data-access';

import DotAnalyticsDashboardComponent from './dot-analytics-dashboard.component';

describe('DotAnalyticsDashboardComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardComponent>;

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
            data: { 'request.totalUser': '342' },
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

    const mockStore = {
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

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            {
                provide: DotAnalyticsDashboardStore,
                useValue: mockStore
            },
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn().mockReturnValue('Translated message')
                }
            }
        ]
    });

    beforeEach(() => {
        // Reset all mocks
        jest.clearAllMocks();

        spectator = createComponent({
            detectChanges: false // Prevent automatic change detection
        });
    });

    describe('Component Initialization', () => {
        it('should create component successfully', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have all required child components with data-testid', () => {
            spectator.detectChanges();

            // Verificar que los componentes hijos existan usando data-testid
            expect(spectator.query(byTestId('analytics-filters'))).toExist();
            expect(spectator.query(byTestId('refresh-button'))).toExist();
            expect(spectator.query(byTestId('analytics-metric-card'))).toExist();
            expect(spectator.query(byTestId('analytics-timeline-chart'))).toExist();
            expect(spectator.query(byTestId('analytics-device-chart'))).toExist();
            expect(spectator.query(byTestId('analytics-table'))).toExist();
        });

        it('should render multiple metric cards', () => {
            spectator.detectChanges();

            // Verificar que se renderizan múltiples metric cards
            const metricCards = spectator.queryAll(byTestId('analytics-metric-card'));
            expect(metricCards).toHaveLength(3); // Basado en nuestro mock que tiene 3 elementos
        });
    });

    describe('Component State', () => {
        it('should expose store signals correctly', () => {
            // Time range
            expect(spectator.component['$currentTimeRange']()).toBe('from 7 days ago to now');

            // Direct resource signals
            expect(spectator.component['$totalPageViews']()).toEqual({
                status: ComponentStatus.LOADED,
                data: { 'request.totalRequest': '1250' },
                error: null
            });

            expect(spectator.component['$uniqueVisitors']()).toEqual({
                status: ComponentStatus.LOADED,
                data: { 'request.totalUser': '342' },
                error: null
            });

            expect(spectator.component['$topPagePerformance']()).toEqual({
                status: ComponentStatus.LOADED,
                data: {
                    'request.totalRequest': '89',
                    'request.pageTitle': 'Home Page',
                    'request.path': '/home'
                },
                error: null
            });

            expect(spectator.component['$pageViewTimeLine']()).toEqual({
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
            });

            expect(spectator.component['$pageViewDeviceBrowsers']()).toEqual({
                status: ComponentStatus.LOADED,
                data: [
                    { 'request.totalRequest': '60', 'request.userAgent': 'Chrome' },
                    { 'request.totalRequest': '40', 'request.userAgent': 'Firefox' }
                ],
                error: null
            });

            expect(spectator.component['$topPagesTable']()).toEqual({
                status: ComponentStatus.LOADED,
                data: [
                    {
                        'request.pageTitle': 'Home',
                        'request.path': '/home',
                        'request.totalRequest': '100'
                    }
                ],
                error: null
            });

            // Computed/transformed data
            expect(spectator.component['$metricsData']()).toHaveLength(3);
            expect(spectator.component['$topPagesTableData']()).toHaveLength(1);
            expect(spectator.component['$pageviewsTimelineData']()).toEqual({
                labels: ['Jan', 'Feb'],
                datasets: [{ label: 'Page Views', data: [10, 20] }]
            });
            expect(spectator.component['$deviceBreakdownData']()).toEqual({
                labels: ['Chrome', 'Firefox'],
                datasets: [{ label: 'Device Usage', data: [60, 40] }]
            });

            // Status signal (the one the user kept)
            expect(spectator.component['$deviceBreakdownStatus']()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('Public Methods', () => {
        describe('onTimeRangeChange', () => {
            it('should call store setTimeRange with provided time range', () => {
                const newTimeRange: TimeRange = 'from 30 days ago to now';

                spectator.component.onTimeRangeChange(newTimeRange);

                expect(mockStore.setTimeRange).toHaveBeenCalledWith(newTimeRange);
            });
        });

        describe('onRefresh', () => {
            it('should call loadAllDashboardData with current time range', () => {
                const currentTimeRange = 'from 7 days ago to now';

                spectator.component.onRefresh();

                expect(mockStore.loadAllDashboardData).toHaveBeenCalledWith(currentTimeRange);
            });
        });

        describe('onReset', () => {
            it('should exist but not throw error (pending implementation)', () => {
                expect(() => spectator.component.onReset()).not.toThrow();
            });
        });
    });

    describe('Store Integration', () => {
        it('should have access to store methods', () => {
            // Verificar que el componente tiene acceso a los métodos del store
            expect(typeof spectator.component.onTimeRangeChange).toBe('function');
            expect(typeof spectator.component.onRefresh).toBe('function');
            expect(typeof spectator.component.onReset).toBe('function');
        });

        it('should handle different time ranges', () => {
            const timeRanges: TimeRange[] = ['from 7 days ago to now', 'from 30 days ago to now'];

            timeRanges.forEach((timeRange) => {
                spectator.component.onTimeRangeChange(timeRange);
                expect(mockStore.setTimeRange).toHaveBeenCalledWith(timeRange);
            });
        });
    });

    describe('Component Properties', () => {
        it('should have all required signal properties', () => {
            // Verificar que el componente tiene todas las propiedades signals esperadas
            expect(spectator.component['$currentTimeRange']).toBeDefined();

            // Direct resource signals
            expect(spectator.component['$totalPageViews']).toBeDefined();
            expect(spectator.component['$uniqueVisitors']).toBeDefined();
            expect(spectator.component['$topPagePerformance']).toBeDefined();
            expect(spectator.component['$pageViewTimeLine']).toBeDefined();
            expect(spectator.component['$pageViewDeviceBrowsers']).toBeDefined();
            expect(spectator.component['$topPagesTable']).toBeDefined();

            // Computed/transformed data
            expect(spectator.component['$metricsData']).toBeDefined();
            expect(spectator.component['$topPagesTableData']).toBeDefined();
            expect(spectator.component['$pageviewsTimelineData']).toBeDefined();
            expect(spectator.component['$deviceBreakdownData']).toBeDefined();

            // Status signal (the one the user kept)
            expect(spectator.component['$deviceBreakdownStatus']).toBeDefined();
        });
    });

    describe('Button Interactions', () => {
        it('should call onRefresh when refresh button is clicked', () => {
            const spy = jest.spyOn(spectator.component, 'onRefresh');

            // Trigger change detection to render the button
            spectator.detectChanges();

            // Find the refresh button
            const refreshButton = spectator.query(byTestId('refresh-button'));
            expect(refreshButton).toExist();

            // Simulate click event using triggerEventHandler
            spectator.triggerEventHandler('[data-testid="refresh-button"]', 'onClick', null);

            // Verify the method was called
            expect(spy).toHaveBeenCalledTimes(1);
        });

        it('should verify refresh button has correct configuration', () => {
            spectator.detectChanges();

            const refreshButton = spectator.query(byTestId('refresh-button'));
            expect(refreshButton).toExist();

            // Verify button attributes
            expect(refreshButton).toHaveAttribute('data-testid', 'refresh-button');
            expect(refreshButton).toHaveClass('analytics-dashboard__refresh-button');
        });
    });
});
