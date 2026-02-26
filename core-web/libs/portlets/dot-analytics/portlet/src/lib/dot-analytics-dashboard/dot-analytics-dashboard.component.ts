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
import { MessagesModule } from 'primeng/messages';
import { TabViewModule } from 'primeng/tabview';

import { map } from 'rxjs/operators';

import { DotLocalstorageService } from '@dotcms/data-access';
import {
    DASHBOARD_TAB_LIST,
    DASHBOARD_TABS,
    DotAnalyticsDashboardStore,
    TimeRangeInput
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import DotAnalyticsConversionsReportComponent from './reports/conversions/dot-analytics-conversions-report/dot-analytics-conversions-report.component';
import DotAnalyticsEngagementReportComponent from './reports/engagement/dot-analytics-engagement-report/dot-analytics-engagement-report.component';
import DotAnalyticsPageviewReportComponent from './reports/pageview/dot-analytics-pageview-report/dot-analytics-pageview-report.component';
import { DotAnalyticsFiltersComponent } from './shared/components/dot-analytics-filters/dot-analytics-filters.component';

const HIDE_ANALYTICS_MESSAGE_BANNER_KEY = 'analytics-dashboard-hide-message-banner';

@Component({
    selector: 'lib-dot-analytics-dashboard',
    imports: [
        CommonModule,
        ButtonModule,
        MessagesModule,
        TabViewModule,
        DotAnalyticsFiltersComponent,
        DotAnalyticsPageviewReportComponent,
        DotAnalyticsConversionsReportComponent,
        DotAnalyticsEngagementReportComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-dashboard.component.html',
    styleUrl: './dot-analytics-dashboard.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
/**
 * Root analytics dashboard component. Manages tab navigation, time range filters,
 * and feature-flag-gated visibility of the Engagement tab.
 */
export default class DotAnalyticsDashboardComponent {
    /** Analytics dashboard store providing data and actions */
    readonly store = inject(DotAnalyticsDashboardStore);
    readonly #activatedRoute = inject(ActivatedRoute);
    readonly #localStorageService = inject(DotLocalstorageService);

    /** Controls visibility of the top informational message banner */
    readonly $showMessage = signal<boolean>(
        !this.#localStorageService.getItem(HIDE_ANALYTICS_MESSAGE_BANNER_KEY)
    );

    /**
     * Whether the Engagement tab is enabled via feature flag.
     * TODO: Remove this signal when the feature flag is removed.
     */
    readonly $engagementEnabled = toSignal(
        this.#activatedRoute.data.pipe(
            map((data: Record<string, unknown>) => data['engagementEnabled'] === true)
        )
    );

    /** Visible tabs, filtered by feature flag (Engagement tab hidden when disabled) */
    readonly $tabs = computed(() => {
        const enabled = this.$engagementEnabled();
        return DASHBOARD_TAB_LIST.filter((tab) => tab.id !== DASHBOARD_TABS.engagement || enabled);
    });

    /** Index of the currently active tab within the visible tabs list */
    readonly $activeTabIndex = computed(() => {
        const currentTab = this.store.currentTab();

        return this.$tabs().findIndex((tab) => tab.id === currentTab);
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
     * Handles tab change event from p-tabView.
     * Updates the store and URL query param.
     */
    onTabChange(event: { index: number }): void {
        const tab = this.$tabs()[event.index];
        if (tab) {
            this.store.setCurrentTabAndNavigate(tab.id);
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
