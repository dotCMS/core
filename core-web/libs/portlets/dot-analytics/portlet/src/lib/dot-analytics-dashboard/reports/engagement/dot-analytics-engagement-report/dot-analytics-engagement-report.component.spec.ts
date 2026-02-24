import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { signal } from '@angular/core';
import { DeferBlockState } from '@angular/core/testing';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import type {
    ChartData,
    EngagementKPIs,
    EngagementPlatforms
} from '@dotcms/portlets/dot-analytics/data-access';
// eslint-disable-next-line no-duplicate-imports
import { DotAnalyticsDashboardStore } from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import DotAnalyticsEngagementReportComponent from './dot-analytics-engagement-report.component';

import { DotAnalyticsChartComponent } from '../../../shared/components/dot-analytics-chart/dot-analytics-chart.component';
import { DotAnalyticsMetricComponent } from '../../../shared/components/dot-analytics-metric/dot-analytics-metric.component';
import { DotAnalyticsSparklineComponent } from '../../../shared/components/dot-analytics-sparkline/dot-analytics-sparkline.component';
import { DotAnalyticsPlatformsTableComponent } from '../dot-analytics-platforms-table/dot-analytics-platforms-table.component';

const MOCK_KPIS: EngagementKPIs = {
    engagementRate: {
        value: 45,
        trend: 8,
        subtitle: '29,203 Engaged Sessions',
        label: 'Engagement Rate'
    },
    avgInteractions: { value: 6.4, trend: 18, label: 'Avg Interactions (Engaged)' },
    avgSessionTime: { value: '2m 34s', trend: 12, label: 'Average Session Time' },
    conversionRate: { value: '3.2%', trend: -0.3, label: 'Conversion Rate' }
};

const MOCK_BREAKDOWN: ChartData = {
    labels: ['Engaged Sessions (65%)', 'Bounced Sessions (35%)'],
    datasets: [
        { label: 'Engagement Breakdown', data: [65, 35], backgroundColor: ['#6366F1', '#000000'] }
    ]
};

const MOCK_PLATFORMS: EngagementPlatforms = {
    device: [
        { name: 'Desktop', views: 77053, percentage: 72, time: '2m 45s' },
        { name: 'Mobile', views: 16071, percentage: 20, time: '1m 47s' },
        { name: 'Tablet', views: 2531, percentage: 8, time: '2m 00s' }
    ],
    browser: [
        { name: 'Chrome', views: 60000, percentage: 65, time: '2m 50s' },
        { name: 'Safari', views: 20000, percentage: 25, time: '2m 30s' },
        { name: 'Firefox', views: 10000, percentage: 10, time: '2m 40s' }
    ],
    language: [
        { name: 'English', views: 80000, percentage: 80, time: '2m 55s' },
        { name: 'Spanish', views: 10000, percentage: 10, time: '2m 20s' },
        { name: 'French', views: 5000, percentage: 5, time: '2m 10s' }
    ]
};

describe('DotAnalyticsEngagementReportComponent', () => {
    let spectator: Spectator<DotAnalyticsEngagementReportComponent>;

    const mockKpis = signal({
        status: ComponentStatus.LOADED,
        data: MOCK_KPIS,
        error: null
    });
    const mockBreakdown = signal({
        status: ComponentStatus.LOADED,
        data: MOCK_BREAKDOWN,
        error: null
    });
    const mockPlatforms = signal({
        status: ComponentStatus.LOADED,
        data: MOCK_PLATFORMS,
        error: null
    });
    const mockSparkline = signal({
        status: ComponentStatus.LOADED,
        data: [],
        error: null
    });

    const mockGlobalStore = {
        addNewBreadcrumb: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotAnalyticsEngagementReportComponent,
        imports: [
            ButtonModule,
            DialogModule,
            DotMessagePipe,
            MockComponent(DotAnalyticsMetricComponent),
            MockComponent(DotAnalyticsChartComponent),
            MockComponent(DotAnalyticsPlatformsTableComponent),
            MockComponent(DotAnalyticsSparklineComponent)
        ],
        providers: [
            {
                provide: DotAnalyticsDashboardStore,
                useValue: {
                    engagementKpis: mockKpis,
                    engagementBreakdown: mockBreakdown,
                    engagementPlatforms: mockPlatforms,
                    engagementSparkline: mockSparkline
                }
            },
            {
                provide: GlobalStore,
                useValue: mockGlobalStore
            },
            mockProvider(DotMessageService, {
                get: jest.fn().mockReturnValue('Engagement')
            })
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        mockKpis.set({
            status: ComponentStatus.LOADED,
            data: MOCK_KPIS,
            error: null
        });
        mockBreakdown.set({
            status: ComponentStatus.LOADED,
            data: MOCK_BREAKDOWN,
            error: null
        });
        mockPlatforms.set({
            status: ComponentStatus.LOADED,
            data: MOCK_PLATFORMS,
            error: null
        });
        mockSparkline.set({
            status: ComponentStatus.LOADED,
            data: [],
            error: null
        });
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            spectator = createComponent();
            spectator.detectChanges();
            expect(spectator.component).toBeTruthy();
        });

        it('should add breadcrumb on init', () => {
            spectator = createComponent();
            spectator.detectChanges();
            expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledWith({
                id: 'engagement',
                label: 'Engagement'
            });
        });
    });

    describe('Dashboard Layout', () => {
        it('should display 4 metric components (1 engagement rate + 3 KPIs)', () => {
            spectator = createComponent();
            spectator.detectChanges();
            const metrics = spectator.queryAll(DotAnalyticsMetricComponent);
            expect(metrics.length).toBe(4);
        });

        it('should display 1 chart (breakdown doughnut) in deferred content', async () => {
            spectator = createComponent();
            spectator.detectChanges();
            const deferBlocks = await spectator.fixture.getDeferBlocks();
            await deferBlocks[0].render(DeferBlockState.Complete);
            spectator.detectChanges();
            const charts = spectator.queryAll(DotAnalyticsChartComponent);
            expect(charts.length).toBe(1);
        });

        it('should display sparkline component inside engagement rate metric', () => {
            spectator = createComponent();
            spectator.detectChanges();
            const sparklines = spectator.queryAll(DotAnalyticsSparklineComponent);
            expect(sparklines.length).toBe(1);
        });

        it('should display platforms table in deferred content', async () => {
            spectator = createComponent();
            spectator.detectChanges();
            const deferBlocks = await spectator.fixture.getDeferBlocks();
            await deferBlocks[0].render(DeferBlockState.Complete);
            spectator.detectChanges();
            const platformsTable = spectator.query(DotAnalyticsPlatformsTableComponent);
            expect(platformsTable).toBeTruthy();
        });
    });

    describe("How it's calculated Dialog", () => {
        it('should have dialog hidden by default', () => {
            spectator = createComponent();
            spectator.detectChanges();
            expect(spectator.component.$showCalculationDialog()).toBe(false);
        });

        it('should show info icon in engagement rate metric', () => {
            spectator = createComponent();
            spectator.detectChanges();
            const infoIcon = spectator.query('.pi-info-circle');
            expect(infoIcon).toBeTruthy();
        });

        it('should open dialog when info icon is clicked', () => {
            spectator = createComponent();
            spectator.detectChanges();
            expect(spectator.component.$showCalculationDialog()).toBe(false);

            spectator.click('.pi-info-circle');
            spectator.detectChanges();
            expect(spectator.component.$showCalculationDialog()).toBe(true);
        });
    });

    describe('Loading State', () => {
        it('should have $isKpisLoaded as false when KPIs are loading', () => {
            mockKpis.set({
                status: ComponentStatus.LOADING,
                data: null,
                error: null
            });

            spectator = createComponent();
            spectator.detectChanges();
            expect(spectator.component.$isKpisLoaded()).toBe(false);
        });

        it('should have $isKpisLoaded as true when KPIs are loaded', () => {
            mockKpis.set({
                status: ComponentStatus.LOADED,
                data: MOCK_KPIS,
                error: null
            });

            spectator = createComponent();
            spectator.detectChanges();
            expect(spectator.component.$isKpisLoaded()).toBe(true);
        });
    });
});
