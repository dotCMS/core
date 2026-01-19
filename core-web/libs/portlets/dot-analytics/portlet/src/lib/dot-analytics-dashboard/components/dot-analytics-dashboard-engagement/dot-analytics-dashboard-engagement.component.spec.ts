import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { signal } from '@angular/core';

import { CardModule } from 'primeng/card';
import { ProgressBarModule } from 'primeng/progressbar';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';

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
import { DotAnalyticsSparklineComponent } from '../dot-analytics-sparkline/dot-analytics-sparkline.component';

describe('DotAnalyticsDashboardEngagementComponent', () => {
    let spectator: Spectator<DotAnalyticsDashboardEngagementComponent>;
    const mockEngagementData = signal({
        status: ComponentStatus.LOADED,
        data: MOCK_ENGAGEMENT_DATA,
        error: null
    });

    const createComponent = createComponentFactory({
        component: DotAnalyticsDashboardEngagementComponent,
        imports: [CardModule, ProgressBarModule, TableModule, TabViewModule, DotMessagePipe],
        declarations: [
            MockComponent(DotAnalyticsDashboardMetricsComponent),
            MockComponent(DotAnalyticsDashboardChartComponent),
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
                useValue: {
                    addNewBreadcrumb: jest.fn()
                }
            },
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn().mockReturnValue('Engagement')
                }
            }
        ]
    });

    it('should create', () => {
        spectator = createComponent();
        expect(spectator.component).toBeTruthy();
    });

    it('should display KPI metrics', () => {
        spectator = createComponent();
        const metrics = spectator.queryAll(DotAnalyticsDashboardMetricsComponent);
        // 1 Engagement Rate + 3 metrics (Avg Interactions, Avg Session Time, Conversion Rate)
        expect(metrics.length).toBe(4);
    });

    it('should display Charts', () => {
        spectator = createComponent();
        const charts = spectator.queryAll(DotAnalyticsDashboardChartComponent);
        expect(charts.length).toBe(2);
    });

    it('should display Sparkline in Engagement Rate metric', () => {
        spectator = createComponent();
        const sparklines = spectator.queryAll(DotAnalyticsSparklineComponent);
        expect(sparklines.length).toBe(1);
    });

    it('should display Platform tabs', () => {
        spectator = createComponent();
        const tabView = spectator.query('p-tabView');
        expect(tabView).toBeTruthy();
    });
});
