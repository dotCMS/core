import {
    byTestId,
    createRoutingFactory,
    mockProvider,
    SpectatorRouting
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { MessageModule } from 'primeng/message';

import { DotLocalstorageService, DotMessageService } from '@dotcms/data-access';
import {
    DotAnalyticsDashboardStore,
    DotAnalyticsService,
    TIME_RANGE_OPTIONS
} from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe, MockDotMessageService } from '@dotcms/utils-testing';

import DotAnalyticsDashboardComponent from './dot-analytics-dashboard.component';
import { DotAnalyticsTopPagesTableComponent } from './reports/pageview/dot-analytics-top-pages-table/dot-analytics-top-pages-table.component';
import { DotAnalyticsChartComponent } from './shared/components/dot-analytics-chart/dot-analytics-chart.component';
import { DotAnalyticsFiltersComponent } from './shared/components/dot-analytics-filters/dot-analytics-filters.component';
import { DotAnalyticsMetricComponent } from './shared/components/dot-analytics-metric/dot-analytics-metric.component';

const messageServiceMock = new MockDotMessageService({
    'analytics.metrics.total-pageviews': 'Total Pageviews',
    'analytics.metrics.unique-visitors': 'Unique Visitors',
    'analytics.metrics.top-page-performance': 'Top Page Performance',
    'analytics.feature.state': 'This feature is in',
    development: 'development',
    'analytics.dashboard.tabs.pageview': 'Pageview',
    'analytics.dashboard.tabs.conversions': 'Conversions',
    'analytics.dashboard.tabs.engagement': 'Engagement'
});

describe('DotAnalyticsDashboardComponent', () => {
    let spectator: SpectatorRouting<DotAnalyticsDashboardComponent>;
    let store: InstanceType<typeof DotAnalyticsDashboardStore>;

    const defaultLocalStorageMock = {
        getItem: jest.fn().mockReturnValue(true), // Por defecto, el banner está oculto
        setItem: jest.fn()
    };

    const createComponent = createRoutingFactory({
        component: DotAnalyticsDashboardComponent,
        imports: [MessageModule, DotMessagePipe],
        declarations: [
            MockComponent(DotAnalyticsChartComponent),
            MockComponent(DotAnalyticsFiltersComponent),
            MockComponent(DotAnalyticsMetricComponent),
            MockComponent(DotAnalyticsTopPagesTableComponent)
        ],
        componentProviders: [DotAnalyticsDashboardStore],
        providers: [
            mockProvider(DotAnalyticsService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(GlobalStore, {
                currentSiteId: jest.fn().mockReturnValue('test-site-123'),
                addNewBreadcrumb: jest.fn()
            }),
            {
                provide: DotLocalstorageService,
                useValue: defaultLocalStorageMock
            },
            mockProvider(Router)
        ]
    });

    describe('Component Rendering', () => {
        beforeEach(() => {
            spectator = createComponent();
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);
        });

        it('should create component successfully', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should render engagement report component by default', () => {
            const engagementReport = spectator.query('dot-analytics-engagement-report');
            expect(engagementReport).toExist();
        });

        it('should render tab panels for each report type', () => {
            const tabPanels = spectator.queryAll('p-tabpanel');
            expect(tabPanels.length).toBe(3);
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
                const spy = jest.spyOn(spectator.component, 'onRefresh');

                const refreshButton = spectator.query(byTestId('refresh-button'));
                expect(refreshButton).toExist();

                spectator.triggerEventHandler('[data-testid="refresh-button"]', 'onClick', null);

                expect(spy).toHaveBeenCalled();
            });
        });
    });

    describe('Query Params Logic', () => {
        it('should timeRange be last7days when empty query params', () => {
            spectator = createComponent();
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should timeRange be last7days when valid predefined time range in query params', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'last7days'
                }
            });
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);

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
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toEqual(['2024-01-01', '2024-01-31']);
        });

        it('should fall back to last7days when query param is invalid', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'invalid-range'
                }
            });
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should set timeRange to last7days when custom range has incomplete dates', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'custom',
                    from: '2024-01-01'
                    // to: '2024-01-31'
                }
            });
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toBe('last7days');
        });

        it('should set timeRange to custom range when dates are inverted in query params', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'custom',
                    from: '2024-01-01',
                    to: '1993-01-31'
                }
            });
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);

            // paramsToTimeRange passes through raw date values without order validation;
            // date validation is enforced in the UI layer via isValidCustomDateRange
            expect(store.timeRange()).toEqual(['2024-01-01', '1993-01-31']);
        });

        it('should set timeRange to last7days when today is in query params', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'today'
                }
            });
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toBe(TIME_RANGE_OPTIONS.last7days);
        });

        it('should set timeRange to last7days when yesterday is in query params', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'yesterday'
                }
            });
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toBe(TIME_RANGE_OPTIONS.last7days);
        });
    });

    describe('Breadcrumb Management', () => {
        let globalStore: InstanceType<typeof GlobalStore>;

        beforeEach(() => {
            spectator = createComponent();
            store = spectator.fixture.debugElement.injector.get(DotAnalyticsDashboardStore);
            globalStore = spectator.inject(GlobalStore);
            TestBed.flushEffects();
        });

        it('should call addNewBreadcrumb with the default tab on initialization', () => {
            expect(globalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                expect.objectContaining({ id: 'analytics-engagement', label: 'Engagement' })
            );
        });

        it('should call addNewBreadcrumb with the new tab when tab changes', () => {
            jest.clearAllMocks();
            store.setCurrentTab('conversions');
            TestBed.flushEffects();

            expect(globalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                expect.objectContaining({ id: 'analytics-conversions', label: 'Conversions' })
            );
        });

        it('should call addNewBreadcrumb with pageview tab when switched', () => {
            jest.clearAllMocks();
            store.setCurrentTab('pageview');
            TestBed.flushEffects();

            expect(globalStore.addNewBreadcrumb).toHaveBeenCalledWith(
                expect.objectContaining({ id: 'analytics-pageview', label: 'Pageview' })
            );
        });
    });

    describe('Development Status Banner', () => {
        beforeEach(() => {
            jest.clearAllMocks();
        });

        it('should show the message banner', () => {
            defaultLocalStorageMock.getItem.mockReturnValue(null);
            spectator = createComponent();
            spectator.detectChanges();

            const message = spectator.query(byTestId('analytics-message'));
            expect(message).toExist();
        });

        it('should show the message content', () => {
            defaultLocalStorageMock.getItem.mockReturnValue(null);
            spectator = createComponent();
            spectator.detectChanges();

            const messageContent = spectator.query(byTestId('message-content'));
            expect(messageContent).toExist();
        });

        it('should set $showMessage to false when close button is clicked', () => {
            defaultLocalStorageMock.getItem.mockReturnValue(null);
            spectator = createComponent();
            spectator.detectChanges();

            // PrimeNG 21: close button is inside p-message with aria-label attribute
            const closeButton = spectator.query('p-message button[aria-label]');
            closeButton?.dispatchEvent(new Event('click'));
            spectator.detectChanges();

            expect(spectator.component.$showMessage()).toBe(false);
        });

        it('should return true if the hide message banner key is not set', () => {
            defaultLocalStorageMock.getItem.mockReturnValue(undefined);
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.component.$showMessage()).toBe(true);
        });

        it('should return false if the hide message banner key is set', () => {
            defaultLocalStorageMock.getItem.mockReturnValue(true);
            spectator = createComponent();
            spectator.detectComponentChanges();

            expect(spectator.component.$showMessage()).toBe(false);
        });

        it('should call the localStorage service to set the hide message banner key', () => {
            defaultLocalStorageMock.getItem.mockReturnValue(null);
            spectator = createComponent();
            spectator.detectChanges();

            // PrimeNG 21: close button is inside p-message with aria-label attribute
            const closeButton = spectator.query('p-message button[aria-label]');
            closeButton?.dispatchEvent(new Event('click'));
            spectator.detectChanges();

            expect(defaultLocalStorageMock.setItem).toHaveBeenCalledWith(
                'analytics-dashboard-hide-message-banner',
                true
            );
        });
    });
});
