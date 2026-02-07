import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TabsModule } from 'primeng/tabs';

import { map } from 'rxjs/operators';

import { DotLocalstorageService } from '@dotcms/data-access';
import {
    DASHBOARD_TAB_LIST,
    DASHBOARD_TABS,
    DashboardTab,
    DotAnalyticsDashboardStore,
    isValidTab,
    TimeRangeInput
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import DotAnalyticsDashboardConversionsReportComponent from './components/dot-analytics-dashboard-conversions-report/dot-analytics-dashboard-conversions-report.component';
import DotAnalyticsDashboardEngagementReportComponent from './components/dot-analytics-dashboard-engagement-report/dot-analytics-dashboard-engagement-report.component';
import { DotAnalyticsDashboardFiltersComponent } from './components/dot-analytics-dashboard-filters/dot-analytics-dashboard-filters.component';
import DotAnalyticsDashboardPageviewReportComponent from './components/dot-analytics-dashboard-pageview-report/dot-analytics-dashboard-pageview-report.component';

const HIDE_ANALYTICS_MESSAGE_BANNER_KEY = 'analytics-dashboard-hide-message-banner';

@Component({
    selector: 'lib-dot-analytics-dashboard',
    imports: [
        CommonModule,
        ButtonModule,
        MessageModule,
        TabsModule,
        DotAnalyticsDashboardFiltersComponent,
        DotAnalyticsDashboardPageviewReportComponent,
        DotAnalyticsDashboardConversionsReportComponent,
        DotAnalyticsDashboardEngagementReportComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-dashboard.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsDashboardComponent {
    store = inject(DotAnalyticsDashboardStore);
    readonly #activatedRoute = inject(ActivatedRoute);

    readonly #localStorageService = inject(DotLocalstorageService);

    // Message banner visibility
    readonly $showMessage = signal<boolean>(
        !this.#localStorageService.getItem(HIDE_ANALYTICS_MESSAGE_BANNER_KEY)
    );

    // Engagement dashboard enabled
    // TODO: Remove this signal when the feature flag is removed
    readonly $engagementEnabled = toSignal(
        this.#activatedRoute.data.pipe(
            map((data: Record<string, unknown>) => data['engagementEnabled'] as boolean)
        )
    );

    // Tab configuration from constants
    readonly $tabs = computed(() => {
        const enabled = this.$engagementEnabled();
        return DASHBOARD_TAB_LIST.filter((tab) => tab.id !== DASHBOARD_TABS.engagement || enabled);
    });

    constructor() {
        // TODO: Remove this effect when the feature flag is removed
        effect(() => {
            const enabled = this.$engagementEnabled();
            const params = this.#activatedRoute.snapshot.queryParamMap;

            if (enabled && !params.has('tab')) {
                this.store.setCurrentTabAndNavigate(DASHBOARD_TABS.engagement);
            }
        });
    }

    /**
     * Closes the message banner and stores the preference in localStorage
     */
    onCloseMessage(): void {
        this.$showMessage.set(false);
        this.#localStorageService.setItem(HIDE_ANALYTICS_MESSAGE_BANNER_KEY, true);
    }

    /**
     * Handles tab change event from p-tabs.
     * Updates the store and URL query param.
     */
    onTabChange(tabId: string | number | undefined): void {
        if (typeof tabId === 'string' && isValidTab(tabId)) {
            this.store.setCurrentTabAndNavigate(tabId as DashboardTab);
        }
    }

    /**
     * Refresh dashboard data
     */
    onRefresh(): void {
        this.store.refreshAllData();
    }

    /**
     * Updates time range when filters change
     */
    onTimeRangeChange(timeRange: TimeRangeInput): void {
        this.store.updateTimeRange(timeRange);
    }
}
