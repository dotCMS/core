import { NgComponentOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal, Type } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { MessageModule } from 'primeng/message';
import { TabsModule } from 'primeng/tabs';
import type { TabListPassThrough } from 'primeng/types/tabs';

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

import DotAnalyticsConversionsReportComponent from './reports/conversions/dot-analytics-conversions-report/dot-analytics-conversions-report.component';
import DotAnalyticsEngagementReportComponent from './reports/engagement/dot-analytics-engagement-report/dot-analytics-engagement-report.component';
import DotAnalyticsPageviewReportComponent from './reports/pageview/dot-analytics-pageview-report/dot-analytics-pageview-report.component';
import { DotAnalyticsFiltersComponent } from './shared/components/dot-analytics-filters/dot-analytics-filters.component';

const HIDE_ANALYTICS_MESSAGE_BANNER_KEY = 'analytics-dashboard-hide-message-banner';

@Component({
    selector: 'dot-analytics-dashboard',
    imports: [
        ButtonModule,
        MessageModule,
        TabsModule,
        DotAnalyticsFiltersComponent,
        DotMessagePipe,
        NgComponentOutlet,
        DividerModule
    ],
    providers: [DotAnalyticsDashboardStore],
    templateUrl: './dot-analytics-dashboard.component.html',
    styleUrl: './dot-analytics-dashboard.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
/**
 * Root analytics dashboard component. Manages tab navigation, time range filters,
 * and the Engagement, Pageview, and Conversions tabs.
 */
export default class DotAnalyticsDashboardComponent {
    /** Analytics dashboard store providing data and actions */
    protected readonly store = inject(DotAnalyticsDashboardStore);
    readonly #localStorageService = inject(DotLocalstorageService);

    /** Controls visibility of the top informational message banner */
    readonly $showMessage = signal<boolean>(
        !this.#localStorageService.getItem(HIDE_ANALYTICS_MESSAGE_BANNER_KEY)
    );

    readonly tabs = DASHBOARD_TAB_LIST;

    readonly tabComponents: Record<DashboardTab, Type<unknown>> = {
        [DASHBOARD_TABS.pageview]: DotAnalyticsPageviewReportComponent,
        [DASHBOARD_TABS.conversions]: DotAnalyticsConversionsReportComponent,
        [DASHBOARD_TABS.engagement]: DotAnalyticsEngagementReportComponent
    };

    /**
     * Aligns the active ink bar with the toolbar bottom edge (single baseline with outer border-b)
     * and avoids duplicate / top-edge active styling from the theme.
     */
    readonly tabListPt: TabListPassThrough = {
        root: { class: '!border-0 !shadow-none !bg-transparent' },
        content: { class: '!border-0' },
        tabList: { class: '!border-0 !border-b-0 items-end' },
        activeBar: {
            class: '!h-0 !min-h-0 !max-h-0 !bg-transparent !opacity-0 !pointer-events-none'
        }
    };

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
        if (tabId !== undefined && isValidTab(String(tabId))) {
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
