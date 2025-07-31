import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { BehaviorSubject, of } from 'rxjs';

import { ActivatedRoute } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotAnalyticsDashboardStore } from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';

import { DotAnalyticsDashboardChartComponent } from './components/dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardFiltersComponent } from './components/dot-analytics-dashboard-filters/dot-analytics-dashboard-filters.component';
import { DotAnalyticsDashboardMetricsComponent } from './components/dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsDashboardTableComponent } from './components/dot-analytics-dashboard-table/dot-analytics-dashboard-table.component';
import DotAnalyticsDashboardComponent from './dot-analytics-dashboard.component';

describe('DotAnalyticsDashboardComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardComponent>;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let mockStore: any;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let mockGlobalStore: any;

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardComponent,
        declarations: [
            MockComponent(DotAnalyticsDashboardChartComponent),
            MockComponent(DotAnalyticsDashboardFiltersComponent),
            MockComponent(DotAnalyticsDashboardMetricsComponent),
            MockComponent(DotAnalyticsDashboardTableComponent)
        ],
        mocks: [DotMessageService]
    });

    beforeEach(() => {
        jest.clearAllMocks();

        // Simple store mock with basic signals
        mockStore = {
            timeRange: jest.fn().mockReturnValue('from 7 days ago to now'),
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
                data: { 'request.totalRequest': '89', 'request.pageTitle': 'Home' },
                error: null
            }),
            pageViewTimeLine: jest.fn().mockReturnValue({
                status: ComponentStatus.LOADED,
                data: [],
                error: null
            }),
            pageViewDeviceBrowsers: jest.fn().mockReturnValue({
                status: ComponentStatus.LOADED,
                data: [],
                error: null
            }),
            topPagesTable: jest.fn().mockReturnValue({
                status: ComponentStatus.LOADED,
                data: [],
                error: null
            }),
            setTimeRange: jest.fn(),
            loadAllDashboardData: jest.fn()
        };

        // Simple GlobalStore mock
        mockGlobalStore = {
            currentSiteId: jest.fn().mockReturnValue('test-site-123')
        };

        spectator = createComponent({
            providers: [
                {
                    provide: DotAnalyticsDashboardStore,
                    useValue: mockStore
                },
                {
                    provide: GlobalStore,
                    useValue: mockGlobalStore
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        queryParams: of({}),
                        snapshot: { queryParams: {} }
                    }
                }
            ]
        });
    });

    describe('Component Rendering', () => {
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
    });

    describe('User Interactions', () => {
        it('should call onRefresh when refresh button is clicked', () => {
            const spy = jest.spyOn(spectator.component, 'onRefresh');

            const refreshButton = spectator.query(byTestId('refresh-button'));
            expect(refreshButton).toExist();

            spectator.triggerEventHandler('[data-testid="refresh-button"]', 'onClick', null);

            expect(spy).toHaveBeenCalledTimes(1);
        });

        it('should call store loadAllDashboardData when onRefresh is executed', () => {
            spectator.component.onRefresh();

            expect(mockStore.loadAllDashboardData).toHaveBeenCalledWith(
                'from 7 days ago to now',
                'test-site-123'
            );
        });
    });

    describe('OnInit Query Params Logic', () => {
        it('should call setTimeRange when valid predefined time range in query params', () => {
            // Mock the route.queryParams to emit new values
            const mockRoute = spectator.inject(ActivatedRoute);
            const queryParamsSubject = new BehaviorSubject({ time_range: 'last7days' });

            // Replace the observable
            Object.defineProperty(mockRoute, 'queryParams', {
                value: queryParamsSubject.asObservable()
            });

            // Clear previous calls and call ngOnInit
            jest.clearAllMocks();
            spectator.component.ngOnInit();

            expect(mockStore.setTimeRange).toHaveBeenCalledWith('from 7 days ago to now');
        });

        it('should call setTimeRange when valid custom date range in query params', () => {
            const mockRoute = spectator.inject(ActivatedRoute);
            const queryParamsSubject = new BehaviorSubject({
                time_range: 'custom',
                from: '2024-01-01',
                to: '2024-01-31'
            });

            Object.defineProperty(mockRoute, 'queryParams', {
                value: queryParamsSubject.asObservable()
            });

            jest.clearAllMocks();
            spectator.component.ngOnInit();

            expect(mockStore.setTimeRange).toHaveBeenCalledWith(['2024-01-01', '2024-01-31']);
        });

        it('should not call setTimeRange when invalid query params', () => {
            const mockRoute = spectator.inject(ActivatedRoute);
            const queryParamsSubject = new BehaviorSubject({ time_range: 'invalid-range' });

            Object.defineProperty(mockRoute, 'queryParams', {
                value: queryParamsSubject.asObservable()
            });

            jest.clearAllMocks();
            spectator.component.ngOnInit();

            expect(mockStore.setTimeRange).not.toHaveBeenCalled();
        });

        it('should not call setTimeRange when custom range has incomplete dates', () => {
            const mockRoute = spectator.inject(ActivatedRoute);
            const queryParamsSubject = new BehaviorSubject({
                time_range: 'custom',
                from: '2024-01-01'
                // missing 'to' date
            });

            Object.defineProperty(mockRoute, 'queryParams', {
                value: queryParamsSubject.asObservable()
            });

            jest.clearAllMocks();
            spectator.component.ngOnInit();

            expect(mockStore.setTimeRange).not.toHaveBeenCalled();
        });

        it('should not call setTimeRange when no query params provided', () => {
            const mockRoute = spectator.inject(ActivatedRoute);
            const queryParamsSubject = new BehaviorSubject({});

            Object.defineProperty(mockRoute, 'queryParams', {
                value: queryParamsSubject.asObservable()
            });

            jest.clearAllMocks();
            spectator.component.ngOnInit();

            expect(mockStore.setTimeRange).not.toHaveBeenCalled();
        });
    });
});
