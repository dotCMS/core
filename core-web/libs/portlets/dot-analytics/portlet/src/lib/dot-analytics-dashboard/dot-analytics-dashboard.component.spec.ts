import {
    byTestId,
    createRoutingFactory,
    mockProvider,
    SpectatorRouting
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

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

    const defaultLocalStorageMock = {
        getItem: jest.fn().mockReturnValue(true), // Por defecto, el banner está oculto
        setItem: jest.fn()
    };

    const createComponent = createRoutingFactory({
        component: DotAnalyticsDashboardComponent,
        imports: [MessageModule, DotMessagePipe],
        declarations: [
            MockComponent(DotAnalyticsDashboardChartComponent),
            MockComponent(DotAnalyticsDashboardFiltersComponent),
            MockComponent(DotAnalyticsDashboardMetricsComponent),
            MockComponent(DotAnalyticsDashboardTableComponent)
        ],
        providers: [
            DotAnalyticsDashboardStore,
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
            store = spectator.inject(DotAnalyticsDashboardStore);
        });

        it('should create component successfully', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should render exactly 3 metric cards', () => {
            // p-tabs renders all panels in DOM, so we need to check only active panel
            const activePanels = spectator.queryAll('p-tabpanel');
            const firstPanel = activePanels[0];
            const metricCards = firstPanel?.querySelectorAll(
                '[data-testid="analytics-metric-card"]'
            );
            expect(metricCards?.length).toBe(3);
        });

        it('should render line chart component', () => {
            const timelineChart = spectator.query(byTestId('analytics-timeline-chart'));
            expect(timelineChart).toExist();
        });

        it('should render pageview report when pageview tab is active', () => {
            const pageviewReport = spectator.query('dot-analytics-dashboard-pageview-report');
            expect(pageviewReport).toExist();
        });

        it('should render tab panels for each report type', () => {
            const tabPanels = spectator.queryAll('p-tabpanel');
            expect(tabPanels.length).toBeGreaterThanOrEqual(1);
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

        it('should set timeRange to invalid value when query param is invalid', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'invalid-range'
                }
            });
            store = spectator.inject(DotAnalyticsDashboardStore);

            // Without validation, invalid values are accepted as-is
            expect(store.timeRange()).toBe('invalid-range');
        });

        it('should set timeRange to last7days when custom range has incomplete dates', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'custom',
                    from: '2024-01-01'
                    // to: '2024-01-31'
                }
            });
            store = spectator.inject(DotAnalyticsDashboardStore);

            expect(store.timeRange()).toBe('last7days');
        });

        it('should set timeRange to custom range even when dates are inverted', () => {
            spectator = createComponent({
                queryParams: {
                    time_range: 'custom',
                    from: '2024-01-01',
                    to: '1993-01-31'
                }
            });
            store = spectator.inject(DotAnalyticsDashboardStore);

            // Without validation, inverted date ranges are accepted
            expect(store.timeRange()).toEqual(['2024-01-01', '1993-01-31']);
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
