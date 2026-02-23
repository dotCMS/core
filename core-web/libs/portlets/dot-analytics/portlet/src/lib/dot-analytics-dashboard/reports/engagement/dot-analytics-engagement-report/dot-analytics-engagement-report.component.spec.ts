import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    DotAnalyticsDashboardStore,
    MOCK_ENGAGEMENT_DATA
} from '@dotcms/portlets/dot-analytics/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import DotAnalyticsEngagementReportComponent from './dot-analytics-engagement-report.component';

import { DotAnalyticsChartComponent } from '../../../shared/components/dot-analytics-chart/dot-analytics-chart.component';
import { DotAnalyticsMetricComponent } from '../../../shared/components/dot-analytics-metric/dot-analytics-metric.component';
import { DotAnalyticsSparklineComponent } from '../../../shared/components/dot-analytics-sparkline/dot-analytics-sparkline.component';
import { DotAnalyticsPlatformsTableComponent } from '../dot-analytics-platforms-table/dot-analytics-platforms-table.component';

describe('DotAnalyticsEngagementReportComponent', () => {
    let spectator: Spectator<DotAnalyticsEngagementReportComponent>;

    const mockEngagementData = signal({
        status: ComponentStatus.LOADED,
        data: MOCK_ENGAGEMENT_DATA,
        error: null
    });

    const mockGlobalStore = {
        addNewBreadcrumb: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotAnalyticsEngagementReportComponent,
        imports: [ButtonModule, DialogModule, DotMessagePipe],
        declarations: [
            MockComponent(DotAnalyticsMetricComponent),
            MockComponent(DotAnalyticsChartComponent),
            MockComponent(DotAnalyticsPlatformsTableComponent),
            MockComponent(DotAnalyticsSparklineComponent)
        ],
        providers: [
            {
                provide: DotAnalyticsDashboardStore,
                useValue: {
                    engagementData: mockEngagementData
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
        mockEngagementData.set({
            status: ComponentStatus.LOADED,
            data: MOCK_ENGAGEMENT_DATA,
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

        it('should display 1 chart (breakdown doughnut)', () => {
            spectator = createComponent();
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

        it('should display platforms table in breakdown section', () => {
            spectator = createComponent();
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
        it('should have $isLoaded as false when loading', () => {
            mockEngagementData.set({
                status: ComponentStatus.LOADING,
                data: null,
                error: null
            });

            spectator = createComponent();
            spectator.detectChanges();
            expect(spectator.component.$isLoaded()).toBe(false);
        });

        it('should have $isLoaded as true when loaded', () => {
            mockEngagementData.set({
                status: ComponentStatus.LOADED,
                data: MOCK_ENGAGEMENT_DATA,
                error: null
            });

            spectator = createComponent();
            spectator.detectChanges();
            expect(spectator.component.$isLoaded()).toBe(true);
        });
    });
});
