import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
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

import DotAnalyticsDashboardEngagementComponent from './dot-analytics-dashboard-engagement.component';

import { DotAnalyticsDashboardChartComponent } from '../dot-analytics-dashboard-chart/dot-analytics-dashboard-chart.component';
import { DotAnalyticsDashboardMetricsComponent } from '../dot-analytics-dashboard-metrics/dot-analytics-dashboard-metrics.component';
import { DotAnalyticsPlatformsTableComponent } from '../dot-analytics-platforms-table/dot-analytics-platforms-table.component';
import { DotAnalyticsSparklineComponent } from '../dot-analytics-sparkline/dot-analytics-sparkline.component';

describe('DotAnalyticsDashboardEngagementComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardEngagementComponent>;

    const mockEngagementData = signal({
        status: ComponentStatus.LOADED,
        data: MOCK_ENGAGEMENT_DATA,
        error: null
    });

    const mockGlobalStore = {
        addNewBreadcrumb: jest.fn()
    };

    const mockMessageService = {
        get: jest.fn().mockReturnValue('Engagement')
    };

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardEngagementComponent,
        imports: [ButtonModule, DialogModule, DotMessagePipe],
        declarations: [
            MockComponent(DotAnalyticsDashboardMetricsComponent),
            MockComponent(DotAnalyticsDashboardChartComponent),
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
            {
                provide: DotMessageService,
                useValue: mockMessageService
            }
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            spectator = createComponent();
            expect(spectator.component).toBeTruthy();
        });

        it('should add breadcrumb on init', () => {
            spectator = createComponent();
            expect(mockGlobalStore.addNewBreadcrumb).toHaveBeenCalledWith({
                label: 'Engagement'
            });
        });
    });

    describe('Dashboard Layout', () => {
        it('should display 4 metric components (1 engagement rate + 3 KPIs)', () => {
            spectator = createComponent();
            const metrics = spectator.queryAll(DotAnalyticsDashboardMetricsComponent);
            expect(metrics.length).toBe(4);
        });

        it('should display 2 chart components (trend bar + breakdown doughnut)', () => {
            spectator = createComponent();
            const charts = spectator.queryAll(DotAnalyticsDashboardChartComponent);
            expect(charts.length).toBe(2);
        });

        it('should display sparkline component inside engagement rate metric', () => {
            spectator = createComponent();
            const sparklines = spectator.queryAll(DotAnalyticsSparklineComponent);
            expect(sparklines.length).toBe(1);
        });

        it('should display platforms table component', () => {
            spectator = createComponent();
            const platformsTable = spectator.query(DotAnalyticsPlatformsTableComponent);
            expect(platformsTable).toBeTruthy();
        });
    });

    describe("How it's calculated Dialog", () => {
        it('should have dialog hidden by default', () => {
            spectator = createComponent();
            expect(spectator.component.$showCalculationDialog()).toBe(false);
        });

        it('should show dialog when button is clicked', () => {
            spectator = createComponent();
            const button = spectator.query('button[pButton]');
            expect(button).toBeTruthy();

            spectator.click(button!);
            expect(spectator.component.$showCalculationDialog()).toBe(true);
        });
    });

    describe('Loading State', () => {
        it('should not show "How it\'s calculated" button when loading', () => {
            mockEngagementData.set({
                status: ComponentStatus.LOADING,
                data: null,
                error: null
            });

            spectator = createComponent();
            const button = spectator.query('button[pButton]');
            expect(button).toBeFalsy();
        });

        it('should show "How it\'s calculated" button when loaded', () => {
            mockEngagementData.set({
                status: ComponentStatus.LOADED,
                data: MOCK_ENGAGEMENT_DATA,
                error: null
            });

            spectator = createComponent();
            const button = spectator.query('button[pButton]');
            expect(button).toBeTruthy();
        });
    });
});
