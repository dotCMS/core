import {
    byTestId,
    createRoutingFactory,
    mockProvider,
    SpectatorRouting,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { ActivatedRoute, Router } from '@angular/router';

import { DotLocalstorageService, DotMessageService } from '@dotcms/data-access';
import {
    DotAnalyticsDashboardStore,
    DotAnalyticsService,
    TIME_RANGE_OPTIONS
} from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAnalyticsDashboardChartComponent } from './components/dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardFiltersComponent } from './components/dot-analytics-dashboard-filters/dot-analytics-dashboard-filters.component';
import { DotAnalyticsDashboardMetricsComponent } from './components/dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsDashboardTableComponent } from './components/dot-analytics-dashboard-table/dot-analytics-dashboard-table.component';
import DotAnalyticsDashboardComponent from './dot-analytics-dashboard.component';

const messageServiceMock = new MockDotMessageService({
    'analytics.metrics.total-pageviews': 'Total Pageviews',
    'analytics.metrics.unique-visitors': 'Unique Visitors',
    'analytics.metrics.top-page-performance': 'Top Page Performance',
    'analytics.feature.state': 'This feature is in',
    development: 'development'
});

describe('DotAnalyticsDashboardComponent', () => {
    let spectator: SpectatorRouting<DotAnalyticsDashboardComponent>;
    let store: InstanceType<typeof DotAnalyticsDashboardStore>;
    let router: SpyObject<Router>;

    const createComponent = createRoutingFactory({
        component: DotAnalyticsDashboardComponent,
        declarations: [
            MockComponent(DotAnalyticsDashboardChartComponent),
            MockComponent(DotAnalyticsDashboardFiltersComponent),
            MockComponent(DotAnalyticsDashboardMetricsComponent),
            MockComponent(DotAnalyticsDashboardTableComponent)
        ],
        providers: [
            DotAnalyticsDashboardStore,
            mockProvider(DotMessageService),
            mockProvider(DotAnalyticsService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(GlobalStore, {
                currentSiteId: jest.fn().mockReturnValue('test-site-123')
            }),
            mockProvider(DotLocalstorageService),
            mockProvider(Router)
        ]
    });

    describe('Component Rendering', () => {
        beforeEach(() => {
            spectator = createComponent();
            store = spectator.inject(DotAnalyticsDashboardStore);
        });

        it('should create component successfully', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should render exactly 3 metric cards', () => {
            const metricCards = spectator.queryAll(byTestId('analytics-metric-card'));
            expect(metricCards).toHaveLength(3);
        });

        it('should render line chart component', () => {
            const timelineChart = spectator.query(byTestId('analytics-timeline-chart'));
            expect(timelineChart).toExist();
        });

        it('should render pie chart component', () => {
            const deviceChart = spectator.query(byTestId('analytics-device-chart'));
            expect(deviceChart).toExist();
        });

        it('should render table component', () => {
            const table = spectator.query(byTestId('analytics-table'));
            expect(table).toExist();
        });

        it('should render filters component', () => {
            const filters = spectator.query(byTestId('analytics-filters'));
            expect(filters).toExist();
        });

        it('should render refresh button', () => {
            const refreshButton = spectator.query(byTestId('refresh-button'));
            expect(refreshButton).toExist();
        });

        describe('User Interactions', () => {
            it('should call onRefresh when refresh button is clicked', () => {
                const spy = jest.spyOn(store, 'loadAllDashboardData');

                const refreshButton = spectator.query(byTestId('refresh-button'));
                expect(refreshButton).toExist();

                spectator.triggerEventHandler('[data-testid="refresh-button"]', 'onClick', null);

                expect(spy).toHaveBeenCalledWith(TIME_RANGE_OPTIONS.last7days, 'test-site-123');
            });
        });
    });

    describe('Query Params Logic', () => {
        it('should timeRange be last7days when empty query params', () => {
            spectator = createComponent();
            store = spectator.inject(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should timeRange be last7days when valid predefined time range in query params', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'last7days'
                }
            });
            store = spectator.inject(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should timeRange be custom date range when valid custom date range in query params', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'custom',
                    from: '2024-01-01',
                    to: '2024-01-31'
                }
            });
            store = spectator.inject(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toEqual(['2024-01-01', '2024-01-31']);
        });

        it('should call router.navigate with invalid query params', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'invalid-range'
                }
            });
            router = spectator.inject(Router);
            const route = spectator.inject(ActivatedRoute);

            expect(router.navigate).toHaveBeenCalledWith([], {
                relativeTo: route,
                queryParams: {
                    time_range: 'last7days'
                },
                queryParamsHandling: 'replace',
                replaceUrl: true
            });
        });

        it('should call router.navigate with custom range has incomplete dates', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'custom',
                    from: '2024-01-01'
                    // to: '2024-01-31'
                }
            });
            router = spectator.inject(Router);
            const route = spectator.inject(ActivatedRoute);

            expect(router.navigate).toHaveBeenCalledWith([], {
                relativeTo: route,
                queryParams: {
                    time_range: 'last7days'
                },
                queryParamsHandling: 'replace',
                replaceUrl: true
            });
        });

        it('should call router.navigate with invalid custom range ', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'custom',
                    from: '2024-01-01',
                    to: '1993-01-31'
                }
            });
            router = spectator.inject(Router);
            const route = spectator.inject(ActivatedRoute);

            expect(router.navigate).toHaveBeenCalledWith([], {
                relativeTo: route,
                queryParams: {
                    time_range: 'last7days'
                },
                queryParamsHandling: 'replace',
                replaceUrl: true
            });
        });
    });

    describe('Development Status Banner', () => {
        it('should show message banner when localStorage key is not present', () => {
            const mockLocalStorage = {
                getItem: jest.fn().mockReturnValue(null),
                setItem: jest.fn()
            };

            spectator = createComponent({
                providers: [
                    {
                        provide: DotLocalstorageService,
                        useValue: mockLocalStorage
                    }
                ]
            });

            spectator.detectChanges();

            const messageBanner = spectator.query(byTestId('analytics-message'));
            expect(messageBanner).toExist();
        });

        it('should hide message banner when localStorage key is present', () => {
            const mockLocalStorage = {
                getItem: jest.fn().mockReturnValue(true),
                setItem: jest.fn()
            };

            spectator = createComponent({
                providers: [
                    {
                        provide: DotLocalstorageService,
                        useValue: mockLocalStorage
                    }
                ]
            });

            spectator.detectChanges();

            const messageBanner = spectator.query(byTestId('analytics-message'));
            expect(messageBanner).not.toExist();
        });

        it('should display correct message content', () => {
            const mockLocalStorage = {
                getItem: jest.fn().mockReturnValue(null),
                setItem: jest.fn()
            };

            spectator = createComponent({
                providers: [
                    {
                        provide: DotLocalstorageService,
                        useValue: mockLocalStorage
                    }
                ]
            });

            spectator.detectChanges();

            const messageContent = spectator.query(byTestId('message-content'));
            expect(messageContent).toExist();
            expect(messageContent).toContainText('This feature is in');
            expect(messageContent).toContainText('development');
        });

        it('should hide message and save to localStorage when close button is clicked', () => {
            const mockLocalStorage = {
                getItem: jest.fn().mockReturnValue(null),
                setItem: jest.fn()
            };

            spectator = createComponent({
                providers: [
                    {
                        provide: DotLocalstorageService,
                        useValue: mockLocalStorage
                    }
                ]
            });

            spectator.detectChanges();

            let messageBanner = spectator.query(byTestId('analytics-message'));
            expect(messageBanner).toExist();

            const closeButton = spectator.query(byTestId('close-message'));
            expect(closeButton).toExist();

            spectator.click(closeButton);
            spectator.detectChanges();

            expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
                'analytics-dashboard-hide-message-banner',
                true
            );

            messageBanner = spectator.query(byTestId('analytics-message'));
            expect(messageBanner).not.toExist();
        });
    });
});
