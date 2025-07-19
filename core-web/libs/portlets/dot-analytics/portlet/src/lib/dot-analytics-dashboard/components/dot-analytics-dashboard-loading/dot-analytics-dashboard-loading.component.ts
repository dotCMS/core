import { ChangeDetectionStrategy, Component } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

/**
 * Loading skeleton component for the analytics dashboard
 *
 * Provides a loading state that mimics the dashboard structure with skeleton placeholders
 * for metrics cards, charts, and data table.
 *
 */
@Component({
    selector: 'dot-analytics-dashboard-loading',
    templateUrl: './dot-analytics-dashboard-loading.component.html',
    styleUrls: ['./dot-analytics-dashboard-loading.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [SkeletonModule, CardModule]
})
export class DotAnalyticsDashboardLoadingComponent {
    /**
     * Array for metrics cards loop (3 cards)
     */
    readonly metricsArray = [1, 2, 3];
}
