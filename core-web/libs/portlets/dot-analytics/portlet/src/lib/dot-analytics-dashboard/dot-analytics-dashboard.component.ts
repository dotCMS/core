import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Params } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { MessagesModule } from 'primeng/messages';
import { TabViewModule } from 'primeng/tabview';

import { DotLocalstorageService } from '@dotcms/data-access';
import {
    DASHBOARD_TAB_LIST,
    DotAnalyticsDashboardStore
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
        MessagesModule,
        TabViewModule,
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

    // Tab index derived from store.currentTab()
    readonly $activeTabIndex = computed(() => {
        const currentTab = this.store.currentTab();

        return this.tabs.findIndex((tab) => tab.id === currentTab);
    });

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
        const tab = this.tabs[event.index];
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

    refreshQueryParams(queryParams: Params): void {
        this.store.refreshQueryParams(queryParams);
    }
}
