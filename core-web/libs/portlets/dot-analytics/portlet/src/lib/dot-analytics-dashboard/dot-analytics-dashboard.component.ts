import { filter, startWith } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    ActivatedRoute,
    NavigationEnd,
    Router,
    RouterLink,
    RouterLinkActive,
    RouterOutlet
} from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { MessageModule } from 'primeng/message';
import { TabsModule } from 'primeng/tabs';
import type { TabListPassThrough } from 'primeng/types/tabs';

import { DotLocalstorageService } from '@dotcms/data-access';
import {
    DASHBOARD_TAB_LIST,
    DotAnalyticsDashboardStore,
    isValidTab,
    TimeRangeInput
} from '@dotcms/portlets/dot-analytics/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAnalyticsFiltersComponent } from './shared/components/dot-analytics-filters/dot-analytics-filters.component';

const HIDE_ANALYTICS_MESSAGE_BANNER_KEY = 'analytics-dashboard-hide-message-banner';

@Component({
    selector: 'dot-analytics-dashboard',
    imports: [
        RouterOutlet,
        RouterLink,
        RouterLinkActive,
        ButtonModule,
        MessageModule,
        TabsModule,
        DotAnalyticsFiltersComponent,
        DotMessagePipe,
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
    readonly #route = inject(ActivatedRoute);
    readonly #router = inject(Router);

    constructor() {
        this.#router.events
            .pipe(
                filter((event): event is NavigationEnd => event instanceof NavigationEnd),
                startWith(null),
                takeUntilDestroyed()
            )
            .subscribe(() => {
                const childPath = this.#route.firstChild?.snapshot?.url?.[0]?.path;

                if (childPath && isValidTab(childPath)) {
                    this.store.setCurrentTab(childPath);
                }
            });
    }

    /** Controls visibility of the top informational message banner */
    readonly $showMessage = signal<boolean>(
        !this.#localStorageService.getItem(HIDE_ANALYTICS_MESSAGE_BANNER_KEY)
    );

    readonly tabs = DASHBOARD_TAB_LIST;

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
