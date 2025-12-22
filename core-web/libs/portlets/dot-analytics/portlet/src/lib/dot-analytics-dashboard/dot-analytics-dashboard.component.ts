import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TabsModule } from 'primeng/tabs';

import { DotLocalstorageService } from '@dotcms/data-access';
import {
    DASHBOARD_TAB_LIST,
    DotAnalyticsDashboardStore,
    TimeRangeInput
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import DotAnalyticsDashboardConversionsReportComponent from './components/dot-analytics-dashboard-conversions-report/dot-analytics-dashboard-conversions-report.component';
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
        DotMessagePipe
    ],
    templateUrl: './dot-analytics-dashboard.component.html',
    styleUrl: './dot-analytics-dashboard.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export default class DotAnalyticsDashboardComponent {
    store = inject(DotAnalyticsDashboardStore);

    readonly #localStorageService = inject(DotLocalstorageService);

    // Message banner visibility
    readonly $showMessage = signal<boolean>(
        !this.#localStorageService.getItem(HIDE_ANALYTICS_MESSAGE_BANNER_KEY)
    );

    // Tab configuration from constants
    readonly tabs = DASHBOARD_TAB_LIST;

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
    onTabChange(tabId: unknown): void {
        const normalizedId =
            typeof tabId === 'number' ? tabId.toString() : typeof tabId === 'string' ? tabId : null;
        const tab = this.tabs.find(({ id }) => id === normalizedId);

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
