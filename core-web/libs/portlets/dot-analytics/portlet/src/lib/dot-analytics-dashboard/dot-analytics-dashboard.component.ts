import { injectDispatch } from '@ngrx/signals/events';

import { NgComponentOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal, Type } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TabsModule } from 'primeng/tabs';

import { DotLocalstorageService } from '@dotcms/data-access';
import {
    DASHBOARD_TAB_LIST,
    DASHBOARD_TABS,
    DashboardTab,
    DotAnalyticsDashboardStore,
    filtersEvents,
    isValidTab,
    TimeRangeInput,
    uiEvents
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
        NgComponentOutlet
    ],
    providers: [DotAnalyticsDashboardStore],
    templateUrl: './dot-analytics-dashboard.component.html',
    styleUrl: './dot-analytics-dashboard.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
/**
 * Root analytics dashboard component. Manages tab navigation, time range filters,
 * and the Engagement, Pageview, and Conversions tabs.
 *
 * The component reads state via signals on the store and writes back through
 * dispatched events — never by calling store methods.
 */
export default class DotAnalyticsDashboardComponent {
    /** Analytics dashboard store providing data and computed signals */
    protected readonly store = inject(DotAnalyticsDashboardStore);
    readonly #localStorageService = inject(DotLocalstorageService);
    readonly #filtersDispatch = injectDispatch(filtersEvents);
    readonly #uiDispatch = injectDispatch(uiEvents);

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
     * Closes the message banner. The store's navigation handler persists the
     * preference to localStorage in response to the dispatched event.
     */
    onCloseMessage(): void {
        this.$showMessage.set(false);
        this.#uiDispatch.messageBannerDismissed();
    }

    /**
     * Handles tab change event from p-tabs. Dispatches a `tabSelected` intent;
     * the reducer updates state and the navigation handler syncs the URL.
     */
    onTabChange(tabId: string | number | undefined): void {
        if (tabId !== undefined && isValidTab(String(tabId))) {
            this.#filtersDispatch.tabSelected({ tab: tabId as DashboardTab });
        }
    }

    /** Refresh dashboard data — autoload handler fans out per-metric *Requested events. */
    onRefresh(): void {
        this.#filtersDispatch.refreshRequested();
    }

    /** Updates time range when filters change. */
    onTimeRangeChange(timeRange: TimeRangeInput): void {
        this.#filtersDispatch.timeRangeSelected({ timeRange });
    }
}
